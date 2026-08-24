package dev.vantafyn.feature.home.pairing

import android.util.Log
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
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
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
    private const val TAG = "MobileTvPairingClient"
    private const val UDP_DISCOVERY_PORT = 8899
    private const val DEFAULT_HTTP_PORT = 8765
    private const val CONNECT_TIMEOUT_MS = 3_000
    private const val READ_TIMEOUT_MS = 4_000

    /**
     * Broadcasts discovery request on LAN and listens for TV beacons.
     */
    suspend fun discoverNearbyTvs(timeoutMs: Long = 1_500L): List<DiscoveredTv> =
        withContext(Dispatchers.IO) {
            val discovered = mutableMapOf<String, DiscoveredTv>()
            var socket: DatagramSocket? = null

            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 300

                val requestBytes = "VANTAFYN_TV_DISCOVER_REQ:1".toByteArray()

                // Broadcast to global 255.255.255.255
                val globalBroadcast = DatagramPacket(
                    requestBytes,
                    requestBytes.size,
                    InetAddress.getByName("255.255.255.255"),
                    UDP_DISCOVERY_PORT,
                )
                socket.send(globalBroadcast)

                // Also broadcast on each active network interface
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    if (interfaces != null) {
                        for (intf in interfaces) {
                            if (intf.isLoopback || !intf.isUp) continue
                            for (interfaceAddress in intf.interfaceAddresses) {
                                val broadcast = interfaceAddress.broadcast ?: continue
                                val packet = DatagramPacket(requestBytes, requestBytes.size, broadcast, UDP_DISCOVERY_PORT)
                                socket.send(packet)
                            }
                        }
                    }
                } catch (_: Exception) {}

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
                            Log.d(TAG, "Discovered TV via UDP: $ip -> ${beacon.tvName} on port ${beacon.httpPort}")
                            discovered[ip] = DiscoveredTv(
                                ipAddress = ip,
                                port = beacon.httpPort,
                                deviceName = beacon.tvName,
                            )
                        }
                    } catch (_: SocketTimeoutException) {
                        // Keep listening until deadline
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP discovery error: ${e.message}")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
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
                Log.d(TAG, "Attempting pairing with $targetIp:$port...")
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

                Log.d(TAG, "Pairing response from $targetIp:$port: HTTP $responseCode -> $responseBody")
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
                PairingClientResult.Failure("Connection timed out reaching TV at $targetIp.", "TIMEOUT")
            } catch (e: Exception) {
                PairingClientResult.Failure("Couldn't connect to TV at $targetIp: ${e.message}", "CONNECT_ERROR")
            } finally {
                connection?.disconnect()
            }
        }

    /**
     * High-level pairing attempt:
     * 1. Broadcast discovery on LAN.
     * 2. Try all discovered TVs with the payload.
     * 3. Run high-speed concurrent TCP scan across local /24 subnet on ports 8765-8768 to bypass router UDP blocking.
     */
    suspend fun pairWithCode(
        payload: TvPairingPayload,
    ): PairingClientResult =
        withContext(Dispatchers.IO) {
            // Step 1: Fast UDP Discovery
            val discovered = discoverNearbyTvs(timeoutMs = 1_200L)

            for (tv in discovered) {
                val result = sendPairingPayload(tv.ipAddress, tv.port, payload)
                if (result is PairingClientResult.Success) {
                    return@withContext result.copy(tvName = tv.deviceName)
                }
                // If the TV responded with an explicit code error, return immediately
                if (result is PairingClientResult.Failure &&
                    (result.errorCode == "INVALID_CODE" || result.errorCode == "RATE_LIMITED" || result.errorCode == "EXPIRED")
                ) {
                    return@withContext result
                }
            }

            // Step 2: Concurrent Subnet TCP Probe (bypasses Wi-Fi AP isolation or UDP drops)
            val localIps = getLocalSubnetIps()
            if (localIps.isNotEmpty()) {
                Log.d(TAG, "Probing subnet (${localIps.size} IPs) across ports 8765..8767...")
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
        val candidatePorts = listOf(DEFAULT_HTTP_PORT, 8766, 8767, 8775)

        // Find IPs where any pairing port is open (using ultra-fast 250ms TCP probe)
        val chunks = ips.chunked(48)
        for (chunk in chunks) {
            val openTargets = chunk.map { ip ->
                async(Dispatchers.IO) {
                    for (port in candidatePorts) {
                        try {
                            val socket = Socket()
                            socket.connect(InetSocketAddress(ip, port), 250)
                            socket.close()
                            return@async Pair(ip, port)
                        } catch (_: Exception) {}
                    }
                    null
                }
            }.awaitAll().filterNotNull()

            for ((ip, port) in openTargets) {
                val result = sendPairingPayload(ip, port, payload)
                if (result is PairingClientResult.Success) {
                    return@coroutineScope result
                }
                if (result is PairingClientResult.Failure &&
                    (result.errorCode == "INVALID_CODE" || result.errorCode == "RATE_LIMITED" || result.errorCode == "EXPIRED")
                ) {
                    return@coroutineScope result
                }
            }
        }
        null
    }

    private fun getLocalSubnetIps(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ips
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name.lowercase()
                if (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") || name.startsWith("dummy")) continue

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
