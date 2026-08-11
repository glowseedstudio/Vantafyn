package dev.vantafyn.core.jellyfin

import java.net.URLDecoder
import java.util.UUID

object WatchPartyInviteEventMapper {
    private const val Marker = "VANTAFYN_WATCH_PARTY_INVITE"

    fun fromGeneralCommand(
        event: JellyfinWebSocketEvent.GeneralCommandReceived,
        fallbackServerAccountId: String?,
        recipientUserId: UUID?,
        recipientDisplayName: String,
        now: Long = System.currentTimeMillis(),
    ): WatchPartyInvite? {
        val payloadText = buildString {
            append(event.command)
            event.arguments.forEach { (key, value) ->
                append('|').append(key).append('=').append(value)
            }
        }
        val markerIndex = payloadText.indexOf(Marker)
        if (markerIndex < 0) return null
        val fields = payloadText.substring(markerIndex)
            .split('|')
            .drop(1)
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) null else part.substring(0, index) to part.substring(index + 1).decodeInviteValue()
            }
            .toMap()
        val inviteId = fields["inviteId"]?.toUuidOrNull() ?: return null
        val partyId = fields["partyId"]?.toUuidOrNull() ?: return null
        val mode = fields["mode"]?.let { runCatching { WatchPartyMode.valueOf(it) }.getOrNull() } ?: WatchPartyMode.SwipeToMatch
        val expiresAt = fields["expiresAt"]?.toLongOrNull() ?: (now + 60_000L)
        if (expiresAt <= now) {
            return WatchPartyInvite(
                inviteId = inviteId,
                partyId = partyId,
                serverAccountId = fields["serverAccountId"] ?: fallbackServerAccountId,
                mode = mode,
                mediaItemId = fields["mediaItemId"]?.toUuidOrNull(),
                mediaType = fields["mediaType"],
                mediaTitle = fields["mediaTitle"],
                mediaArtworkUrl = fields["mediaArtworkUrl"],
                hostUserId = fields["hostUserId"]?.toUuidOrNull() ?: UUID(0L, 0L),
                hostDisplayName = fields["host"].orEmpty().ifBlank { "Someone" },
                recipientUserId = recipientUserId,
                recipientDisplayName = recipientDisplayName,
                createdAt = fields["createdAt"]?.toLongOrNull() ?: now,
                expiresAt = expiresAt,
                status = WatchPartyInviteStatus.Expired,
            )
        }
        return WatchPartyInvite(
            inviteId = inviteId,
            partyId = partyId,
            serverAccountId = fields["serverAccountId"] ?: fallbackServerAccountId,
            mode = mode,
            mediaItemId = fields["mediaItemId"]?.toUuidOrNull(),
            mediaType = fields["mediaType"],
            mediaTitle = fields["mediaTitle"],
            mediaArtworkUrl = fields["mediaArtworkUrl"],
            hostUserId = fields["hostUserId"]?.toUuidOrNull() ?: UUID(0L, 0L),
            hostDisplayName = fields["host"].orEmpty().ifBlank { "Someone" },
            recipientUserId = recipientUserId,
            recipientDisplayName = recipientDisplayName,
            createdAt = fields["createdAt"]?.toLongOrNull() ?: now,
            expiresAt = expiresAt,
            status = WatchPartyInviteStatus.Pending,
        )
    }

    private fun String.toUuidOrNull(): UUID? =
        runCatching { UUID.fromString(this) }.getOrNull()

    private fun String.decodeInviteValue(): String =
        runCatching { URLDecoder.decode(this, Charsets.UTF_8.name()) }.getOrDefault(this)
}
