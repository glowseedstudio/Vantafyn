package dev.vantafyn.core.jellyfin

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface JellyfinGamesRepository {
    suspend fun isGamesAvailable(session: JellyfinSession): Result<Boolean>
    suspend fun getGameLibraries(session: JellyfinSession): Result<List<GameLibrary>>
    suspend fun getGameSystems(session: JellyfinSession, libraryId: String): Result<List<GameSystem>>
    suspend fun getGames(session: JellyfinSession, libraryId: String, systemId: String): Result<List<GameSummary>>
    suspend fun getGameDetail(session: JellyfinSession, libraryId: String, gameId: String): Result<GameDetail>
    fun getRomDownloadUrl(session: JellyfinSession, libraryId: String, token: String): String
    suspend fun getCloudSave(session: JellyfinSession, gameId: String, kind: GameSaveKind = GameSaveKind.Sram): Result<ByteArray?>
    suspend fun getCloudSaveWithMetadata(session: JellyfinSession, gameId: String, kind: GameSaveKind = GameSaveKind.Sram): Result<CloudSaveEntry?>
    suspend fun uploadCloudSave(session: JellyfinSession, gameId: String, kind: GameSaveKind = GameSaveKind.Sram, data: ByteArray): Result<Unit>
    suspend fun deleteCloudSave(session: JellyfinSession, gameId: String, kind: GameSaveKind = GameSaveKind.Sram): Result<Unit>
}

class DefaultJellyfinGamesRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinGamesRepository {

