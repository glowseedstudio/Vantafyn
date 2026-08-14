package dev.vantafyn.core.downloads

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.vantafyn.core.jellyfin.JellyfinResult
import dev.vantafyn.core.jellyfin.JellyfinRepositoryProvider
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineUserDataSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val repository = SqliteDownloadRepository(appContext)
    private val jellyfin = JellyfinRepositoryProvider(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pending = repository.listPendingUserDataMutations()
        if (pending.isEmpty()) return@withContext Result.success()

        var shouldRetry = false
        pending.forEach { mutation ->
            val profileId = mutation.profileId.takeIf { it.isNotBlank() }
            if (profileId == null) {
                repository.markPendingUserDataMutationFailed(
                    serverId = mutation.serverId,
                    userId = mutation.userId,
                    itemId = mutation.itemId,
                    retryCount = mutation.retryCount + 1,
                    failureReason = "Saved profile id was missing for this offline change.",
                )
                return@forEach
            }

            val itemId = runCatching { UUID.fromString(mutation.itemId) }.getOrNull()
            if (itemId == null) {
                repository.markPendingUserDataMutationFailed(
                    serverId = mutation.serverId,
                    userId = mutation.userId,
                    itemId = mutation.itemId,
                    retryCount = mutation.retryCount + 1,
                    failureReason = "Offline item id was not valid.",
                )
                return@forEach
            }

            val session = when (val restored = jellyfin.authRepository.restoreSession(profileId)) {
                is JellyfinResult.Success -> restored.value
                is JellyfinResult.Failure -> {
                    shouldRetry = true
                    repository.markPendingUserDataMutationFailed(
                        serverId = mutation.serverId,
                        userId = mutation.userId,
                        itemId = mutation.itemId,
                        retryCount = mutation.retryCount + 1,
                        failureReason = restored.message,
                    )
                    return@forEach
                }
            }

            val playback = when (
                val info = jellyfin.playbackRepository.getPlaybackInfo(
                    session = session,
                    itemId = itemId,
                    title = "Offline media",
                    subtitle = null,
                    startPositionTicks = mutation.playbackPositionTicks,
                    forceTranscode = false,
                )
            ) {
                is JellyfinResult.Success -> info.value
                is JellyfinResult.Failure -> {
                    shouldRetry = true
                    repository.markPendingUserDataMutationFailed(
                        serverId = mutation.serverId,
                        userId = mutation.userId,
                        itemId = mutation.itemId,
                        retryCount = mutation.retryCount + 1,
                        failureReason = info.message,
                    )
                    return@forEach
                }
            }

            val stopped = jellyfin.playbackRepository.reportStopped(
                session = session,
                info = playback,
                positionTicks = mutation.playbackPositionTicks,
            )
            if (stopped is JellyfinResult.Failure) {
                shouldRetry = true
                repository.markPendingUserDataMutationFailed(
                    serverId = mutation.serverId,
                    userId = mutation.userId,
                    itemId = mutation.itemId,
                    retryCount = mutation.retryCount + 1,
                    failureReason = stopped.message,
                )
                return@forEach
            }

            if (mutation.played) {
                when (val played = jellyfin.mediaRepository.setPlayed(session, itemId, true)) {
                    is JellyfinResult.Failure -> {
                        shouldRetry = true
                        repository.markPendingUserDataMutationFailed(
                            serverId = mutation.serverId,
                            userId = mutation.userId,
                            itemId = mutation.itemId,
                            retryCount = mutation.retryCount + 1,
                            failureReason = played.message,
                        )
                        return@forEach
                    }
                    is JellyfinResult.Success -> Unit
                }
            }

            repository.markPendingUserDataMutationSynced(
                serverId = mutation.serverId,
                userId = mutation.userId,
                itemId = mutation.itemId,
            )
        }

        if (shouldRetry) Result.retry() else Result.success()
    }
}
