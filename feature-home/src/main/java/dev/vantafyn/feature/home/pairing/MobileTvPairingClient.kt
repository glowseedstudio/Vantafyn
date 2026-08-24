package dev.vantafyn.feature.home.pairing

import dev.vantafyn.core.jellyfin.TvDiscoveryBeacon
import dev.vantafyn.core.jellyfin.TvPairingPayload
import dev.vantafyn.core.jellyfin.TvPairingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URL

data class DiscoveredTv(
    val ipAddress: String,
    val port: Int,
    val deviceName: String,
)

sealed interface PairingClientResult {
    data class Success(val tvName: String) : PairingClientResult
    data class Failure(val message: String, val errorCode: String? = null) : PairingClientResult
}

object MobileTvPairingClient {
    private const val UDP_DISCOVERY_PORT = 8766
    private const val DEFAULT_HTTP_PORT = 8765
    private const val CONNECT_TIMEOUT_MS = 3_500
    private const val READ_TIMEOUT_MS = 4_500

    /**
     * Broadcasts discovery request on LAN and listens for TV beacons.
     */
    suspend fun discoverNearbyTvs(timeoutMs: Long = 2_500L): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            val discovered = mutableMapOf<String, DiscoveredTv>()
            var socket: DatagramSocket? = null

            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 400

                val requestBytes = "VANTAFYN_TV_DISCOVER_REQ:1".toByteArray()
                val broadcastPacket = DatagramPacket(
                    requestBytes,
                    requestBytes.size,
                    InetAddress.getByName("255.255.255.255"),
                    UDP_DISCOVERY_PORT,
                )

                // Send 2 quick probe packets
                socket.send(broadcastPacket)
                delay(50L)
                socket.send(broadcastPacket)

                val deadline = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(1024)

                while (System.currentTimeMillis() < deadline) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val message = String(packet.data, 0, packet.length).trim()
                        val beacon = TvDiscoveryBeacon.fromPacketString(message)
                        if (beacon != null) {
                            val ip = packet.address.hostAddress ?: continue
                            discovered[ip] = DiscoveredTv(
                                ipAddress = ip,
                                port = beacon.httpPort,
                                deviceName = beacon.tvName,
                            )
                        }
                    } catch (_: SocketTimeoutException) {
                        // Loop until deadline
                    }
                }
            } catch (_: Exception) {
                // Fallback will handle
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }

            discovered.values.toList()
        }

    /**
     * Sends the pairing payload to a target TV endpoint.
     */
    suspend fun sendPairingPayload(
        targetIp: String,
        port: Int,
        payload: TvPairingPayload,
    ): PairingClientResult =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("http://$targetIp:$port/api/v1/pair")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")

                val jsonBytes = payload.toJson().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { os: OutputStream ->
                    os.write(jsonBytes)
                    os.flush()
                }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                val parsedResponse = TvPairingResponse.fromJson(responseBody)

                if (responseCode == 200 && parsedResponse?.status == "ok") {
                    PairingClientResult.Success(tvName = "Android TV")
                } else {
                    val errorCode = parsedResponse?.errorCode ?: "PAIRING_FAILED"
                    val msg = when (errorCode) {
                        "EXPIRED" -> "The pairing code on your TV has expired. Please refresh the code."
                        "RATE_LIMITED" -> "Too many failed attempts. Please generate a new code on your TV."
                        "INVALID_CODE" -> "The TV rejected the code. Please check the code and try again."
                        else -> parsedResponse?.message ?: "TV rejected pairing request ($responseCode)"
                    }
                    PairingClientResult.Failure(msg, errorCode)
                }
            } catch (e: SocketTimeoutException) {
                PairingClientResult.Failure("Connection timed out. Ensure your TV and phone are on the same Wi-Fi.")
            } catch (e: Exception) {
                PairingClientResult.Failure("Couldn't reach your TV. Ensure both devices are on the same local network.")
            } finally {
                connection?.disconnect()
            }
        }

    /**
     * High-level pairing attempt:
     * 1. Broadcast discovery on LAN.
     * 2. Try all discovered TVs with the payload.
     * 3. If discovery yielded 0 TVs, scan local /24 subnet on port 8765 concurrently.
     */
    suspend fun pairWithCode(
        payload: TvPairingPayload,
    ): PairingClientResult =
        withContext(Dispatchers.IO) {
            // Step 1: Fast UDP Discovery
            val discovered = discoverNearbyTvs(timeoutMs = 1_800L)

            for (tv in discovered) {
                val result = sendPairingPayload(tv.ipAddress, tv.port, payload)
                if (result is PairingClientResult.Success) {
                    return@withContext result.copy(tvName = tv.deviceName)
                }
                // If it failed due to invalid code or rate limit, fail fast rather than trying other targets
                if (result is PairingClientResult.Failure &&
                    (result.errorCode == "INVALID_CODE" || result.errorCode == "RATE_LIMITED" || result.errorCode == "EXPIRED")
                ) {
                    return@withContext result
                }
            }

            // Step 2: Subnet Probe Fallback (if router drops UDP broadcast between Wi-Fi clients)
            val localIps = getLocalSubnetIps()
            if (localIps.isNotEmpty()) {
                val found = probeSubnetAndPair(localIps, payload)
                if (found != null) {
                    return@withContext found
                }
            }

            PairingClientResult.Failure("No Android TV in pairing mode found on your local network. Make sure the pairing screen is open on your TV.")
        }

    private suspend fun probeSubnetAndPair(
        ips: List<String>,
        payload: TvPairingPayload,
    ): PairingClientResult? = coroutineScope {
        // Probe in chunks to avoid overloading sockets
        val chunks = ips.chunked(32)
        for (chunk in chunks) {
            val jobs = chunk.map { ip ->
                async(Dispatchers.IO) {
                    try {
                        val result = sendPairingPayload(ip, DEFAULT_HTTP_PORT, payload)
                        if (result is PairingClientResult.Success) result else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val results = jobs.awaitAll()
            val successful = results.filterNotNull().firstOrNull()
            if (successful != null) return@coroutineScope successful
        }
        null
    }

    private fun getLocalSubnetIps(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ips
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val prefix = host.substringBeforeLast(".")
                            val currentOctet = host.substringAfterLast(".").toIntOrNull() ?: -1
                            for (i in 1..254) {
                                if (i != currentOctet) {
                                    ips.add("$prefix.$i")
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return ips
    }
}
