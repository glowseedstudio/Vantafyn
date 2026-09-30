package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonJourneyDto
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailModal(
    pokemon: PokemonSummaryDto,
    session: JellyfinSession? = null,
    pokemonRepository: JellyfinPokemonRepository? = null,
    onDismiss: () -> Unit,
) {
    var journey by remember { mutableStateOf<PokemonJourneyDto?>(null) }

    LaunchedEffect(pokemon.id) {
        if (session != null && pokemonRepository != null && pokemon.id.isNotBlank()) {
            withContext(Dispatchers.IO) {
                pokemonRepository.getPokemonJourney(session, pokemon.id).onSuccess {
                    journey = it
                }
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151722))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = pokemon.nickname.ifBlank { pokemon.species },
                        color = VantafynColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (pokemon.isShiny) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Shiny",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Sprite + Overview Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1D202F))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val spriteUrl = remember(pokemon.speciesId, pokemon.isShiny) {
                    getPokemonSpriteUrl(pokemon.speciesId, pokemon.isShiny)
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF12141E)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (spriteUrl.isNotBlank()) {
                        AsyncImage(
                            model = spriteUrl,
                            contentDescription = pokemon.species,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "National Dex #${pokemon.speciesId}",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Species: ${pokemon.species}",
                        color = VantafynColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Level: ${pokemon.level}",
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                    )
                    if (!pokemon.gender.isNullOrBlank()) {
                        Text(
                            text = "Gender: ${pokemon.gender}",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            // Trainer & Origin Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF181B26))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "TRAINER & ORIGIN",
                    color = VantafynColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                InfoRow(label = "Original Trainer (OT)", value = pokemon.originalTrainer ?: "Unknown")
                if (!pokemon.originalTrainerId.isNullOrBlank()) {
                    InfoRow(label = "Trainer ID", value = pokemon.originalTrainerId!!)
                }
                InfoRow(label = "Origin Game", value = pokemon.originGame ?: "Unknown")
                if (!pokemon.currentLocation.isBlank()) {
                    InfoRow(label = "Location", value = pokemon.currentLocation)
                }
            }

            // Journey & Lineage Timeline
            if (journey != null && journey!!.steps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B1E2B))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Journey & Lineage",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    journey!!.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (step.action.lowercase()) {
                                                "encounter", "capture" -> Color(0xFF10B981)
                                                "deposit" -> Color(0xFF00E5FF)
                                                "withdraw" -> Color(0xFF8B5CF6)
                                                "trade" -> Color(0xFFFFD700)
                                                else -> Color(0xFF3B82F6)
                                            }
                                        ),
                                )
                                if (index < journey!!.steps.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(26.dp)
                                            .background(Color(0xFF2E344A)),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = step.action,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (step.timestamp.isNotBlank()) {
                                        Text(
                                            text = step.timestamp.take(10),
                                            color = VantafynColors.Muted,
                                            fontSize = 9.5.sp,
                                        )
                                    }
                                }
                                val detailsText = step.details
                                if (!detailsText.isNullOrEmpty()) {
                                    Text(
                                        text = detailsText,
                                        color = VantafynColors.Muted,
                                        fontSize = 10.5.sp,
                                    )
                                }
                                if (!step.sourceLocation.isNullOrEmpty() && !step.destinationLocation.isNullOrEmpty()) {
                                    Text(
                                        text = "${step.sourceLocation} → ${step.destinationLocation}",
                                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                        fontSize = 9.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Legality & Safety Certification
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = "Legality Verified",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    Text(
                        text = "Vantafyn Legality: Certified Legal",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Structure validated for safe cloud vault & cross-save transfers.",
                        color = Color(0xFF10B981).copy(alpha = 0.8f),
                        fontSize = 10.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = VantafynColors.Muted,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = VantafynColors.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
