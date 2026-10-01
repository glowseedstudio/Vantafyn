package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.vantafyn.core.jellyfin.PokemonDirectTransferRequest
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.jellyfin.PokemonTransferCompatibilityResult
import dev.vantafyn.core.jellyfin.PokemonTransferValidateRequest
import dev.vantafyn.core.jellyfin.PokemonVaultSummary
import dev.vantafyn.core.jellyfin.PokemonWithdrawRequest
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PokemonVaultScreen(
    session: JellyfinSession?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    pokemonRepository: JellyfinPokemonRepository = remember { DefaultJellyfinPokemonRepository() },
) {
    val coroutineScope = rememberCoroutineScope()

    var availableGames by remember { mutableStateOf<List<GameSummary>>(emptyList()) }
    var vaultSummary by remember { mutableStateOf<PokemonVaultSummary?>(null) }
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

    // Active Selection & Inspection
    var selectedPokemonItem by remember { mutableStateOf<SelectedPokemonItem?>(null) }
    var inspectedPokemon by remember { mutableStateOf<PokemonSummaryDto?>(null) }
    var compatibilityResult by remember { mutableStateOf<PokemonTransferCompatibilityResult?>(null) }
    var isValidatingCompatibility by remember { mutableStateOf(false) }
    var isExecutingTransfer by remember { mutableStateOf(false) }
    var operationResultMessage by remember { mutableStateOf<String?>(null) }
    var isTradeModalOpen by remember { mutableStateOf(false) }
    var isBackupRestoreModalOpen by remember { mutableStateOf(false) }
    var isPokedexModalOpen by remember { mutableStateOf(false) }
    var isAchievementsModalOpen by remember { mutableStateOf(false) }

    fun loadVaultSummary() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            pokemonRepository.getVaultSummary(session).onSuccess {
                vaultSummary = it
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
                    val libId = "default"
                    val saveRes = pokemonRepository.getGameSave(session, libId, target.game.id)
                    val lockRes = pokemonRepository.getGameLockState(session, libId, target.game.id)

                    val save = saveRes.getOrNull()
                    val lock = lockRes.getOrNull()

                    if (isUpper) {
                        upperState = upperState.copy(
                            gameSave = save,
                            lockState = lock,
                            isLoading = false,
                            errorMessage = saveRes.exceptionOrNull()?.message,
                        )
                    } else {
                        lowerState = lowerState.copy(
                            gameSave = save,
                            lockState = lock,
                            isLoading = false,
                            errorMessage = saveRes.exceptionOrNull()?.message,
                        )
                    }
                }
            }
        }
    }

    fun loadAll() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            isLoadingInitial = true
            val gamesRes = pokemonRepository.getPokemonGames(session)
            gamesRes.onSuccess { games ->
                availableGames = games
                // If lower state is PersonalVault, but games exist, default lower to first game for convenience
                if (lowerState.containerType is StorageContainerType.PersonalVault && games.isNotEmpty()) {
                    lowerState = lowerState.copy(containerType = StorageContainerType.GameCartridge(games.first()))
                }
            }
            loadVaultSummary()
            loadContainerData(TransferSide.Source)
            loadContainerData(TransferSide.Destination)
            isLoadingInitial = false
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

                // Game -> Game (Direct Transfer)
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

                // Vault -> Vault (Move between vault boxes)
                else -> {
                    Result.failure(Exception("Direct move between two Vault boxes can be done by selecting different target boxes."))
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 14.dp, vertical = 2.dp),
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
                CompactBackButton(onClick = onBack)
                Column {
                    Text(
                        text = "Pokémon Vault",
                        color = VantafynColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "Safe Lock Active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "Safe-Session Transaction Protection",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
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
                        Text(
                            text = "${vaultSummary!!.totalOccupied}/900",
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
                    imageVector = Icons.Rounded.SwapHoriz,
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
                        upperState = upperState.copy(containerType = target, selectedBoxIndex = 1, isPartyMode = false)
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                    },
                    onPreviousBox = {
                        val prev = if (upperState.selectedBoxIndex > 1) upperState.selectedBoxIndex - 1 else upperState.totalBoxes
                        upperState = upperState.copy(selectedBoxIndex = prev)
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                    },
                    onNextBox = {
                        val next = if (upperState.selectedBoxIndex < upperState.totalBoxes) upperState.selectedBoxIndex + 1 else 1
                        upperState = upperState.copy(selectedBoxIndex = next)
                        loadContainerData(TransferSide.Source)
                        upperSelectedSlotIndex = null
                    },
                    onTogglePartyMode = {
                        upperState = upperState.copy(isPartyMode = !upperState.isPartyMode)
                        upperSelectedSlotIndex = null
                    },
                    onSelectSlot = { slot, pokemon ->
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
                            if (selectedPokemonItem?.side == TransferSide.Source) {
                                selectedPokemonItem = null
                            }
                        }
                    },
                    onOpenDetails = { pokemon ->
                        inspectedPokemon = pokemon
                    },
                    onSortBox = { criterion, ascending ->
                        handleSortBox(TransferSide.Source, criterion, ascending)
                    },
                )

                // ACTION TRANSFER BRIDGE
                PokemonActionBridge(
                    selectedPokemon = selectedPokemonItem,
                    compatibility = compatibilityResult,
                    isValidating = isValidatingCompatibility,
                    isExecutingTransfer = isExecutingTransfer,
                    onExecuteTransfer = { executeTransfer() },
                    onInspectDetails = {
                        inspectedPokemon = selectedPokemonItem?.summary
                    },
                )

                // LOWER BOX SECTION (Destination)
                PokemonContainerBoxSection(
                    uiState = lowerState,
                    availableGames = availableGames,
                    selectedSlotIndex = lowerSelectedSlotIndex,
                    onSelectContainer = { target ->
                        lowerState = lowerState.copy(containerType = target, selectedBoxIndex = 1, isPartyMode = false)
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                    },
                    onPreviousBox = {
                        val prev = if (lowerState.selectedBoxIndex > 1) lowerState.selectedBoxIndex - 1 else lowerState.totalBoxes
                        lowerState = lowerState.copy(selectedBoxIndex = prev)
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                    },
                    onNextBox = {
                        val next = if (lowerState.selectedBoxIndex < lowerState.totalBoxes) lowerState.selectedBoxIndex + 1 else 1
                        lowerState = lowerState.copy(selectedBoxIndex = next)
                        loadContainerData(TransferSide.Destination)
                        lowerSelectedSlotIndex = null
                    },
                    onTogglePartyMode = {
                        lowerState = lowerState.copy(isPartyMode = !lowerState.isPartyMode)
                        lowerSelectedSlotIndex = null
                    },
                    onSelectSlot = { slot, pokemon ->
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
                            if (selectedPokemonItem?.side == TransferSide.Destination) {
                                selectedPokemonItem = null
                            }
                        }
                    },
                    onOpenDetails = { pokemon ->
                        inspectedPokemon = pokemon
                    },
                    onSortBox = { criterion, ascending ->
                        handleSortBox(TransferSide.Destination, criterion, ascending)
                    },
                )

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }

    // Inspection Modal
    if (inspectedPokemon != null) {
        val selectedItem = selectedPokemonItem
        val isVault = selectedItem?.isVault 
            ?: (upperState.vaultBox?.entries?.any { it.id == inspectedPokemon!!.id } == true)
            ?: (lowerState.vaultBox?.entries?.any { it.id == inspectedPokemon!!.id } == true)
        val gameId = selectedItem?.gameId
            ?: (lowerState.containerType as? StorageContainerType.GameCartridge)?.game?.id
            ?: (upperState.containerType as? StorageContainerType.GameCartridge)?.game?.id

        PokemonDetailModal(
            pokemon = inspectedPokemon!!,
            session = session,
            pokemonRepository = pokemonRepository,
            gameId = gameId,
            isVault = isVault,
            onDismiss = { inspectedPokemon = null },
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

    // Trade Center Modal
    if (isTradeModalOpen) {
        PokemonTradeModal(
            session = session,
            pokemonRepository = pokemonRepository,
            selectedPokemon = inspectedPokemon ?: selectedPokemonItem?.let { sel ->
                val upperMatch = upperState.vaultBox?.entries?.firstOrNull { it.id == sel.pokemonId }
                if (upperMatch != null) {
                    PokemonSummaryDto(
                        id = upperMatch.id,
                        species = upperMatch.species,
                        speciesId = upperMatch.speciesId,
                        nickname = upperMatch.nickname,
                        level = upperMatch.level,
                        isShiny = upperMatch.isShiny,
                        boxIndex = upperMatch.boxIndex,
                        slotIndex = upperMatch.slotIndex,
                    )
                } else null
            },
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
            onDismiss = { isPokedexModalOpen = false },
        )
    }

    // Achievements & Social Activity Modal
    if (isAchievementsModalOpen) {
        PokemonAchievementsModal(
            session = session,
            pokemonRepository = pokemonRepository,
            onDismiss = { isAchievementsModalOpen = false },
        )
    }
}
