package dev.vantafyn.feature.home.remoteinput

import android.util.Log
import dev.vantafyn.core.jellyfin.TvCryptoUtils
import dev.vantafyn.core.jellyfin.TvDiscoveryBeacon
import dev.vantafyn.core.jellyfin.TvRemoteInputPayload
import dev.vantafyn.core.jellyfin.TvRemoteInputResponse
import dev.vantafyn.feature.home.pairing.DiscoveredTv
import kotlinx.coroutines.Dispatchers
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

object MobileTvInputClient {
    private const val TAG = "MobileTvInputClient"
    private const val UDP_DISCOVERY_PORT = 8899
    private const val DEFAULT_HTTP_PORT = 8767
    private const val CONNECT_TIMEOUT_MS = 2_500
    private const val READ_TIMEOUT_MS = 3_500

    /**
     * Broadcasts discovery request on LAN to find active TVs.
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
                val broadcastPacket = DatagramPacket(
                    requestBytes,
                    requestBytes.size,
                    InetAddress.getByName("255.255.255.255"),
                    UDP_DISCOVERY_PORT,
                )

                socket.send(broadcastPacket)

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
                            discovered[ip] = DiscoveredTv(
                                ipAddress = ip,
                                port = beacon.httpPort,
                                deviceName = beacon.tvName,
                            )
                        }
                    } catch (_: SocketTimeoutException) {
                        // Keep polling until deadline
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }

            discovered.values.toList()
        }

    /**
     * Checks status of active text field and retrieves ephemeral server public key.
     */
    suspend fun getTvFieldStatus(targetIp: String, port: Int = DEFAULT_HTTP_PORT): TvRemoteInputResponse =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://$targetIp:$port/api/v1/status")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    TvRemoteInputResponse.fromJsonString(responseText)
                        ?: TvRemoteInputResponse(success = false, message = "Invalid response from TV")
                } else {
                    TvRemoteInputResponse(success = false, message = "HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                TvRemoteInputResponse(success = false, message = e.message ?: "Connection failed")
            }
        }

    /**
     * Encrypts and sends text to the active text field on the target TV using ECDH + AES-256-GCM.
     */
    suspend fun sendTextToTv(
        targetIp: String,
        port: Int = DEFAULT_HTTP_PORT,
        text: String,
        fieldType: String? = null,
        serverPublicKey: String? = null,
    ): TvRemoteInputResponse =
        withContext(Dispatchers.IO) {
            try {
                // Fetch server public key if not already cached
                val resolvedServerPubKey = serverPublicKey ?: getTvFieldStatus(targetIp, port).serverPublicKey

                val payload = if (!resolvedServerPubKey.isNullOrBlank()) {
                    try {
                        val clientKeyPair = TvCryptoUtils.generateEcKeyPair()
                        val serverPub = TvCryptoUtils.decodePublicKey(resolvedServerPubKey)
                        val sharedAesKey = TvCryptoUtils.deriveSharedAesKey(clientKeyPair.private, serverPub)
                        val (iv, ciphertext) = TvCryptoUtils.encryptAesGcm(text, sharedAesKey)

                        TvRemoteInputPayload(
                            encrypted = true,
                            clientPublicKey = TvCryptoUtils.encodePublicKey(clientKeyPair.public),
                            iv = iv,
                            ciphertext = ciphertext,
                            fieldType = fieldType,
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Encryption failed, falling back to plain payload: ${e.message}")
                        TvRemoteInputPayload(text = text, fieldType = fieldType)
                    }
                } else {
                    TvRemoteInputPayload(text = text, fieldType = fieldType)
                }

                val jsonBytes = payload.toJsonString().toByteArray(Charsets.UTF_8)
                val url = URL("http://$targetIp:$port/api/v1/remote-input")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }

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

                TvRemoteInputResponse.fromJsonString(responseBody)
                    ?: TvRemoteInputResponse(success = false, message = "HTTP $responseCode")
            } catch (e: Exception) {
                TvRemoteInputResponse(success = false, message = e.message ?: "Connection error")
            }
        }
}
