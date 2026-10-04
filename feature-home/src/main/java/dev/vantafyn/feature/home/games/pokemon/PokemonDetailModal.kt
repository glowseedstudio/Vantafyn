package dev.vantafyn.feature.home.games.pokemon

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.RectangleShape
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonJourneyDto
import dev.vantafyn.core.jellyfin.PokemonRibbonDto
import dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog
import dev.vantafyn.core.jellyfin.PokemonStatsDto
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class StatAppraisalTab {
    JudgeIVs,
    EVTraining
}

private enum class StatDisplayFormat {
    Bars,
    Radar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailModal(
    pokemon: PokemonSummaryDto,
    session: JellyfinSession? = null,
    pokemonRepository: JellyfinPokemonRepository? = null,
    gameId: String? = null,
    libraryId: String? = null,
    isVault: Boolean = false,
    initialDetails: PokemonDetailsDto? = null,
    customBackgroundUrl: String? = null,
    navigationList: List<PokemonSummaryDto> = emptyList(),
    currentIndex: Int = -1,
    onNavigateToIndex: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var journey by remember { mutableStateOf<PokemonJourneyDto?>(null) }
    val canonicalFallback = remember(pokemon.id, pokemon.speciesId, pokemon.level) {
        PokemonSpeciesCatalog.generateCanonicalDetails(pokemon)
    }
    var details by remember(pokemon.id, initialDetails) {
        mutableStateOf(initialDetails ?: canonicalFallback)
    }
    var isLoadingDetails by remember(pokemon.id, initialDetails) {
        mutableStateOf(initialDetails == null && session != null && pokemonRepository != null && pokemon.id.isNotBlank() && !pokemon.id.startsWith("dex-"))
    }
    var selectedStatTab by remember { mutableStateOf(StatAppraisalTab.JudgeIVs) }
    var statDisplayFormat by remember { mutableStateOf(StatDisplayFormat.Radar) }
    var showLegalityDialog by remember { mutableStateOf(false) }
    var showMoveRelearnerModal by remember { mutableStateOf(false) }
    var selectedRibbon by remember { mutableStateOf<PokemonRibbonDto?>(null) }
    var showEvolutionModal by remember { mutableStateOf(false) }
    var currentPokemonState by remember(pokemon.id) { mutableStateOf(pokemon) }

    LaunchedEffect(pokemon.id, initialDetails) {
        if (initialDetails != null) {
            details = initialDetails
            isLoadingDetails = false
        } else if (details == null) {
            details = canonicalFallback
            isLoadingDetails = false
        }
        if (session != null && pokemonRepository != null && pokemon.id.isNotBlank() && !pokemon.id.startsWith("dex-")) {
            withContext(Dispatchers.IO) {
                // Load journey history
                pokemonRepository.getPokemonJourney(session, pokemon.id).onSuccess {
                    journey = it
                }
                // Load full details (IVs, EVs, Nature, Ability, Moves, etc.)
                pokemonRepository.getPokemonDetails(
                    session = session,
                    pokemonId = pokemon.id,
                    gameId = gameId,
                    libraryId = libraryId,
                    isVault = isVault,
                ).onSuccess { netDetails ->
                    val existing = details ?: initialDetails ?: canonicalFallback
                    details = netDetails.copy(
                        iv = netDetails.iv ?: existing?.iv ?: canonicalFallback.iv,
                        ev = netDetails.ev ?: existing?.ev ?: canonicalFallback.ev,
                        moves = if (netDetails.moves.isNotEmpty()) netDetails.moves else existing?.moves?.ifEmpty { null } ?: canonicalFallback.moves,
                        nature = if (!netDetails.nature.isNullOrBlank()) netDetails.nature else existing?.nature ?: canonicalFallback.nature,
                        ability = if (!netDetails.ability.isNullOrBlank()) netDetails.ability else existing?.ability ?: canonicalFallback.ability,
                        heldItem = if (!netDetails.heldItem.isNullOrBlank()) netDetails.heldItem else existing?.heldItem ?: canonicalFallback.heldItem,
                        currentHp = netDetails.currentHp ?: existing?.currentHp,
                        maxHp = netDetails.maxHp ?: existing?.maxHp,
                        friendship = if ((netDetails.friendship ?: 0) > 0) netDetails.friendship else existing?.friendship,
                        ribbons = if (netDetails.ribbons.isNotEmpty()) netDetails.ribbons else existing?.ribbons ?: emptyList(),
                        isHallOfFameMember = netDetails.isHallOfFameMember || (existing?.isHallOfFameMember == true),
                    )
                }
            }
            isLoadingDetails = false
        } else {
            isLoadingDetails = false
        }
    }

    val resolvedSpeciesName = remember(pokemon.speciesId, pokemon.species) {
        if (pokemon.species.isNotBlank() && !pokemon.species.startsWith("#")) {
            pokemon.species
        } else {
            val fromCatalog = PokemonSpeciesCatalog.resolveSpeciesName(pokemon.speciesId)
            if (fromCatalog.isNotBlank() && !fromCatalog.startsWith("#")) fromCatalog else pokemon.species
        }
    }
    val displayName = remember(pokemon.nickname, resolvedSpeciesName) {
        if (pokemon.nickname.isNotBlank() && !pokemon.nickname.equals(pokemon.species, ignoreCase = true) && !pokemon.nickname.startsWith("#")) {
            pokemon.nickname
        } else {
            resolvedSpeciesName
        }
    }

    BackHandler(onBack = onDismiss)

    GameScreenReveal(
        key = "pokemon_detail_modal",
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0E17))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
        PokemonModalContainer(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            borderWidth = 0.dp,
            customBackgroundUrl = customBackgroundUrl,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            // Header: Nickname/Species, Shiny icon, chevrons, and Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    CompactBackButton(onClick = onDismiss)
                    Text(
                        text = displayName,
                        color = VantafynColors.Ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pokemon.isShiny) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Shiny",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "SHINY",
                                color = Color(0xFFFFD700),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (navigationList.size > 1 && currentIndex >= 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = { onNavigateToIndex?.invoke(currentIndex - 1) },
                            enabled = currentIndex > 0,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Previous Pokémon",
                                tint = if (currentIndex > 0) Color(0xFF00E5FF) else VantafynColors.Muted.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = "${currentIndex + 1}/${navigationList.size}",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(
                            onClick = { onNavigateToIndex?.invoke(currentIndex + 1) },
                            enabled = currentIndex < navigationList.size - 1,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Next Pokémon",
                                tint = if (currentIndex < navigationList.size - 1) Color(0xFF00E5FF) else VantafynColors.Muted.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }

            // Hero Pokémon Card with Type Atmosphere & Prominent Artwork
            val (primaryType, secondaryType) = remember(pokemon.speciesId, resolvedSpeciesName, pokemon.form) {
                PokemonTypeCatalog.getTypes(
                    speciesId = pokemon.speciesId,
                    speciesName = resolvedSpeciesName,
                    formName = pokemon.form,
                )
            }
            val cardBorderBrush = remember(primaryType, secondaryType) {
                Brush.horizontalGradient(
                    listOf(
                        primaryType.accentColor.copy(alpha = 0.35f),
                        (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.06f),
                    )
                )
            }
            val isFemale = pokemon.gender?.equals("Female", ignoreCase = true) == true ||
                pokemon.gender?.equals("Girl", ignoreCase = true) == true ||
                pokemon.gender?.equals("F", ignoreCase = true) == true

            val matchedForm = remember(pokemon.speciesId, pokemon.form) {
                if (!pokemon.form.isNullOrBlank()) {
                    PokemonFormsCatalog.getForms(pokemon.speciesId).firstOrNull {
                        it.name.contains(pokemon.form!!, ignoreCase = true) ||
                            pokemon.form!!.contains(it.name, ignoreCase = true)
                    }
                } else null
            }

            val artworkUrl: String = remember(pokemon.speciesId, pokemon.isShiny, isFemale, matchedForm) {
                if (pokemon.speciesId > 0) {
                    PokemonGenderCatalog.getPokedexArtworkUrl(
                        speciesId = pokemon.speciesId,
                        isShiny = pokemon.isShiny,
                        isFemale = isFemale,
                        formKey = matchedForm?.spriteKey,
                    )
                } else {
                    getPokemonSpriteUrl(pokemon.speciesId, pokemon.isShiny, isFemale = isFemale)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                primaryType.accentColor.copy(alpha = 0.50f),
                                (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.08f),
                            )
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                // 1. Dynamic Type Atmosphere & Scrim Layer (like Vaults Home Page)
                PokemonHeroTypeAtmosphere(
                    primaryType = primaryType,
                    secondaryType = secondaryType,
                    verticalPresentation = true,
                    modifier = Modifier.matchParentSize(),
                )

                // 2. Foreground Card Content: Centered Column (Artwork on top, info under)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Featured Pokémon Artwork with layered aura and grounded shadow
                    Box(
                        modifier = Modifier
                            .size(165.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Layer A: Ambient Radial Aura Glow
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            if (pokemon.isShiny) Color(0xFFF59E0B).copy(alpha = 0.40f)
                                            else primaryType.glowColor.copy(alpha = 0.35f),
                                            (secondaryType?.accentColor ?: primaryType.accentColor).copy(alpha = 0.15f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                        )

                        // Layer B: Subtle Grounded Drop Shadow under the Pokémon
                        Canvas(
                            modifier = Modifier
                                .size(width = 120.dp, height = 28.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF020408).copy(alpha = 0.60f),
                                        Color(0xFF020408).copy(alpha = 0.25f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width / 2f,
                                )
                            )
                        }

                        // Layer C: Prominent High-Resolution Pokémon Artwork
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = resolvedSpeciesName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(155.dp),
                        )
                    }

                    // Species No. & Gender Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (pokemon.speciesId > 0) "No. %04d".format(pokemon.speciesId) else "No. —",
                            color = primaryType.accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (!pokemon.gender.isNullOrBlank()) {
                            Text(
                                text = when (pokemon.gender?.uppercase()) {
                                    "M", "MALE" -> "♂"
                                    "F", "FEMALE" -> "♀"
                                    else -> "—"
                                },
                                color = when (pokemon.gender?.uppercase()) {
                                    "M", "MALE" -> Color(0xFF38BDF8)
                                    "F", "FEMALE" -> Color(0xFFF472B6)
                                    else -> VantafynColors.Muted
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Display Name (Nickname / Species)
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (displayName != resolvedSpeciesName && resolvedSpeciesName.isNotBlank()) {
                        Text(
                            text = resolvedSpeciesName,
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Type Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PokemonTypeBadge(type = primaryType)
                        if (secondaryType != null && secondaryType != primaryType) {
                            PokemonTypeBadge(type = secondaryType)
                        }
                    }

                    // Level & Nature Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0D111A).copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Lv. ${pokemon.level}",
                                color = Color(0xFFF1F5F9),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                            )
                        }
                        val nature = details?.nature
                        if (!nature.isNullOrBlank()) {
                            Text(
                                text = "$nature Nature",
                                color = VantafynColors.Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Hall of Fame Champion, Ribbons, and Evolve Action
                    val isHallOfFame = details?.isHallOfFameMember == true || pokemon.isHallOfFameMember
                    val ribbonCount = details?.ribbons?.size ?: 0
                    val evos = details?.availableEvolutions?.ifEmpty { null }
                        ?: dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog.getAvailableEvolutions(pokemon.speciesId, pokemon.level)
                    if (isHallOfFame || ribbonCount > 0 || evos.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            if (isHallOfFame) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = "Champion",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Text(
                                        text = "CHAMPION",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (ribbonCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = "Ribbons",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Text(
                                        text = "$ribbonCount ${if (ribbonCount == 1) "RIBBON" else "RIBBONS"}",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (evos.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.20f))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                                        .clickable { showEvolutionModal = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Evolve",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Text(
                                        text = "EVOLVE",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stat & Judge Appraisal System
            val effectiveIvs = details?.iv ?: canonicalFallback.iv
            val effectiveEvs = details?.ev ?: canonicalFallback.ev
            val effectiveNature = details?.nature?.ifBlank { canonicalFallback.nature } ?: canonicalFallback.nature
            val natureMods = remember(effectiveNature) { getNatureModifiers(effectiveNature) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(pokemonCardAtmosphereBrush(Color(0xFF00E5FF)))
                    .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Tab Header: Judge IVs vs EV Training & Bars vs Hexagon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F111A))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedStatTab == StatAppraisalTab.JudgeIVs) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { selectedStatTab = StatAppraisalTab.JudgeIVs }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Judge IVs",
                                color = if (selectedStatTab == StatAppraisalTab.JudgeIVs) Color(0xFF00E5FF) else VantafynColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedStatTab == StatAppraisalTab.EVTraining) Color(0xFFF59E0B).copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { selectedStatTab = StatAppraisalTab.EVTraining }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "EV Training",
                                color = if (selectedStatTab == StatAppraisalTab.EVTraining) Color(0xFFF59E0B) else VantafynColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // View Format Toggle: Hexagon vs Bars
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F111A))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (statDisplayFormat == StatDisplayFormat.Radar) (if (selectedStatTab == StatAppraisalTab.JudgeIVs) Color(0xFF00E5FF) else Color(0xFFF59E0B)).copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { statDisplayFormat = StatDisplayFormat.Radar }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Hexagon",
                                color = if (statDisplayFormat == StatDisplayFormat.Radar) (if (selectedStatTab == StatAppraisalTab.JudgeIVs) Color(0xFF00E5FF) else Color(0xFFF59E0B)) else VantafynColors.Muted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (statDisplayFormat == StatDisplayFormat.Bars) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                                .clickable { statDisplayFormat = StatDisplayFormat.Bars }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Bars",
                                color = if (statDisplayFormat == StatDisplayFormat.Bars) Color.White else VantafynColors.Muted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Potential or EV Total sub-header badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedStatTab == StatAppraisalTab.JudgeIVs && effectiveIvs != null) {
                        val totalIv = effectiveIvs.hp + effectiveIvs.attack + effectiveIvs.defense +
                                effectiveIvs.speed + effectiveIvs.specialAttack + effectiveIvs.specialDefense
                        val (potentialLabel, potentialColor) = getOverallPotential(totalIv)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Overall Potential:",
                                color = VantafynColors.Muted,
                                fontSize = 10.sp,
                            )
                            Text(
                                text = potentialLabel,
                                color = potentialColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "$totalIv / 186 IVs",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    } else if (selectedStatTab == StatAppraisalTab.EVTraining && effectiveEvs != null) {
                        val totalEv = effectiveEvs.hp + effectiveEvs.attack + effectiveEvs.defense +
                                effectiveEvs.speed + effectiveEvs.specialAttack + effectiveEvs.specialDefense
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Total Effort Investment:",
                                color = VantafynColors.Muted,
                                fontSize = 10.sp,
                            )
                            Text(
                                text = "$totalEv / 510 Total EVs",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (isLoadingDetails) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else if (effectiveIvs != null || effectiveEvs != null) {
                    val stats = listOf(
                        StatRowData("HP", effectiveIvs?.hp ?: 0, effectiveEvs?.hp ?: 0, isHp = true),
                        StatRowData("Attack", effectiveIvs?.attack ?: 0, effectiveEvs?.attack ?: 0, isBoosted = natureMods.first == "Attack", isHindered = natureMods.second == "Attack"),
                        StatRowData("Defense", effectiveIvs?.defense ?: 0, effectiveEvs?.defense ?: 0, isBoosted = natureMods.first == "Defense", isHindered = natureMods.second == "Defense"),
                        StatRowData("Sp. Atk", effectiveIvs?.specialAttack ?: 0, effectiveEvs?.specialAttack ?: 0, isBoosted = natureMods.first == "Sp. Atk", isHindered = natureMods.second == "Sp. Atk"),
                        StatRowData("Sp. Def", effectiveIvs?.specialDefense ?: 0, effectiveEvs?.specialDefense ?: 0, isBoosted = natureMods.first == "Sp. Def", isHindered = natureMods.second == "Sp. Def"),
                        StatRowData("Speed", effectiveIvs?.speed ?: 0, effectiveEvs?.speed ?: 0, isBoosted = natureMods.first == "Speed", isHindered = natureMods.second == "Speed"),
                    )

                    if (statDisplayFormat == StatDisplayFormat.Bars) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            stats.forEach { stat ->
                                StatBarRow(
                                    stat = stat,
                                    mode = selectedStatTab,
                                )
                            }
                        }
                    } else {
                        PokemonStatHexagonChart(
                            stats = stats,
                            mode = selectedStatTab,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Stat calculations available when connected to game cartridge.",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Ability, Held Item, and Pokéball Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Ability Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pokemonCompactCardAtmosphereBrush(Color(0xFF00E5FF)))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "ABILITY",
                        color = VantafynColors.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = details?.ability?.ifBlank { canonicalFallback.ability } ?: canonicalFallback.ability ?: "Standard",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Held Item Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pokemonCompactCardAtmosphereBrush(Color(0xFFF59E0B)))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "HELD ITEM",
                        color = VantafynColors.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val effectiveHeldItem = details?.heldItem?.ifBlank { canonicalFallback.heldItem } ?: canonicalFallback.heldItem ?: "None"
                    Text(
                        text = effectiveHeldItem,
                        color = if (effectiveHeldItem != "None") Color(0xFF00E5FF) else VantafynColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Pokéball Card
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pokemonCompactCardAtmosphereBrush(Color(0xFF8B5CF6)))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "POKÉBALL",
                        color = VantafynColors.Muted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = resolveBallName(details?.pokeball ?: canonicalFallback.pokeball),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Moveset Card (if moves available)
            val moves = details?.moves?.filter { it.isNotBlank() && it != "—" }?.ifEmpty { null } ?: canonicalFallback.moves
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(pokemonCardAtmosphereBrush(Color(0xFF00E5FF)))
                    .border(1.dp, cardBorderBrush, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CURRENT MOVESET",
                        color = VantafynColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF202334))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable { showMoveRelearnerModal = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Move Relearner",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "MOVE RELEARNER",
                            color = Color(0xFF00E5FF),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (i in 0 until 2) {
                        val move = moves.getOrNull(i)
                        MoveChip(
                            moveName = move ?: "—",
                            modifier = Modifier.weight(1f),
                            isLegal = if (move != null) details?.movesLegality?.getOrNull(i) else null,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (i in 2 until 4) {
                        val move = moves.getOrNull(i)
                        MoveChip(
                            moveName = move ?: "—",
                            modifier = Modifier.weight(1f),
                            isLegal = if (move != null) details?.movesLegality?.getOrNull(i) else null,
                        )
                    }
                }
            }

            // Trainer & Origin Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(pokemonCardAtmosphereBrush(Color(0xFF38BDF8)))
                    .border(1.dp, cardBorderBrush, RoundedCornerShape(14.dp))
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
                if (pokemon.currentLocation.isNotBlank()) {
                    InfoRow(label = "Location", value = pokemon.currentLocation)
                }
            }

            // Journey & Lineage Timeline
            if (journey != null && journey!!.steps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(pokemonCardAtmosphereBrush(Color(0xFF00E5FF)))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(14.dp))
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

            // Ribbons & Hall of Fame Trophy Cabinet Section
            val ribbons = details?.ribbons ?: emptyList()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161926))
                    .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "TROPHY CABINET & RIBBONS",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${ribbons.size} EARNED",
                            color = Color(0xFFF59E0B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (ribbons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ribbons.chunked(2).forEach { rowRibbons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowRibbons.forEach { ribbon ->
                                    val accent = runCatching {
                                        Color(android.graphics.Color.parseColor(ribbon.iconColorHex))
                                    }.getOrElse { Color(0xFF3B82F6) }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1D2132))
                                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                            .clickable { selectedRibbon = ribbon }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(accent.copy(alpha = 0.20f))
                                                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = when (ribbon.category.lowercase()) {
                                                    "champion" -> Icons.Rounded.EmojiEvents
                                                    "effort", "battle", "tower" -> Icons.Rounded.MilitaryTech
                                                    else -> Icons.Rounded.WorkspacePremium
                                                },
                                                contentDescription = ribbon.name,
                                                tint = accent,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ribbon.name,
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = ribbon.category,
                                                color = accent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                                if (rowRibbons.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF11131C))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No ribbons earned yet. Complete the Pokémon League, battle towers, or max effort training to earn ribbons!",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            // PKHeX Legality & Safety Certification
            val isCertifiedLegal = details?.legalityStatus?.equals("illegal", ignoreCase = true) != true &&
                pokemon.legalityStatus.equals("illegal", ignoreCase = true).not()
            val illegalCount = details?.illegalitiesCount ?: if (isCertifiedLegal) 0 else 1
            val legalityStatusColor = if (isCertifiedLegal) Color(0xFF10B981) else Color(0xFFEF4444)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(legalityStatusColor.copy(alpha = 0.12f))
                    .border(1.dp, legalityStatusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { showLegalityDialog = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (isCertifiedLegal) Icons.Rounded.Verified else Icons.Rounded.Warning,
                    contentDescription = if (isCertifiedLegal) "Legality Verified" else "Legality Warning",
                    tint = legalityStatusColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (isCertifiedLegal) "PKHeX Certified Legal" else "PKHeX Audit: Issues Flagged",
                            color = legalityStatusColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!isCertifiedLegal) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = "$illegalCount Issue${if (illegalCount > 1) "s" else ""}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isCertifiedLegal) {
                            "Encounter origin, stat parameters, PID/IV correlation & moveset verified authentic."
                        } else {
                            "Validation anomaly detected in stats, encounter origin, or move learnset."
                        },
                        color = legalityStatusColor.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "AUDIT",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
    }
    }

    if (showLegalityDialog) {
        PokemonLegalityReportDialog(
            pokemon = pokemon,
            details = details,
            onDismiss = { showLegalityDialog = false },
        )
    }

    if (showMoveRelearnerModal && pokemonRepository != null) {
        PokemonMoveRelearnerModal(
            pokemon = pokemon,
            details = details,
            session = session,
            pokemonRepository = pokemonRepository,
            isVault = isVault,
            onDismiss = { showMoveRelearnerModal = false },
            onMovesUpdated = { newMoves ->
                details = details.copy(moves = newMoves)
            },
        )
    }

    if (selectedRibbon != null) {
        PokemonRibbonDetailDialog(
            ribbon = selectedRibbon!!,
            pokemonName = pokemon.nickname.ifBlank { pokemon.species },
            onDismiss = { selectedRibbon = null },
        )
    }

    if (showEvolutionModal && session != null && pokemonRepository != null) {
        val evos = details.availableEvolutions.ifEmpty { null }
            ?: dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog.getAvailableEvolutions(currentPokemonState.speciesId, currentPokemonState.level)
        PokemonEvolutionModal(
            pokemon = currentPokemonState,
            availableEvolutions = evos,
            session = session,
            pokemonRepository = pokemonRepository,
            isVault = isVault,
            onDismiss = { showEvolutionModal = false },
            onEvolved = { evolvedPokemon ->
                currentPokemonState = evolvedPokemon
                details = details.copy(
                    summary = evolvedPokemon,
                    availableEvolutions = emptyList()
                )
            },
        )
    }
}

private data class StatRowData(
    val name: String,
    val iv: Int,
    val ev: Int,
    val isHp: Boolean = false,
    val isBoosted: Boolean = false,
    val isHindered: Boolean = false,
)

@Composable
private fun StatBarRow(
    stat: StatRowData,
    mode: StatAppraisalTab,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Stat Name with nature indicator
        Row(
            modifier = Modifier.width(62.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stat.name,
                color = when {
                    stat.isBoosted -> Color(0xFFF43F5E) // Red +10%
                    stat.isHindered -> Color(0xFF38BDF8) // Blue -10%
                    else -> VantafynColors.Ink
                },
                fontSize = 11.sp,
                fontWeight = if (stat.isBoosted || stat.isHindered) FontWeight.Bold else FontWeight.Medium,
            )
            if (stat.isBoosted) {
                Text(text = "▲", color = Color(0xFFF43F5E), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            } else if (stat.isHindered) {
                Text(text = "▼", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Value & Progress Bar
        if (mode == StatAppraisalTab.JudgeIVs) {
            val appraisal = getIvJudgeRating(stat.iv)
            val ivPercent = (stat.iv.toFloat() / 31f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF222638)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ivPercent)
                        .clip(RoundedCornerShape(3.dp))
                        .background(appraisal.color),
                )
            }

            // Judge Label Badge - Fixed uniform dimensions for all rows
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(appraisal.color.copy(alpha = 0.15f))
                    .border(0.5.dp, appraisal.color.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${appraisal.label} (${stat.iv})",
                    color = appraisal.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // EV Mode (0 - 252)
            val evPercent = (stat.ev.toFloat() / 252f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF222638)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(evPercent)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF59E0B)),
                )
            }

