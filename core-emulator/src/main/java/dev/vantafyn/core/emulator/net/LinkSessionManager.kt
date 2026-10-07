package dev.vantafyn.core.emulator.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import dev.vantafyn.core.emulator.NativeEmulatorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Supported Link Cable / Multiplayer Communication Modes.
 */
enum class LinkTransportMode(val id: String, val displayName: String) {
    LOCAL_WIFI("local_wifi", "Local Wi-Fi / Wi-Fi Direct"),
    SERVER_RELAY("server_relay", "Vantafyn Server Relay"),
    OFFLINE("offline", "Disabled (Offline)"),
    ;

    companion object {
        fun fromId(id: String?): LinkTransportMode = entries.firstOrNull { it.id == id } ?: LOCAL_WIFI
    }
}

/**
 * State of the wireless communication link.
 */
enum class LinkSessionState {
    DISCONNECTED,
    DISCOVERING,
    ADVERTISING,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Information about a nearby peer discovered on the local network or Wi-Fi Direct.
 */
data class DiscoveredLinkPeer(
    val serviceName: String,
    val hostName: String,
    val gameId: String,
    val gameTitle: String,
    val core: String,
    val ip: String,
    val port: Int,
    val discoveredAt: Long = System.currentTimeMillis(),
)

/**
 * Active connected session details.
 */
data class ActiveLinkSession(
    val isHost: Boolean,
    val gameId: String,
    val core: String,
    val remoteIp: String,
    val port: Int,
    val transportMode: LinkTransportMode,
    val peerName: String,
)

/**
 * Unified Link Cable and Wireless Communication Manager for Vantafyn Native Cores.
 * Coordinates local network discovery (Android NsdManager), peer pairing, and core options.
 */
class LinkSessionManager(private val context: Context) {