    override suspend fun isGamesAvailable(session: JellyfinSession): Result<Boolean> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Capabilities")
                if (conn.responseCode !in 200..299) return@runCatching false
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val games = json.optJSONObject("games")
                val state = games?.optString("state", "") ?: ""
                state.equals("ready", ignoreCase = true)
            }
        }

    override suspend fun getGameLibraries(session: JellyfinSession): Result<List<GameLibrary>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Games/Libraries")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<GameLibrary>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GameLibrary(
                            id = obj.getString("id"),
                            name = obj.optString("name", "Games"),
                            path = obj.optString("path", ""),
                        )
                    )
                }
                list
            }
        }

    override suspend fun getGameSystems(session: JellyfinSession, libraryId: String): Result<List<GameSystem>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Games/$libraryId/Systems")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<GameSystem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val extsArray = obj.optJSONArray("extensions")
                    val extensions = mutableListOf<String>()
                    if (extsArray != null) {
                        for (j in 0 until extsArray.length()) {
                            extensions.add(extsArray.getString(j))
                        }
                    }
                    list.add(
                        GameSystem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            core = obj.optString("core", ""),
                            extensions = extensions,
                            gameCount = obj.optInt("gameCount", 0),
                            icon = obj.optString("icon", obj.getString("id")),
                            logoUrl = obj.optString("logoUrl", "").ifBlank { null },
                        )
                    )
                }
                list
            }
        }

    override suspend fun getGames(
        session: JellyfinSession,
        libraryId: String,
        systemId: String,
    ): Result<List<GameSummary>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Games/$libraryId/Systems/$systemId/Games")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<GameSummary>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val rawBoxart = obj.optString("boxartUrl", "")
                    val fullBoxartUrl = if (rawBoxart.isBlank()) {
                        null
                    } else if (rawBoxart.startsWith("http://") || rawBoxart.startsWith("https://")) {
                        rawBoxart
                    } else {
                        "${session.server.url.trimEnd('/')}/${rawBoxart.trimStart('/')}?api_key=${session.accessToken}"
                    }
                    val id = obj.optString("id", "")
                    val token = obj.optString("token", id)
                    val filename = obj.optString("fileName", obj.optString("filename", ""))
                    val extension = obj.optString("extension", filename.substringAfterLast('.', ""))
                    val system = obj.optString("system", obj.optString("systemId", systemId))
                    val size = if (obj.has("sizeBytes")) obj.optLong("sizeBytes", 0L) else obj.optLong("size", 0L)

                    list.add(
                        GameSummary(
                            id = id,
                            title = obj.optString("title", ""),
                            systemId = system,
                            filename = filename,
                            sizeBytes = size,
                            token = token,
                            extension = extension,
                            boxartUrl = fullBoxartUrl,
                            pokemon = parsePokemonMetadata(obj),
                        )
                    )
                }
                list
            }
        }

    override suspend fun getGameDetail(
        session: JellyfinSession,
        libraryId: String,
        gameId: String,
    ): Result<GameDetail> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Games/$libraryId/Games/$gameId")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(body)
                val id = obj.optString("id", gameId)
                val token = obj.optString("token", id)
                val filename = obj.optString("fileName", obj.optString("filename", ""))
                val extension = obj.optString("extension", filename.substringAfterLast('.', ""))
                val system = obj.optString("system", obj.optString("systemId", ""))
                val core = obj.optString("core", "")
                val size = if (obj.has("sizeBytes")) obj.optLong("sizeBytes", 0L) else obj.optLong("size", 0L)

                val rawRelativeUrl = obj.optString("downloadUrl", "")
                val relativeUrl = if (rawRelativeUrl.isNotBlank()) rawRelativeUrl else "Vantafyn/Games/$libraryId/ROM/$token"
                val fullDownloadUrl = "${session.server.url.trimEnd('/')}/${relativeUrl.trimStart('/')}?api_key=${session.accessToken}"

                val rawBoxart = obj.optString("boxartUrl", "")
                val fullBoxartUrl = if (rawBoxart.isBlank()) {
                    null
                } else if (rawBoxart.startsWith("http://") || rawBoxart.startsWith("https://")) {
                    rawBoxart
                } else {
                    "${session.server.url.trimEnd('/')}/${rawBoxart.trimStart('/')}?api_key=${session.accessToken}"
                }
                GameDetail(
                    id = id,
                    title = obj.optString("title", ""),
                    systemId = system,
                    filename = filename,
                    sizeBytes = size,
                    token = token,
                    extension = extension,
                    core = core,
                    cleanTitle = obj.optString("cleanTitle", cleanGameTitle(obj.optString("title", ""))),
                    region = obj.optString("region", "").ifEmpty { extractGameRegion(obj.optString("title", "")) },
                    downloadUrl = fullDownloadUrl,
                    boxartUrl = fullBoxartUrl,
                    pokemon = parsePokemonMetadata(obj),
                )
            }
        }

    private fun parsePokemonMetadata(obj: JSONObject): GamePokemonMetadata? {
        val pObj = obj.optJSONObject("pokemon") ?: return null
        return GamePokemonMetadata(
            isPokemonGame = pObj.optBoolean("isPokemonGame", true),
            pokemonGameId = pObj.optString("pokemonGameId", ""),
            canonicalTitle = pObj.optString("canonicalTitle", ""),
            generation = pObj.optInt("generation", 0),
            platform = pObj.optString("platform", ""),
            saveType = pObj.optString("saveType", "sram"),
            hasSave = pObj.optBoolean("hasSave", false),
            vaultSupported = pObj.optBoolean("vaultSupported", true),
            detectionConfidence = pObj.optString("detectionConfidence", "high"),
        )
    }

    override fun getRomDownloadUrl(session: JellyfinSession, libraryId: String, token: String): String {
        return "${session.server.url.trimEnd('/')}/Vantafyn/Games/$libraryId/ROM/$token?api_key=${session.accessToken}"
    }

    override suspend fun getCloudSave(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind,
    ): Result<ByteArray?> =
        getCloudSaveWithMetadata(session, gameId, kind).map { it?.data }

    override suspend fun getCloudSaveWithMetadata(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind,
    ): Result<CloudSaveEntry?> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Games/Saves/$gameId?kind=${kind.value}")
                if (conn.responseCode == 404) {
                    return@runCatching null
                }
                checkResponseCode(conn)
                val lastModified = if (conn.lastModified > 0) conn.lastModified else conn.date
                val data = conn.inputStream.use { it.readBytes() }
                CloudSaveEntry(
                    data = data,
                    lastModifiedMs = lastModified,
                    sizeBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else data.size.toLong(),
                )
            }
        }

    override suspend fun uploadCloudSave(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind,
        data: ByteArray,
    ): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Games/Saves/$gameId?kind=${kind.value}",
                    method = "PUT",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/octet-stream")
                conn.outputStream.use { it.write(data) }
                checkResponseCode(conn)
            }
        }

    override suspend fun deleteCloudSave(
        session: JellyfinSession,
        gameId: String,
        kind: GameSaveKind,
    ): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Games/Saves/$gameId?kind=${kind.value}",
                    method = "DELETE",
                )
                checkResponseCode(conn)
            }
        }

    private fun checkResponseCode(conn: HttpURLConnection) {
        val code = conn.responseCode
        if (code !in 200..299) {
            val errorBody = runCatching {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            throw IllegalStateException("Companion API call failed with HTTP $code: ${errorBody ?: conn.responseMessage}")
        }
    }
}
