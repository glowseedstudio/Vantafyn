package dev.vantafyn.core.jellyfin

import java.net.HttpURLConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface JellyfinPokemonRepository {
    suspend fun isPokemonAvailable(session: JellyfinSession): Result<Boolean>
    suspend fun getStatus(session: JellyfinSession): Result<PokemonIntegrationStatus>
    suspend fun getPokemonGames(session: JellyfinSession): Result<List<GameSummary>>
    suspend fun getVaultSummary(session: JellyfinSession): Result<PokemonVaultSummary>
    suspend fun getVaultBoxes(session: JellyfinSession): Result<List<PokemonVaultBoxSummary>>
    suspend fun getVaultBox(session: JellyfinSession, boxIndex: Int): Result<PokemonVaultBox>
    suspend fun getVaultEntry(session: JellyfinSession, entryId: String): Result<PokemonVaultEntry?>
    suspend fun getGameSave(session: JellyfinSession, libraryId: String, gameId: String): Result<PokemonGameSaveDto>
    suspend fun getGameLockState(session: JellyfinSession, libraryId: String, gameId: String): Result<SaveLockStateDto>
    suspend fun depositPokemon(session: JellyfinSession, request: PokemonDepositRequest): Result<PokemonOperationResponse>
    suspend fun withdrawPokemon(session: JellyfinSession, request: PokemonWithdrawRequest): Result<PokemonOperationResponse>
    suspend fun directTransferPokemon(session: JellyfinSession, request: PokemonDirectTransferRequest): Result<PokemonOperationResponse>
    suspend fun validateTransfer(session: JellyfinSession, request: PokemonTransferValidateRequest): Result<PokemonTransferCompatibilityResult>
    suspend fun createTrade(session: JellyfinSession, request: CreateTradeRequest): Result<PokemonTradeOperationResponse>
    suspend fun joinLinkTrade(session: JellyfinSession, request: JoinLinkTradeRequest): Result<PokemonTradeOperationResponse>
    suspend fun acceptTrade(session: JellyfinSession, request: AcceptTradeRequest): Result<PokemonTradeOperationResponse>
    suspend fun cancelTrade(session: JellyfinSession, request: CancelTradeRequest): Result<PokemonTradeOperationResponse>
    suspend fun getPendingTrades(session: JellyfinSession): Result<List<PokemonTradeSession>>
    suspend fun getTrade(session: JellyfinSession, tradeId: String): Result<PokemonTradeSession>
    suspend fun getTradeHistory(session: JellyfinSession): Result<List<PokemonTradeSession>>
    suspend fun getGameBackups(session: JellyfinSession, libraryId: String, gameId: String): Result<List<PokemonBackupDto>>
    suspend fun restoreBackup(session: JellyfinSession, request: RestoreBackupRequest): Result<RestoreBackupResponse>
    suspend fun getDiagnostics(session: JellyfinSession): Result<PokemonDiagnosticsDto>
    suspend fun getPokedex(session: JellyfinSession): Result<PokemonPokedexDto>
    suspend fun getPokemonJourney(session: JellyfinSession, pokemonId: String): Result<PokemonJourneyDto>
    suspend fun getAchievements(session: JellyfinSession): Result<PokemonAchievementsSummaryDto>
    suspend fun getSocialActivity(session: JellyfinSession, limit: Int = 30): Result<List<PokemonSocialActivityEvent>>
}

class DefaultJellyfinPokemonRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JellyfinPokemonRepository {

