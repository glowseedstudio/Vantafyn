package dev.vantafyn.tv.pairing

import android.os.Build
import android.util.Log
import dev.vantafyn.core.jellyfin.TvDiscoveryBeacon
import dev.vantafyn.core.jellyfin.TvPairingPayload
import dev.vantafyn.core.jellyfin.TvPairingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Ephemeral local-network ServerSocket HTTP server and UDP discovery responder for Android TV pairing.
 *
 * Runs ONLY while the TV Pairing screen is actively displayed.
 * Shuts down immediately upon successful pairing, user exit, or expiration.
 */
class TvPairingServer(
    val deviceName: String = defaultTvDeviceName(),
    private val onPairedPayload: (TvPairingPayload) -> Unit,
    private val onExpiredOrLimitReached: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var broadcastJob: Job? = null
    private var udpListenJob: Job? = null

    private val isRunning = AtomicBoolean(false)
    private val failedAttempts = AtomicInteger(0)

    var currentCode: String = ""
        private set

    var expiresAtEpochMs: Long = 0L
        private set

    var assignedPort: Int = DEFAULT_HTTP_PORT
        private set

    companion object {
        private const val TAG = "TvPairingServer"
        const val DEFAULT_HTTP_PORT = 8765
        const val UDP_DISCOVERY_PORT = 8899
        const val EXPIRY_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        const val MAX_FAILED_ATTEMPTS = 6

        // Safe unambiguous character set for 10-foot readability
        private const val CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        private val random = SecureRandom()

        fun generatePairingCode(): String {
            val sb = StringBuilder(6)
            for (i in 0 until 6) {
                sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)])
            }
            return sb.toString()
        }

        private fun defaultTvDeviceName(): String {
            val model = Build.MODEL ?: "Android TV"
            return if (model.contains("TV", ignoreCase = true) || model.contains("Shield", ignoreCase = true)) {
                model
            } else {
                "$model TV"
            }
        }
    }

    /**
     * Starts the pairing server and UDP discovery beacon.
     */
    fun start(): Boolean {
        if (isRunning.getAndSet(true)) return true

        currentCode = generatePairingCode()
        expiresAtEpochMs = System.currentTimeMillis() + EXPIRY_DURATION_MS
        failedAttempts.set(0)

        Log.d(TAG, "Starting TV Pairing Server with code: $currentCode")

        // 1. Start ServerSocket
        var startedSocket: ServerSocket? = null
        var boundPort = DEFAULT_HTTP_PORT

        for (port in DEFAULT_HTTP_PORT..(DEFAULT_HTTP_PORT + 10)) {
            try {
                startedSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                boundPort = port
                Log.d(TAG, "TvPairingServer HTTP bound to port $port")
                break
            } catch (e: Exception) {
                Log.w(TAG, "Port $port unavailable: ${e.message}")
            }
        }

        if (startedSocket == null) {
            Log.e(TAG, "Failed to bind TvPairingServer to any port in range")
            isRunning.set(false)
            return false
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

        // 2. Start UDP Discovery Responder & Periodic Broadcast
        startUdpServices()

        // 3. Start Expiration Timer
        scope.launch {
            delay(EXPIRY_DURATION_MS)
            if (isRunning.get()) {
                Log.d(TAG, "Pairing code expired")
                withContext(Dispatchers.Main) {
                    onExpiredOrLimitReached()
                }
                stop()
            }
        }

        return true
    }

    fun refreshCode(): String {
        currentCode = generatePairingCode()
        expiresAtEpochMs = System.currentTimeMillis() + EXPIRY_DURATION_MS
        failedAttempts.set(0)
        Log.d(TAG, "Pairing code refreshed: $currentCode")
        return currentCode
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

            Log.d(TAG, "HTTP $method $path from ${socket.inetAddress.hostAddress}, bodyLen: $contentLength")

            if (method == "OPTIONS") {
                sendHttpResponse(socket, 200, TvPairingResponse.success())
                return
            }

            if (method != "POST" || !path.startsWith("/api/v1/pair")) {
                sendHttpResponse(socket, 405, TvPairingResponse.error("METHOD_NOT_ALLOWED", "Method not allowed"))
                return
            }

            // Check expiry
            if (System.currentTimeMillis() > expiresAtEpochMs) {
                sendHttpResponse(socket, 401, TvPairingResponse.error("EXPIRED", "Pairing code has expired"))
                scope.launch(Dispatchers.Main) { onExpiredOrLimitReached() }
                return
            }

            // Check rate limiting
            if (failedAttempts.get() >= MAX_FAILED_ATTEMPTS) {
                sendHttpResponse(socket, 429, TvPairingResponse.error("RATE_LIMITED", "Too many failed attempts"))
                scope.launch(Dispatchers.Main) { onExpiredOrLimitReached() }
                return
            }

            val payload = TvPairingPayload.fromJson(body)
            if (payload == null) {
                sendHttpResponse(socket, 400, TvPairingResponse.error("INVALID_PAYLOAD", "Malformed pairing payload"))
                return
            }

            // Validate code (case-insensitive and trimmed)
            val normalizedProvided = payload.code.trim().replace("-", "").replace(" ", "").uppercase()
            val normalizedCurrent = currentCode.trim().replace("-", "").replace(" ", "").uppercase()

            Log.d(TAG, "Checking code: provided='$normalizedProvided' vs current='$normalizedCurrent'")

            if (normalizedProvided != normalizedCurrent) {
                val currentFails = failedAttempts.incrementAndGet()
                if (currentFails >= MAX_FAILED_ATTEMPTS) {
                    sendHttpResponse(socket, 429, TvPairingResponse.error("RATE_LIMITED", "Too many failed attempts"))
                    scope.launch(Dispatchers.Main) { onExpiredOrLimitReached() }
                } else {
                    sendHttpResponse(socket, 401, TvPairingResponse.error("INVALID_CODE", "Invalid pairing code"))
                }
                return
            }

            // Valid code! Send success response
            Log.d(TAG, "Pairing successful for user ${payload.userName} on server ${payload.serverName}")
            sendHttpResponse(socket, 200, TvPairingResponse.success())

            // Notify callback on Main dispatcher
            scope.launch(Dispatchers.Main) {
                stop()
                onPairedPayload(payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client socket: ${e.message}", e)
            try {
                sendHttpResponse(socket, 500, TvPairingResponse.error("INTERNAL_ERROR", "Failed to process pairing"))
            } catch (_: Exception) {}
        }
    }

    private fun sendHttpResponse(socket: Socket, statusCode: Int, response: TvPairingResponse) {
        try {
            val bodyBytes = response.toJson().toByteArray(Charsets.UTF_8)
            val statusText = when (statusCode) {
                200 -> "OK"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                405 -> "Method Not Allowed"
                429 -> "Too Many Requests"
                else -> "Error"
            }
            val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: POST, OPTIONS\r\n" +
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

    private fun startUdpServices() {
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
                        Log.d(TAG, "Received UDP discovery request from ${packet.address.hostAddress}")
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
            } catch (e: Exception) {
                Log.w(TAG, "UDP listener stopped: ${e.message}")
            }
        }

        // UDP Periodic Broadcast (every 2.5 seconds on LAN subnet broadcast)
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
                delay(2_500L)
            }
        }
    }

    /**
     * Completely shuts down the HTTP server, UDP sockets, and background jobs.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) return

        Log.d(TAG, "Stopping TvPairingServer")

        try {
            broadcastJob?.cancel()
            udpListenJob?.cancel()
            udpSocket?.close()
            udpSocket = null
        } catch (_: Exception) {}

        try {
            serverJob?.cancel()
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}

        scope.cancel()
    }
}
