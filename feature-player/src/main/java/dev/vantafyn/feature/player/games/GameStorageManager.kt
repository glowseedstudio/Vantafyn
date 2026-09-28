package dev.vantafyn.feature.player.games

import android.content.Context
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameStorageManager(
    private val context: Context,
    private val gamesRepository: JellyfinGamesRepository,
) {
    private val romsDir: File
        get() = File(context.cacheDir, "games/roms").apply { mkdirs() }

    private val savesDir: File
        get() = File(context.filesDir, "games/saves").apply { mkdirs() }

    fun getCachedRomFile(token: String, extension: String): File {
        val safeToken = token.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val ext = if (extension.startsWith(".")) extension else ".$extension"
        return File(romsDir, "$safeToken$ext")
    }

    fun getLocalSaveFile(gameId: String, kind: GameSaveKind): File {
        val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(savesDir, "$safeId.${kind.value}")
    }

    suspend fun downloadRomIfNeeded(
        session: JellyfinSession,
        libraryId: String,
        game: GameDetail,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val target = getCachedRomFile(game.token.ifEmpty { game.id }, game.extension)
        if (target.exists() && target.length() > 0) {
            onProgress(1f)
            return@withContext target
        }

        val tempFile = File(romsDir, "${target.name}.tmp")
        if (tempFile.exists()) tempFile.delete()

        val downloadUrl = game.downloadUrl.ifEmpty {
            gamesRepository.getRomDownloadUrl(session, libraryId, game.token)
        }

        val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Accept", "*/*")
        }

        val totalLength = conn.contentLengthLong.coerceAtLeast(1L)
        var downloaded = 0L

        conn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalLength > 0) {
                        onProgress((downloaded.toFloat() / totalLength).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        }

        if (tempFile.renameTo(target)) {
            onProgress(1f)
            target
        } else {
            throw IllegalStateException("Failed to finalize downloaded ROM file.")
        }
    }

    suspend fun loadSaveState(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind = GameSaveKind.State,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val localFile = getLocalSaveFile(gameId, kind)
        val cloudSave = gamesRepository.getCloudSave(session, gameId, kind).getOrNull()

        if (cloudSave != null && cloudSave.isNotEmpty()) {
            localFile.writeBytes(cloudSave)
            return@withContext cloudSave
        }

        if (localFile.exists() && localFile.length() > 0) {
            return@withContext localFile.readBytes()
        }

        null
    }

    suspend fun saveState(
        session: JellyfinSession,
        gameId: String,
        data: ByteArray,
        kind: GameSaveKind = GameSaveKind.State,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            localFile.writeBytes(data)
            gamesRepository.uploadCloudSave(session, gameId, kind, data).getOrThrow()
        }
    }

    suspend fun deleteSave(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind = GameSaveKind.State,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val localFile = getLocalSaveFile(gameId, kind)
            if (localFile.exists()) localFile.delete()
            gamesRepository.deleteCloudSave(session, gameId, kind).getOrThrow()
        }
    }
}
