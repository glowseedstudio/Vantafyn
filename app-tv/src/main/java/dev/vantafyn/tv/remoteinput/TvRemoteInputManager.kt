package dev.vantafyn.tv.remoteinput

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import dev.vantafyn.core.jellyfin.TvCryptoUtils
import dev.vantafyn.core.jellyfin.TvDiscoveryBeacon
import dev.vantafyn.core.jellyfin.TvRemoteInputPayload
import dev.vantafyn.core.jellyfin.TvRemoteInputResponse
import dev.vantafyn.core.jellyfin.TvRemoteInputTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TV Remote Input Manager with End-to-End Encryption.
 *
 * Runs a lightweight ServerSocket HTTP listener and UDP discovery beacon
 * to safely receive text from paired/connected Vantafyn mobile devices on LAN.
 * Uses ECDH (NIST P-256) key agreement and AES-256-GCM authenticated encryption.
 */
object TvRemoteInputManager {
    private const val TAG = "TvRemoteInputManager"
    private const val DEFAULT_HTTP_PORT = 8767
    private const val UDP_DISCOVERY_PORT = 8899

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var udpListenJob: Job? = null
    private var broadcastJob: Job? = null

    // Ephemeral EC KeyPair for end-to-end encryption session
    private var serverKeyPair: KeyPair = TvCryptoUtils.generateEcKeyPair()

    var assignedPort: Int = DEFAULT_HTTP_PORT
        private set

    @Volatile
    private var currentTarget: TvRemoteInputTarget? = null

    val deviceName: String by lazy {
        val model = Build.MODEL ?: "Android TV"
        if (model.contains("TV", ignoreCase = true) || model.contains("Shield", ignoreCase = true)) {
            model
        } else {
            "$model TV"
        }
    }

    /**
     * Registers an active text field as the remote input target.
     */
    fun registerTarget(target: TvRemoteInputTarget) {
        currentTarget = target
    }

    /**
     * Unregisters a target when it loses focus.
     */
    fun unregisterTarget(fieldId: String) {
        if (currentTarget?.fieldId == fieldId) {
            currentTarget = null
        }
    }

    /**
     * Returns whether any text field is currently focused.
     */
    fun hasActiveTarget(): Boolean = currentTarget != null

