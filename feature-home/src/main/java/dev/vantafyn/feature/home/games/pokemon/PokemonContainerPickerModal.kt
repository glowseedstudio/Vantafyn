package dev.vantafyn.feature.home.games.pokemon

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

/**
 * Pokémon HOME-style storage container selector dialog.
 * Displays high-res game box art cards and a featured Personal Cloud Vault option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonContainerPickerModal(
    currentContainer: StorageContainerType,
    availableGames: List<GameSummary>,
    onSelectContainer: (StorageContainerType) -> Unit,
    onDismiss: () -> Unit,
) {
    val isVaultSelected = currentContainer is StorageContainerType.PersonalVault
    val selectedGameId = (currentContainer as? StorageContainerType.GameCartridge)?.game?.id

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        PokemonModalContainer(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Select Pokémon Storage",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Connect to Cloud Vault or Game Cartridge",
                            color = VantafynColors.Muted,
                            fontSize = 11.5.sp,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Grid of Storage Containers
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 1. Featured Personal Cloud Vault Card (Spans full width)
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF1E2438),
                                        Color(0xFF171B2B),
                                    )
                                )
                            )
                            .border(
                                width = if (isVaultSelected) 2.dp else 1.dp,
                                brush = if (isVaultSelected) VantafynGradients.accentHorizontal()
                                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f))),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                onSelectContainer(StorageContainerType.PersonalVault)
                                onDismiss()
                            }
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF00E5FF).copy(alpha = 0.22f),
                                                    Color(0xFF8B5CF6).copy(alpha = 0.22f),
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF00E5FF).copy(alpha = 0.45f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Personal Vault (Cloud)",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "32 Storage Boxes • 960 Slots • Cross-Game Compatible",
                                        color = VantafynColors.Muted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            if (isVaultSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Section Header: Game Cartridges
                if (availableGames.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Detected Pokémon Games (${availableGames.size})",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }

                    // 3. Grid of Game Cards with Box Art
                    items(availableGames, key = { it.id }) { game ->
                        GameBoxArtPickerCard(
                            game = game,
                            isSelected = selectedGameId == game.id,
                            onSelect = {
                                onSelectContainer(StorageContainerType.GameCartridge(game))
                                onDismiss()
                            },
                        )
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No Pokémon game saves detected.",
                                color = VantafynColors.Muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun GameBoxArtPickerCard(
    game: GameSummary,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val context = LocalContext.current
    val effectiveBoxart = remember(game.id, game.boxartUrl) {
        val local = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
            .getString("boxart_${game.id}", null)
        GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181B2A))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) VantafynGradients.accentHorizontal()
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.10f))),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Box Art Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D0F18)),
            contentAlignment = Alignment.Center,
        ) {
            if (!effectiveBoxart.isNullOrBlank()) {
                AsyncImage(
                    model = effectiveBoxart,
                    contentDescription = game.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = VantafynColors.Muted.copy(alpha = 0.35f),
                    modifier = Modifier.size(36.dp),
                )
            }

            // Platform Badge in top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0B0E17).copy(alpha = 0.85f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = game.systemId.uppercase(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Active Badge in top-right
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "ACTIVE",
                        color = Color(0xFF0B0E17),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        // Title and Save Info
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = game.pokemon?.canonicalTitle ?: game.cleanTitle,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val hasSave = game.pokemon?.hasSave == true
                Icon(
                    imageVector = if (hasSave) Icons.Rounded.CheckCircle else Icons.Rounded.Save,
                    contentDescription = null,
                    tint = if (hasSave) Color(0xFF10B981) else VantafynColors.Muted,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = if (hasSave) "Save File Linked" else "Battery Save",
                    color = if (hasSave) Color(0xFF10B981) else VantafynColors.Muted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
