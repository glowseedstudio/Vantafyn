package dev.vantafyn.core.integrations.push

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.UnifiedPush
import java.security.MessageDigest

class UnifiedPushManager private constructor(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(readCurrentStatus())
    val status: StateFlow<UnifiedPushStatus> = _status.asStateFlow()

    fun refreshStatus() {
        _status.update { readCurrentStatus() }
    }

    fun getAvailableDistributors(): List<UnifiedPushDistributorInfo> {
        val pm = appContext.packageManager
        val saved = UnifiedPush.getSavedDistributor(appContext)
        val packageNames = runCatching { UnifiedPush.getDistributors(appContext) }.getOrDefault(emptyList())

        return packageNames.map { pkg ->
            val label = runCatching {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(pkg)

            UnifiedPushDistributorInfo(
                packageName = pkg,
                name = label,
                isSelected = pkg == saved,
            )
        }
    }

    /**
     * Registers Vantafyn with the chosen distributor.
     * If [distributorPackage] is null, it checks for a saved distributor or the default installed distributor.
     */
    fun registerWithDistributor(distributorPackage: String? = null) {
        val target = distributorPackage ?: UnifiedPush.getSavedDistributor(appContext)
        if (target != null) {
            Log.i(TAG, "Saving UnifiedPush distributor: $target")
            UnifiedPush.saveDistributor(appContext, target)
        }

        val activeDistributor = UnifiedPush.getSavedDistributor(appContext)
        if (activeDistributor.isNullOrBlank()) {
            Log.w(TAG, "No distributor selected or installed for UnifiedPush")
            _status.update {
                it.copy(
                    registrationState = UnifiedPushRegistrationState.Failed,
                    lastFailureReason = "No distributor available",
                )
            }
            return
        }

        Log.i(TAG, "Initiating UnifiedPush registration with distributor: $activeDistributor")
        _status.update {
            it.copy(
                selectedDistributor = activeDistributor,
                registrationState = UnifiedPushRegistrationState.Registering,
                lastFailureReason = null,
            )
        }

        try {
            UnifiedPush.register(
                context = appContext,
                instance = INSTANCE_DEFAULT,
                messageForDistributor = "Vantafyn",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call UnifiedPush.register", e)
            _status.update {
                it.copy(
                    registrationState = UnifiedPushRegistrationState.Failed,
                    lastFailureReason = e.message ?: "Failed to initiate registration",
                )
            }
        }
    }

    fun unregister() {
        Log.i(TAG, "Unregistering UnifiedPush")
        try {
            UnifiedPush.unregister(appContext, INSTANCE_DEFAULT)
        } catch (e: Exception) {
            Log.w(TAG, "Error during UnifiedPush.unregister", e)
        }

        UnifiedPush.removeDistributor(appContext)
        clearStoredEndpoint()

        _status.update {
            it.copy(
                selectedDistributor = null,
                registrationState = UnifiedPushRegistrationState.Unregistered,
                hasEndpoint = false,
                lastFailureReason = null,
            )
        }
    }

    private val endpointListeners = java.util.concurrent.CopyOnWriteArrayList<EndpointListener>()

    fun addEndpointListener(listener: EndpointListener) {
        endpointListeners.add(listener)
    }

    fun removeEndpointListener(listener: EndpointListener) {
        endpointListeners.remove(listener)
    }

    fun getStoredEndpoint(): String? = prefs.getString(KEY_ENDPOINT_URL, null)

    fun updateServerSyncStatus(state: ServerPushSyncState, timestamp: Long? = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_SERVER_SYNC_STATE, state.name)
            .apply {
                if (timestamp != null) putLong(KEY_SERVER_SYNC_AT, timestamp)
                else remove(KEY_SERVER_SYNC_AT)
            }
            .apply()

        _status.update {
            it.copy(
                serverSyncState = state,
                lastServerSyncTimestampMillis = timestamp,
            )
        }
    }

    internal fun onEndpointReceived(rawEndpointUrl: String) {
        val hash = hashString(rawEndpointUrl)
        prefs.edit()
            .putString(KEY_ENDPOINT_URL, rawEndpointUrl)
            .putString(KEY_ENDPOINT_HASH, hash)
            .putLong(KEY_ENDPOINT_SAVED_AT, System.currentTimeMillis())
            .putString(KEY_REG_STATE, UnifiedPushRegistrationState.Registered.name)
            .remove(KEY_LAST_FAILURE)
            .apply()

        Log.i(TAG, "UnifiedPush endpoint successfully registered (hashPrefix=${hash.take(8)})")
        refreshStatus()

        for (listener in endpointListeners) {
            runCatching { listener.onEndpointChanged(rawEndpointUrl) }
        }
    }

    internal fun onRegistrationFailed(reason: FailedReason) {
        val reasonStr = reason.name
        Log.w(TAG, "UnifiedPush registration failed: reason=$reasonStr")
        prefs.edit()
            .putString(KEY_REG_STATE, UnifiedPushRegistrationState.Failed.name)
            .putString(KEY_LAST_FAILURE, reasonStr)
            .apply()

        refreshStatus()
    }

    internal fun onUnregistered() {
        Log.i(TAG, "UnifiedPush unregistered successfully")
        clearStoredEndpoint()
        prefs.edit()
            .putString(KEY_REG_STATE, UnifiedPushRegistrationState.Unregistered.name)
            .remove(KEY_LAST_FAILURE)
            .putString(KEY_SERVER_SYNC_STATE, ServerPushSyncState.NotSynced.name)
            .remove(KEY_SERVER_SYNC_AT)
            .apply()

        refreshStatus()

        for (listener in endpointListeners) {
            runCatching { listener.onEndpointChanged(null) }
        }
    }

    internal fun onPushReceived(sizeBytes: Int) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_PUSH_RECEIVED_AT, now)
            .apply()

        Log.i(TAG, "UnifiedPush message recorded: size=$sizeBytes bytes at $now")
        _status.update { it.copy(lastPushTimestampMillis = now) }
    }

    private fun clearStoredEndpoint() {
        prefs.edit()
            .remove(KEY_ENDPOINT_URL)
            .remove(KEY_ENDPOINT_HASH)
            .remove(KEY_ENDPOINT_SAVED_AT)
            .apply()
    }

    private fun readCurrentStatus(): UnifiedPushStatus {
        val distributors = getAvailableDistributors()
        val isSupported = distributors.isNotEmpty()
        val savedDistributor = UnifiedPush.getSavedDistributor(appContext)
        val hasEndpoint = prefs.contains(KEY_ENDPOINT_HASH)
        val regStateName = prefs.getString(KEY_REG_STATE, UnifiedPushRegistrationState.Unregistered.name)
        val regState = runCatching {
            UnifiedPushRegistrationState.valueOf(regStateName ?: UnifiedPushRegistrationState.Unregistered.name)
        }.getOrDefault(UnifiedPushRegistrationState.Unregistered)

        val lastPushAt = prefs.getLong(KEY_LAST_PUSH_RECEIVED_AT, 0L).takeIf { it > 0L }
        val lastFailure = prefs.getString(KEY_LAST_FAILURE, null)

        val serverSyncName = prefs.getString(KEY_SERVER_SYNC_STATE, ServerPushSyncState.NotSynced.name)
        val serverSync = runCatching {
            ServerPushSyncState.valueOf(serverSyncName ?: ServerPushSyncState.NotSynced.name)
        }.getOrDefault(ServerPushSyncState.NotSynced)
        val lastServerSyncAt = prefs.getLong(KEY_SERVER_SYNC_AT, 0L).takeIf { it > 0L }

        return UnifiedPushStatus(
            isSupported = isSupported,
            selectedDistributor = savedDistributor,
            availableDistributors = distributors,
            registrationState = if (hasEndpoint) UnifiedPushRegistrationState.Registered else regState,
            hasEndpoint = hasEndpoint,
            lastPushTimestampMillis = lastPushAt,
            lastFailureReason = lastFailure,
            serverSyncState = serverSync,
            lastServerSyncTimestampMillis = lastServerSyncAt,
        )
    }

    private fun hashString(input: String): String =
        runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            digest.fold("") { str, it -> str + "%02x".format(it) }
        }.getOrDefault(input.hashCode().toString())

    fun interface EndpointListener {
        fun onEndpointChanged(endpoint: String?)
    }

    companion object {
        private const val TAG = "UnifiedPushManager"
        private const val PREFS_NAME = "vantafyn_unifiedpush"
        private const val INSTANCE_DEFAULT = "default"
        private const val KEY_ENDPOINT_URL = "endpoint_url"
        private const val KEY_ENDPOINT_HASH = "endpoint_hash"
        private const val KEY_ENDPOINT_SAVED_AT = "endpoint_saved_at"
        private const val KEY_REG_STATE = "registration_state"
        private const val KEY_LAST_FAILURE = "last_failure_reason"
        private const val KEY_LAST_PUSH_RECEIVED_AT = "last_push_received_at"
        private const val KEY_SERVER_SYNC_STATE = "server_sync_state"
        private const val KEY_SERVER_SYNC_AT = "server_sync_at"

        @Volatile
        private var instance: UnifiedPushManager? = null

        fun getInstance(context: Context): UnifiedPushManager =
            instance ?: synchronized(this) {
                instance ?: UnifiedPushManager(context.applicationContext).also { instance = it }
            }
    }
}