    /**
     * Starts the TV remote input listener.
     */
    fun start() {
        if (isRunning.getAndSet(true)) return

        // Rotate ephemeral keypair on start
        serverKeyPair = TvCryptoUtils.generateEcKeyPair()

        var startedSocket: ServerSocket? = null
        var boundPort = DEFAULT_HTTP_PORT

        for (port in listOf(8767, 8768, 8769, 8770, 8765)) {
            try {
                startedSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                boundPort = port
                break
            } catch (_: Exception) {
                // Try next port
            }
        }

        if (startedSocket == null) {
            isRunning.set(false)
            return
        }

        serverSocket = startedSocket
        assignedPort = boundPort

        serverJob = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    val client = startedSocket.accept()
                    launch { handleClientSocket(client) }
                } catch (_: Exception) {
                    break
                }
            }
        }

        startUdpDiscovery()
    }

    /**
     * Stops the listener.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) return

        serverJob?.cancel()
        serverJob = null

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        udpListenJob?.cancel()
        udpListenJob = null
        broadcastJob?.cancel()
        broadcastJob = null

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
    }

    private fun handleClientSocket(socket: Socket) {
        try {
            socket.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }

            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }

            val body = if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(buffer, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(buffer, 0, totalRead)
            } else {
                ""
            }

            if (method == "OPTIONS") {
                sendHttpResponse(socket, 200, "{\"status\":\"ok\"}")
                return
            }

            when {
                method == "POST" && path.startsWith("/api/v1/remote-input") -> {
                    val payload = TvRemoteInputPayload.fromJsonString(body)
                    if (payload == null) {
                        val response = TvRemoteInputResponse(success = false, message = "Invalid request format")
                        sendHttpResponse(socket, 400, response.toJsonString())
                        return
                    }

                    val target = currentTarget
                    if (target == null) {
                        val response = TvRemoteInputResponse(
                            success = false,
                            message = "No text field is currently focused on your TV",
                            serverPublicKey = TvCryptoUtils.encodePublicKey(serverKeyPair.public),
                        )
                        sendHttpResponse(socket, 200, response.toJsonString())
                        return
                    }

                    // Resolve decrypted text
                    val clientPubStr = payload.clientPublicKey
                    val ivStr = payload.iv
                    val cipherStr = payload.ciphertext

                    val resolvedText = if (payload.encrypted &&
                        !cipherStr.isNullOrBlank() &&
                        !ivStr.isNullOrBlank() &&
                        !clientPubStr.isNullOrBlank()
                    ) {
                        try {
                            val clientPub = TvCryptoUtils.decodePublicKey(clientPubStr)
                            val sharedAesKey = TvCryptoUtils.deriveSharedAesKey(serverKeyPair.private, clientPub)
                            TvCryptoUtils.decryptAesGcm(ivStr, cipherStr, sharedAesKey)
                        } catch (e: Exception) {
                            Log.e(TAG, "Decryption failed: ${e.message}")
                            val response = TvRemoteInputResponse(
                                success = false,
                                message = "Decryption failed. Please try again.",
                                serverPublicKey = TvCryptoUtils.encodePublicKey(serverKeyPair.public),
                            )
                            sendHttpResponse(socket, 400, response.toJsonString())
                            return
                        }
                    } else {
                        // Plain text fallback
                        payload.text
                    }

                    // Dispatch to focused text field on Main Looper
                    mainHandler.post {
                        target.onTextReceived(resolvedText)
                    }

                    val response = TvRemoteInputResponse(
                        success = true,
                        fieldName = target.fieldName,
                        isSensitive = target.isSensitive,
                        serverPublicKey = TvCryptoUtils.encodePublicKey(serverKeyPair.public),
                    )
                    sendHttpResponse(socket, 200, response.toJsonString())
                }

                method == "GET" && path.startsWith("/api/v1/status") -> {
                    val target = currentTarget
                    val response = TvRemoteInputResponse(
                        success = true,
                        fieldName = target?.fieldName,
                        isSensitive = target?.isSensitive == true,
                        serverPublicKey = TvCryptoUtils.encodePublicKey(serverKeyPair.public),
                    )
                    sendHttpResponse(socket, 200, response.toJsonString())
                }

                else -> {
                    sendHttpResponse(socket, 404, "{\"error\":\"Not Found\"}")
                }
            }
        } catch (_: Exception) {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendHttpResponse(socket: Socket, statusCode: Int, json: String) {
        try {
            val bodyBytes = json.toByteArray(Charsets.UTF_8)
            val statusText = when (statusCode) {
                200 -> "OK"
                400 -> "Bad Request"
                404 -> "Not Found"
                else -> "Error"
            }
            val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n\r\n"

            val out = socket.getOutputStream()
            out.write(headers.toByteArray(Charsets.UTF_8))
            out.write(bodyBytes)
            out.flush()
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun startUdpDiscovery() {
        // UDP Discovery Listener
        udpListenJob = scope.launch {
            try {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_DISCOVERY_PORT))
                }
                udpSocket = socket
                val buffer = ByteArray(1024)
                while (isActive && isRunning.get()) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    if (message.startsWith("VANTAFYN_TV_DISCOVER_REQ")) {
                        val beacon = TvDiscoveryBeacon(tvName = deviceName, httpPort = assignedPort)
                        val responseData = beacon.toPacketString().toByteArray()
                        val responsePacket = DatagramPacket(
                            responseData,
                            responseData.size,
                            packet.address,
                            packet.port,
                        )
                        socket.send(responsePacket)
                    }
                }
            } catch (_: Exception) {}
        }

        // UDP Periodic Broadcast (every 3 seconds)
        broadcastJob = scope.launch {
            val beacon = TvDiscoveryBeacon(tvName = deviceName, httpPort = assignedPort)
            val packetBytes = beacon.toPacketString().toByteArray()
            while (isActive && isRunning.get()) {
                try {
                    val broadcastSocket = DatagramSocket()
                    broadcastSocket.broadcast = true
                    val packet = DatagramPacket(
                        packetBytes,
                        packetBytes.size,
                        InetAddress.getByName("255.255.255.255"),
                        UDP_DISCOVERY_PORT,
                    )
                    broadcastSocket.send(packet)
                    broadcastSocket.close()
                } catch (_: Exception) {}
                delay(3_000L)
            }
        }
    }
}
