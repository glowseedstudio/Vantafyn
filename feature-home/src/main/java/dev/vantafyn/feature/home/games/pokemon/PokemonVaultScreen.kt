package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.DefaultJellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonDepositRequest
import dev.vantafyn.core.jellyfin.PokemonBulkDepositRequest
import dev.vantafyn.core.jellyfin.PokemonBulkWithdrawRequest
import dev.vantafyn.core.jellyfin.PokemonBulkDirectTransferRequest
import dev.vantafyn.core.jellyfin.PokemonExternalSavePreview
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonDirectTransferRequest
import dev.vantafyn.core.jellyfin.PokemonIntegrationStatus
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.jellyfin.PokemonTransferCompatibilityResult
import dev.vantafyn.core.jellyfin.PokemonTransferValidateRequest
import androidx.compose.ui.platform.LocalContext
import dev.vantafyn.core.jellyfin.DefaultJellyfinGamesRepository
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.feature.player.games.GameStorageManager
import dev.vantafyn.feature.player.games.SaveSyncStatus
import dev.vantafyn.core.jellyfin.PokemonVaultSummary
import dev.vantafyn.core.jellyfin.PokemonVaultMoveRequest
import dev.vantafyn.core.jellyfin.PokemonWithdrawRequest
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal
import dev.vantafyn.feature.home.games.gamesTabTransitionSpec
import dev.vantafyn.feature.home.rememberReducedMotionPreference
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VaultSubScreen {
    Home,
    BoxTransfer,
    BadgeCase,
    DiplomaCase,
    EventVault,
}

data class BulkTransferConfirmation(
    val direction: TransferSide,
    val count: Int,
    val fromName: String,
    val toName: String,
    val selectedIds: List<String>? = null,
)

