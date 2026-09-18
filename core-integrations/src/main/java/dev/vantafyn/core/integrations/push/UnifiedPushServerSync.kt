package dev.vantafyn.core.integrations.push

import android.content.Context
import android.util.Log
import dev.vantafyn.core.jellyfin.JellyfinSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UnifiedPushServerSync private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val pushManager = UnifiedPushManager.getInstance(appContext)
    private val pushRepo = CompanionPushRepository()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val deviceId: String = VantafynDeviceIdProvider.getDeviceId(appContext)

    @Volatile
    private var activeSession: JellyfinSession? = null

    init {
        pushManager.addEndpointListener { newEndpoint ->
            val session = activeSession
            if (session != null) {
                if (!newEndpoint.isNullOrBlank()) {
                    syncWithServer(session, newEndpoint)
                } else {
                    syncScope.launch {
                        pushRepo.unregisterDevice(session, deviceId)
                        pushManager.updateServerSyncStatus(ServerPushSyncState.NotSynced)
                    }
                }
            }
        }
    }

    fun onSessionChanged(session: JellyfinSession?) {
        activeSession = session
        if (session != null) {
            val storedEndpoint = pushManager.getStoredEndpoint()
            if (!storedEndpoint.isNullOrBlank()) {
                syncWithServer(session, storedEndpoint)
            }
        }
    }

    fun syncCurrentEndpointWithSession(session: JellyfinSession) {
        activeSession = session
        val endpoint = pushManager.getStoredEndpoint()
        if (!endpoint.isNullOrBlank()) {
            syncWithServer(session, endpoint)
        } else {
            pushManager.updateServerSyncStatus(ServerPushSyncState.NotSynced)
        }
    }

    fun unregisterFromServer(session: JellyfinSession) {
        syncScope.launch {
            pushRepo.unregisterDevice(session, deviceId)
            pushManager.updateServerSyncStatus(ServerPushSyncState.NotSynced)
        }
    }

    suspend fun sendTestPush(session: JellyfinSession): Result<String> {
        return pushRepo.sendTestPush(session, deviceId)
    }

    private fun syncWithServer(session: JellyfinSession, endpoint: String) {
        syncScope.launch {
            pushManager.updateServerSyncStatus(ServerPushSyncState.Syncing)

            val supported = pushRepo.checkCapabilities(session)
            if (!supported) {
                Log.i(TAG, "Companion server does not support background push or notifications are disabled")
                pushManager.updateServerSyncStatus(ServerPushSyncState.ServerUnsupported)
                return@launch
            }

            val clientName = if (appContext.packageName.contains("mobile", ignoreCase = true)) {
                "Vantafyn Mobile"
            } else {
                "Vantafyn TV"
            }

            val distributor = pushManager.status.value.selectedDistributor
            val result = pushRepo.registerDevice(
                session = session,
                deviceId = deviceId,
                endpoint = endpoint,
                distributor = distributor,
                clientName = clientName,
            )

            if (result.isSuccess) {
                pushManager.updateServerSyncStatus(ServerPushSyncState.Synced)
            } else {
                pushManager.updateServerSyncStatus(ServerPushSyncState.Failed)
            }
        }
    }

    companion object {
        private const val TAG = "UnifiedPushServerSync"

        @Volatile
        private var instance: UnifiedPushServerSync? = null

        fun getInstance(context: Context): UnifiedPushServerSync =
            instance ?: synchronized(this) {
                instance ?: UnifiedPushServerSync(context.applicationContext).also { instance = it }
            }
    }
}
