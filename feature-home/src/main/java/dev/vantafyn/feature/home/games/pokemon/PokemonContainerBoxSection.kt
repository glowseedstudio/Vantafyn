package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

@Composable
fun PokemonContainerBoxSection(
    uiState: ContainerUiState,
    availableGames: List<GameSummary>,
    selectedSlotIndex: Int?,
    onSelectContainer: (StorageContainerType) -> Unit,
    onPreviousBox: () -> Unit,
    onNextBox: () -> Unit,
    onTogglePartyMode: () -> Unit,
    onSelectSlot: (Int, PokemonSummaryDto?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF13151E).copy(alpha = 0.90f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        // Top selector bar: Container Dropdown & Side Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Container Dropdown (Personal Vault or Game)
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E2B))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .clickable { isDropdownExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val icon = when (uiState.containerType) {
                        is StorageContainerType.PersonalVault -> Icons.Rounded.Inventory2
                        is StorageContainerType.GameCartridge -> Icons.Rounded.SportsEsports
                    }
                    val label = when (val type = uiState.containerType) {
                        is StorageContainerType.PersonalVault -> "Personal Vault (Cloud)"
                        is StorageContainerType.GameCartridge -> type.game.cleanTitle
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = label,
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(180.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "Expand",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1B1E2B)),
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Personal Vault (Cloud)",
                                    color = VantafynColors.Ink,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        onClick = {
                            isDropdownExpanded = false
                            onSelectContainer(StorageContainerType.PersonalVault)
                        },
                    )

                    availableGames.forEach { game ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SportsEsports,
                                        contentDescription = null,
                                        tint = VantafynColors.Muted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Column {
                                        Text(
                                            text = game.cleanTitle,
                                            color = VantafynColors.Ink,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = game.systemId.uppercase(),
                                            color = VantafynColors.Muted,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                isDropdownExpanded = false
                                onSelectContainer(StorageContainerType.GameCartridge(game))
                            },
                        )
                    }
                }
            }

            // Party vs Box Toggle (Only for GameCartridge)
            if (uiState.containerType is StorageContainerType.GameCartridge) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (uiState.isPartyMode) Color(0xFF7C4DFF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (uiState.isPartyMode) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable(onClick = onTogglePartyMode)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (uiState.isPartyMode) "Party (6)" else "Boxes",
                        color = if (uiState.isPartyMode) Color(0xFF7C4DFF) else VantafynColors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text(
                    text = "${uiState.side.name.uppercase()}",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Closed-Game Safety Warning Banner (if game session is active)
        AnimatedVisibility(visible = uiState.isLocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Game is currently running! Close game on your device to edit storage safely.",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Box Carousel Navigation Header (< Box Title & Occupancy >)
        if (!uiState.isPartyMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousBox,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous Box",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = uiState.currentBoxName,
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "(${uiState.currentOccupiedCount}/${uiState.currentCapacity})",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                    )
                }

                IconButton(
                    onClick = onNextBox,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next Box",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Pokemon Slots Grid
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            val entries = when (uiState.containerType) {
                is StorageContainerType.PersonalVault -> {
                    uiState.vaultBox?.entries?.map { e ->
                        PokemonSummaryDto(
                            id = e.id,
                            species = e.species,
                            speciesId = e.speciesId,
                            form = e.form,
                            nickname = e.nickname,
                            level = e.level,
                            gender = e.gender,
                            isShiny = e.isShiny,
                            originalTrainer = e.originalTrainer,
                            originalTrainerId = e.originalTrainerId,
                            originGame = e.originGame,
                            currentLocation = e.currentLocation,
                            boxIndex = e.boxIndex,
                            slotIndex = e.slotIndex,
                            isInParty = false,
                        )
                    } ?: emptyList()
                }
                is StorageContainerType.GameCartridge -> {
                    if (uiState.isPartyMode) {
                        uiState.gameSave?.party ?: emptyList()
                    } else {
                        val currentBox = uiState.gameSave?.boxes?.firstOrNull { it.boxIndex == uiState.selectedBoxIndex }
                        currentBox?.entries ?: emptyList()
                    }
                }
            }

            PokemonBoxGridView(
                entries = entries,
                selectedSlotIndex = selectedSlotIndex,
                onSelectSlot = onSelectSlot,
                isParty = uiState.isPartyMode,
                isLocked = uiState.isLocked,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