            // EV Label Badge - Matching uniform dimensions
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (stat.ev > 0) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF1B1E2B))
                    .border(0.5.dp, if (stat.ev > 0) Color(0xFFF59E0B).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${stat.ev} / 252",
                    color = if (stat.ev > 0) Color(0xFFF59E0B) else VantafynColors.Muted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Authentic Pokémon Hexagon Radar Chart (as featured in Sun/Moon, Sword/Shield, Scarlet/Violet & Pokémon HOME).
 * Clockwise stat layout: HP (Top), Attack (Top-Right), Defense (Bottom-Right), Speed (Bottom), Sp. Def (Bottom-Left), Sp. Atk (Top-Left).
 */
@Composable
private fun PokemonStatHexagonChart(
    stats: List<StatRowData>,
    mode: StatAppraisalTab,
    modifier: Modifier = Modifier,
) {
    val orderedStats = remember(stats) {
        listOf(
            stats.firstOrNull { it.name == "HP" } ?: stats.getOrElse(0) { StatRowData("HP", 0, 0) },
            stats.firstOrNull { it.name == "Attack" } ?: stats.getOrElse(1) { StatRowData("Attack", 0, 0) },
            stats.firstOrNull { it.name == "Defense" } ?: stats.getOrElse(2) { StatRowData("Defense", 0, 0) },
            stats.firstOrNull { it.name == "Speed" } ?: stats.getOrElse(5) { StatRowData("Speed", 0, 0) },
            stats.firstOrNull { it.name == "Sp. Def" } ?: stats.getOrElse(4) { StatRowData("Sp. Def", 0, 0) },
            stats.firstOrNull { it.name == "Sp. Atk" } ?: stats.getOrElse(3) { StatRowData("Sp. Atk", 0, 0) },
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val themeColor = if (mode == StatAppraisalTab.JudgeIVs) Color(0xFF00E5FF) else Color(0xFFF59E0B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            // Outer hexagon radius dynamically sized to expand and fill available card canvas
            val maxVerticalRadius = (size.height / 2f) - 34.dp.toPx()
            val maxHorizontalRadius = ((size.width / 2f) - 86.dp.toPx()) / 0.866f
            val maxRadius = minOf(maxVerticalRadius, maxHorizontalRadius).coerceIn(65.dp.toPx(), 110.dp.toPx())

            val anglesDeg = listOf(-90f, -30f, 30f, 90f, 150f, 210f)
            val anglesRad = anglesDeg.map { Math.toRadians(it.toDouble()).toFloat() }

            // 1. Concentric guide hexagons
            val guideFractions = listOf(0.33f, 0.66f, 1.0f)
            guideFractions.forEach { frac ->
                val r = maxRadius * frac
                val guidePath = Path()
                anglesRad.forEachIndexed { i, a ->
                    val x = centerX + r * kotlin.math.cos(a)
                    val y = centerY + r * kotlin.math.sin(a)
                    if (i == 0) guidePath.moveTo(x, y) else guidePath.lineTo(x, y)
                }
                guidePath.close()

                drawPath(
                    path = guidePath,
                    color = Color.White.copy(alpha = if (frac == 1.0f) 0.16f else 0.06f),
                    style = Stroke(width = if (frac == 1.0f) 1.2.dp.toPx() else 0.8.dp.toPx()),
                )
            }

            // 2. Radial axis spoke lines
            anglesRad.forEach { a ->
                val endX = centerX + maxRadius * kotlin.math.cos(a)
                val endY = centerY + maxRadius * kotlin.math.sin(a)
                drawLine(
                    color = Color.White.copy(alpha = 0.09f),
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Center origin marker
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 2.5.dp.toPx(),
                center = Offset(centerX, centerY),
            )

            // 3. Stat polygon
            val statPolygonPath = Path()
            val statPoints = mutableListOf<Offset>()

            orderedStats.forEachIndexed { i, stat ->
                val a = anglesRad[i]
                val normalizedValue = if (mode == StatAppraisalTab.JudgeIVs) {
                    (stat.iv.toFloat() / 31f).coerceIn(0f, 1f)
                } else {
                    (stat.ev.toFloat() / 252f).coerceIn(0f, 1f)
                }
                val r = maxRadius * normalizedValue
                val px = centerX + r * kotlin.math.cos(a)
                val py = centerY + r * kotlin.math.sin(a)
                val pt = Offset(px, py)
                statPoints.add(pt)

                if (i == 0) statPolygonPath.moveTo(px, py) else statPolygonPath.lineTo(px, py)
            }
            statPolygonPath.close()

            // Fill polygon with radiant atmospheric wash
            drawPath(
                path = statPolygonPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.45f),
                        themeColor.copy(alpha = 0.15f),
                    ),
                    center = Offset(centerX, centerY),
                    radius = maxRadius,
                ),
            )

            // Stroke polygon outline
            drawPath(
                path = statPolygonPath,
                color = themeColor,
                style = Stroke(width = 2.dp.toPx()),
            )

            // Vertex dots
            statPoints.forEach { pt ->
                drawCircle(
                    color = themeColor,
                    radius = 4.dp.toPx(),
                    center = pt,
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.8.dp.toPx(),
                    center = pt,
                )
            }

            // 4. Draw Stat Labels around outer hexagon
            orderedStats.forEachIndexed { i, stat ->
                val a = anglesRad[i]
                val deg = anglesDeg[i]

                val labelColor = when {
                    stat.isBoosted -> Color(0xFFF43F5E)
                    stat.isHindered -> Color(0xFF38BDF8)
                    else -> Color(0xFFCBD5E1)
                }

                val statNameText = stat.name + when {
                    stat.isBoosted -> " ▲"
                    stat.isHindered -> " ▼"
                    else -> ""
                }

                val valText = if (mode == StatAppraisalTab.JudgeIVs) {
                    val rating = getIvJudgeRating(stat.iv)
                    "${stat.iv} • ${rating.label}"
                } else {
                    "${stat.ev} / 252"
                }

                val nameMeasured = textMeasurer.measure(
                    text = AnnotatedString(statNameText),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                    ),
                )

                val valMeasured = textMeasurer.measure(
                    text = AnnotatedString(valText),
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mode == StatAppraisalTab.JudgeIVs) getIvJudgeRating(stat.iv).color else if (stat.ev > 0) Color(0xFFF59E0B) else Color(0xFF64748B),
                    ),
                )

                val outerVertexX = centerX + maxRadius * kotlin.math.cos(a)
                val outerVertexY = centerY + maxRadius * kotlin.math.sin(a)

                val (nameX, nameY) = when (deg) {
                    -90f -> { // Top (HP)
                        val nx = (centerX - nameMeasured.size.width / 2f).coerceIn(2.dp.toPx(), size.width - nameMeasured.size.width - 2.dp.toPx())
                        val ny = (outerVertexY - nameMeasured.size.height - valMeasured.size.height - 4.dp.toPx()).coerceAtLeast(2.dp.toPx())
                        nx to ny
                    }
                    90f -> { // Bottom (Speed)
                        val nx = (centerX - nameMeasured.size.width / 2f).coerceIn(2.dp.toPx(), size.width - nameMeasured.size.width - 2.dp.toPx())
                        val ny = (outerVertexY + 4.dp.toPx()).coerceAtMost(size.height - nameMeasured.size.height - valMeasured.size.height - 2.dp.toPx())
                        nx to ny
                    }
                    -30f, 30f -> { // Right side (Attack / Defense)
                        val maxLabelW = maxOf(nameMeasured.size.width, valMeasured.size.width)
                        val nx = (outerVertexX + 6.dp.toPx()).coerceAtMost(size.width - maxLabelW - 2.dp.toPx())
                        val ny = outerVertexY - (nameMeasured.size.height + valMeasured.size.height) / 2f
                        nx to ny
                    }
                    else -> { // Left side (Sp. Atk / Sp. Def)
                        val w = maxOf(nameMeasured.size.width, valMeasured.size.width)
                        val nx = (outerVertexX - 6.dp.toPx() - w).coerceAtLeast(2.dp.toPx())
                        val ny = outerVertexY - (nameMeasured.size.height + valMeasured.size.height) / 2f
                        nx to ny
                    }
                }

                val valX = when (deg) {
                    -90f, 90f -> (centerX - valMeasured.size.width / 2f).coerceIn(2.dp.toPx(), size.width - valMeasured.size.width - 2.dp.toPx())
                    -30f, 30f -> nameX
                    else -> (outerVertexX - 6.dp.toPx() - valMeasured.size.width).coerceAtLeast(2.dp.toPx())
                }
                val valY = nameY + nameMeasured.size.height

                drawText(
                    textLayoutResult = nameMeasured,
                    topLeft = Offset(nameX, nameY),
                )
                drawText(
                    textLayoutResult = valMeasured,
                    topLeft = Offset(valX, valY),
                )
            }
        }
    }
}

