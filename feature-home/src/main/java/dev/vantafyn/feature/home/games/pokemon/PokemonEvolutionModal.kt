package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonEvolutionOptionDto
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonEvolutionModal(
    pokemon: PokemonSummaryDto,
    availableEvolutions: List<PokemonEvolutionOptionDto>,
    session: JellyfinSession,
    pokemonRepository: JellyfinPokemonRepository,
    isVault: Boolean,
    onDismiss: () -> Unit,
    onEvolved: (PokemonSummaryDto) -> Unit,
) {
    var selectedOption by remember { mutableStateOf<PokemonEvolutionOptionDto?>(availableEvolutions.firstOrNull()) }
    var isEvolving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    BasicAlertDialog(onDismissRequest = { if (!isEvolving) onDismiss() }) {
        PokemonModalContainer(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = "CLOUD EVOLUTION",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                    }

                    if (!isEvolving) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Evolution Visual Transition Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF191D2C),
                                    Color(0xFF1B2032),
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // Current Pokémon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val currentSprite = remember(pokemon.speciesId, pokemon.isShiny) {
                            getPokemonSpriteUrl(pokemon.speciesId, pokemon.isShiny)
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F121C))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (currentSprite.isNotBlank()) {
                                AsyncImage(
                                    model = currentSprite,
                                    contentDescription = pokemon.species,
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Pets,
                                    contentDescription = null,
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = pokemon.nickname.ifBlank { pokemon.species },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Evolution Arrow Indicator
                    Box(
                        modifier = Modifier
                            .scale(if (isEvolving) pulseScale else 1f)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CompareArrows,
                            contentDescription = "Evolves to",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Target Evolution
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val targetId = selectedOption?.targetSpeciesId ?: 0
                        val targetSprite = remember(targetId, pokemon.isShiny) {
                            getPokemonSpriteUrl(targetId, pokemon.isShiny)
                        }
                        Box(
                            modifier = Modifier
                                .scale(if (isEvolving) pulseScale else 1f)
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.22f),
                                            Color(0xFF0F121C),
                                        )
                                    )
                                )
                                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (targetSprite.isNotBlank()) {
                                AsyncImage(
                                    model = targetSprite,
                                    contentDescription = selectedOption?.targetSpecies,
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = selectedOption?.targetSpecies ?: "—",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Available Evolution Paths Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "EVOLUTION PATHS",
                        color = VantafynColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    availableEvolutions.forEach { option ->
                        val isSelected = selectedOption == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF1D243A) else Color(0xFF161925))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable(enabled = !isEvolving && option.canEvolveNow) {
                                    selectedOption = option
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = option.triggerMethod.uppercase(),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Evolve into ${option.targetSpecies}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = option.description,
                                    color = VantafynColors.Muted,
                                    fontSize = 10.5.sp,
                                )
                            }

                            if (!option.canEvolveNow) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked",
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                // Action Button
                val currentTarget = selectedOption
                val canTrigger = isVault && currentTarget != null && currentTarget.canEvolveNow && !isEvolving

                Button(
                    onClick = {
                        if (currentTarget != null && !isEvolving) {
                            isEvolving = true
                            errorMessage = null
                            scope.launch {
                                pokemonRepository.evolveVaultEntry(
                                    session = session,
                                    entryId = pokemon.id,
                                    targetSpeciesId = currentTarget.targetSpeciesId,
                                ).onSuccess { evolvedEntry ->
                                    isEvolving = false
                                    onEvolved(evolvedEntry.toSummaryDto())
                                    onDismiss()
                                }.onFailure { error ->
                                    isEvolving = false
                                    errorMessage = error.message ?: "Failed to evolve Pokémon."
                                }
                            }
                        }
                    },
                    enabled = canTrigger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF090D1A),
                        disabledContainerColor = Color(0xFF1E2235),
                        disabledContentColor = VantafynColors.Muted,
                    ),
                ) {
                    if (isEvolving) {
                        CircularProgressIndicator(
                            color = Color(0xFF090D1A),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (!isVault) "Deposit in Vault to Evolve" else "EVOLVE TO ${currentTarget?.targetSpecies?.uppercase() ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