    companion object {
        private const val TAG = "LinkSessionManager"
        private const val SERVICE_TYPE = "_vantafyn-link._tcp."
        private const val PREFS_NAME = "vantafyn_retro_settings"

        // Default ports per core architecture
        const val PORT_NDS_NIFI = 7064
        const val PORT_GBA_LINK = 6400
        const val PORT_GB_LINK = 8765

        @Volatile
        private var instance: LinkSessionManager? = null

        fun getInstance(context: Context): LinkSessionManager {
            return instance ?: synchronized(this) {
                instance ?: LinkSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Observable states
    private val _sessionState = MutableStateFlow(LinkSessionState.DISCONNECTED)
    val sessionState: StateFlow<LinkSessionState> = _sessionState.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredLinkPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredLinkPeer>> = _discoveredPeers.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveLinkSession?>(null)
    val activeSession: StateFlow<ActiveLinkSession?> = _activeSession.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // Preferences getters / setters
    var transportMode: LinkTransportMode
        get() = LinkTransportMode.fromId(prefs.getString("link_transport_mode", LinkTransportMode.LOCAL_WIFI.id))
        set(value) = prefs.edit().putString("link_transport_mode", value.id).apply()

    var autoDiscoveryEnabled: Boolean
        get() = prefs.getBoolean("link_auto_discovery", true)
        set(value) = prefs.edit().putBoolean("link_auto_discovery", value).apply()

    var playerHandle: String
        get() = prefs.getString("link_player_handle", "")?.ifBlank { Build.MODEL } ?: Build.MODEL
        set(value) = prefs.edit().putString("link_player_handle", value).apply()

    var serverRoomCode: String
        get() = prefs.getString("link_server_room_code", "1234") ?: "1234"
        set(value) = prefs.edit().putString("link_server_room_code", value).apply()

    var serverBaseUrl: String
        get() = prefs.getString("link_server_base_url", "") ?: ""
        set(value) = prefs.edit().putString("link_server_base_url", value).apply()

    var serverAuthToken: String
        get() = prefs.getString("link_server_auth_token", "") ?: ""
        set(value) = prefs.edit().putString("link_server_auth_token", value).apply()

    private var heartbeatJob: Job? = null

    /**
     * Resolves the appropriate communication port for a given core/system.
     */
    fun getDefaultPortForCore(core: String, systemId: String): Int {
        val c = core.lowercase()
        val s = systemId.lowercase()
        return when {
            s in setOf("nds", "ds") || c.contains("melonds") -> PORT_NDS_NIFI
            s == "gba" || c.contains("mgba") || c.contains("gpsp") -> PORT_GBA_LINK
            s in setOf("gb", "gbc") || c.contains("tgbdual") || c.contains("gambatte") -> PORT_GB_LINK
            else -> PORT_GBA_LINK
        }
    }

    /**
     * Starts advertising this device as a link host on the local Wi-Fi / Wi-Fi Direct network.
     */
    fun startHosting(
        gameId: String,
        gameTitle: String,
        core: String,
        port: Int = getDefaultPortForCore(core, ""),
    ) {
        if (transportMode == LinkTransportMode.OFFLINE) {
            _statusMessage.value = "Link mode is disabled in Settings"
            return
        }

        stopAdvertising()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Vantafyn-${playerHandle.replace(" ", "_")}"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("gameId", gameId)
            setAttribute("gameTitle", gameTitle.take(32))
            setAttribute("core", core)
            setAttribute("hostName", playerHandle)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.i(TAG, "Link service registered: ${info.serviceName} on port $port")
                _sessionState.value = LinkSessionState.ADVERTISING
                _statusMessage.value = "Broadcasting room: ${info.serviceName}"
                _activeSession.value = ActiveLinkSession(
                    isHost = true,
                    gameId = gameId,
                    core = core,
                    remoteIp = getLocalIpAddress() ?: "127.0.0.1",
                    port = port,
                    transportMode = transportMode,
                    peerName = playerHandle,
                )
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Link service registration failed: code $errorCode")
                _sessionState.value = LinkSessionState.ERROR
                _statusMessage.value = "Failed to broadcast local room (code $errorCode)"
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.i(TAG, "Link service unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Link service unregistration failed: code $errorCode")
            }
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
            _sessionState.value = LinkSessionState.ERROR
            _statusMessage.value = e.message
        }
    }

    /**
     * Stops advertising the current session.
     */
    fun stopAdvertising() {
        registrationListener?.let { listener ->
            try {
                nsdManager?.unregisterService(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Could not unregister NSD service: ${e.message}")
            }
        }
        registrationListener = null
    }

    /**
     * Starts discovering nearby link cable sessions on the local Wi-Fi or Wi-Fi Direct.
     */
    fun startDiscovery(filterGameId: String? = null) {
        if (transportMode == LinkTransportMode.OFFLINE || !autoDiscoveryEnabled) return
        stopDiscovery()

        _sessionState.value = LinkSessionState.DISCOVERING
        _discoveredPeers.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: code $errorCode")
                _sessionState.value = LinkSessionState.ERROR
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Discovery stop failed: code $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.i(TAG, "Nearby link discovery started: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Nearby link discovery stopped")
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                Log.i(TAG, "Discovered link service candidate: ${info.serviceName}")
                if (info.serviceType == SERVICE_TYPE || info.serviceType.contains("vantafyn-link")) {
                    resolveService(info, filterGameId)
                }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                Log.i(TAG, "Lost link service: ${info.serviceName}")
                _discoveredPeers.value = _discoveredPeers.value.filter { it.serviceName != info.serviceName }
            }
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NSD discovery", e)
            _sessionState.value = LinkSessionState.ERROR
        }
    }

