package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonPokedexEntryDto
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients

/**
 * Authentic, encyclopedic Pokédex Entry modal presenting Pokémon species lore, physical stats,
 * canonical base stats, typing, and personal PKVault save lineage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonPokedexEntryModal(
    entry: PokemonPokedexEntryDto,
    matchedSpecimen: PokemonSummaryDto? = null,
    matchedDetails: PokemonDetailsDto? = null,
    totalOwnedCount: Int = 0,
    onInspectSpecimen: ((PokemonSummaryDto, PokemonDetailsDto?) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val dexData = remember(entry.speciesId) {
        PokemonPokedexCatalog.getPokedexData(entry.speciesId)
    }
    val (primaryType, secondaryType) = remember(entry.speciesId, dexData.name) {
        PokemonTypeCatalog.getTypes(entry.speciesId, dexData.name)
    }

    var showShinyArtwork by remember { mutableStateOf(entry.hasShiny) }

    val artworkUrl = remember(entry.speciesId, showShinyArtwork) {
        if (showShinyArtwork) {
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/shiny/${entry.speciesId}.png"
        } else {
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${entry.speciesId}.png"
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.92f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0C0E17))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        primaryType.accentColor.copy(alpha = 0.5f),
                        Color(0xFF1E2336),
                        Color(0xFF111422),
                    )
                ),
                RoundedCornerShape(24.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(primaryType.accentColor.copy(alpha = 0.18f))
                            .border(1.dp, primaryType.accentColor.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = "Pokédex",
                            tint = primaryType.accentColor,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "No. %04d".format(entry.speciesId),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = "• ${dexData.regionName} (${dexData.generationName})",
                                color = VantafynColors.Muted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            text = "NATIONAL POKÉDEX",
                            color = primaryType.accentColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }
                }

                // Clean Close Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161928))
                        .border(1.dp, Color(0xFF282F48), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Hero Artwork & Type Backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryType.accentColor.copy(alpha = 0.35f),
                                (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.15f),
                                Color(0xFF121420),
                            ),
                        )
                    )
                    .border(
                        1.dp,
                        primaryType.accentColor.copy(alpha = 0.25f),
                        RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // High-resolution Official Pokémon Artwork (clean backdrop without numbers)
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = dexData.name,
                    modifier = Modifier
                        .size(195.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )

                // Shiny toggle chip (if user has unlocked shiny in their save archive)
                if (entry.hasShiny) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (showShinyArtwork) Color(0xFFFFD700).copy(alpha = 0.25f)
                                else Color(0xFF1B1E2E).copy(alpha = 0.8f)
                            )
                            .border(
                                1.dp,
                                if (showShinyArtwork) Color(0xFFFFD700) else Color(0xFF38405E),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { showShinyArtwork = !showShinyArtwork }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Shiny Form",
                                tint = if (showShinyArtwork) Color(0xFFFFD700) else VantafynColors.Muted,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (showShinyArtwork) "Shiny Art" else "Normal Art",
                                color = if (showShinyArtwork) Color(0xFFFFD700) else VantafynColors.Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Species Title, Classification & Types
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = dexData.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                )

                Text(
                    text = dexData.category,
                    color = primaryType.accentColor.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Type Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeBadge(type = primaryType)
                    if (secondaryType != null) {
                        TypeBadge(type = secondaryType)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Registration Status Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.isCaught) {
                    StatusPill(
                        label = "CAUGHT IN ARCHIVE",
                        tint = Color(0xFF10B981),
                        icon = Icons.Rounded.CatchingPokemon,
                    )
                } else if (entry.isSeen) {
                    StatusPill(
                        label = "SEEN IN BATTLE",
                        tint = Color(0xFF00E5FF),
                        icon = Icons.Rounded.Visibility,
                    )
                }

                if (entry.hasShiny) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        label = "SHINY DISCOVERED",
                        tint = Color(0xFFFFD700),
                        icon = Icons.Rounded.AutoAwesome,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Physical Attributes Card (Height / Weight / Archetype)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141724))
                    .border(1.dp, Color(0xFF252B40), RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhysicalSpecItem(
                    label = "HEIGHT",
                    metricValue = "%.1f m".format(dexData.heightMeters),
                    imperialValue = dexData.heightFeetInches,
                    icon = Icons.Rounded.Straighten,
                    accentColor = Color(0xFF38BDF8),
                )

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0xFF2B324D)),
                )

                PhysicalSpecItem(
                    label = "WEIGHT",
                    metricValue = "%.1f kg".format(dexData.weightKg),
                    imperialValue = dexData.weightLbs,
                    icon = Icons.Rounded.Scale,
                    accentColor = Color(0xFFF97316),
                )

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0xFF2B324D)),
                )

                PhysicalSpecItem(
                    label = "ARCHETYPE",
                    metricValue = dexData.archetype,
                    imperialValue = "BST ${dexData.bst}",
                    icon = Icons.Rounded.Public,
                    accentColor = primaryType.accentColor,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pokédex Lore / Description Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF131726),
                                Color(0xFF0F121E),
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                primaryType.accentColor.copy(alpha = 0.4f),
                                Color(0xFF262C42),
                            )
                        ),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(primaryType.accentColor),
                    )
                    Text(
                        text = "POKÉDEX ENTRY",
                        color = primaryType.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }

                Text(
                    text = "“${dexData.flavorText}”",
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Species Base Stats Profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141724))
                    .border(1.dp, Color(0xFF252B40), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "BASE STAT SPREAD",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryType.accentColor.copy(alpha = 0.2f))
                            .border(1.dp, primaryType.accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "BST ${dexData.bst}",
                            color = primaryType.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                StatBarRow(label = "HP", value = dexData.hp, barColor = Color(0xFF10B981))
                StatBarRow(label = "Attack", value = dexData.attack, barColor = Color(0xFFF97316))
                StatBarRow(label = "Defense", value = dexData.defense, barColor = Color(0xFFFBBF24))
                StatBarRow(label = "Sp. Atk", value = dexData.spAtk, barColor = Color(0xFF06B6D4))
                StatBarRow(label = "Sp. Def", value = dexData.spDef, barColor = Color(0xFF6366F1))
                StatBarRow(label = "Speed", value = dexData.speed, barColor = Color(0xFFEC4899))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trainer Archival Records (PKVault Lineage)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141724))
                    .border(1.dp, Color(0xFF252B40), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                    )
                    Text(
                        text = "VAULT ARCHIVE LINEAGE",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }

                // First Registered Game (Stacked so long titles have complete breathing room)
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "FIRST REGISTERED GAME",
                        color = VantafynColors.Muted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = entry.firstEncounteredGame ?: "PKVault Save Archive",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Two balanced stat tiles for Encounters and Living Specimens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Encounters Tile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F121C))
                            .border(1.dp, Color(0xFF1E2336), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "ENCOUNTERS",
                                color = VantafynColors.Muted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = "${entry.encounterCount.coerceAtLeast(1)} across saves",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Living Specimens Tile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F121C))
                            .border(
                                1.dp,
                                if (totalOwnedCount > 0) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF1E2336),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "LIVING SPECIMENS",
                                color = VantafynColors.Muted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = if (totalOwnedCount > 0) "$totalOwnedCount in saves" else "None in saves",
                                color = if (totalOwnedCount > 0) Color(0xFF10B981) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Optional Button to Inspect Caught Specimen
            if (matchedSpecimen != null && onInspectSpecimen != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(VantafynGradients.accentHorizontal())
                        .clickable { onInspectSpecimen(matchedSpecimen, matchedDetails) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CatchingPokemon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Inspect Caught Specimen (Lv. ${matchedSpecimen.level})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TypeBadge(type: PokemonType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(type.accentColor.copy(alpha = 0.2f))
            .border(1.dp, type.accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = type.displayName.uppercase(),
            color = type.accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun PhysicalSpecItem(
    label: String,
    metricValue: String,
    imperialValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                color = VantafynColors.Muted,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
        Text(
            text = metricValue,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = imperialValue,
            color = VantafynColors.Muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatBarRow(
    label: String,
    value: Int,
    barColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = VantafynColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(55.dp),
        )

        Text(
            text = "%3d".format(value),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End,
        )

        LinearProgressIndicator(
            progress = { (value / 255f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = Color(0xFF1E2235),
            strokeCap = StrokeCap.Round,
        )
    }
}
