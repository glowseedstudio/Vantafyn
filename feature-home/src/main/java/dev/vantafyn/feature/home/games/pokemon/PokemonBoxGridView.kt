package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

@Composable
fun PokemonBoxGridView(
    entries: List<PokemonSummaryDto>,
    selectedSlotIndex: Int?,
    onSelectSlot: (Int, PokemonSummaryDto?) -> Unit,
    modifier: Modifier = Modifier,
    isParty: Boolean = false,
    isLocked: Boolean = false,
) {
    val totalSlots = if (isParty) 6 else 30
    val columns = if (isParty) 3 else 6

    val entriesBySlot = remember(entries) {
        entries.associateBy { it.slotIndex }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(totalSlots) { index ->
            val slotIndex = index + 1
            val pokemon = entriesBySlot[slotIndex]
            val isSelected = selectedSlotIndex == slotIndex

            PokemonSlotCell(
                slotIndex = slotIndex,
                pokemon = pokemon,
                isSelected = isSelected,
                isLocked = isLocked,
                onClick = {
                    if (!isLocked) {
                        onSelectSlot(slotIndex, pokemon)
                    }
                },
            )
        }
    }
}

@Composable
fun PokemonSlotCell(
    slotIndex: Int,
    pokemon: PokemonSummaryDto?,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF00E5FF)
            pokemon != null -> Color.White.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        label = "slotBorderColor",
    )

    val backgroundModifier = if (isSelected) {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.22f),
                    Color(0xFF7C4DFF).copy(alpha = 0.15f),
                )
            ),
            RoundedCornerShape(10.dp)
        )
    } else if (pokemon != null) {
        Modifier.background(
            Color(0xFF181A24).copy(alpha = 0.85f),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.background(
            Color(0xFF10121A).copy(alpha = 0.45f),
            RoundedCornerShape(10.dp)
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(10.dp))
            .then(backgroundModifier)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(enabled = !isLocked, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (pokemon != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top row: slot number and shiny star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#$slotIndex",
                        color = VantafynColors.Muted.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (pokemon.isShiny) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Shiny",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }

                // Sprite
                val spriteUrl = remember(pokemon.speciesId, pokemon.isShiny) {
                    getPokemonSpriteUrl(pokemon.speciesId, pokemon.isShiny)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (spriteUrl.isNotBlank()) {
                        AsyncImage(
                            model = spriteUrl,
                            contentDescription = pokemon.species,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Pets,
                            contentDescription = null,
                            tint = VantafynColors.Muted.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Bottom row: Nickname/Species & Level
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pokemon.nickname.ifBlank { pokemon.species },
                        color = VantafynColors.Ink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Lv. ${pokemon.level}",
                        color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        } else {
            // Empty Slot
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "$slotIndex",
                    color = Color.White.copy(alpha = 0.15f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target",
                        color = Color(0xFF00E5FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