@Composable
fun PokemonVaultScreen(
    session: JellyfinSession?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    allGames: List<GameSummary> = emptyList(),
    vaultHomeTrigger: Long = 0L,
    pokemonRepository: JellyfinPokemonRepository = remember { DefaultJellyfinPokemonRepository() },
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val gamesRepository = remember { DefaultJellyfinGamesRepository() }
    val storageManager = remember(context) { GameStorageManager(context, gamesRepository) }
    val reducedMotion = rememberReducedMotionPreference()

    var subScreen by remember { mutableStateOf(VaultSubScreen.Home) }

    BackHandler(enabled = subScreen != VaultSubScreen.Home) {
        subScreen = VaultSubScreen.Home
    }

    var availableGames by remember { mutableStateOf<List<GameSummary>>(emptyList()) }
    var vaultSummary by remember { mutableStateOf<PokemonVaultSummary?>(null) }
    var integrationStatus by remember { mutableStateOf<PokemonIntegrationStatus?>(null) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Upper (Source) State
    var upperState by remember {
        mutableStateOf(
            ContainerUiState(
                side = TransferSide.Source,
                containerType = StorageContainerType.PersonalVault,
                selectedBoxIndex = 1,
            )
        )
    }
    var upperSelectedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var isUpperMultiSelectMode by remember { mutableStateOf(false) }
    var upperMultiSelectedSlots by remember { mutableStateOf(emptySet<Int>()) }

    // Lower (Destination) State
    var lowerState by remember {
        mutableStateOf(
            ContainerUiState(
                side = TransferSide.Destination,
                containerType = StorageContainerType.PersonalVault,
                selectedBoxIndex = 2,
            )
        )
    }
    var lowerSelectedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var isLowerMultiSelectMode by remember { mutableStateOf(false) }
    var lowerMultiSelectedSlots by remember { mutableStateOf(emptySet<Int>()) }

    var bulkTransferConfirmation by remember { mutableStateOf<BulkTransferConfirmation?>(null) }

    // Active Selection & Inspection
    var selectedPokemonItem by remember { mutableStateOf<SelectedPokemonItem?>(null) }
    var inspectedPokemon by remember { mutableStateOf<PokemonSummaryDto?>(null) }
    var inspectedPokemonDetails by remember { mutableStateOf<PokemonDetailsDto?>(null) }
    var inspectedPokemonGameId by remember { mutableStateOf<String?>(null) }
    var inspectedPokemonIsVault by remember { mutableStateOf<Boolean?>(null) }
    var inspectedPokemonNavigationList by remember { mutableStateOf<List<PokemonSummaryDto>>(emptyList()) }
    var inspectedPokemonIndex by remember { mutableIntStateOf(-1) }
    var compatibilityResult by remember { mutableStateOf<PokemonTransferCompatibilityResult?>(null) }
    var isValidatingCompatibility by remember { mutableStateOf(false) }
    var isExecutingTransfer by remember { mutableStateOf(false) }
    var operationResultMessage by remember { mutableStateOf<String?>(null) }
    var externalSavePreview by remember { mutableStateOf<PokemonExternalSavePreview?>(null) }
    var selectedExternalPokemonIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isTradeModalOpen by remember { mutableStateOf(false) }
    var isBackupRestoreModalOpen by remember { mutableStateOf(false) }
    var isPokedexModalOpen by remember { mutableStateOf(false) }
    var isAchievementsModalOpen by remember { mutableStateOf(false) }
    var remoteVaultBoxes by remember { mutableStateOf<List<dev.vantafyn.core.jellyfin.PokemonVaultBox>>(emptyList()) }

    LaunchedEffect(vaultHomeTrigger) {
        if (vaultHomeTrigger > 0L) {
            subScreen = VaultSubScreen.Home
            isTradeModalOpen = false
            isBackupRestoreModalOpen = false
            isPokedexModalOpen = false
            isAchievementsModalOpen = false
            inspectedPokemon = null
            inspectedPokemonDetails = null
            inspectedPokemonGameId = null
            inspectedPokemonIsVault = null
            inspectedPokemonNavigationList = emptyList()
            inspectedPokemonIndex = -1
        }
    }

    fun loadVaultSummary() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            pokemonRepository.getVaultSummary(session).onSuccess { summary ->
                val effectiveCount = maxOf(summary.boxCount, 100)
                vaultSummary = summary.copy(
                    totalCapacity = maxOf(summary.totalCapacity, 3000),
                    boxCount = effectiveCount,
                )
                upperState = upperState.copy(vaultBoxCount = effectiveCount)
                lowerState = lowerState.copy(vaultBoxCount = effectiveCount)
            }
            pokemonRepository.getAllVaultBoxes(session).onSuccess { boxes ->
                remoteVaultBoxes = boxes
            }
        }
    }

    fun loadContainerData(side: TransferSide) {
        if (session == null) return
        val isUpper = side == TransferSide.Source
        val current = if (isUpper) upperState else lowerState

        coroutineScope.launch(Dispatchers.IO) {
            if (isUpper) {
                upperState = upperState.copy(isLoading = true)
            } else {
                lowerState = lowerState.copy(isLoading = true)
            }

            when (val target = current.containerType) {
                is StorageContainerType.PersonalVault -> {
                    pokemonRepository.getVaultBox(session, current.selectedBoxIndex).fold(
                        onSuccess = { box ->
                            if (isUpper) {
                                upperState = upperState.copy(vaultBox = box, isLoading = false, errorMessage = null)
                            } else {
                                lowerState = lowerState.copy(vaultBox = box, isLoading = false, errorMessage = null)
                            }
                        },
                        onFailure = { err ->
                            if (isUpper) {
                                upperState = upperState.copy(isLoading = false, errorMessage = err.message)
                            } else {
                                lowerState = lowerState.copy(isLoading = false, errorMessage = err.message)
                            }
                        }
                    )
                }
                is StorageContainerType.GameCartridge -> {
                    val gameId = target.game.id
                    val safeGameId = gameId.replace(Regex("[^a-zA-Z0-9_-]"), "_")

                    // 1. Locate local save file (.sram, .sav, .srm, .main, etc.)
                    val localSaveFile = storageManager.findExistingLocalSaveFile(gameId, GameSaveKind.Sram, target.game.title)

                    // 2. Query companion plugin for game save and lock state
                    var saveDto: dev.vantafyn.core.jellyfin.PokemonGameSaveDto? = null
                    var lockState: dev.vantafyn.core.jellyfin.SaveLockStateDto? = null
                    var errorMsg: String? = null

                    if (session != null) {
                        val libId = "default"
                        val saveRes = pokemonRepository.getGameSave(session, libId, gameId)
                        val lockRes = pokemonRepository.getGameLockState(session, libId, gameId)
                        saveDto = saveRes.getOrNull()
                        lockState = lockRes.getOrNull()
                        val targetPokemon = target.game.pokemon
                        if (saveDto == null || !saveDto.saveFound || !saveDto.providerAvailable) {
                            errorMsg = saveRes.exceptionOrNull()?.message
                        } else if (targetPokemon != null) {
                            val genMismatch = targetPokemon.generation > 0 && saveDto.generation > 0 && saveDto.generation != targetPokemon.generation
                            val platMismatch = targetPokemon.platform.isNotBlank() && saveDto.platform.isNotBlank() && !saveDto.platform.equals(targetPokemon.platform, ignoreCase = true)
                            if (genMismatch || platMismatch) {
                                saveDto = null
                            }
                        }
                    }

                    // 3. Fallback to native client-side parsing if server is offline, missing save, or returned empty/unavailable
                    val isServerEmpty = saveDto?.party?.isEmpty() == true && saveDto.boxes.all { it.entries.isEmpty() }
                    if (saveDto == null || !saveDto.saveFound || !saveDto.providerAvailable || isServerEmpty) {
                        val saveBytes = if (localSaveFile.exists() && localSaveFile.length() > 0L) {
                            runCatching { localSaveFile.readBytes() }.getOrNull()
                        } else if (session != null) {
                            storageManager.loadSaveState(session, gameId, GameSaveKind.Sram)
                        } else null

                        if (saveBytes != null && saveBytes.isNotEmpty()) {
                            val meta = target.game.pokemon
                            val localParsed = dev.vantafyn.core.jellyfin.PokemonNativeSaveParser.parse(
                                saveBytes = saveBytes,
                                gameTitle = target.game.title,
                                gameId = meta?.pokemonGameId?.ifBlank { null } ?: gameId,
                                expectedGeneration = meta?.generation ?: 0,
                                expectedPlatform = meta?.platform ?: "",
                            )
                            if (localParsed != null && (localParsed.party.isNotEmpty() || localParsed.boxes.any { it.entries.isNotEmpty() } || localParsed.gymBadges.isNotEmpty())) {
                                saveDto = localParsed.copy(
                                    gameId = gameId,
                                    title = target.game.title,
                                )
                                errorMsg = null
                            }
                        }
                    }

                    if (isUpper) {
                        upperState = upperState.copy(
                            gameSave = saveDto,
                            lockState = lockState,
                            isLoading = false,
                            errorMessage = errorMsg,
                        )
                    } else {
                        lowerState = lowerState.copy(
                            gameSave = saveDto,
                            lockState = lockState,
                            isLoading = false,
                            errorMessage = errorMsg,
                        )
                    }
                }
            }
        }
    }

    fun loadAll() {
        coroutineScope.launch(Dispatchers.IO) {
            isLoadingInitial = true
            if (session != null) {
                pokemonRepository.getStatus(session).onSuccess { st ->
                    integrationStatus = st
                }
            }
            val serverGames = if (session != null) {
                pokemonRepository.getPokemonGames(session).getOrDefault(emptyList())
            } else emptyList()

            val local3ds = storageManager.scanLocal3dsGameSummaries()
            val candidateGames = (allGames + serverGames + local3ds)

            val mergedGames = candidateGames
                .map { game ->
                    if (game.pokemon == null) {
                        val detected = dev.vantafyn.core.jellyfin.PokemonGameDetector.detect(game.cleanTitle, game.filename, game.systemId)
                        if (detected != null) game.copy(pokemon = detected) else game
                    } else game
                }
                .filter { it.pokemon?.isPokemonGame == true || dev.vantafyn.core.jellyfin.PokemonGameDetector.detect(it.cleanTitle, it.filename, it.systemId) != null }
                .distinctBy { it.id }

            withContext(Dispatchers.Main) {
                availableGames = mergedGames
                // If lower state is PersonalVault, but games exist, default lower to first game for convenience
                if (lowerState.containerType is StorageContainerType.PersonalVault && mergedGames.isNotEmpty()) {
                    lowerState = lowerState.copy(
                        containerType = StorageContainerType.GameCartridge(mergedGames.first()),
                        gameSave = null,
                        vaultBox = null,
                    )
                }
            }
            loadVaultSummary()
            loadContainerData(TransferSide.Source)
            loadContainerData(TransferSide.Destination)
            withContext(Dispatchers.Main) {
                isLoadingInitial = false
            }
        }
    }

    val customBackgroundUrl = remember(session, integrationStatus) {
        if (session != null && integrationStatus?.hasCustomBackground != false) {
            val base = session.server.url.trimEnd('/')
            "$base/Vantafyn/Pokemon/Background?api_key=${session.accessToken}"
        } else {
            null
        }
    }

    LaunchedEffect(session) {
        loadAll()
    }

    // Trigger validation whenever a Pokémon is selected
    LaunchedEffect(selectedPokemonItem, upperState.containerType, lowerState.containerType) {
        val selected = selectedPokemonItem
        if (selected == null || session == null) {
            compatibilityResult = null
            return@LaunchedEffect
        }

        // Only direct game-to-game transfers require strict compatibility validation; vault transfers are universal
        val srcGame = if (selected.side == TransferSide.Source) {
            (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id
        } else {
            (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id
        }

        val dstGame = if (selected.side == TransferSide.Source) {
            (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id
        } else {
            (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id
        }

        if (srcGame != null && dstGame != null) {
            if (srcGame == dstGame) {
                compatibilityResult = PokemonTransferCompatibilityResult(
                    isCompatible = true,
                    reason = "Compatible: Moving within the same game save.",
                )
            } else {
                isValidatingCompatibility = true
                coroutineScope.launch(Dispatchers.IO) {
                    val req = PokemonTransferValidateRequest(
                        sourceGameId = srcGame,
                        destinationGameId = dstGame,
                        pokemonId = selected.pokemonId,
                        isInParty = selected.isInParty,
                        boxIndex = selected.boxIndex,
                        slotIndex = selected.slotIndex,
                    )
                    pokemonRepository.validateTransfer(session, req).fold(
                        onSuccess = { res ->
                            compatibilityResult = res
                            isValidatingCompatibility = false
                        },
                        onFailure = {
                            compatibilityResult = PokemonTransferCompatibilityResult(
                                isCompatible = false,
                                reason = it.message ?: "Validation failed",
                            )
                            isValidatingCompatibility = false
                        }
                    )
                }
            }
        } else {
            // Vault deposit or withdraw: Always compatible
            compatibilityResult = PokemonTransferCompatibilityResult(
                isCompatible = true,
                reason = "Compatible: Cloud Vault storage is universally supported across generations.",
            )
        }
    }

    fun executeTransfer() {
        val selected = selectedPokemonItem ?: return
        if (session == null) return

        isExecutingTransfer = true
        coroutineScope.launch(Dispatchers.IO) {
            val isSource = selected.side == TransferSide.Source
            val srcContainer = if (isSource) upperState.containerType else lowerState.containerType
            val dstContainer = if (isSource) lowerState.containerType else upperState.containerType

            val targetSlot = if (isSource) lowerSelectedSlotIndex else upperSelectedSlotIndex
            val targetBox = if (isSource) lowerState.selectedBoxIndex else upperState.selectedBoxIndex
            val targetIsInParty = if (isSource) lowerState.isPartyMode else upperState.isPartyMode

            val result = when {
                // Game -> Personal Vault (Deposit)
                srcContainer is StorageContainerType.GameCartridge && dstContainer is StorageContainerType.PersonalVault -> {
                    pokemonRepository.depositPokemon(
                        session,
                        PokemonDepositRequest(
                            gameId = srcContainer.game.id,
                            pokemonId = selected.pokemonId,
                            isInParty = selected.isInParty,
                            boxIndex = selected.boxIndex,
                            slotIndex = selected.slotIndex,
                            targetVaultBoxIndex = targetBox,
                            targetVaultSlotIndex = targetSlot,
                        )
                    )
                }

                // Personal Vault -> Game (Withdraw)
                srcContainer is StorageContainerType.PersonalVault && dstContainer is StorageContainerType.GameCartridge -> {
                    pokemonRepository.withdrawPokemon(
                        session,
                        PokemonWithdrawRequest(
                            vaultEntryId = selected.entryId ?: selected.pokemonId,
                            targetGameId = dstContainer.game.id,
                            targetBoxIndex = targetBox,
                            targetSlotIndex = targetSlot,
                            targetIsInParty = targetIsInParty,
                        )
                    )
                }

                // Game -> Game (Direct Transfer / Same Game Move)
                srcContainer is StorageContainerType.GameCartridge && dstContainer is StorageContainerType.GameCartridge -> {
                    pokemonRepository.directTransferPokemon(
                        session,
                        PokemonDirectTransferRequest(
                            sourceGameId = srcContainer.game.id,
                            destinationGameId = dstContainer.game.id,
                            pokemonId = selected.pokemonId,
                            isInParty = selected.isInParty,
                            sourceBoxIndex = selected.boxIndex,
                            sourceSlotIndex = selected.slotIndex,
                            targetBoxIndex = targetBox,
                            targetSlotIndex = targetSlot,
                            targetIsInParty = targetIsInParty,
                        )
                    )
                }

                // Vault -> Vault (Move / Swap between vault boxes / slots)
                srcContainer is StorageContainerType.PersonalVault && dstContainer is StorageContainerType.PersonalVault -> {
                    pokemonRepository.moveVaultEntry(
                        session,
                        PokemonVaultMoveRequest(
                            entryId = selected.entryId ?: selected.pokemonId,
                            targetBoxIndex = targetBox,
                            targetSlotIndex = targetSlot,
                        )
                    )
                }

                else -> {
                    Result.failure(Exception("Unable to determine transfer operation for selected containers."))
                }
            }

            withContext(Dispatchers.Main) {
                isExecutingTransfer = false
                result.fold(
                    onSuccess = { op ->
                        val backupNote = if (op.backupCreated) "\nBackup created: ${op.backupId}" else ""
                        operationResultMessage = "Success! ${op.message}$backupNote"
                        selectedPokemonItem = null
                        upperSelectedSlotIndex = null
                        lowerSelectedSlotIndex = null
                        loadVaultSummary()
                        loadContainerData(TransferSide.Source)
                        loadContainerData(TransferSide.Destination)
                    },
                    onFailure = { err ->
                        operationResultMessage = "Operation failed: ${err.message}"
                    }
                )
            }
        }
    }

    fun executeInContainerMove(
        side: TransferSide,
        targetBox: Int,
        targetSlot: Int,
        targetIsInParty: Boolean,
    ) {
        val selected = selectedPokemonItem ?: return
        if (session == null) return
        if (selected.side != side) return

        isExecutingTransfer = true
        coroutineScope.launch(Dispatchers.IO) {
            val container = if (side == TransferSide.Source) upperState.containerType else lowerState.containerType
            val result = when (container) {
                is StorageContainerType.PersonalVault -> {
                    pokemonRepository.moveVaultEntry(
                        session,
                        PokemonVaultMoveRequest(
                            entryId = selected.entryId ?: selected.pokemonId,
                            targetBoxIndex = targetBox,
                            targetSlotIndex = targetSlot,
                        )
                    )
                }
                is StorageContainerType.GameCartridge -> {
                    pokemonRepository.directTransferPokemon(
                        session,
                        PokemonDirectTransferRequest(
                            sourceGameId = container.game.id,
                            destinationGameId = container.game.id,
                            pokemonId = selected.pokemonId,
                            isInParty = selected.isInParty,
                            sourceBoxIndex = selected.boxIndex,
                            sourceSlotIndex = selected.slotIndex,
                            targetBoxIndex = targetBox,
                            targetSlotIndex = targetSlot,
                            targetIsInParty = targetIsInParty,
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                isExecutingTransfer = false
                result.fold(
                    onSuccess = { op ->
                        val backupNote = if (op.backupCreated) "\nBackup created: ${op.backupId}" else ""
                        operationResultMessage = "Success! ${op.message}$backupNote"
                        selectedPokemonItem = null
                        upperSelectedSlotIndex = null
                        lowerSelectedSlotIndex = null
                        loadVaultSummary()
                        loadContainerData(TransferSide.Source)
                        loadContainerData(TransferSide.Destination)
                    },
                    onFailure = { err ->
                        operationResultMessage = "Move failed: ${err.message}"
                    }
                )
            }
        }
    }

    fun executeBulkTransfer(direction: TransferSide, selectedIds: List<String>? = null) {
        if (session == null) return

        isExecutingTransfer = true
        coroutineScope.launch(Dispatchers.IO) {
            val isSource = direction == TransferSide.Source
            val srcContainer = if (isSource) upperState.containerType else lowerState.containerType
            val dstContainer = if (isSource) lowerState.containerType else upperState.containerType

            val srcBox = if (isSource) upperState.selectedBoxIndex else lowerState.selectedBoxIndex
            val srcParty = if (isSource) upperState.isPartyMode else lowerState.isPartyMode
            val dstBox = if (isSource) lowerState.selectedBoxIndex else upperState.selectedBoxIndex

            val result = when {
                // Game -> Personal Vault (Bulk Deposit)
                srcContainer is StorageContainerType.GameCartridge && dstContainer is StorageContainerType.PersonalVault -> {
                    pokemonRepository.bulkDepositPokemon(
                        session,
                        PokemonBulkDepositRequest(
                            gameId = srcContainer.game.id,
                            sourceBoxIndex = if (srcParty) null else srcBox,
                            isParty = srcParty,
                            targetVaultBoxIndex = dstBox,
                            pokemonIds = selectedIds,
                        )
                    )
                }

                // Personal Vault -> Game (Bulk Withdraw)
                srcContainer is StorageContainerType.PersonalVault && dstContainer is StorageContainerType.GameCartridge -> {
                    pokemonRepository.bulkWithdrawPokemon(
                        session,
                        PokemonBulkWithdrawRequest(
                            gameId = dstContainer.game.id,
                            sourceVaultBoxIndex = srcBox,
                            targetBoxIndex = dstBox,
                            vaultEntryIds = selectedIds,
                        )
                    )
                }

                // Game -> Game (Bulk Direct Transfer)
                srcContainer is StorageContainerType.GameCartridge && dstContainer is StorageContainerType.GameCartridge -> {
                    pokemonRepository.bulkDirectTransferPokemon(
                        session,
                        PokemonBulkDirectTransferRequest(
                            sourceGameId = srcContainer.game.id,
                            destinationGameId = dstContainer.game.id,
                            sourceBoxIndex = if (srcParty) null else srcBox,
                            sourceParty = srcParty,
                            targetBoxIndex = dstBox,
                            pokemonIds = selectedIds,
                        )
                    )
                }

                else -> {
                    Result.failure(Exception("Bulk move between identical containers is not supported."))
                }
            }

            withContext(Dispatchers.Main) {
                isExecutingTransfer = false
                result.fold(
                    onSuccess = { op ->
                        val backupNote = if (op.backupCreated) "\nBackup created: ${op.backupId}" else ""
                        val errNote = if (op.failedCount > 0) "\nFailed: ${op.failedCount} (${op.errors.firstOrNull() ?: ""})" else ""
                        operationResultMessage = "Success! ${op.message} (${op.transferredCount} moved)$backupNote$errNote"
                        selectedPokemonItem = null
                        upperSelectedSlotIndex = null
                        lowerSelectedSlotIndex = null
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                        loadVaultSummary()
                        loadContainerData(TransferSide.Source)
                        loadContainerData(TransferSide.Destination)
                    },
                    onFailure = { err ->
                        operationResultMessage = "Bulk transfer failed: ${err.message}"
                    }
                )
            }
        }
    }

    fun handleSortBox(side: TransferSide, criterion: String, ascending: Boolean) {
        val state = if (side == TransferSide.Source) upperState else lowerState
        if (session == null) return

        when (val container = state.containerType) {
            is StorageContainerType.PersonalVault -> {
                coroutineScope.launch(Dispatchers.IO) {
                    val boxIndex = state.selectedBoxIndex
                    val res = pokemonRepository.sortVaultBox(session, boxIndex, criterion, ascending)
                    withContext(Dispatchers.Main) {
                        res.fold(
                            onSuccess = { sortedBox ->
                                if (side == TransferSide.Source) {
                                    upperState = upperState.copy(
                                        vaultBox = sortedBox,
                                    )
                                    upperSelectedSlotIndex = null
                                } else {
                                    lowerState = lowerState.copy(
                                        vaultBox = sortedBox,
                                    )
                                    lowerSelectedSlotIndex = null
                                }
                                operationResultMessage = "Sorted ${sortedBox.name} by ${criterion.uppercase()}!"
                            },
                            onFailure = { err ->
                                operationResultMessage = "Sort failed: ${err.message}"
                            }
                        )
                    }
                }
            }
            is StorageContainerType.GameCartridge -> {
                val save = state.gameSave ?: return
                val boxIndex = state.selectedBoxIndex
                val targetBox = save.boxes.firstOrNull { it.boxIndex == boxIndex } ?: return
                if (targetBox.entries.isEmpty()) return

                val sortedEntries = when (criterion.lowercase()) {
                    "dex" -> {
                        if (ascending) targetBox.entries.sortedWith(compareBy({ it.speciesId }, { it.level }))
                        else targetBox.entries.sortedWith(compareByDescending<PokemonSummaryDto> { it.speciesId }.thenByDescending { it.level })
                    }
                    "shiny" -> {
                        targetBox.entries.sortedWith(compareByDescending<PokemonSummaryDto> { it.isShiny }.thenBy { it.speciesId })
                    }
                    "level" -> {
                        if (ascending) targetBox.entries.sortedBy { it.level }
                        else targetBox.entries.sortedByDescending { it.level }
                    }
                    "name" -> {
                        if (ascending) targetBox.entries.sortedBy { it.species }
                        else targetBox.entries.sortedByDescending { it.species }
                    }
                    "iv" -> {
                        targetBox.entries.sortedByDescending { it.level }
                    }
                    else -> targetBox.entries
                }.mapIndexed { idx, pkm ->
                    pkm.copy(slotIndex = idx + 1)
                }

                val updatedBoxes = save.boxes.map { b ->
                    if (b.boxIndex == boxIndex) {
                        b.copy(entries = sortedEntries, occupiedCount = sortedEntries.size)
                    } else b
                }
                val updatedSave = save.copy(boxes = updatedBoxes)
                if (side == TransferSide.Source) {
                    upperState = upperState.copy(gameSave = updatedSave)
                    upperSelectedSlotIndex = null
                } else {
                    lowerState = lowerState.copy(gameSave = updatedSave)
                    lowerSelectedSlotIndex = null
                }
                operationResultMessage = "Sorted ${targetBox.name} by ${criterion.uppercase()}!"
            }
        }
    }

    val allVaultBoxes = remember(remoteVaultBoxes, upperState.vaultBox, lowerState.vaultBox) {
        val map = remoteVaultBoxes.associateBy { it.boxIndex }.toMutableMap()
        upperState.vaultBox?.let { map[it.boxIndex] = it }
        lowerState.vaultBox?.let { map[it.boxIndex] = it }
        map.values.sortedBy { it.boxIndex }.map { box ->
            dev.vantafyn.core.jellyfin.PokemonBoxDto(
                boxIndex = box.boxIndex,
                name = box.name,
                entries = box.entries.map { it.toSummaryDto() },
            )
        }
    }

    val allDetectedSaves = remember(upperState.gameSave, lowerState.gameSave, availableGames) {
        val map = mutableMapOf<String, dev.vantafyn.core.jellyfin.PokemonGameSaveDto>()
        fun isValidSave(save: dev.vantafyn.core.jellyfin.PokemonGameSaveDto?, game: GameSummary?): Boolean {
            if (save == null || !save.saveFound || !save.providerAvailable) return false
            val meta = game?.pokemon
            if (meta != null) {
                if (meta.generation > 0 && save.generation > 0 && save.generation != meta.generation) return false
                if (meta.platform.isNotBlank() && save.platform.isNotBlank() && !save.platform.equals(meta.platform, ignoreCase = true)) return false
            }
            return save.party.isNotEmpty() ||
                save.boxes.any { it.entries.isNotEmpty() } ||
                save.totalPokemonCount > 0 ||
                (save.pokedexCaught ?: 0) > 0 ||
                save.gymBadges.any { r -> r.badges.any { it.isEarned } }
        }
        val upperGame = (upperState.containerType as? StorageContainerType.GameCartridge)?.game
        val lowerGame = (lowerState.containerType as? StorageContainerType.GameCartridge)?.game
        upperState.gameSave?.takeIf { isValidSave(it, upperGame) }?.let { map[it.gameId] = it }
        lowerState.gameSave?.takeIf { isValidSave(it, lowerGame) }?.let { map[it.gameId] = it }
        for (game in availableGames) {
            if (!map.containsKey(game.id)) {
                val localSaveFile = storageManager.findExistingLocalSaveFile(game.id, GameSaveKind.Sram, game.title)
                if (localSaveFile.exists() && localSaveFile.length() > 0L) {
                    val bytes = runCatching { localSaveFile.readBytes() }.getOrNull()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val parsed = dev.vantafyn.core.jellyfin.PokemonNativeSaveParser.parse(
                            saveBytes = bytes,
                            gameTitle = game.title,
                            gameId = game.pokemon?.pokemonGameId?.ifBlank { null } ?: game.id,
                            expectedGeneration = game.pokemon?.generation ?: 0,
                            expectedPlatform = game.pokemon?.platform ?: "",
                        )
                        if (parsed != null && isValidSave(parsed, game)) {
                            map[game.id] = parsed.copy(
                                gameId = game.id,
                                title = game.title,
                            )
                        }
                    }
                }
            }
        }
        map.values.toList()
    }

    val importSaveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || session == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            operationResultMessage = "Reading your save…"
            pokemonRepository.previewExternalSave(session, context.contentResolver, uri).fold(
                onSuccess = { preview ->
                    externalSavePreview = preview
                    selectedExternalPokemonIds = (preview.party + preview.boxes.flatMap { it.entries }).map { it.id }.toSet()
                    operationResultMessage = null
                },
                onFailure = { operationResultMessage = it.message ?: "Could not read that save." },
            )
        }
    }

    if (externalSavePreview != null) {
        val preview = externalSavePreview!!
        val pokemon = preview.party + preview.boxes.flatMap { it.entries }
        AlertDialog(
            onDismissRequest = { externalSavePreview = null },
            title = { Text("Choose Pokémon to import", color = VantafynColors.Ink, fontWeight = FontWeight.Bold) },
            text = { Column(Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
                Text("Gen ${preview.generation}${preview.trainerName?.let { " • $it" } ?: ""}", color = VantafynColors.Muted, fontSize = 12.sp)
                TextButton(onClick = { selectedExternalPokemonIds = pokemon.map { it.id }.toSet() }) { Text("Select all", color = Color(0xFF00E5FF)) }
                preview.party.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(p.id in selectedExternalPokemonIds, { selectedExternalPokemonIds = selectedExternalPokemonIds.let { if (p.id in it) it - p.id else it + p.id } }); Text("${p.nickname.ifBlank { p.species }} • Lv ${p.level}", color = VantafynColors.Ink, fontSize = 13.sp) } }
                preview.boxes.forEach { box -> Column { TextButton(onClick = { val ids = box.entries.map { it.id }.toSet(); selectedExternalPokemonIds = if (ids.all { it in selectedExternalPokemonIds }) selectedExternalPokemonIds - ids else selectedExternalPokemonIds + ids }) { Text(box.name.ifBlank { "Box ${box.boxIndex}" }, color = Color(0xFFB8A4FF)) }; box.entries.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(p.id in selectedExternalPokemonIds, { selectedExternalPokemonIds = selectedExternalPokemonIds.let { if (p.id in it) it - p.id else it + p.id } }); Text("${p.nickname.ifBlank { p.species }} • Lv ${p.level}", color = VantafynColors.Ink, fontSize = 13.sp) } } } }
            } },
            confirmButton = {
                TextButton(onClick = {
                    externalSavePreview = null
                    coroutineScope.launch {
                        val activeSession = session ?: return@launch
                        operationResultMessage = "Importing ${selectedExternalPokemonIds.size} Pokémon…"
                        pokemonRepository.commitExternalSavePreview(activeSession, preview.previewId, selectedExternalPokemonIds).fold(
                            onSuccess = { result ->
                                operationResultMessage = result.message
                                loadAll()
                            },
                            onFailure = { error -> operationResultMessage = error.message ?: "Could not import that save." },
                        )
                    }
                }, enabled = selectedExternalPokemonIds.isNotEmpty()) { Text("Import ${selectedExternalPokemonIds.size}", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { externalSavePreview = null }) { Text("Cancel", color = VantafynColors.Muted) } },
            containerColor = Color(0xFF1B1E2B),
        )
    }

    CompositionLocalProvider(LocalPokemonModalBackground provides customBackgroundUrl) {
        GameScreenReveal(
            key = "pokemon_vault_screen_root",
            modifier = modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 14.dp, vertical = 2.dp),
            ) {
                AnimatedContent(
                    targetState = subScreen,
                    transitionSpec = {
                        gamesTabTransitionSpec(reducedMotion)
                    },
                    label = "VaultScreenTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { screen ->
                when (screen) {
                    VaultSubScreen.Home -> {
                        PokemonVaultHomeScreen(
                            session = session,
                            vaultSummary = vaultSummary,
                            availableGames = availableGames,
                            allDetectedSaves = allDetectedSaves,
                            allVaultBoxes = allVaultBoxes,
                            isLoading = isLoadingInitial,
                            isRefreshing = isRefreshing,
                            onMovePokemon = { subScreen = VaultSubScreen.BoxTransfer },
                            onImportSave = { importSaveLauncher.launch(arrayOf("application/octet-stream", "application/x-spss-sav", "*/*")) },
                            onSelectGameForTransfer = { game ->
                                lowerState = lowerState.copy(
                                    containerType = StorageContainerType.GameCartridge(game),
                                    selectedBoxIndex = 1,
                                    isPartyMode = false,
                                    gameSave = null,
                                    vaultBox = null,
                                )
                                loadContainerData(TransferSide.Destination)
                                subScreen = VaultSubScreen.BoxTransfer
                            },
                            onOpenTradeCenter = { subScreen = VaultSubScreen.BoxTransfer },
                            onOpenPokedex = { isPokedexModalOpen = true },
                            onOpenBadgeCase = { subScreen = VaultSubScreen.BadgeCase },
                            onOpenDiplomaCase = { subScreen = VaultSubScreen.DiplomaCase },
                            onOpenEventVault = { subScreen = VaultSubScreen.EventVault },
                            onOpenAchievements = { isAchievementsModalOpen = true },
                            onOpenBackups = { isBackupRestoreModalOpen = true },
                            onInspectPokemon = { pkm, details, gameId, isVault, summaries, idx ->
                                inspectedPokemon = pkm
                                inspectedPokemonDetails = details
                                inspectedPokemonGameId = gameId
                                inspectedPokemonIsVault = isVault
                                inspectedPokemonNavigationList = summaries
                                inspectedPokemonIndex = idx
                            },
                            onRefresh = { loadAll() },
                            onBack = onBack,
                        )
                    }
                    VaultSubScreen.BadgeCase -> {
                        PokemonBadgeCaseScreen(
                            session = session,
                            pokemonRepository = pokemonRepository,
                            availableGames = availableGames,
                            detectedSaves = allDetectedSaves,
                            onBack = { subScreen = VaultSubScreen.Home },
                            onRefresh = { loadAll() },
                        )
                    }
                    VaultSubScreen.DiplomaCase -> {
                        PokemonDiplomaCaseScreen(
                            session = session,
                            pokemonRepository = pokemonRepository,
                            availableGames = availableGames,
                            detectedSaves = allDetectedSaves,
                            onBack = { subScreen = VaultSubScreen.Home },
                            onRefresh = { loadAll() },
                        )
                    }
                    VaultSubScreen.EventVault -> {
                        PokemonEventVaultScreen(
                            session = session,
                            pokemonRepository = pokemonRepository,
                            availableGames = availableGames,
                            detectedSaves = allDetectedSaves,
                            onBack = { subScreen = VaultSubScreen.Home },
                            onRefresh = { loadAll() },
                            storageManager = storageManager,
                        )
                    }
                    VaultSubScreen.BoxTransfer -> {
                        GameScreenReveal(key = "pokemon_vault_box_transfer", modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                            // Header Row 1: Back, Title, Subtitle, Occupancy Pill, Refresh
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CompactBackButton(onClick = { subScreen = VaultSubScreen.Home })
                                    Text(
                                        text = "Move Pokémon",
                                        color = VantafynColors.Ink,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Vault occupancy pill
                if (vaultSummary != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1E2B))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp),
                        )
                        val cap = maxOf(vaultSummary!!.totalCapacity, 3000)
                        Text(
                            text = "${vaultSummary!!.totalOccupied}/$cap",
                            color = VantafynColors.Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                IconButton(
                    onClick = { loadAll() },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) Color(0xFF00E5FF) else VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Header Row 2: Dedicated Action Chips (Trade, Backups, Pokédex, Badges)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Trade Center Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(VantafynGradients.accentHorizontal())
                    .clickable { isTradeModalOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cable,
                    contentDescription = "Trade Center",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Trade Center",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Backups & Diagnostics Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202334))
                    .border(1.dp, Color(0xFF3B425A), RoundedCornerShape(10.dp))
                    .clickable { isBackupRestoreModalOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = "Backups & Diagnostics",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Backups",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // National Pokédex Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202334))
                    .border(1.dp, Color(0xFF3B425A), RoundedCornerShape(10.dp))
                    .clickable { isPokedexModalOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = "National Pokédex",
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Pokédex",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Trainer Badges & Social Activity Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202334))
                    .border(1.dp, Color(0xFF3B425A), RoundedCornerShape(10.dp))
                    .clickable { isAchievementsModalOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Badges & Activity",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Badges",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Split View Container
        if (isLoadingInitial) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.size(36.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // UPPER BOX SECTION (Source)
                PokemonContainerBoxSection(
                    uiState = upperState,
                    availableGames = availableGames,
                    selectedSlotIndex = upperSelectedSlotIndex,
                    onSelectContainer = { target ->
                        upperState = upperState.copy(
                            containerType = target,
                            selectedBoxIndex = 1,
                            isPartyMode = false,
                            gameSave = null,
                            vaultBox = null,
                        )
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                    },
                    onPreviousBox = {
                        val prev = if (upperState.selectedBoxIndex > 1) upperState.selectedBoxIndex - 1 else upperState.totalBoxes
                        upperState = upperState.copy(selectedBoxIndex = prev)
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                    },
                    onNextBox = {
                        val next = if (upperState.selectedBoxIndex < upperState.totalBoxes) upperState.selectedBoxIndex + 1 else 1
                        upperState = upperState.copy(selectedBoxIndex = next)
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                    },
                    onSelectBox = { targetIndex ->
                        val clamped = targetIndex.coerceIn(1, upperState.totalBoxes)
                        upperState = upperState.copy(selectedBoxIndex = clamped)
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                    },
                    onTogglePartyMode = {
                        upperState = upperState.copy(isPartyMode = !upperState.isPartyMode)
                        upperSelectedSlotIndex = null
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                    },
                    onSelectSlot = { slot, pokemon ->
                        val currentSelected = selectedPokemonItem
                        if (currentSelected != null && currentSelected.side == TransferSide.Source) {
                            val currentSlot = currentSelected.slotIndex
                            val isSamePartyMode = currentSelected.isInParty == upperState.isPartyMode
                            val isSameBox = upperState.isPartyMode || (currentSelected.boxIndex == upperState.selectedBoxIndex)

                            if (isSamePartyMode && isSameBox && currentSlot == slot) {
                                selectedPokemonItem = null
                                upperSelectedSlotIndex = null
                            } else if (pokemon == null) {
                                val targetBox = if (upperState.isPartyMode) 1 else upperState.selectedBoxIndex
                                executeInContainerMove(
                                    side = TransferSide.Source,
                                    targetBox = targetBox,
                                    targetSlot = slot,
                                    targetIsInParty = upperState.isPartyMode
                                )
                            } else {
                                upperSelectedSlotIndex = slot
                                val isVault = upperState.containerType is StorageContainerType.PersonalVault
                                val gameId = (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id
                                selectedPokemonItem = SelectedPokemonItem(
                                    side = TransferSide.Source,
                                    isVault = isVault,
                                    gameId = gameId,
                                    entryId = if (isVault) pokemon.id else null,
                                    pokemonId = pokemon.id,
                                    summary = pokemon,
                                    boxIndex = if (upperState.isPartyMode) null else upperState.selectedBoxIndex,
                                    slotIndex = slot,
                                    isInParty = upperState.isPartyMode,
                                )
                            }
                        } else if (currentSelected != null && currentSelected.side == TransferSide.Destination) {
                            upperSelectedSlotIndex = slot
                        } else {
                            upperSelectedSlotIndex = slot
                            if (pokemon != null) {
                                val isVault = upperState.containerType is StorageContainerType.PersonalVault
                                val gameId = (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id
                                selectedPokemonItem = SelectedPokemonItem(
                                    side = TransferSide.Source,
                                    isVault = isVault,
                                    gameId = gameId,
                                    entryId = if (isVault) pokemon.id else null,
                                    pokemonId = pokemon.id,
                                    summary = pokemon,
                                    boxIndex = if (upperState.isPartyMode) null else upperState.selectedBoxIndex,
                                    slotIndex = slot,
                                    isInParty = upperState.isPartyMode,
                                )
                            } else {
                                selectedPokemonItem = null
                            }
                        }
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onOpenDetails = { pokemon ->
                        val boxList: List<PokemonSummaryDto> = (if (upperState.isPartyMode) {
                            upperState.gameSave?.party
                        } else if (upperState.containerType is StorageContainerType.PersonalVault) {
                            upperState.vaultBox?.entries?.map { it.toSummaryDto() }
                        } else {
                            upperState.gameSave?.boxes?.getOrNull(upperState.selectedBoxIndex - 1)?.entries
                        }) ?: listOf(pokemon)
                        inspectedPokemon = pokemon
                        inspectedPokemonDetails = null
                        inspectedPokemonGameId = (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id ?: upperState.gameSave?.gameId
                        inspectedPokemonIsVault = upperState.containerType is StorageContainerType.PersonalVault
                        inspectedPokemonNavigationList = boxList
                        inspectedPokemonIndex = boxList.indexOfFirst { it.id == pokemon.id }
                    },
                    onSortBox = { criterion, ascending ->
                        handleSortBox(TransferSide.Source, criterion, ascending)
                    },
                    isMultiSelectMode = isUpperMultiSelectMode,
                    selectedMultiSlots = upperMultiSelectedSlots,
                    onToggleMultiSelectMode = {
                        isUpperMultiSelectMode = !isUpperMultiSelectMode
                        upperMultiSelectedSlots = emptySet()
                        if (isUpperMultiSelectMode) {
                            selectedPokemonItem = null
                            upperSelectedSlotIndex = null
                            isLowerMultiSelectMode = false
                            lowerMultiSelectedSlots = emptySet()
                            lowerSelectedSlotIndex = null
                        }
                    },
                    onToggleMultiSelectSlot = { slot, _ ->
                        upperMultiSelectedSlots = if (upperMultiSelectedSlots.contains(slot)) {
                            upperMultiSelectedSlots - slot
                        } else {
                            upperMultiSelectedSlots + slot
                        }
                        selectedPokemonItem = null
                        upperSelectedSlotIndex = null
                    },
                    onSelectAllInBox = {
                        val allEntries: List<PokemonSummaryDto> = if (upperState.isPartyMode) {
                            upperState.gameSave?.party ?: emptyList()
                        } else if (upperState.containerType is StorageContainerType.PersonalVault) {
                            upperState.vaultBox?.entries?.map { it.toSummaryDto() } ?: emptyList()
                        } else {
                            upperState.gameSave?.boxes?.firstOrNull { it.boxIndex == upperState.selectedBoxIndex }?.entries ?: emptyList()
                        }
                        upperMultiSelectedSlots = allEntries.map { it.slotIndex }.toSet()
                        selectedPokemonItem = null
                        upperSelectedSlotIndex = null
                    },
                )

                val upperSelectedPokemonList = remember(upperMultiSelectedSlots, upperState) {
                    if (upperMultiSelectedSlots.isEmpty()) emptyList()
                    else {
                        val allEntries: List<PokemonSummaryDto> = if (upperState.isPartyMode) {
                            upperState.gameSave?.party ?: emptyList()
                        } else if (upperState.containerType is StorageContainerType.PersonalVault) {
                            upperState.vaultBox?.entries?.map { it.toSummaryDto() } ?: emptyList()
                        } else {
                            upperState.gameSave?.boxes?.firstOrNull { it.boxIndex == upperState.selectedBoxIndex }?.entries ?: emptyList()
                        }
                        allEntries.filter { upperMultiSelectedSlots.contains(it.slotIndex) }
                    }
                }

                val lowerSelectedPokemonList = remember(lowerMultiSelectedSlots, lowerState) {
                    if (lowerMultiSelectedSlots.isEmpty()) emptyList()
                    else {
                        val allEntries: List<PokemonSummaryDto> = if (lowerState.isPartyMode) {
                            lowerState.gameSave?.party ?: emptyList()
                        } else if (lowerState.containerType is StorageContainerType.PersonalVault) {
                            lowerState.vaultBox?.entries?.map { it.toSummaryDto() } ?: emptyList()
                        } else {
                            lowerState.gameSave?.boxes?.firstOrNull { it.boxIndex == lowerState.selectedBoxIndex }?.entries ?: emptyList()
                        }
                        allEntries.filter { lowerMultiSelectedSlots.contains(it.slotIndex) }
                    }
                }

                val activeMultiSide = when {
                    upperMultiSelectedSlots.isNotEmpty() -> TransferSide.Source
                    lowerMultiSelectedSlots.isNotEmpty() -> TransferSide.Destination
                    else -> null
                }
                val activeMultiCount = when (activeMultiSide) {
                    TransferSide.Source -> upperMultiSelectedSlots.size
                    TransferSide.Destination -> lowerMultiSelectedSlots.size
                    null -> 0
                }

                // ACTION TRANSFER BRIDGE
                PokemonActionBridge(
                    selectedPokemon = selectedPokemonItem,
                    compatibility = compatibilityResult,
                    isValidating = isValidatingCompatibility,
                    isExecutingTransfer = isExecutingTransfer,
                    onExecuteTransfer = { executeTransfer() },
                    onInspectDetails = {
                        selectedPokemonItem?.summary?.let { pkm ->
                            inspectedPokemon = pkm
                            inspectedPokemonDetails = null
                            inspectedPokemonGameId = selectedPokemonItem?.gameId
                            inspectedPokemonIsVault = selectedPokemonItem?.isVault
                            inspectedPokemonNavigationList = listOf(pkm)
                            inspectedPokemonIndex = 0
                        }
                    },
                    multiSelectedCount = activeMultiCount,
                    multiSelectedSide = activeMultiSide,
                    onExecuteMultiTransfer = {
                        if (activeMultiSide == TransferSide.Source) {
                            val ids = upperSelectedPokemonList.map { it.id }
                            bulkTransferConfirmation = BulkTransferConfirmation(
                                direction = TransferSide.Source,
                                count = ids.size,
                                fromName = upperState.currentBoxName,
                                toName = lowerState.currentBoxName,
                                selectedIds = ids,
                            )
                        } else if (activeMultiSide == TransferSide.Destination) {
                            val ids = lowerSelectedPokemonList.map { it.id }
                            bulkTransferConfirmation = BulkTransferConfirmation(
                                direction = TransferSide.Destination,
                                count = ids.size,
                                fromName = lowerState.currentBoxName,
                                toName = upperState.currentBoxName,
                                selectedIds = ids,
                            )
                        }
                    },
                    onClearMultiSelection = {
                        upperMultiSelectedSlots = emptySet()
                        isUpperMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                        isLowerMultiSelectMode = false
                    },
                    upperBoxOccupiedCount = upperState.currentOccupiedCount,
                    lowerBoxOccupiedCount = lowerState.currentOccupiedCount,
                    upperBoxName = upperState.currentBoxName,
                    lowerBoxName = lowerState.currentBoxName,
                    upperIsLocked = upperState.isLocked,
                    lowerIsLocked = lowerState.isLocked,
                    isSameContainerOrGame = remember(upperState.containerType, lowerState.containerType) {
                        val upper = upperState.containerType
                        val lower = lowerState.containerType
                        (upper is StorageContainerType.PersonalVault && lower is StorageContainerType.PersonalVault) ||
                        (upper is StorageContainerType.GameCartridge && lower is StorageContainerType.GameCartridge && upper.game.id == lower.game.id)
                    },
                    onTransferEntireBox = { direction ->
                        if (direction == TransferSide.Source) {
                            bulkTransferConfirmation = BulkTransferConfirmation(
                                direction = TransferSide.Source,
                                count = upperState.currentOccupiedCount,
                                fromName = upperState.currentBoxName,
                                toName = lowerState.currentBoxName,
                                selectedIds = null,
                            )
                        } else {
                            bulkTransferConfirmation = BulkTransferConfirmation(
                                direction = TransferSide.Destination,
                                count = lowerState.currentOccupiedCount,
                                fromName = lowerState.currentBoxName,
                                toName = upperState.currentBoxName,
                                selectedIds = null,
                            )
                        }
                    },
                )

                // LOWER BOX SECTION (Destination)
                PokemonContainerBoxSection(
                    uiState = lowerState,
                    availableGames = availableGames,
                    selectedSlotIndex = lowerSelectedSlotIndex,
                    onSelectContainer = { target ->
                        lowerState = lowerState.copy(
                            containerType = target,
                            selectedBoxIndex = 1,
                            isPartyMode = false,
                            gameSave = null,
                            vaultBox = null,
                        )
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onPreviousBox = {
                        val prev = if (lowerState.selectedBoxIndex > 1) lowerState.selectedBoxIndex - 1 else lowerState.totalBoxes
                        lowerState = lowerState.copy(selectedBoxIndex = prev)
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onNextBox = {
                        val next = if (lowerState.selectedBoxIndex < lowerState.totalBoxes) lowerState.selectedBoxIndex + 1 else 1
                        lowerState = lowerState.copy(selectedBoxIndex = next)
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onSelectBox = { targetIndex ->
                        val clamped = targetIndex.coerceIn(1, lowerState.totalBoxes)
                        lowerState = lowerState.copy(selectedBoxIndex = clamped)
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onTogglePartyMode = {
                        lowerState = lowerState.copy(isPartyMode = !lowerState.isPartyMode)
                        lowerSelectedSlotIndex = null
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onSelectSlot = { slot, pokemon ->
                        val currentSelected = selectedPokemonItem
                        if (currentSelected != null && currentSelected.side == TransferSide.Destination) {
                            val currentSlot = currentSelected.slotIndex
                            val isSamePartyMode = currentSelected.isInParty == lowerState.isPartyMode
                            val isSameBox = lowerState.isPartyMode || (currentSelected.boxIndex == lowerState.selectedBoxIndex)

                            if (isSamePartyMode && isSameBox && currentSlot == slot) {
                                selectedPokemonItem = null
                                lowerSelectedSlotIndex = null
                            } else if (pokemon == null) {
                                val targetBox = if (lowerState.isPartyMode) 1 else lowerState.selectedBoxIndex
                                executeInContainerMove(
                                    side = TransferSide.Destination,
                                    targetBox = targetBox,
                                    targetSlot = slot,
                                    targetIsInParty = lowerState.isPartyMode
                                )
                            } else {
                                lowerSelectedSlotIndex = slot
                                val isVault = lowerState.containerType is StorageContainerType.PersonalVault
                                val gameId = (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id
                                selectedPokemonItem = SelectedPokemonItem(
                                    side = TransferSide.Destination,
                                    isVault = isVault,
                                    gameId = gameId,
                                    entryId = if (isVault) pokemon.id else null,
                                    pokemonId = pokemon.id,
                                    summary = pokemon,
                                    boxIndex = if (lowerState.isPartyMode) null else lowerState.selectedBoxIndex,
                                    slotIndex = slot,
                                    isInParty = lowerState.isPartyMode,
                                )
                            }
                        } else if (currentSelected != null && currentSelected.side == TransferSide.Source) {
                            lowerSelectedSlotIndex = slot
                        } else {
                            lowerSelectedSlotIndex = slot
                            if (pokemon != null) {
                                val isVault = lowerState.containerType is StorageContainerType.PersonalVault
                                val gameId = (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id
                                selectedPokemonItem = SelectedPokemonItem(
                                    side = TransferSide.Destination,
                                    isVault = isVault,
                                    gameId = gameId,
                                    entryId = if (isVault) pokemon.id else null,
                                    pokemonId = pokemon.id,
                                    summary = pokemon,
                                    boxIndex = if (lowerState.isPartyMode) null else lowerState.selectedBoxIndex,
                                    slotIndex = slot,
                                    isInParty = lowerState.isPartyMode,
                                )
                            } else {
                                selectedPokemonItem = null
                            }
                        }
                        isUpperMultiSelectMode = false
                        upperMultiSelectedSlots = emptySet()
                        isLowerMultiSelectMode = false
                        lowerMultiSelectedSlots = emptySet()
                    },
                    onOpenDetails = { pokemon ->
                        val boxList: List<PokemonSummaryDto> = (if (lowerState.isPartyMode) {
                            lowerState.gameSave?.party
                        } else if (lowerState.containerType is StorageContainerType.PersonalVault) {
                            lowerState.vaultBox?.entries?.map { it.toSummaryDto() }
                        } else {
                            lowerState.gameSave?.boxes?.getOrNull(lowerState.selectedBoxIndex - 1)?.entries
                        }) ?: listOf(pokemon)
                        inspectedPokemon = pokemon
                        inspectedPokemonDetails = null
                        inspectedPokemonGameId = (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id ?: lowerState.gameSave?.gameId
                        inspectedPokemonIsVault = lowerState.containerType is StorageContainerType.PersonalVault
                        inspectedPokemonNavigationList = boxList
                        inspectedPokemonIndex = boxList.indexOfFirst { it.id == pokemon.id }
                    },
                    onSortBox = { criterion, ascending ->
                        handleSortBox(TransferSide.Destination, criterion, ascending)
                    },
                    isMultiSelectMode = isLowerMultiSelectMode,
                    selectedMultiSlots = lowerMultiSelectedSlots,
                    onToggleMultiSelectMode = {
                        isLowerMultiSelectMode = !isLowerMultiSelectMode
                        lowerMultiSelectedSlots = emptySet()
                        if (isLowerMultiSelectMode) {
                            selectedPokemonItem = null
                            lowerSelectedSlotIndex = null
                            isUpperMultiSelectMode = false
                            upperMultiSelectedSlots = emptySet()
                            upperSelectedSlotIndex = null
                        }
                    },
                    onToggleMultiSelectSlot = { slot, _ ->
                        lowerMultiSelectedSlots = if (lowerMultiSelectedSlots.contains(slot)) {
                            lowerMultiSelectedSlots - slot
                        } else {
                            lowerMultiSelectedSlots + slot
                        }
                        selectedPokemonItem = null
                        lowerSelectedSlotIndex = null
                    },
                    onSelectAllInBox = {
                        val allEntries: List<PokemonSummaryDto> = if (lowerState.isPartyMode) {
                            lowerState.gameSave?.party ?: emptyList()
                        } else if (lowerState.containerType is StorageContainerType.PersonalVault) {
                            lowerState.vaultBox?.entries?.map { it.toSummaryDto() } ?: emptyList()
                        } else {
                            lowerState.gameSave?.boxes?.firstOrNull { it.boxIndex == lowerState.selectedBoxIndex }?.entries ?: emptyList()
                        }
                        lowerMultiSelectedSlots = allEntries.map { it.slotIndex }.toSet()
                        selectedPokemonItem = null
                        lowerSelectedSlotIndex = null
                    },
                )

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
    }
    }
    }
    }
    }
    }

    // Inspection Modal
    if (inspectedPokemon != null) {
        val currentPkm = inspectedPokemon!!
        val matchingSave = allDetectedSaves.firstOrNull { save ->
            save.party.any { it.id == currentPkm.id } ||
            save.boxes.any { b -> b.entries.any { it.id == currentPkm.id } }
        }
        val matchingVaultBox = allVaultBoxes.firstOrNull { box ->
            box.entries.any { it.id == currentPkm.id }
        }
        val selectedItem = selectedPokemonItem
        val isVault = inspectedPokemonIsVault
            ?: selectedItem?.isVault 
            ?: (matchingVaultBox != null ||
                upperState.vaultBox?.entries?.any { it.id == currentPkm.id } == true ||
                lowerState.vaultBox?.entries?.any { it.id == currentPkm.id } == true)
        val gameId = inspectedPokemonGameId
            ?: selectedItem?.gameId
            ?: matchingSave?.gameId
            ?: (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id
            ?: (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id

        val initialDetails = inspectedPokemonDetails
            ?: matchingSave?.pokemonDetails?.get(currentPkm.id)
            ?: upperState.gameSave?.pokemonDetails?.get(currentPkm.id)
            ?: lowerState.gameSave?.pokemonDetails?.get(currentPkm.id)
            ?: matchingSave?.pokemonDetails?.values?.firstOrNull { it.summary.speciesId == currentPkm.speciesId && it.summary.level == currentPkm.level }
            ?: dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog.generateCanonicalDetails(currentPkm)

        PokemonDetailModal(
            pokemon = currentPkm,
            session = session,
            pokemonRepository = pokemonRepository,
            gameId = gameId,
            isVault = isVault,
            initialDetails = initialDetails,
            customBackgroundUrl = customBackgroundUrl,
            navigationList = inspectedPokemonNavigationList,
            currentIndex = inspectedPokemonIndex,
            onNavigateToIndex = { newIndex ->
                if (newIndex in inspectedPokemonNavigationList.indices) {
                    val nextPkm = inspectedPokemonNavigationList[newIndex]
                    inspectedPokemon = nextPkm
                    inspectedPokemonIndex = newIndex
                    inspectedPokemonDetails = null
                }
            },
            onDismiss = {
                inspectedPokemon = null
                inspectedPokemonDetails = null
                inspectedPokemonGameId = null
                inspectedPokemonIsVault = null
                inspectedPokemonNavigationList = emptyList()
                inspectedPokemonIndex = -1
            },
        )
    }

    // Operation Result Alert
    if (operationResultMessage != null) {
        AlertDialog(
            onDismissRequest = { operationResultMessage = null },
            modifier = Modifier.border(1.5.dp, VantafynGradients.accentHorizontal(), RoundedCornerShape(28.dp)),
            title = {
                Text(
                    text = "Pokémon Vault",
                    color = VantafynColors.Ink,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = operationResultMessage!!,
                    color = VantafynColors.Ink,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { operationResultMessage = null }) {
                    Text("OK", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1B1E2B),
        )
    }

    // Bulk Transfer Confirmation Alert
    if (bulkTransferConfirmation != null) {
        val conf = bulkTransferConfirmation!!
        AlertDialog(
            onDismissRequest = { bulkTransferConfirmation = null },
            modifier = Modifier.border(1.5.dp, VantafynGradients.accentHorizontal(), RoundedCornerShape(28.dp)),
            title = {
                Text(
                    text = if (conf.selectedIds == null) "Transfer Entire Box?" else "Transfer ${conf.count} Pokémon?",
                    color = VantafynColors.Ink,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = if (conf.selectedIds == null)
                        "Are you sure you want to transfer all ${conf.count} Pokémon from ${conf.fromName} into ${conf.toName}?"
                    else
                        "Are you sure you want to transfer ${conf.count} selected Pokémon from ${conf.fromName} into ${conf.toName}?",
                    color = VantafynColors.Ink,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = conf
                        bulkTransferConfirmation = null
                        executeBulkTransfer(target.direction, target.selectedIds)
                    }
                ) {
                    Text("Transfer", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bulkTransferConfirmation = null }) {
                    Text("Cancel", color = VantafynColors.Muted)
                }
            },
            containerColor = Color(0xFF1B1E2B),
        )
    }

    // Trade Center Modal
    if (isTradeModalOpen) {
        val selectedTradePokemon = inspectedPokemon ?: selectedPokemonItem?.summary ?: selectedPokemonItem?.let { sel ->
            val upperMatch = upperState.vaultBox?.entries?.firstOrNull { it.id == sel.pokemonId }
            val lowerMatch = lowerState.vaultBox?.entries?.firstOrNull { it.id == sel.pokemonId }
            val match = upperMatch ?: lowerMatch
            if (match != null) {
                PokemonSummaryDto(
                    id = match.id,
                    species = match.species,
                    speciesId = match.speciesId,
                    nickname = match.nickname,
                    level = match.level,
                    isShiny = match.isShiny,
                    boxIndex = match.boxIndex,
                    slotIndex = match.slotIndex,
                )
            } else null
        }

        PokemonTradeModal(
            session = session,
            pokemonRepository = pokemonRepository,
            selectedPokemon = selectedTradePokemon,
            vaultBoxes = allVaultBoxes,
            onDismiss = { isTradeModalOpen = false },
            onTradeCompleted = {
                loadAll()
            },
        )
    }

    // Backups & System Health Diagnostics Modal
    if (isBackupRestoreModalOpen) {
        val activeGame = (lowerState.containerType as? StorageContainerType.GameCartridge)?.game
            ?: (upperState.containerType as? StorageContainerType.GameCartridge)?.game
            ?: availableGames.firstOrNull()

        PokemonBackupRestoreModal(
            session = session,
            pokemonRepository = pokemonRepository,
            selectedGame = activeGame,
            availableGames = availableGames,
            onDismiss = { isBackupRestoreModalOpen = false },
            onSaveRestored = {
                loadAll()
            },
        )
    }

    // National Pokédex Modal
    if (isPokedexModalOpen) {
        PokemonPokedexModal(
            session = session,
            pokemonRepository = pokemonRepository,
            localSaves = allDetectedSaves,
            vaultBoxes = allVaultBoxes,
            onDismiss = { isPokedexModalOpen = false },
        )
    }

    // Achievements & Social Activity Modal
    if (isAchievementsModalOpen) {
        PokemonAchievementsModal(
            session = session,
            pokemonRepository = pokemonRepository,
            localSaves = allDetectedSaves,
            vaultBoxes = allVaultBoxes,
            onDismiss = { isAchievementsModalOpen = false },
        )
    }
}
}
