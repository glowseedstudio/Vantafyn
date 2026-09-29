package dev.vantafyn.feature.player.games

import android.content.Context
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.mediaBrowserAuthHeader
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
        val token = game.token.ifEmpty { game.id }
        val ext = game.extension.ifEmpty { game.filename.substringAfterLast('.', "") }
        val target = getCachedRomFile(token, ext)
        if (target.exists() && target.length() > 0) {
            onProgress(1f)
            return@withContext target
        }

        val tempFile = File(romsDir, "${target.name}.tmp")
        if (tempFile.exists()) tempFile.delete()

        val downloadUrl = if (game.downloadUrl.isNotBlank() && !game.downloadUrl.contains("/ROM/?")) {
            game.downloadUrl
        } else {
            gamesRepository.getRomDownloadUrl(session, libraryId, token)
        }

        var currentUrl = downloadUrl
        var conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 90_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "*/*")
            val authHeader = session.mediaBrowserAuthHeader()
            setRequestProperty("Authorization", authHeader)
            setRequestProperty("X-Emby-Token", session.accessToken)
            setRequestProperty("X-MediaBrowser-Token", session.accessToken)
            setRequestProperty("X-Emby-Authorization", authHeader)
        }

        var responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
            responseCode == 307 || responseCode == 308) {
            val redirectUrl = conn.getHeaderField("Location")
            if (!redirectUrl.isNullOrBlank()) {
                conn.disconnect()
                currentUrl = redirectUrl
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 90_000
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "*/*")
                    val authHeader = session.mediaBrowserAuthHeader()
                    setRequestProperty("Authorization", authHeader)
                    setRequestProperty("X-Emby-Token", session.accessToken)
                    setRequestProperty("X-MediaBrowser-Token", session.accessToken)
                    setRequestProperty("X-Emby-Authorization", authHeader)
                }
                responseCode = conn.responseCode
            }
        }

        if (responseCode !in 200..299) {
            val safeUrl = currentUrl.substringBefore('?')
            val rawError = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            val sanitizedError = if (session.accessToken.isNotBlank()) {
                rawError.replace(session.accessToken, "[REDACTED]")
            } else rawError
            throw IllegalStateException("Server returned HTTP $responseCode downloading ROM ($safeUrl). $sanitizedError".trim())
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
        if (localFile.exists() && localFile.length() > 0) {
            return@withContext localFile.readBytes()
        }

        if (kind == GameSaveKind.Sram) {
            val safeId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val altSrm = File(savesDir, "$safeId.srm")
            if (altSrm.exists() && altSrm.length() > 0) {
                return@withContext altSrm.readBytes()
            }
            val altSav = File(savesDir, "$safeId.sav")
            if (altSav.exists() && altSav.length() > 0) {
                return@withContext altSav.readBytes()
            }
        }

        val cloudSave = gamesRepository.getCloudSave(session, gameId, kind).getOrNull()
        if (cloudSave != null && cloudSave.isNotEmpty()) {
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(cloudSave)
            return@withContext cloudSave
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
            localFile.parentFile?.mkdirs()
            localFile.writeBytes(data)
            val cloudRes = gamesRepository.uploadCloudSave(session, gameId, kind, data)
            if (cloudRes.isFailure) {
                android.util.Log.w("GameStorageManager", "Cloud save sync error: ${cloudRes.exceptionOrNull()?.message}")
            }
            Unit
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