    override suspend fun isPokemonAvailable(session: JellyfinSession): Result<Boolean> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Status")
                if (conn.responseCode !in 200..299) return@runCatching false
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                json.optBoolean("enabled", false)
            }
        }

    override suspend fun getStatus(session: JellyfinSession): Result<PokemonIntegrationStatus> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Status")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val gens = mutableListOf<Int>()
                val gensArray = json.optJSONArray("supportedGenerations")
                if (gensArray != null) {
                    for (i in 0 until gensArray.length()) {
                        gens.add(gensArray.getInt(i))
                    }
                }
                PokemonIntegrationStatus(
                    enabled = json.optBoolean("enabled", false),
                    provider = json.optString("provider", "none"),
                    providerHealthy = json.optBoolean("providerHealthy", false),
                    providerMessage = json.optString("providerMessage", "").ifEmpty { null },
                    supportedGenerations = gens,
                    vaultAvailable = json.optBoolean("vaultAvailable", false),
                    transfersAvailable = json.optBoolean("transfersAvailable", false),
                    crossGenerationAvailable = json.optBoolean("crossGenerationAvailable", false),
                    tradingAvailable = json.optBoolean("tradingAvailable", false),
                )
            }
        }

    override suspend fun getPokemonGames(session: JellyfinSession): Result<List<GameSummary>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Games")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<GameSummary>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseGameSummary(obj, session))
                }
                list
            }
        }

    override suspend fun getVaultSummary(session: JellyfinSession): Result<PokemonVaultSummary> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Vault")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                PokemonVaultSummary(
                    totalCapacity = json.optInt("totalCapacity", 900),
                    totalOccupied = json.optInt("totalOccupied", 0),
                    boxCount = json.optInt("boxCount", 30),
                    shinyCount = json.optInt("shinyCount", 0),
                    speciesCount = json.optInt("speciesCount", 0),
                )
            }
        }

    override suspend fun getVaultBoxes(session: JellyfinSession): Result<List<PokemonVaultBoxSummary>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Vault/Boxes")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<PokemonVaultBoxSummary>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PokemonVaultBoxSummary(
                            boxIndex = obj.optInt("boxIndex", i + 1),
                            name = obj.optString("name", "Box ${i + 1}"),
                            capacity = obj.optInt("capacity", 30),
                            occupiedCount = obj.optInt("occupiedCount", 0),
                            shinyCount = obj.optInt("shinyCount", 0),
                        )
                    )
                }
                list
            }
        }

    override suspend fun getVaultBox(session: JellyfinSession, boxIndex: Int): Result<PokemonVaultBox> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Vault/Boxes/$boxIndex")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val entriesArray = json.optJSONArray("entries") ?: JSONArray()
                val entriesList = mutableListOf<PokemonVaultEntry>()
                for (i in 0 until entriesArray.length()) {
                    entriesList.add(parseVaultEntry(entriesArray.getJSONObject(i)))
                }
                PokemonVaultBox(
                    boxIndex = json.optInt("boxIndex", boxIndex),
                    name = json.optString("name", "Box $boxIndex"),
                    entries = entriesList,
                )
            }
        }

    override suspend fun getVaultEntry(session: JellyfinSession, entryId: String): Result<PokemonVaultEntry?> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Vault/Entries/$entryId")
                if (conn.responseCode == 404) return@runCatching null
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseVaultEntry(JSONObject(body))
            }
        }

    override suspend fun getGameSave(
        session: JellyfinSession,
        libraryId: String,
        gameId: String,
    ): Result<PokemonGameSaveDto> =
        withContext(ioDispatcher) {
            runCatching {
                val path = if (libraryId.isBlank()) {
                    "Vantafyn/Pokemon/Games/default/$gameId/Save"
                } else {
                    "Vantafyn/Pokemon/Games/$libraryId/$gameId/Save"
                }
                val conn = session.openAuthenticatedConnection(path)
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseGameSaveDto(JSONObject(body))
            }
        }

    override suspend fun getGameLockState(
        session: JellyfinSession,
        libraryId: String,
        gameId: String,
    ): Result<SaveLockStateDto> =
        withContext(ioDispatcher) {
            runCatching {
                val path = if (libraryId.isBlank()) {
                    "Vantafyn/Pokemon/Games/default/$gameId/LockState"
                } else {
                    "Vantafyn/Pokemon/Games/$libraryId/$gameId/LockState"
                }
                val conn = session.openAuthenticatedConnection(path)
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val clients = mutableListOf<String>()
                val clientsArray = json.optJSONArray("activeClients")
                if (clientsArray != null) {
                    for (i in 0 until clientsArray.length()) {
                        clients.add(clientsArray.getString(i))
                    }
                }
                SaveLockStateDto(
                    isLocked = json.optBoolean("isLocked", false),
                    lockReason = json.optString("lockReason", "").ifEmpty { null },
                    activeSessionCount = json.optInt("activeSessionCount", 0),
                    activeClients = clients,
                )
            }
        }

    override suspend fun depositPokemon(
        session: JellyfinSession,
        request: PokemonDepositRequest,
    ): Result<PokemonOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Vault/Deposit",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("gameId", request.gameId)
                    put("pokemonId", request.pokemonId)
                    put("isInParty", request.isInParty)
                    if (request.boxIndex != null) put("boxIndex", request.boxIndex)
                    put("slotIndex", request.slotIndex)
                    put("targetVaultBoxIndex", request.targetVaultBoxIndex)
                    if (request.targetVaultSlotIndex != null) put("targetVaultSlotIndex", request.targetVaultSlotIndex)
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseOperationResponse(JSONObject(body))
            }
        }

    override suspend fun withdrawPokemon(
        session: JellyfinSession,
        request: PokemonWithdrawRequest,
    ): Result<PokemonOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Vault/Withdraw",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("vaultEntryId", request.vaultEntryId)
                    put("targetGameId", request.targetGameId)
                    if (request.targetBoxIndex != null) put("targetBoxIndex", request.targetBoxIndex)
                    if (request.targetSlotIndex != null) put("targetSlotIndex", request.targetSlotIndex)
                    put("targetIsInParty", request.targetIsInParty)
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseOperationResponse(JSONObject(body))
            }
        }

    override suspend fun directTransferPokemon(
        session: JellyfinSession,
        request: PokemonDirectTransferRequest,
    ): Result<PokemonOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Transfers/Direct",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("sourceGameId", request.sourceGameId)
                    put("destinationGameId", request.destinationGameId)
                    put("pokemonId", request.pokemonId)
                    put("isInParty", request.isInParty)
                    if (request.sourceBoxIndex != null) put("sourceBoxIndex", request.sourceBoxIndex)
                    put("sourceSlotIndex", request.sourceSlotIndex)
                    if (request.targetBoxIndex != null) put("targetBoxIndex", request.targetBoxIndex)
                    if (request.targetSlotIndex != null) put("targetSlotIndex", request.targetSlotIndex)
                    put("targetIsInParty", request.targetIsInParty)
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseOperationResponse(JSONObject(body))
            }
        }

    override suspend fun validateTransfer(
        session: JellyfinSession,
        request: PokemonTransferValidateRequest,
    ): Result<PokemonTransferCompatibilityResult> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Transfers/Validate",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("sourceGameId", request.sourceGameId)
                    put("destinationGameId", request.destinationGameId)
                    put("pokemonId", request.pokemonId)
                    put("isInParty", request.isInParty)
                    if (request.boxIndex != null) put("boxIndex", request.boxIndex)
                    put("slotIndex", request.slotIndex)
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val warnings = json.optJSONArray("warnings")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                val forbiddenMoves = json.optJSONArray("forbiddenMoves")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                val forbiddenItems = json.optJSONArray("forbiddenItems")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                val allowedGens = json.optJSONArray("allowedGenerations")?.let { arr ->
                    (0 until arr.length()).map { arr.getInt(it) }
                } ?: emptyList()

                PokemonTransferCompatibilityResult(
                    isCompatible = json.optBoolean("isCompatible", false),
                    reason = json.optString("reason", ""),
                    warnings = warnings,
                    isOneWay = json.optBoolean("isOneWay", false),
                    requiresItemRemoval = json.optBoolean("requiresItemRemoval", false),
                    forbiddenMoves = forbiddenMoves,
                    forbiddenItems = forbiddenItems,
                    allowedGenerations = allowedGens,
                )
            }
        }

    override suspend fun createTrade(
        session: JellyfinSession,
        request: CreateTradeRequest,
    ): Result<PokemonTradeOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Trades/Create",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    if (request.targetUserId != null) put("targetUserId", request.targetUserId)
                    if (request.targetUserName != null) put("targetUserName", request.targetUserName)
                    if (request.linkCode != null) put("linkCode", request.linkCode)
                    put("offer", tradeOfferToJson(request.offer))
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseTradeOperationResponse(JSONObject(body))
            }
        }

    override suspend fun joinLinkTrade(
        session: JellyfinSession,
        request: JoinLinkTradeRequest,
    ): Result<PokemonTradeOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Trades/Join",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("linkCode", request.linkCode)
                    put("offer", tradeOfferToJson(request.offer))
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseTradeOperationResponse(JSONObject(body))
            }
        }

    override suspend fun acceptTrade(
        session: JellyfinSession,
        request: AcceptTradeRequest,
    ): Result<PokemonTradeOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Trades/Accept",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("tradeId", request.tradeId)
                    put("counterOffer", tradeOfferToJson(request.counterOffer))
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseTradeOperationResponse(JSONObject(body))
            }
        }

    override suspend fun cancelTrade(
        session: JellyfinSession,
        request: CancelTradeRequest,
    ): Result<PokemonTradeOperationResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Trades/Cancel",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("tradeId", request.tradeId)
                    if (request.reason != null) put("reason", request.reason)
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseTradeOperationResponse(JSONObject(body))
            }
        }

    override suspend fun getPendingTrades(session: JellyfinSession): Result<List<PokemonTradeSession>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Trades/Pending")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<PokemonTradeSession>()
                for (i in 0 until array.length()) {
                    list.add(parseTradeSession(array.getJSONObject(i)))
                }
                list
            }
        }

    override suspend fun getTrade(session: JellyfinSession, tradeId: String): Result<PokemonTradeSession> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Trades/$tradeId")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseTradeSession(JSONObject(body))
            }
        }

    override suspend fun getTradeHistory(session: JellyfinSession): Result<List<PokemonTradeSession>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Trades/History")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<PokemonTradeSession>()
                for (i in 0 until array.length()) {
                    list.add(parseTradeSession(array.getJSONObject(i)))
                }
                list
            }
        }

    override suspend fun getGameBackups(
        session: JellyfinSession,
        libraryId: String,
        gameId: String,
    ): Result<List<PokemonBackupDto>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Games/$libraryId/$gameId/Backups")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<PokemonBackupDto>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PokemonBackupDto(
                            backupId = obj.optString("backupId", ""),
                            gameId = obj.optString("gameId", ""),
                            createdAtUtc = obj.optString("createdAtUtc", ""),
                            reason = obj.optString("reason", ""),
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            isRestored = obj.optBoolean("isRestored", false),
                            restoredAtUtc = obj.optString("restoredAtUtc", "").ifEmpty { null },
                        )
                    )
                }
                list
            }
        }

    override suspend fun restoreBackup(
        session: JellyfinSession,
        request: RestoreBackupRequest,
    ): Result<RestoreBackupResponse> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection(
                    pathAndQuery = "Vantafyn/Pokemon/Backups/Restore",
                    method = "POST",
                )
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().apply {
                    put("backupId", request.backupId)
                    put("gameId", request.gameId)
                }
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                RestoreBackupResponse(
                    isSuccess = json.optBoolean("isSuccess", false),
                    message = json.optString("message", ""),
                    backupId = json.optString("backupId", "").ifEmpty { null },
                    gameId = json.optString("gameId", "").ifEmpty { null },
                )
            }
        }

    override suspend fun getDiagnostics(session: JellyfinSession): Result<PokemonDiagnosticsDto> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Diagnostics")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val gens = mutableListOf<String>()
                val gensArray = json.optJSONArray("supportedGenerations")
                if (gensArray != null) {
                    for (i in 0 until gensArray.length()) {
                        gens.add(gensArray.getString(i))
                    }
                }
                PokemonDiagnosticsDto(
                    enabled = json.optBoolean("enabled", false),
                    providerType = json.optString("providerType", ""),
                    providerHealthy = json.optBoolean("providerHealthy", false),
                    providerMessage = json.optString("providerMessage", "").ifEmpty { null },
                    supportedGenerations = gens,
                    totalStoredPokemon = json.optInt("totalStoredPokemon", 0),
                    totalShinyPokemon = json.optInt("totalShinyPokemon", 0),
                    totalBackups = json.optInt("totalBackups", 0),
                    activeSessions = json.optInt("activeSessions", 0),
                    activeTrades = json.optInt("activeTrades", 0),
                )
            }
        }

    private fun tradeOfferToJson(offer: PokemonTradeOffer): JSONObject = JSONObject().apply {
        put("pokemonId", offer.pokemonId)
        put("species", offer.species)
        put("speciesId", offer.speciesId)
        put("nickname", offer.nickname)
        put("level", offer.level)
        put("isShiny", offer.isShiny)
        put("generation", offer.generation)
        if (offer.gameId != null) put("gameId", offer.gameId)
        put("isVault", offer.isVault)
        if (offer.boxIndex != null) put("boxIndex", offer.boxIndex)
        if (offer.slotIndex != null) put("slotIndex", offer.slotIndex)
        put("isInParty", offer.isInParty)
        if (offer.targetVaultBoxIndex != null) put("targetVaultBoxIndex", offer.targetVaultBoxIndex)
        if (offer.targetVaultSlotIndex != null) put("targetVaultSlotIndex", offer.targetVaultSlotIndex)
    }

    private fun parseTradeOffer(obj: JSONObject): PokemonTradeOffer = PokemonTradeOffer(
        pokemonId = obj.optString("pokemonId", ""),
        species = obj.optString("species", ""),
        speciesId = obj.optInt("speciesId", 0),
        nickname = obj.optString("nickname", ""),
        level = obj.optInt("level", 1),
        isShiny = obj.optBoolean("isShiny", false),
        generation = obj.optInt("generation", 0),
        gameId = obj.optString("gameId", "").ifEmpty { null },
        isVault = obj.optBoolean("isVault", true),
        boxIndex = if (obj.has("boxIndex") && !obj.isNull("boxIndex")) obj.optInt("boxIndex") else null,
        slotIndex = if (obj.has("slotIndex") && !obj.isNull("slotIndex")) obj.optInt("slotIndex") else null,
        isInParty = obj.optBoolean("isInParty", false),
        targetVaultBoxIndex = if (obj.has("targetVaultBoxIndex") && !obj.isNull("targetVaultBoxIndex")) obj.optInt("targetVaultBoxIndex") else null,
        targetVaultSlotIndex = if (obj.has("targetVaultSlotIndex") && !obj.isNull("targetVaultSlotIndex")) obj.optInt("targetVaultSlotIndex") else null,
    )

    private fun parseTradeSession(obj: JSONObject): PokemonTradeSession {
        val typeStr = obj.optString("type", "Direct")
        val type = if (typeStr.equals("LinkCode", ignoreCase = true) || obj.optInt("type", -1) == 1) PokemonTradeType.LinkCode else PokemonTradeType.Direct

        val statusStr = obj.optString("status", "Pending")
        val status = try {
            PokemonTradeStatus.valueOf(statusStr)
        } catch (_: Exception) {
            when (obj.optInt("status", 0)) {
                1 -> PokemonTradeStatus.Accepted
                2 -> PokemonTradeStatus.Completed
                3 -> PokemonTradeStatus.Cancelled
                4 -> PokemonTradeStatus.Expired
                5 -> PokemonTradeStatus.Failed
                else -> PokemonTradeStatus.Pending
            }
        }

        val initOfferObj = obj.optJSONObject("initiatorOffer") ?: JSONObject()
        val targetOfferObj = obj.optJSONObject("targetOffer")

        return PokemonTradeSession(
            id = obj.optString("id", ""),
            type = type,
            status = status,
            initiatorUserId = obj.optString("initiatorUserId", ""),
            initiatorUserName = obj.optString("initiatorUserName", ""),
            targetUserId = obj.optString("targetUserId", "").ifEmpty { null },
            targetUserName = obj.optString("targetUserName", "").ifEmpty { null },
            linkCode = obj.optString("linkCode", "").ifEmpty { null },
            initiatorOffer = parseTradeOffer(initOfferObj),
            targetOffer = targetOfferObj?.let { parseTradeOffer(it) },
            createdAtUtc = obj.optString("createdAtUtc", ""),
            completedAtUtc = obj.optString("completedAtUtc", "").ifEmpty { null },
            cancellationReason = obj.optString("cancellationReason", "").ifEmpty { null },
            transactionId = obj.optString("transactionId", "").ifEmpty { null },
        )
    }

    private fun parseTradeOperationResponse(obj: JSONObject): PokemonTradeOperationResponse =
        PokemonTradeOperationResponse(
            isSuccess = obj.optBoolean("isSuccess", false),
            message = obj.optString("message", ""),
            tradeSession = obj.optJSONObject("tradeSession")?.let { parseTradeSession(it) },
            transactionId = obj.optString("transactionId", "").ifEmpty { null },
        )


    private fun parseOperationResponse(json: JSONObject): PokemonOperationResponse {
        val warnings = json.optJSONArray("warnings")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        return PokemonOperationResponse(
            isSuccess = json.optBoolean("isSuccess", false),
            operationType = json.optString("operationType", ""),
            message = json.optString("message", ""),
            transactionId = json.optString("transactionId", "").ifEmpty { null },
            affectedPokemonId = json.optString("affectedPokemonId", "").ifEmpty { null },
            targetLocation = json.optString("targetLocation", "").ifEmpty { null },
            backupCreated = json.optBoolean("backupCreated", false),
            backupId = json.optString("backupId", "").ifEmpty { null },
            auditId = json.optString("auditId", "").ifEmpty { null },
            warnings = warnings,
        )
    }

    private fun parseVaultEntry(obj: JSONObject): PokemonVaultEntry {
        return PokemonVaultEntry(
            id = obj.optString("id", ""),
            boxIndex = obj.optInt("boxIndex", 1),
            slotIndex = obj.optInt("slotIndex", 1),
            species = obj.optString("species", ""),
            speciesId = obj.optInt("speciesId", 0),
            form = obj.optString("form", "").ifEmpty { null },
            nickname = obj.optString("nickname", ""),
            level = obj.optInt("level", 1),
            gender = obj.optString("gender", "").ifEmpty { null },
            isShiny = obj.optBoolean("isShiny", false),
            generation = obj.optInt("generation", 0),
            originalTrainer = obj.optString("originalTrainer", ""),
            originalTrainerId = obj.optString("originalTrainerId", "").ifEmpty { null },
            originGame = obj.optString("originGame", ""),
            originGameId = obj.optString("originGameId", "").ifEmpty { null },
            currentLocation = obj.optString("currentLocation", ""),
            depositedAtUtc = obj.optString("depositedAtUtc", "").ifEmpty { null },
            details = obj.optJSONObject("details")?.let { parsePokemonDetails(it) },
        )
    }

    private fun parsePokemonSummary(obj: JSONObject): PokemonSummaryDto {
        return PokemonSummaryDto(
            id = obj.optString("id", ""),
            species = obj.optString("species", ""),
            speciesId = obj.optInt("speciesId", 0),
            form = obj.optString("form", "").ifEmpty { null },
            nickname = obj.optString("nickname", ""),
            level = obj.optInt("level", 1),
            gender = obj.optString("gender", "").ifEmpty { null },
            isShiny = obj.optBoolean("isShiny", false),
            originalTrainer = obj.optString("originalTrainer", "").ifEmpty { null },
            originalTrainerId = obj.optString("originalTrainerId", "").ifEmpty { null },
            originGame = obj.optString("originGame", "").ifEmpty { null },
            currentGame = obj.optString("currentGame", "").ifEmpty { null },
            currentLocation = obj.optString("currentLocation", ""),
            boxIndex = if (obj.has("boxIndex")) obj.optInt("boxIndex") else null,
            slotIndex = obj.optInt("slotIndex", 1),
            isInParty = obj.optBoolean("isInParty", false),
            legalityStatus = obj.optString("legalityStatus", "valid"),
        )
    }

    private fun parsePokemonDetails(obj: JSONObject): PokemonDetailsDto {
        val summaryObj = obj.optJSONObject("summary")
        val summary = if (summaryObj != null) parsePokemonSummary(summaryObj) else PokemonSummaryDto()
        val moves = obj.optJSONArray("moves")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        return PokemonDetailsDto(
            summary = summary,
            nature = obj.optString("nature", "").ifEmpty { null },
            ability = obj.optString("ability", "").ifEmpty { null },
            heldItem = obj.optString("heldItem", "").ifEmpty { null },
            moves = moves,
            iv = obj.optJSONObject("iv")?.let { parseStats(it) },
            ev = obj.optJSONObject("ev")?.let { parseStats(it) },
            currentHp = if (obj.has("currentHp")) obj.optInt("currentHp") else null,
            maxHp = if (obj.has("maxHp")) obj.optInt("maxHp") else null,
            friendship = if (obj.has("friendship")) obj.optInt("friendship") else null,
            pokeball = obj.optString("pokeball", "").ifEmpty { null },
            rawData = obj.optString("rawData", "").ifEmpty { null },
        )
    }

    private fun parseStats(obj: JSONObject): PokemonStatsDto {
        return PokemonStatsDto(
            hp = obj.optInt("hp", 0),
            attack = obj.optInt("attack", 0),
            defense = obj.optInt("defense", 0),
            specialAttack = obj.optInt("specialAttack", 0),
            specialDefense = obj.optInt("specialDefense", 0),
            speed = obj.optInt("speed", 0),
        )
    }

    private fun parseGameSaveDto(json: JSONObject): PokemonGameSaveDto {
        val partyList = mutableListOf<PokemonSummaryDto>()
        val partyArr = json.optJSONArray("party")
        if (partyArr != null) {
            for (i in 0 until partyArr.length()) {
                partyList.add(parsePokemonSummary(partyArr.getJSONObject(i)))
            }
        }

        val boxesList = mutableListOf<PokemonBoxDto>()
        val boxesArr = json.optJSONArray("boxes")
        if (boxesArr != null) {
            for (i in 0 until boxesArr.length()) {
                val boxObj = boxesArr.getJSONObject(i)
                val entriesArr = boxObj.optJSONArray("entries")
                val entries = mutableListOf<PokemonSummaryDto>()
                if (entriesArr != null) {
                    for (j in 0 until entriesArr.length()) {
                        entries.add(parsePokemonSummary(entriesArr.getJSONObject(j)))
                    }
                }
                boxesList.add(
                    PokemonBoxDto(
                        boxIndex = boxObj.optInt("boxIndex", i + 1),
                        name = boxObj.optString("name", "Box ${i + 1}"),
                        capacity = boxObj.optInt("capacity", 30),
                        occupiedCount = boxObj.optInt("occupiedCount", entries.size),
                        entries = entries,
                    )
                )
            }
        }

        return PokemonGameSaveDto(
            gameId = json.optString("gameId", ""),
            title = json.optString("title", ""),
            platform = json.optString("platform", ""),
            generation = json.optInt("generation", 0),
            trainerName = json.optString("trainerName", "").ifEmpty { null },
            trainerId = json.optString("trainerId", "").ifEmpty { null },
            money = if (json.has("money")) json.optInt("money") else null,
            pokedexSeen = if (json.has("pokedexSeen")) json.optInt("pokedexSeen") else null,
            pokedexCaught = if (json.has("pokedexCaught")) json.optInt("pokedexCaught") else null,
            saveFound = json.optBoolean("saveFound", false),
            providerAvailable = json.optBoolean("providerAvailable", false),
            errorMessage = json.optString("errorMessage", "").ifEmpty { null },
            party = partyList,
            boxes = boxesList,
            totalPokemonCount = json.optInt("totalPokemonCount", 0),
            shinyCount = json.optInt("shinyCount", 0),
        )
    }

    private fun parseGameSummary(obj: JSONObject, session: JellyfinSession): GameSummary {
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
        val system = obj.optString("system", obj.optString("systemId", ""))
        val size = if (obj.has("sizeBytes")) obj.optLong("sizeBytes", 0L) else obj.optLong("size", 0L)

        val pObj = obj.optJSONObject("pokemon")
        val pMeta = if (pObj != null) {
            GamePokemonMetadata(
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
        } else null

        return GameSummary(
            id = id,
            title = obj.optString("title", ""),
            systemId = system,
            filename = filename,
            sizeBytes = size,
            token = token,
            extension = extension,
            boxartUrl = fullBoxartUrl,
            pokemon = pMeta,
        )
    }

    override suspend fun getPokedex(session: JellyfinSession): Result<PokemonPokedexDto> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Pokedex")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val genArray = json.optJSONArray("generationProgress")
                val genProgress = mutableListOf<PokemonPokedexGenerationProgressDto>()
                if (genArray != null) {
                    for (i in 0 until genArray.length()) {
                        val g = genArray.getJSONObject(i)
                        genProgress.add(
                            PokemonPokedexGenerationProgressDto(
                                generation = g.optInt("generation", 1),
                                generationName = g.optString("generationName", ""),
                                minDexNumber = g.optInt("minDexNumber", 1),
                                maxDexNumber = g.optInt("maxDexNumber", 151),
                                totalSpecies = g.optInt("totalSpecies", 151),
                                caughtCount = g.optInt("caughtCount", 0),
                                seenCount = g.optInt("seenCount", 0),
                                shinyCount = g.optInt("shinyCount", 0),
                                caughtPercentage = g.optDouble("caughtPercentage", 0.0),
                            )
                        )
                    }
                }

                val entriesArray = json.optJSONArray("entries")
                val entries = mutableListOf<PokemonPokedexEntryDto>()
                if (entriesArray != null) {
                    for (i in 0 until entriesArray.length()) {
                        val e = entriesArray.getJSONObject(i)
                        entries.add(
                            PokemonPokedexEntryDto(
                                speciesId = e.optInt("speciesId", 0),
                                speciesName = e.optString("speciesName", ""),
                                generation = e.optInt("generation", 1),
                                isCaught = e.optBoolean("isCaught", false),
                                isSeen = e.optBoolean("isSeen", false),
                                hasShiny = e.optBoolean("hasShiny", false),
                                firstEncounteredGame = e.optString("firstEncounteredGame", "").ifEmpty { null },
                                firstEncounteredTimestamp = e.optString("firstEncounteredTimestamp", "").ifEmpty { null },
                                encounterCount = e.optInt("encounterCount", 0),
                            )
                        )
                    }
                }

                PokemonPokedexDto(
                    userId = json.optString("userId", ""),
                    isEnabled = json.optBoolean("isEnabled", true),
                    totalCaught = json.optInt("totalCaught", 0),
                    totalSeen = json.optInt("totalSeen", 0),
                    totalShinies = json.optInt("totalShinies", 0),
                    lastUpdatedUtc = json.optString("lastUpdatedUtc", "").ifEmpty { null },
                    generationProgress = genProgress,
                    entries = entries,
                )
            }
        }

    override suspend fun getPokemonJourney(session: JellyfinSession, pokemonId: String): Result<PokemonJourneyDto> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Journey/$pokemonId")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val stepsArray = json.optJSONArray("steps")
                val steps = mutableListOf<PokemonJourneyStepDto>()
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        val s = stepsArray.getJSONObject(i)
                        steps.add(
                            PokemonJourneyStepDto(
                                timestamp = s.optString("timestamp", ""),
                                action = s.optString("action", ""),
                                sourceLocation = s.optString("sourceLocation", "").ifEmpty { null },
                                destinationLocation = s.optString("destinationLocation", "").ifEmpty { null },
                                details = s.optString("details", "").ifEmpty { null },
                                gameTitle = s.optString("gameTitle", "").ifEmpty { null },
                                generation = if (s.has("generation")) s.optInt("generation") else null,
                            )
                        )
                    }
                }

                PokemonJourneyDto(
                    pokemonId = json.optString("pokemonId", pokemonId),
                    species = json.optString("species", ""),
                    speciesId = json.optInt("speciesId", 0),
                    nickname = json.optString("nickname", "").ifEmpty { null },
                    level = json.optInt("level", 1),
                    isShiny = json.optBoolean("isShiny", false),
                    originGame = json.optString("originGame", "").ifEmpty { null },
                    originalTrainer = json.optString("originalTrainer", "").ifEmpty { null },
                    originalTrainerId = json.optString("originalTrainerId", "").ifEmpty { null },
                    steps = steps,
                )
            }
        }

    override suspend fun getAchievements(session: JellyfinSession): Result<PokemonAchievementsSummaryDto> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Achievements")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val achArray = json.optJSONArray("achievements")
                val achievements = mutableListOf<PokemonAchievementDto>()
                if (achArray != null) {
                    for (i in 0 until achArray.length()) {
                        val a = achArray.getJSONObject(i)
                        achievements.add(
                            PokemonAchievementDto(
                                id = a.optString("id", ""),
                                title = a.optString("title", ""),
                                description = a.optString("description", ""),
                                category = a.optString("category", "Pokemon"),
                                rarity = a.optString("rarity", "Common"),
                                score = a.optInt("score", 0),
                                iconName = a.optString("iconName", "catching_pokemon"),
                                isUnlocked = a.optBoolean("isUnlocked", false),
                                unlockedAtUtc = a.optString("unlockedAtUtc", "").ifEmpty { null },
                                currentProgress = a.optInt("currentProgress", 0),
                                maxProgress = a.optInt("maxProgress", 0),
                                progressPercentage = a.optDouble("progressPercentage", 0.0),
                            )
                        )
                    }
                }

                PokemonAchievementsSummaryDto(
                    userId = json.optString("userId", ""),
                    totalScore = json.optInt("totalScore", 0),
                    unlockedCount = json.optInt("unlockedCount", 0),
                    totalCount = json.optInt("totalCount", 0),
                    achievements = achievements,
                )
            }
        }

    override suspend fun getSocialActivity(session: JellyfinSession, limit: Int): Result<List<PokemonSocialActivityEvent>> =
        withContext(ioDispatcher) {
            runCatching {
                val conn = session.openAuthenticatedConnection("Vantafyn/Pokemon/Social/Activity?limit=$limit")
                checkResponseCode(conn)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)

                val list = mutableListOf<PokemonSocialActivityEvent>()
                for (i in 0 until array.length()) {
                    val e = array.getJSONObject(i)
                    list.add(
                        PokemonSocialActivityEvent(
                            id = e.optString("id", ""),
                            userId = e.optString("userId", ""),
                            userName = e.optString("userName", ""),
                            eventType = e.optString("eventType", ""),
                            title = e.optString("title", ""),
                            description = e.optString("description", ""),
                            speciesId = if (e.has("speciesId") && !e.isNull("speciesId")) e.optInt("speciesId") else null,
                            speciesName = e.optString("speciesName", "").ifEmpty { null },
                            isShiny = e.optBoolean("isShiny", false),
                            timestampUtc = e.optString("timestampUtc", ""),
                        )
                    )
                }
                list
            }
        }

    private fun checkResponseCode(conn: HttpURLConnection) {
        val code = conn.responseCode
        if (code !in 200..299) {
            val errorBody = runCatching {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            throw IllegalStateException("Companion Pokémon API call failed with HTTP $code: ${errorBody ?: conn.responseMessage}")
        }
    }
}
