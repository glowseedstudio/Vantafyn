package dev.vantafyn.core.downloads

data class DownloadIdentity(
    val serverId: String,
    val userId: String,
    val itemId: String,
    val mediaSourceId: String,
) {
    init {
        require(serverId.isNotBlank()) { "serverId must not be blank" }
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(itemId.isNotBlank()) { "itemId must not be blank" }
        require(mediaSourceId.isNotBlank()) { "mediaSourceId must not be blank" }
    }

    val stableKey: String = listOf(serverId, userId, itemId, mediaSourceId)
        .joinToString(separator = "|") { it.trim() }
}