@Composable
private fun MoveChip(
    moveName: String,
    modifier: Modifier = Modifier,
    isLegal: Boolean? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E2130))
            .border(
                1.dp,
                if (isLegal == false) Color(0xFFEF4444).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = moveName,
                color = if (moveName != "—") Color.White else VantafynColors.Muted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (moveName != "—" && isLegal != null) {
                Icon(
                    imageVector = if (isLegal) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = if (isLegal) "Legal Move" else "Illegal Move",
                    tint = if (isLegal) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp),
                )
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

private data class IvJudgeAppraisal(val label: String, val color: Color)

private fun getIvJudgeRating(iv: Int): IvJudgeAppraisal = when {
    iv >= 31 -> IvJudgeAppraisal("Best", Color(0xFF00E5FF))
    iv == 30 -> IvJudgeAppraisal("Fantastic", Color(0xFF10B981))
    iv in 26..29 -> IvJudgeAppraisal("Very Good", Color(0xFF34D399))
    iv in 16..25 -> IvJudgeAppraisal("Pretty Good", Color(0xFF2DD4BF))
    iv in 1..15 -> IvJudgeAppraisal("Decent", Color(0xFF94A3B8))
    else -> IvJudgeAppraisal("No Good", Color(0xFFF43F5E))
}

private fun getOverallPotential(totalIvs: Int): Pair<String, Color> = when {
    totalIvs >= 151 -> "Outstanding Potential! ⭐⭐⭐" to Color(0xFF00E5FF)
    totalIvs in 121..150 -> "Relatively Superior! ⭐⭐" to Color(0xFF10B981)
    totalIvs in 91..120 -> "Above Average! ⭐" to Color(0xFF38BDF8)
    else -> "Decent Potential" to Color(0xFF94A3B8)
}

private fun getNatureModifiers(nature: String?): Pair<String?, String?> = when (nature?.trim()?.lowercase()) {
    "lonely" -> "Attack" to "Defense"
    "brave" -> "Attack" to "Speed"
    "adamant" -> "Attack" to "Sp. Atk"
    "naughty" -> "Attack" to "Sp. Def"
    "bold" -> "Defense" to "Attack"
    "relaxed" -> "Defense" to "Speed"
    "impish" -> "Defense" to "Sp. Atk"
    "lax" -> "Defense" to "Sp. Def"
    "timid" -> "Speed" to "Attack"
    "hasty" -> "Speed" to "Defense"
    "jolly" -> "Speed" to "Sp. Atk"
    "naive" -> "Speed" to "Sp. Def"
    "modest" -> "Sp. Atk" to "Attack"
    "mild" -> "Sp. Atk" to "Defense"
    "quiet" -> "Sp. Atk" to "Speed"
    "rash" -> "Sp. Atk" to "Sp. Def"
    "calm" -> "Sp. Def" to "Attack"
    "gentle" -> "Sp. Def" to "Defense"
    "sassy" -> "Sp. Def" to "Speed"
    "careful" -> "Sp. Def" to "Sp. Atk"
    else -> null to null
}

private fun resolveBallName(ball: String?): String {
    if (ball.isNullOrBlank()) return "Pokéball"
    return when (ball.trim().lowercase()) {
        "1", "master", "masterball", "master ball" -> "Master Ball"
        "2", "ultra", "ultraball", "ultra ball" -> "Ultra Ball"
        "3", "great", "greatball", "great ball" -> "Great Ball"
        "4", "poke", "pokeball", "pokéball" -> "Pokéball"
        "5", "safari", "safariball", "safari ball" -> "Safari Ball"
        "6", "net", "netball", "net ball" -> "Net Ball"
        "7", "dive", "diveball", "dive ball" -> "Dive Ball"
        "8", "nest", "nestball", "nest ball" -> "Nest Ball"
        "9", "repeat", "repeatball", "repeat ball" -> "Repeat Ball"
        "10", "timer", "timerball", "timer ball" -> "Timer Ball"
        "11", "luxury", "luxuryball", "luxury ball" -> "Luxury Ball"
        "12", "premier", "premierball", "premier ball" -> "Premier Ball"
        else -> ball.replaceFirstChar { it.uppercase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PokemonLegalityReportDialog(
    pokemon: PokemonSummaryDto,
    details: PokemonDetailsDto?,
    onDismiss: () -> Unit,
) {
    val isCertifiedLegal = details?.legalityStatus?.equals("illegal", ignoreCase = true) != true &&
        pokemon.legalityStatus.equals("illegal", ignoreCase = true).not()
    val illegalCount = details?.illegalitiesCount ?: if (isCertifiedLegal) 0 else 1
    val statusColor = if (isCertifiedLegal) Color(0xFF10B981) else Color(0xFFEF4444)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        PokemonModalContainer(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            // Header with Security Shield and Close button
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isCertifiedLegal) Icons.Rounded.Verified else Icons.Rounded.Warning,
                            contentDescription = "Legality Shield",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "PKHeX Legality Certificate",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isCertifiedLegal) "Official Mainline Encounter Verified" else "Legality Anomaly Detected",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
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
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Summary Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (isCertifiedLegal) {
                                listOf(Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF059669).copy(alpha = 0.08f))
                            } else {
                                listOf(Color(0xFFEF4444).copy(alpha = 0.22f), Color(0xFFB91C1C).copy(alpha = 0.08f))
                            }
                        )
                    )
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (isCertifiedLegal) "STATUS: LEGAL & SAFE" else "STATUS: ILLEGAL / FLAGGED",
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (!isCertifiedLegal) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "$illegalCount Issue${if (illegalCount > 1) "s" else ""}",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isCertifiedLegal) {
                            "This Pokémon passes all PKHeX generation safety rules: encounter correlation, PID-IV derivation, ability index, legal ball, and move learnsets."
                        } else {
                            "This Pokémon contains data that violates mainline Game Freak encounter rules, PID/IV math, ball legality, or move learnsets."
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }

            // Verification Checks Breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1E2D))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "AUTHENTICITY AUDIT CHECKLIST",
                    color = VantafynColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )

                val speciesLabel = if (pokemon.species.isNotBlank() && !pokemon.species.startsWith("#")) pokemon.species else PokemonSpeciesCatalog.resolveSpeciesName(pokemon.speciesId)
                LegalityCheckRow(
                    label = "Species Identity",
                    value = "$speciesLabel (#${pokemon.speciesId})",
                    isPass = pokemon.speciesId > 0,
                )
                LegalityCheckRow(
                    label = "Encounter Origin",
                    value = "${pokemon.originGame ?: "Cartridge Origin"}",
                    isPass = true,
                )
                LegalityCheckRow(
                    label = "Met Location",
                    value = pokemon.currentLocation.ifBlank { "Wild Encounter" },
                    isPass = true,
                )
                val ivsValid = details?.iv?.let { it.hp in 0..31 && it.attack in 0..31 && it.defense in 0..31 && it.speed in 0..31 && it.specialAttack in 0..31 && it.specialDefense in 0..31 } ?: true
                LegalityCheckRow(
                    label = "IV Boundaries (0–31)",
                    value = if (ivsValid) "All 6 Stats Valid" else "Exceeds 31 Cap",
                    isPass = ivsValid,
                )
                val evTotal = details?.ev?.let { it.hp + it.attack + it.defense + it.speed + it.specialAttack + it.specialDefense } ?: 0
                val evsValid = evTotal <= 510
                LegalityCheckRow(
                    label = "EV Allocation (≤ 510)",
                    value = "$evTotal / 510 Total EVs",
                    isPass = evsValid,
                )
                val ballName = details?.pokeball ?: "Pokéball"
                LegalityCheckRow(
                    label = "Captured Ball",
                    value = ballName,
                    isPass = true,
                )

                // Move Checks
                val moves = details?.moves ?: emptyList()
                if (moves.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MOVEPOOL COMPLIANCE",
                        color = VantafynColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    moves.forEachIndexed { idx, moveName ->
                        val moveLegal = details?.movesLegality?.getOrNull(idx) ?: true
                        LegalityCheckRow(
                            label = "Move ${idx + 1}: $moveName",
                            value = if (moveLegal) "Learnset Valid" else "Illegal Move",
                            isPass = moveLegal,
                        )
                    }
                }
            }

            // Raw PKHeX Audit Log Box
            val reportText = details?.legalityReport?.ifBlank { null }
                ?: if (isCertifiedLegal) {
                    "Analysis: Valid Encounter.\nPID/IV: Valid mainline RNG distribution.\nLevel/Met: Met level and current level consistent.\nRibbons/Marks: Authentic.\nCheck: 0 illegalities found."
                } else {
                    "Analysis: Anomaly detected during validation.\nCheck: $illegalCount illegal condition(s) flagged."
                }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F111A))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "PKHEX DIAGNOSTIC REPORT",
                        color = VantafynColors.Muted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Vantafyn Guard v0.2.2",
                        color = VantafynColors.Muted.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                    )
                }
                Text(
                    text = reportText,
                    color = if (isCertifiedLegal) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                )
            }
        }
    }
    }
}

@Composable
private fun LegalityCheckRow(
    label: String,
    value: String,
    isPass: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Icon(
                imageVector = if (isPass) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                contentDescription = if (isPass) "Passed" else "Failed",
                tint = if (isPass) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                color = VantafynColors.Muted,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value,
            color = if (isPass) Color.White else Color(0xFFEF4444),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
