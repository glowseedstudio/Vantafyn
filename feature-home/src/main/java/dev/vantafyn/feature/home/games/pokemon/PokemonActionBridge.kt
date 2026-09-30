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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.PokemonTransferCompatibilityResult
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

@Composable
fun PokemonActionBridge(
    selectedPokemon: SelectedPokemonItem?,
    compatibility: PokemonTransferCompatibilityResult?,
    isValidating: Boolean,
    isExecutingTransfer: Boolean,
    onExecuteTransfer: () -> Unit,
    onInspectDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161822))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        if (selectedPokemon != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Selected Pokémon Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val spriteUrl = remember(selectedPokemon.summary.speciesId, selectedPokemon.summary.isShiny) {
                            getPokemonSpriteUrl(selectedPokemon.summary.speciesId, selectedPokemon.summary.isShiny)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222636)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (spriteUrl.isNotBlank()) {
                                AsyncImage(
                                    model = spriteUrl,
                                    contentDescription = selectedPokemon.summary.species,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Pets,
                                    contentDescription = null,
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = selectedPokemon.summary.nickname.ifBlank { selectedPokemon.summary.species },
                                    color = VantafynColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (selectedPokemon.summary.isShiny) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Shiny",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                            Text(
                                text = "Lv. ${selectedPokemon.summary.level} • ${selectedPokemon.summary.species}${selectedPokemon.summary.originalTrainer?.let { " • OT: $it" } ?: ""}",
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    // Inspect Button
                    OutlinedButton(
                        onClick = onInspectDetails,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Inspect",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inspect",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Transfer Direction Action & Compatibility Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Compatibility explanation
                    if (isValidating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "Checking migration rules...",
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                            )
                        }
                    } else if (compatibility != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = if (compatibility.isCompatible) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (compatibility.isCompatible) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = compatibility.reason.ifBlank {
                                    if (compatibility.isCompatible) "Compatible Transfer" else "Incompatible Transfer"
                                },
                                color = if (compatibility.isCompatible) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = if (selectedPokemon.side == TransferSide.Source) "Ready to move downward" else "Ready to move upward",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                        )
                    }

                    // Main Transfer Action Button
                    val isSource = selectedPokemon.side == TransferSide.Source
                    val actionLabel = if (isSource) "Transfer Down ↓" else "Transfer Up ↑"
                    val actionIcon = if (isSource) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

                    Button(
                        onClick = onExecuteTransfer,
                        enabled = !isExecutingTransfer && (compatibility == null || compatibility.isCompatible),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (compatibility?.isCompatible != false) VantafynGradients.accentHorizontal()
                                else androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.3f))
                            ),
                    ) {
                        if (isExecutingTransfer) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = actionLabel,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        } else {
            // Idle State: Prompt user to tap a Pokémon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SyncAlt,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select a Pokémon from Upper or Lower box to inspect or transfer",
                    color = VantafynColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
