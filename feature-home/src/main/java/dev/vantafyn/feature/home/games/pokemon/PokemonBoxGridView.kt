package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog
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
    onOpenDetails: ((PokemonSummaryDto) -> Unit)? = null,
) {
    val totalSlots = if (isParty) 6 else 30
    val columns = if (isParty) 3 else 6
    val rows = totalSlots / columns

    val entriesBySlot = remember(entries) {
        entries.associateBy { it.slotIndex }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(if (isParty) 6.dp else 3.dp),
        verticalArrangement = Arrangement.spacedBy(if (isParty) 6.dp else 3.dp),
    ) {
        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isParty) 6.dp else 3.dp),
            ) {
                for (colIndex in 0 until columns) {
                    val slotIndex = (rowIndex * columns) + colIndex + 1
                    val pokemon = entriesBySlot[slotIndex]
                    val isSelected = selectedSlotIndex == slotIndex

                    PokemonSlotCell(
                        slotIndex = slotIndex,
                        pokemon = pokemon,
                        isSelected = isSelected,
                        isLocked = isLocked,
                        isParty = isParty,
                        onClick = {
                            if (!isLocked) {
                                onSelectSlot(slotIndex, pokemon)
                            }
                        },
                        onOpenDetails = onOpenDetails?.let { cb ->
                            {
                                if (pokemon != null) {
                                    cb(pokemon)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
    isParty: Boolean = false,
    onOpenDetails: (() -> Unit)? = null,
) {
    var lastTapTime by remember { mutableLongStateOf(0L) }

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
            RoundedCornerShape(if (isParty) 10.dp else 8.dp)
        )
    } else if (pokemon != null) {
        Modifier.background(
            Color(0xFF181A24).copy(alpha = 0.85f),
            RoundedCornerShape(if (isParty) 10.dp else 8.dp)
        )
    } else {
        Modifier.background(
            Color(0xFF10121A).copy(alpha = 0.45f),
            RoundedCornerShape(if (isParty) 10.dp else 8.dp)
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(if (isParty) 0.85f else 0.72f)
            .clip(RoundedCornerShape(if (isParty) 10.dp else 8.dp))
            .then(backgroundModifier)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(if (isParty) 10.dp else 8.dp),
            )
            .clickable(
                enabled = !isLocked,
                onClick = {
                    val now = System.currentTimeMillis()
                    if (pokemon != null && onOpenDetails != null && (isSelected || (now - lastTapTime < 380L))) {
                        onOpenDetails()
                    } else {
                        onClick()
                    }
                    lastTapTime = now
                }
            )
            .padding(if (isParty) 4.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (pokemon != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Top row: slot number, legality alert, level (in box mode), and shiny star
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isParty) 2.dp else 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "#$slotIndex",
                            color = VantafynColors.Muted.copy(alpha = 0.5f),
                            fontSize = if (isParty) 9.sp else 7.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        if (pokemon.legalityStatus.equals("illegal", ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Legality Issue",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(if (isParty) 9.dp else 7.dp),
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        if (!isParty) {
                            Text(
                                text = "L${pokemon.level}",
                                color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted.copy(alpha = 0.85f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                        if (pokemon.isShiny) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Shiny",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(if (isParty) 10.dp else 8.dp),
                            )
                        }
                    }
                }

                // Sprite - dynamically expanded to fill cell prominently in storage boxes
                val isFemale = pokemon.gender?.equals("Female", ignoreCase = true) == true ||
                    pokemon.gender?.equals("Girl", ignoreCase = true) == true ||
                    pokemon.gender?.equals("F", ignoreCase = true) == true
                val spriteUrl = remember(pokemon.speciesId, pokemon.isShiny, isFemale) {
                    getPokemonSpriteUrl(pokemon.speciesId, pokemon.isShiny, isFemale = isFemale)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (spriteUrl.isNotBlank()) {
                        AsyncImage(
                            model = spriteUrl,
                            contentDescription = pokemon.species,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(if (isParty) 1.0f else 1.25f),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Pets,
                            contentDescription = null,
                            tint = VantafynColors.Muted.copy(alpha = 0.4f),
                            modifier = Modifier.size(if (isParty) 20.dp else 26.dp),
                        )
                    }
                }

                val displayName = remember(pokemon.speciesId, pokemon.species, pokemon.nickname) {
                    when {
                        pokemon.nickname.isNotBlank() && !pokemon.nickname.equals(pokemon.species, ignoreCase = true) && !pokemon.nickname.startsWith("#") -> pokemon.nickname
                        pokemon.species.isNotBlank() && !pokemon.species.startsWith("#") -> pokemon.species
                        else -> {
                            val resolved = PokemonSpeciesCatalog.resolveSpeciesName(pokemon.speciesId)
                            if (resolved.isNotBlank() && !resolved.startsWith("#")) resolved
                            else pokemon.nickname.ifBlank { pokemon.species }
                        }
                    }
                }

                // Bottom row: Nickname/Species & Level
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp),
                ) {
                    Text(
                        text = displayName,
                        color = VantafynColors.Ink,
                        fontSize = if (isParty) 10.sp else 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (isParty) {
                        Text(
                            text = "Lv. ${pokemon.level}",
                            color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }
        } else {
            // Empty Slot
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = "$slotIndex",
                    color = Color.White.copy(alpha = 0.15f),
                    fontSize = if (isParty) 11.sp else 9.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Target",
                        color = Color(0xFF00E5FF),
                        fontSize = if (isParty) 8.sp else 7.5.sp,
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
