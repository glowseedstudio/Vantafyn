package dev.vantafyn.core.jellyfin

import java.io.Serializable

data class PokemonIntegrationStatus(
    val enabled: Boolean = false,
    val provider: String = "none",
    val providerHealthy: Boolean = false,
    val providerMessage: String? = null,
    val supportedGenerations: List<Int> = emptyList(),
    val vaultAvailable: Boolean = false,
    val transfersAvailable: Boolean = false,
    val crossGenerationAvailable: Boolean = false,
    val tradingAvailable: Boolean = false,
) : Serializable

data class PokemonVaultSummary(
    val totalCapacity: Int = 900,
    val totalOccupied: Int = 0,
    val boxCount: Int = 30,
    val shinyCount: Int = 0,
    val speciesCount: Int = 0,
) : Serializable

data class PokemonVaultBoxSummary(
    val boxIndex: Int = 1,
    val name: String = "",
    val capacity: Int = 30,
    val occupiedCount: Int = 0,
    val shinyCount: Int = 0,
) : Serializable

data class PokemonVaultEntry(
    val id: String = "",
    val boxIndex: Int = 1,
    val slotIndex: Int = 1,
    val species: String = "",
    val speciesId: Int = 0,
    val form: String? = null,
    val nickname: String = "",
    val level: Int = 1,
    val gender: String? = null,
    val isShiny: Boolean = false,
    val generation: Int = 0,
    val originalTrainer: String = "",
    val originalTrainerId: String? = null,
    val originGame: String = "",
    val originGameId: String? = null,
    val currentLocation: String = "",
    val depositedAtUtc: String? = null,
    val details: PokemonDetailsDto? = null,
) : Serializable {
    fun toSummaryDto(): PokemonSummaryDto = PokemonSummaryDto(
        id = id,
        species = species,
        speciesId = speciesId,
        form = form,
        nickname = nickname,
        level = level,
        gender = gender,
        isShiny = isShiny,
        originalTrainer = originalTrainer,
        originalTrainerId = originalTrainerId,
        originGame = originGame,
        currentLocation = currentLocation,
        boxIndex = boxIndex,
        slotIndex = slotIndex,
        isInParty = false,
        legalityStatus = details?.legalityStatus ?: "valid",
        isHallOfFameMember = details?.isHallOfFameMember ?: false,
    )
}

data class PokemonVaultBox(
    val boxIndex: Int = 1,
    val name: String = "",
    val entries: List<PokemonVaultEntry> = emptyList(),
) : Serializable

data class SaveLockStateDto(
    val isLocked: Boolean = false,
    val lockReason: String? = null,
    val activeSessionCount: Int = 0,
    val activeClients: List<String> = emptyList(),
) : Serializable

data class PokemonOperationResponse(
    val isSuccess: Boolean = false,
    val operationType: String = "",
    val message: String = "",
    val transactionId: String? = null,
    val affectedPokemonId: String? = null,
    val targetLocation: String? = null,
    val backupCreated: Boolean = false,
    val backupId: String? = null,
    val auditId: String? = null,
    val warnings: List<String> = emptyList(),
) : Serializable

data class PokemonTransferCompatibilityResult(
    val isCompatible: Boolean = false,
    val reason: String = "",
    val warnings: List<String> = emptyList(),
    val isOneWay: Boolean = false,
    val requiresItemRemoval: Boolean = false,
    val forbiddenMoves: List<String> = emptyList(),
    val forbiddenItems: List<String> = emptyList(),
    val allowedGenerations: List<Int> = emptyList(),
) : Serializable

data class PokemonDepositRequest(
    val gameId: String,
    val pokemonId: String,
    val isInParty: Boolean = false,
    val boxIndex: Int? = null,
    val slotIndex: Int = 1,
    val targetVaultBoxIndex: Int = 1,
    val targetVaultSlotIndex: Int? = null,
) : Serializable

data class PokemonWithdrawRequest(
    val vaultEntryId: String,
    val targetGameId: String,
    val targetBoxIndex: Int? = null,
    val targetSlotIndex: Int? = null,
    val targetIsInParty: Boolean = false,
) : Serializable

data class PokemonDirectTransferRequest(
    val sourceGameId: String,
    val destinationGameId: String,
    val pokemonId: String,
    val isInParty: Boolean = false,
    val sourceBoxIndex: Int? = null,
    val sourceSlotIndex: Int = 1,
    val targetBoxIndex: Int? = null,
    val targetSlotIndex: Int? = null,
    val targetIsInParty: Boolean = false,
) : Serializable

data class PokemonTransferValidateRequest(
    val sourceGameId: String,
    val destinationGameId: String,
    val pokemonId: String,
    val isInParty: Boolean = false,
    val boxIndex: Int? = null,
    val slotIndex: Int = 1,
) : Serializable

enum class PokemonTradeStatus {
    Pending,
    Accepted,
    Completed,
    Cancelled,
    Expired,
    Failed
}

enum class PokemonTradeType {
    Direct,
    LinkCode
}

data class PokemonTradeOffer(
    val pokemonId: String = "",
    val species: String = "",
    val speciesId: Int = 0,
    val nickname: String = "",
    val level: Int = 1,
    val isShiny: Boolean = false,
    val generation: Int = 0,
    val gameId: String? = null,
    val isVault: Boolean = true,
    val boxIndex: Int? = null,
    val slotIndex: Int? = null,
    val isInParty: Boolean = false,
    val targetVaultBoxIndex: Int? = null,
    val targetVaultSlotIndex: Int? = null,
) : Serializable

