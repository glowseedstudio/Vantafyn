package dev.vantafyn.core.jellyfin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.model.api.GeneralCommandMessage
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.GroupStateUpdateGroupUpdate
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import org.jellyfin.sdk.model.api.PlaystateMessage
import org.jellyfin.sdk.model.api.PlayQueueUpdateGroupUpdate
import org.jellyfin.sdk.model.api.SessionInfoDto
import org.jellyfin.sdk.model.api.SessionsMessage
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import org.jellyfin.sdk.model.api.SyncPlayGroupUpdateCommandMessage
import org.jellyfin.sdk.model.api.request.PostCapabilitiesRequest

class SdkJellyfinRealtimeClient(
    private val jellyfin: Jellyfin,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinRealtimeClient {
    override fun events(session: JellyfinSession): Flow<JellyfinWebSocketEvent> {
        val api = jellyfin.createApi(baseUrl = session.server.url, accessToken = session.accessToken)
        val socket = api.webSocket
        val connectionEvents = socket.state.map { state ->
            val name = state::class.simpleName.orEmpty()
            when {
                name.contains("Connected", ignoreCase = true) ->
                    JellyfinWebSocketEvent.ConnectionChanged(SyncPlayConnectionState.Connected)
                name.contains("Connecting", ignoreCase = true) ->
                    JellyfinWebSocketEvent.ConnectionChanged(SyncPlayConnectionState.Connecting)
                name.contains("Disconnected", ignoreCase = true) ->
                    JellyfinWebSocketEvent.ConnectionChanged(SyncPlayConnectionState.Disconnected)
                else ->
                    JellyfinWebSocketEvent.ConnectionChanged(SyncPlayConnectionState.Reconnecting, name.ifBlank { "Socket state changed" })
            } as JellyfinWebSocketEvent
        }
        val messageEvents = socket.subscribeAll().map { message -> message.toVantafynEvent() }
        return merge(connectionEvents, messageEvents)
            .onStart {
                runCatching<Unit> {
                    api.sessionApi.postCapabilities(
                        PostCapabilitiesRequest(
                            id = null,
                            playableMediaTypes = listOf(MediaType.VIDEO, MediaType.AUDIO),
                            supportedCommands = listOf(GeneralCommandType.DISPLAY_MESSAGE),
                            supportsMediaControl = true,
                            supportsPersistentIdentifier = true,
                        ),
                    )
                }
                emit(JellyfinWebSocketEvent.ConnectionChanged(SyncPlayConnectionState.Connecting))
            }
            .catch { throwable ->
                val message = throwable.toRealtimeUserMessage()
                emit(JellyfinWebSocketEvent.Error(message, recoverable = true))
                emit(JellyfinWebSocketEvent.ConnectionChanged(SyncPlayConnectionState.Failed, message))
            }
            .flowOn(ioDispatcher)
    }
}

private fun OutboundWebSocketMessage.toVantafynEvent(): JellyfinWebSocketEvent =
    when (this) {
        is SessionsMessage -> JellyfinWebSocketEvent.SessionsUpdated(
            data.orEmpty().map { it.toWatchPartyRealtimeMember() },
        )

        is SyncPlayGroupUpdateCommandMessage -> {
            val payload = data
            if (payload == null) {
                JellyfinWebSocketEvent.UnknownMessage(messageType.serialName)
            } else {
                val playQueue = payload as? PlayQueueUpdateGroupUpdate
                val stateUpdate = payload as? GroupStateUpdateGroupUpdate
                val currentQueueItem = playQueue
                    ?.data
                    ?.playlist
                    ?.getOrNull(playQueue.data.playingItemIndex)
                JellyfinWebSocketEvent.SyncPlayGroupUpdated(
                    groupId = payload.groupId,
                    updateType = payload.type.serialName,
                    itemId = currentQueueItem?.itemId,
                    startPositionTicks = playQueue?.data?.startPositionTicks,
                    isPlaying = playQueue?.data?.isPlaying ?: stateUpdate?.data?.state?.serialName?.equals("Playing", ignoreCase = true),
                )
            }
        }

        is SyncPlayCommandMessage -> {
            val payload = data
            if (payload == null) {
                JellyfinWebSocketEvent.UnknownMessage(messageType.serialName)
            } else {
                JellyfinWebSocketEvent.SyncPlayCommandReceived(
                    groupId = payload.groupId,
                    command = payload.command.serialName,
                    positionTicks = payload.positionTicks,
                    playlistItemId = payload.playlistItemId,
                )
            }
        }

        is PlaystateMessage -> {
            val payload = data
            if (payload == null) {
                JellyfinWebSocketEvent.UnknownMessage(messageType.serialName)
            } else {
                JellyfinWebSocketEvent.PlaystateCommandReceived(
                    command = payload.command.serialName,
                    positionTicks = payload.seekPositionTicks,
                    controllingUserId = payload.controllingUserId,
                )
            }
        }

        is GeneralCommandMessage -> {
            val payload = data
            if (payload == null) {
                JellyfinWebSocketEvent.UnknownMessage(messageType.serialName)
            } else {
                JellyfinWebSocketEvent.GeneralCommandReceived(
                    command = payload.name.serialName,
                    arguments = payload.arguments
                        ?.mapNotNull { (key, value) -> value?.let { key to it } }
                        ?.toMap()
                        .orEmpty(),
                )
            }
        }

        else -> JellyfinWebSocketEvent.UnknownMessage(messageType.serialName)
    }

private fun SessionInfoDto.toWatchPartyRealtimeMember(): WatchPartyMemberRealtimeState =
    WatchPartyMemberRealtimeState(
        userId = userId,
        displayName = userName ?: deviceName ?: "Active device",
        deviceName = deviceName,
        presence = if (isActive) WatchPartyMemberPresence.Online else WatchPartyMemberPresence.Offline,
        joined = null,
        ready = WatchPartyMemberReadyStatus.Unknown,
        playback = when {
            nowPlayingItem == null -> WatchPartyMemberPlaybackStatus.Unknown
            playState?.isPaused == true -> WatchPartyMemberPlaybackStatus.Paused
            else -> WatchPartyMemberPlaybackStatus.Playing
        },
        playbackPositionTicks = playState?.positionTicks,
        lastSeenAt = System.currentTimeMillis(),
        connectionQuality = null,
    )

private fun Throwable.toRealtimeUserMessage(): String =
    message
        ?.takeIf { it.isNotBlank() }
        ?.let { "Realtime connection issue: $it" }
        ?: "Realtime connection issue. Check your server connection and try again."