    private fun resolveService(info: NsdServiceInfo, filterGameId: String?) {
        try {
            nsdManager?.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val hostAddress = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val attributes = serviceInfo.attributes

                    val peerGameId = attributes["gameId"]?.let { String(it, StandardCharsets.UTF_8) }.orEmpty()
                    val peerGameTitle = attributes["gameTitle"]?.let { String(it, StandardCharsets.UTF_8) }.orEmpty()
                    val peerCore = attributes["core"]?.let { String(it, StandardCharsets.UTF_8) }.orEmpty()
                    val peerHostName = attributes["hostName"]?.let { String(it, StandardCharsets.UTF_8) } ?: serviceInfo.serviceName

                    // Filter by gameId if requested
                    if (filterGameId != null && peerGameId.isNotBlank() && peerGameId != filterGameId) {
                        return
                    }

                    val peer = DiscoveredLinkPeer(
                        serviceName = serviceInfo.serviceName,
                        hostName = peerHostName,
                        gameId = peerGameId,
                        gameTitle = peerGameTitle,
                        core = peerCore,
                        ip = hostAddress,
                        port = port,
                    )

                    scope.launch(Dispatchers.Main) {
                        val current = _discoveredPeers.value.toMutableList()
                        current.removeAll { it.serviceName == peer.serviceName || it.ip == peer.ip }
                        current.add(peer)
                        _discoveredPeers.value = current
                        Log.i(TAG, "Discovered active peer: ${peer.hostName} playing ${peer.gameTitle} at ${peer.ip}:${peer.port}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Exception resolving service: ${e.message}")
        }
    }

    /**
     * Stops peer discovery.
     */
    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop NSD discovery: ${e.message}")
            }
        }
        discoveryListener = null
    }

    /**
     * Connects to a discovered peer.
     */
    fun connectToPeer(peer: DiscoveredLinkPeer) {
        _sessionState.value = LinkSessionState.CONNECTING
        _statusMessage.value = "Connecting to ${peer.hostName}..."
        stopDiscovery()

        _activeSession.value = ActiveLinkSession(
            isHost = false,
            gameId = peer.gameId,
            core = peer.core,
            remoteIp = peer.ip,
            port = peer.port,
            transportMode = LinkTransportMode.LOCAL_WIFI,
            peerName = peer.hostName,
        )
        _sessionState.value = LinkSessionState.CONNECTED
        _statusMessage.value = "Connected to ${peer.hostName}"
    }

    /**
     * Starts an online multiplayer link session relayed over the Vantafyn server using a Room Code.
     */
    fun startServerRelaySession(
        gameId: String,
        gameTitle: String,
        core: String,
        roomCode: String,
        isHost: Boolean,
        port: Int = getDefaultPortForCore(core, ""),
    ) {
        disconnect()
        serverRoomCode = roomCode
        _sessionState.value = if (isHost) LinkSessionState.ADVERTISING else LinkSessionState.CONNECTING
        _statusMessage.value = if (isHost) "Hosting Server Room #$roomCode..." else "Joining Server Room #$roomCode..."

        _activeSession.value = ActiveLinkSession(
            isHost = isHost,
            gameId = gameId,
            core = core,
            remoteIp = if (isHost) (getLocalIpAddress() ?: "127.0.0.1") else "127.0.0.1",
            port = port,
            transportMode = LinkTransportMode.SERVER_RELAY,
            peerName = "Room #$roomCode",
        )

        _sessionState.value = if (isHost) LinkSessionState.ADVERTISING else LinkSessionState.CONNECTED
        _statusMessage.value = if (isHost) "Server Room #$roomCode active" else "Linked to Server Room #$roomCode"
        Log.i(TAG, "Server relay session initiated: room=$roomCode, host=$isHost, core=$core")

        // Register room with Vantafyn Companion Plugin and maintain heartbeat
        scope.launch {
            try {
                if (serverBaseUrl.isNotBlank()) {
                    val endpoint = if (isHost) {
                        "${serverBaseUrl.trimEnd('/')}/Vantafyn/Games/Link/Rooms"
                    } else {
                        "${serverBaseUrl.trimEnd('/')}/Vantafyn/Games/Link/Rooms/$roomCode/Join"
                    }
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 8_000
                        readTimeout = 12_000
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Accept", "application/json")
                        if (serverAuthToken.isNotBlank()) {
                            setRequestProperty("X-Emby-Token", serverAuthToken)
                            setRequestProperty("X-MediaBrowser-Token", serverAuthToken)
                        }
                        doOutput = true
                        val payload = if (isHost) {
                            """{"gameId":"$gameId","gameTitle":"$gameTitle","core":"$core","roomCode":"$roomCode","playerName":"$playerHandle","port":$port}"""
                        } else {
                            """{"playerName":"$playerHandle"}"""
                        }
                        outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    }
                    val respCode = conn.responseCode
                    Log.i(TAG, "Server room registration response code: $respCode")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server room registration warning: ${e.message}")
            }
        }

        // Start background heartbeat every 30s to keep room alive on server
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000)
                try {
                    if (serverBaseUrl.isNotBlank()) {
                        val hbEndpoint = "${serverBaseUrl.trimEnd('/')}/Vantafyn/Games/Link/Rooms/$roomCode/Heartbeat"
                        val hbConn = (URL(hbEndpoint).openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 5_000
                            readTimeout = 5_000
                            if (serverAuthToken.isNotBlank()) {
                                setRequestProperty("X-Emby-Token", serverAuthToken)
                                setRequestProperty("X-MediaBrowser-Token", serverAuthToken)
                            }
                        }
                        hbConn.responseCode
                    }
                } catch (e: Exception) {
                    Log.v(TAG, "Heartbeat error: ${e.message}")
                }
            }
        }
    }

    /**
     * Configures the native emulator engine options according to the active link session.
     */
    fun configureEngineForLink(engine: NativeEmulatorEngine, session: ActiveLinkSession) {
        val core = session.core.lowercase()

        when {
            // Nintendo DS (melonDS) NiFi Wireless
            core.contains("melonds") -> {
                engine.setOption("melonds_nifi", "enabled")
                engine.setOption("melonds_console_mode", "DS")
                Log.i(TAG, "Applied melonDS NiFi wireless link options")
            }

            // Game Boy Advance (mGBA) Link Cable / Wireless Adapter
            core.contains("mgba") -> {
                // If it's a wireless adapter game (e.g. Pokémon FireRed/LeafGreen/Emerald Union Room)
                val isWirelessAdapter = session.gameId.contains("firered", ignoreCase = true) ||
                        session.gameId.contains("leafgreen", ignoreCase = true) ||
                        session.gameId.contains("emerald", ignoreCase = true)

                val mode = if (isWirelessAdapter) {
                    if (session.isHost) "wireless_host" else "wireless_client"
                } else {
                    if (session.isHost) "cable_host" else "cable_client"
                }

                engine.setOption("mgba_link_mode", mode)
                if (!session.isHost) {
                    engine.setOption("mgba_link_host", session.remoteIp)
                }
                Log.i(TAG, "Applied mGBA link options: mode=$mode, host=${session.remoteIp}")
            }

            // Game Boy / Game Boy Color (TGB Dual)
            core.contains("tgbdual") -> {
                val mode = if (session.isHost) "host" else "client"
                engine.setOption("tgbdual_gblink_mode", mode)
                if (!session.isHost) {
                    engine.setOption("tgbdual_gblink_host", session.remoteIp)
                }
                Log.i(TAG, "Applied TGB Dual link options: mode=$mode, host=${session.remoteIp}")
            }
        }
    }

    /**
     * Terminates all link sessions, advertisements, and discovery.
     */
    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null

        val currentRoom = serverRoomCode
        val currentBaseUrl = serverBaseUrl
        val currentToken = serverAuthToken

        if (transportMode == LinkTransportMode.SERVER_RELAY && currentBaseUrl.isNotBlank() && currentRoom.isNotBlank()) {
            scope.launch {
                try {
                    val closeEndpoint = "${currentBaseUrl.trimEnd('/')}/Vantafyn/Games/Link/Rooms/$currentRoom"
                    val closeConn = (URL(closeEndpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "DELETE"
                        connectTimeout = 5_000
                        readTimeout = 5_000
                        if (currentToken.isNotBlank()) {
                            setRequestProperty("X-Emby-Token", currentToken)
                            setRequestProperty("X-MediaBrowser-Token", currentToken)
                        }
                    }
                    closeConn.responseCode
                } catch (e: Exception) {
                    Log.v(TAG, "Room close notification error: ${e.message}")
                }
            }
        }

        stopAdvertising()
        stopDiscovery()
        _activeSession.value = null
        _sessionState.value = LinkSessionState.DISCONNECTED
        _statusMessage.value = null
        _discoveredPeers.value = emptyList()
    }

    /**
     * Helper to get the device's local IPv4 address on the Wi-Fi / Wi-Fi Direct interface.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error obtaining local IP: ${e.message}")
        }
        return null
    }
}
