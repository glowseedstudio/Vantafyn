package dev.vantafyn.core.integrations.push

enum class UnifiedPushRegistrationState {
    Unregistered,
    Registering,
    Registered,
    Failed,
}

data class UnifiedPushDistributorInfo(
    val packageName: String,
    val name: String,
    val isSelected: Boolean,
)

enum class ServerPushSyncState {
    NotSynced,
    Syncing,
    Synced,
    Failed,
    ServerUnsupported,
}

data class UnifiedPushStatus(
    val isSupported: Boolean,
    val selectedDistributor: String?,
    val availableDistributors: List<UnifiedPushDistributorInfo>,
    val registrationState: UnifiedPushRegistrationState,
    val hasEndpoint: Boolean,
    val lastPushTimestampMillis: Long?,
    val lastFailureReason: String?,
    val serverSyncState: ServerPushSyncState = ServerPushSyncState.NotSynced,
    val lastServerSyncTimestampMillis: Long? = null,
)

data class UnifiedPushPayload(
    val content: ByteArray,
    val isDecrypted: Boolean,
    val receivedAtMillis: Long,
) {
    val sizeBytes: Int get() = content.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UnifiedPushPayload
        if (!content.contentEquals(other.content)) return false
        if (isDecrypted != other.isDecrypted) return false
        if (receivedAtMillis != other.receivedAtMillis) return false
        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + isDecrypted.hashCode()
        result = 31 * result + receivedAtMillis.hashCode()
        return result
    }

    override fun toString(): String =
        "UnifiedPushPayload(sizeBytes=$sizeBytes, isDecrypted=$isDecrypted, receivedAtMillis=$receivedAtMillis)"
}