data class PokemonTradeSession(
    val id: String = "",
    val type: PokemonTradeType = PokemonTradeType.Direct,
    val status: PokemonTradeStatus = PokemonTradeStatus.Pending,
    val initiatorUserId: String = "",
    val initiatorUserName: String = "",
    val targetUserId: String? = null,
    val targetUserName: String? = null,
    val linkCode: String? = null,
    val initiatorOffer: PokemonTradeOffer = PokemonTradeOffer(),
    val targetOffer: PokemonTradeOffer? = null,
    val createdAtUtc: String = "",
    val completedAtUtc: String? = null,
    val cancellationReason: String? = null,
    val transactionId: String? = null,
) : Serializable

data class CreateTradeRequest(
    val targetUserId: String? = null,
    val targetUserName: String? = null,
    val linkCode: String? = null,
    val offer: PokemonTradeOffer,
) : Serializable

data class JoinLinkTradeRequest(
    val linkCode: String,
    val offer: PokemonTradeOffer,
) : Serializable

data class AcceptTradeRequest(
    val tradeId: String,
    val counterOffer: PokemonTradeOffer,
) : Serializable

data class CancelTradeRequest(
    val tradeId: String,
    val reason: String? = null,
) : Serializable

data class PokemonTradeOperationResponse(
    val isSuccess: Boolean = false,
    val message: String = "",
    val tradeSession: PokemonTradeSession? = null,
    val transactionId: String? = null,
) : Serializable

data class PokemonBackupDto(
    val backupId: String = "",
    val gameId: String = "",
    val createdAtUtc: String = "",
    val reason: String = "",
    val sizeBytes: Long = 0L,
    val isRestored: Boolean = false,
    val restoredAtUtc: String? = null,
) : Serializable

data class RestoreBackupRequest(
    val backupId: String,
    val gameId: String,
) : Serializable

data class RestoreBackupResponse(
    val isSuccess: Boolean = false,
    val message: String = "",
    val backupId: String? = null,
    val gameId: String? = null,
) : Serializable

data class PokemonDiagnosticsDto(
    val enabled: Boolean = false,
    val providerType: String = "",
    val providerHealthy: Boolean = false,
    val providerMessage: String? = null,
    val supportedGenerations: List<String> = emptyList(),
    val totalStoredPokemon: Int = 0,
    val totalShinyPokemon: Int = 0,
    val totalBackups: Int = 0,
    val activeSessions: Int = 0,
    val activeTrades: Int = 0,
) : Serializable

data class PokemonJourneyStepDto(
    val timestamp: String = "",
    val action: String = "",
    val sourceLocation: String? = null,
    val destinationLocation: String? = null,
    val details: String? = null,
    val gameTitle: String? = null,
    val generation: Int? = null,
) : Serializable

data class PokemonJourneyDto(
    val pokemonId: String = "",
    val species: String = "",
    val speciesId: Int = 0,
    val nickname: String? = null,
    val level: Int = 1,
    val isShiny: Boolean = false,
    val originGame: String? = null,
    val originalTrainer: String? = null,
    val originalTrainerId: String? = null,
    val steps: List<PokemonJourneyStepDto> = emptyList(),
) : Serializable

data class PokemonPokedexEntryDto(
    val speciesId: Int = 0,
    val speciesName: String = "",
    val generation: Int = 1,
    val isCaught: Boolean = false,
    val isSeen: Boolean = false,
    val hasShiny: Boolean = false,
    val firstEncounteredGame: String? = null,
    val firstEncounteredTimestamp: String? = null,
    val encounterCount: Int = 0,
) : Serializable

data class PokemonPokedexGenerationProgressDto(
    val generation: Int = 1,
    val generationName: String = "",
    val minDexNumber: Int = 1,
    val maxDexNumber: Int = 151,
    val totalSpecies: Int = 151,
    val caughtCount: Int = 0,
    val seenCount: Int = 0,
    val shinyCount: Int = 0,
    val caughtPercentage: Double = 0.0,
) : Serializable

data class PokemonPokedexDto(
    val userId: String = "",
    val isEnabled: Boolean = true,
    val totalCaught: Int = 0,
    val totalSeen: Int = 0,
    val totalShinies: Int = 0,
    val lastUpdatedUtc: String? = null,
    val generationProgress: List<PokemonPokedexGenerationProgressDto> = emptyList(),
    val entries: List<PokemonPokedexEntryDto> = emptyList(),
) : Serializable

data class PokemonSocialActivityEvent(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val eventType: String = "",
    val title: String = "",
    val description: String = "",
    val speciesId: Int? = null,
    val speciesName: String? = null,
    val isShiny: Boolean = false,
    val timestampUtc: String = "",
) : Serializable

data class PokemonAchievementDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Pokemon",
    val rarity: String = "Common",
    val score: Int = 0,
    val iconName: String = "catching_pokemon",
    val isUnlocked: Boolean = false,
    val unlockedAtUtc: String? = null,
    val currentProgress: Int = 0,
    val maxProgress: Int = 0,
    val progressPercentage: Double = 0.0,
) : Serializable

data class PokemonAchievementsSummaryDto(
    val userId: String = "",
    val totalScore: Int = 0,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val achievements: List<PokemonAchievementDto> = emptyList(),
) : Serializable

