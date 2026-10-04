package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import dev.vantafyn.feature.home.games.GameScreenReveal
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonBoxDto
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.jellyfin.PokemonVaultSummary
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class VaultFeaturedCandidate(
    val summary: PokemonSummaryDto,
    val originGameTitle: String,
    val originGameId: String?,
    val isVault: Boolean,
    val details: PokemonDetailsDto? = null,
)


@Composable
fun PokemonVaultHomeScreen(
    session: JellyfinSession?,
    vaultSummary: PokemonVaultSummary?,
    availableGames: List<GameSummary>,
    allDetectedSaves: List<PokemonGameSaveDto>,
    allVaultBoxes: List<PokemonBoxDto>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onMovePokemon: () -> Unit,
    onImportSave: () -> Unit,
    onSelectGameForTransfer: (GameSummary) -> Unit,
    onOpenTradeCenter: () -> Unit,
    onOpenPokedex: () -> Unit,
    onOpenBadgeCase: () -> Unit,
    onOpenDiplomaCase: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenBackups: () -> Unit,
    onInspectPokemon: (PokemonSummaryDto, PokemonDetailsDto?, String?, Boolean, List<PokemonSummaryDto>, Int) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    // Aggregate all Pokémon across vault boxes and detected game saves
    val allCandidatePokemon = remember(allVaultBoxes, allDetectedSaves, availableGames) {
        val list = mutableListOf<VaultFeaturedCandidate>()

        // 1. Vault storage
        for (box in allVaultBoxes) {
            for (pkm in box.entries) {
                if (pkm.speciesId > 0) {
                    list.add(
                        VaultFeaturedCandidate(
                            summary = pkm,
                            originGameTitle = "Cloud Vault (${box.name.ifBlank { "Box ${box.boxIndex}" }})",
                            originGameId = null,
                            isVault = true,
                            details = dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog.generateCanonicalDetails(pkm),
                        )
                    )
                }
            }
        }

        // 2. Detected game saves (Party + Boxes)
        for (save in allDetectedSaves) {
            val matched = availableGames.firstOrNull { it.id == save.gameId }
            val gameTitle = matched?.pokemon?.canonicalTitle
                ?: matched?.cleanTitle
                ?: dev.vantafyn.core.jellyfin.cleanGameTitle(save.title).ifBlank { "Cartridge" }

            for (pkm in save.party) {
                if (pkm.speciesId > 0) {
                    val det = save.pokemonDetails[pkm.id]
                        ?: dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog.generateCanonicalDetails(pkm)
                    list.add(
                        VaultFeaturedCandidate(
                            summary = pkm,
                            originGameTitle = gameTitle,
                            originGameId = save.gameId,
                            isVault = false,
                            details = det,
                        )
                    )
                }
            }

            for (box in save.boxes) {
                for (pkm in box.entries) {
                    if (pkm.speciesId > 0) {
                        val det = save.pokemonDetails[pkm.id]
                            ?: dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog.generateCanonicalDetails(pkm)
                        list.add(
                            VaultFeaturedCandidate(
                                summary = pkm,
                                originGameTitle = gameTitle,
                                originGameId = save.gameId,
                                isVault = false,
                                details = det,
                            )
                        )
                    }
                }
            }
        }
        list
    }

    // Determine featured Pokémon list for the smooth-scrolling Hero Carousel:
    // Shinies first, then sorted by level descending, avoiding duplicate entries
    val heroCandidates = remember(allCandidatePokemon) {
        if (allCandidatePokemon.isEmpty()) {
            emptyList()
        } else {
            val shinies = allCandidatePokemon.filter { it.summary.isShiny }
            val nonShinies = allCandidatePokemon
                .filterNot { it.summary.isShiny }
                .sortedByDescending { it.summary.level }
            (shinies + nonShinies)
                .distinctBy { "${it.summary.speciesId}_${it.summary.level}_${it.summary.nickname}_${it.originGameTitle}" }
                .take(15)
        }
    }

    // Aggregated statistics
    val totalOccupied = vaultSummary?.totalOccupied ?: allCandidatePokemon.count { it.isVault }
    val totalShinyCount = vaultSummary?.shinyCount ?: allCandidatePokemon.count { it.summary.isShiny }
    val distinctSpeciesCount = remember(allCandidatePokemon, allDetectedSaves) {
        val fromCandidates = allCandidatePokemon.map { it.summary.speciesId }.toSet()
        val fromSaves = allDetectedSaves
            .filter { it.totalPokemonCount > 0 && it.caughtSpeciesIds.size <= (it.totalPokemonCount + 40) }
            .flatMap { it.caughtSpeciesIds }
            .toSet()
        (fromCandidates + fromSaves).filter { it in 1..1025 }.size
    }
    val activeSavesCount = remember(allDetectedSaves) {
        allDetectedSaves.count { it.party.isNotEmpty() || it.boxes.any { b -> b.entries.isNotEmpty() } || it.totalPokemonCount > 0 }
    }

    GameScreenReveal(
        key = "pokemon_vault_home_content",
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // Top Header
        VaultHomeHeader(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onBack = onBack,
        )

        if (isLoading && allCandidatePokemon.isEmpty() && availableGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.size(36.dp),
                )
            }
        } else {
            // Large Hero Area Carousel
            VaultHeroCard(
                candidates = heroCandidates,
                availableGames = availableGames,
                session = session,
                onInspect = { candidate ->
                    val summaries = heroCandidates.map { it.summary }
                    val idx = summaries.indexOfFirst { it.id == candidate.summary.id }
                    onInspectPokemon(candidate.summary, candidate.details, candidate.originGameId, candidate.isVault, summaries, idx)
                },
                onOpenBoxes = onMovePokemon,
            )

            // Collection Summary
            VaultCollectionSummaryRow(
                totalOccupied = totalOccupied,
                totalCapacity = vaultSummary?.totalCapacity ?: 900,
                discoveredSpecies = distinctSpeciesCount,
                connectedGamesCount = availableGames.size,
                activeSavesCount = activeSavesCount,
                shinyCount = totalShinyCount,
            )

            // Primary Feature Actions
            VaultPrimaryFeatures(
                onMovePokemon = onMovePokemon,
                onImportSave = onImportSave,
                onOpenTradeCenter = onOpenTradeCenter,
                onOpenPokedex = onOpenPokedex,
                onOpenBadgeCase = onOpenBadgeCase,
                onOpenDiplomaCase = onOpenDiplomaCase,
                onOpenAchievements = onOpenAchievements,
                onOpenBackups = onOpenBackups,
            )

            // Connected Games Horizontal Rail
            if (availableGames.isNotEmpty()) {
                VaultConnectedGamesRail(
                    games = availableGames,
                    detectedSaves = allDetectedSaves,
                    onSelectGame = onSelectGameForTransfer,
                )
            }

            // Recent Pokémon / Activity Section
            if (allCandidatePokemon.isNotEmpty()) {
                val recentCandidates = remember(allCandidatePokemon) { allCandidatePokemon.take(16) }
                VaultRecentPokemonRail(
                    candidates = recentCandidates,
                    availableGames = availableGames,
                    onInspectPokemon = { candidate ->
                        val summaries = recentCandidates.map { it.summary }
                        val idx = summaries.indexOfFirst { it.id == candidate.summary.id }
                        onInspectPokemon(candidate.summary, candidate.details, candidate.originGameId, candidate.isVault, summaries, idx)
                    },
                )
            } else if (totalOccupied == 0) {
                // Empty state explanation banner for fresh 0/900 vaults
                VaultEmptyGuideBanner(onOpenMove = onMovePokemon)
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
}

@Composable
private fun VaultHomeHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Protected",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "Protected",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = if (isRefreshing) Color(0xFF00E5FF) else VantafynColors.Muted,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun VaultHeroItemCard(
    featured: VaultFeaturedCandidate,
    onInspect: () -> Unit,
) {
    val summary = featured.summary
    val speciesName = remember(summary.speciesId) {
        PokemonSpeciesCatalog.resolveSpeciesName(summary.speciesId)
    }
    val displayName = if (summary.nickname.isNotBlank() && !summary.nickname.equals(speciesName, ignoreCase = true)) {
        summary.nickname
    } else {
        speciesName
    }

    val (primaryType, secondaryType) = remember(summary.speciesId, speciesName) {
        PokemonTypeCatalog.getTypes(summary.speciesId, speciesName)
    }

    val isFemale = summary.gender?.equals("Female", ignoreCase = true) == true ||
        summary.gender?.equals("Girl", ignoreCase = true) == true ||
        summary.gender?.equals("F", ignoreCase = true) == true

    val matchedForm = remember(summary.speciesId, summary.form) {
        if (!summary.form.isNullOrBlank()) {
            PokemonFormsCatalog.getForms(summary.speciesId).firstOrNull {
                it.name.contains(summary.form!!, ignoreCase = true) ||
                    summary.form!!.contains(it.name, ignoreCase = true)
            }
        } else null
    }

    val artworkUrl = remember(summary.speciesId, summary.isShiny, isFemale, matchedForm) {
        PokemonGenderCatalog.getPokedexArtworkUrl(
            speciesId = summary.speciesId,
            isShiny = summary.isShiny,
            isFemale = isFemale,
            formKey = matchedForm?.spriteKey,
        )
    }

    val statusDescriptor = when {
        summary.isShiny -> "Rare Shiny"
        summary.level >= 50 -> "Battle Veteran"
        featured.isVault -> "Cloud Vault"
        else -> "Active Team"
    }
    val locationDescriptor = if (featured.isVault) {
        val boxIdx = summary.boxIndex
        if (boxIdx != null) "Box ${boxIdx + 1}" else "Storage"
    } else {
        featured.originGameTitle
    }
    val supportingCopy = "$statusDescriptor · $locationDescriptor"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        primaryType.accentColor.copy(alpha = 0.50f),
                        (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f),
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onInspect),
    ) {
        // 1. Dynamic Type Atmosphere & Scrim Layer
        PokemonHeroTypeAtmosphere(
            primaryType = primaryType,
            secondaryType = secondaryType,
            modifier = Modifier.matchParentSize(),
        )

        // 2. Foreground Content Layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Badges row: Level, Shiny
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Level Badge (Game Hub Dark Console Box Style)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D111A))
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        primaryType.accentColor.copy(alpha = 0.55f),
                                        Color(0xFF334155),
                                    )
                                ),
                                shape = RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "LV. ${summary.level}",
                            color = Color(0xFFF1F5F9),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    // Shiny badge
                    if (summary.isShiny) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFF59E0B).copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Shiny",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(10.dp),
                                )
                                Text(
                                    text = "SHINY",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }
                }

                // Display Name
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Premium Type Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp),
                ) {
                    PokemonTypeBadge(type = primaryType)
                    if (secondaryType != null && secondaryType != primaryType) {
                        PokemonTypeBadge(type = secondaryType)
                    }
                }

                // Tight Curated Supporting Copy
                Text(
                    text = supportingCopy,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Tap to Inspect affordance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "View Pokémon Summary",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // Featured Pokémon Artwork with layered aura and grounded shadow
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(start = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Layer A: Ambient Radial Aura Glow
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (summary.isShiny) Color(0xFFF59E0B).copy(alpha = 0.38f)
                                    else primaryType.glowColor.copy(alpha = 0.32f),
                                    (secondaryType?.accentColor ?: primaryType.accentColor).copy(alpha = 0.12f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                // Layer B: Subtle Grounded Drop Shadow under the Pokémon
                Canvas(
                    modifier = Modifier
                        .size(width = 110.dp, height = 28.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF020408).copy(alpha = 0.55f),
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
                    contentDescription = speciesName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(142.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultHeroCard(
    candidates: List<VaultFeaturedCandidate>,
    availableGames: List<GameSummary> = emptyList(),
    session: JellyfinSession? = null,
    onInspect: (VaultFeaturedCandidate) -> Unit,
    onOpenBoxes: () -> Unit,
) {
    if (candidates.isNotEmpty()) {
        var currentIndex by remember { mutableIntStateOf(0) }
        var totalDrag by remember { mutableFloatStateOf(0f) }

        // Slower, leisurely rotation (8.5 seconds) with smooth auto-advancing crossfade
        LaunchedEffect(candidates.size, currentIndex) {
            if (candidates.size > 1) {
                delay(8500)
                currentIndex = (currentIndex + 1) % candidates.size
            }
        }

        val safeIndex = currentIndex.coerceIn(0, candidates.lastIndex)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(candidates.size) {
                    if (candidates.size > 1) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (totalDrag < -40f) {
                                    currentIndex = (currentIndex + 1) % candidates.size
                                } else if (totalDrag > 40f) {
                                    currentIndex = (currentIndex - 1 + candidates.size) % candidates.size
                                }
                            }
                        )
                    }
                },
        ) {
            AnimatedContent(
                targetState = safeIndex,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                    )
                },
                label = "HeroCardCrossfade",
            ) { targetIndex ->
                val featured = candidates.getOrNull(targetIndex) ?: candidates.first()
                VaultHeroItemCard(
                    featured = featured,
                    onInspect = { onInspect(featured) },
                )
            }
        }
    } else {
        // Empty State Hero Card with Scraped & Cached Pokémon Logo
        val context = LocalContext.current
        val cachedLogo = remember(context) { PokemonLogoScraper.getCachedLogoFile(context) }
        val logoSource by produceState<Any?>(initialValue = cachedLogo) {
            value = PokemonLogoScraper.getOrScrapeLogo(context, availableGames, session)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF191C2C),
                            Color(0xFF131522),
                            Color(0xFF1F1735),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color(0xFF8B5CF6).copy(alpha = 0.25f),
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Scraped Pokémon Logo with subtle ambient lighting
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 160.dp, height = 44.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFCB05).copy(alpha = 0.20f),
                                        Color(0xFF2A75BB).copy(alpha = 0.12f),
                                        Color.Transparent,
                                    )
                                )
                            )
                    )

                    AsyncImage(
                        model = logoSource,
                        contentDescription = "Pokémon",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(52.dp),
                    )
                }

                Text(
                    text = "Cloud Storage Vault",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Store up to 900 Pokémon across 30 boxes.\nConnect your cartridge saves to begin.",
                    color = VantafynColors.Muted,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(VantafynGradients.accentHorizontal())
                        .clickable { onOpenBoxes() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Open Storage Boxes",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultCollectionSummaryRow(
    totalOccupied: Int,
    totalCapacity: Int,
    discoveredSpecies: Int,
    connectedGamesCount: Int,
    activeSavesCount: Int,
    shinyCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // In Storage
        VaultStatTile(
            modifier = Modifier.weight(1f),
            title = "Storage",
            value = "$totalOccupied",
            subtext = "/$totalCapacity",
            icon = Icons.Rounded.CloudDone,
            iconTint = Color(0xFF00E5FF),
        )

        // Pokédex Discovered
        VaultStatTile(
            modifier = Modifier.weight(1f),
            title = "Pokédex",
            value = "$discoveredSpecies",
            subtext = "/1025",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            iconTint = Color(0xFFF43F5E),
        )

        // Connected Games
        VaultStatTile(
            modifier = Modifier.weight(1f),
            title = "Games",
            value = "$connectedGamesCount",
            subtext = "Linked",
            icon = Icons.Rounded.SportsEsports,
            iconTint = Color(0xFFA855F7),
        )

        // Shinies
        VaultStatTile(
            modifier = Modifier.weight(1f),
            title = "Shinies",
            value = "$shinyCount",
            subtext = "Vault",
            icon = Icons.Rounded.AutoAwesome,
            iconTint = Color(0xFFF59E0B),
        )
    }
}

@Composable
private fun VaultStatTile(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161926))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = VantafynColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = value,
                    color = VantafynColors.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = subtext,
                    color = VantafynColors.Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 1.5.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultPrimaryFeatures(
    onMovePokemon: () -> Unit,
    onImportSave: () -> Unit,
    onOpenTradeCenter: () -> Unit,
    onOpenPokedex: () -> Unit,
    onOpenBadgeCase: () -> Unit,
    onOpenDiplomaCase: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenBackups: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Quick links stay visible before the larger action cards.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VaultActionTile(
                modifier = Modifier.weight(1f),
                title = "Pokédex",
                subtitle = "Gens 1-9",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                accentColor = Color(0xFFF43F5E),
                onClick = onOpenPokedex,
            )

            VaultActionTile(
                modifier = Modifier.weight(1f),
                title = "Achievements",
                subtitle = "Trainer Hub",
                icon = Icons.Rounded.Star,
                accentColor = Color(0xFFF59E0B),
                onClick = onOpenAchievements,
            )

            VaultActionTile(
                modifier = Modifier.weight(1f),
                title = "Backups",
                subtitle = "Snapshots",
                icon = Icons.Rounded.Security,
                accentColor = Color(0xFF10B981),
                onClick = onOpenBackups,
            )
        }

        // Move Pokémon (Primary Action Card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1B2238),
                            Color(0xFF141728),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.40f),
                            Color(0xFF8B5CF6).copy(alpha = 0.30f),
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onMovePokemon() }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VantafynGradients.accentHorizontal()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Move Pokémon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Move Pokémon",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Organise 30 storage boxes, deposit or withdraw from cartridge saves",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Manage",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        // External Gen 6–9 emulator saves are copied into the Vault; the selected file is never changed.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF201B38),
                            Color(0xFF151827),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.44f),
                            Color(0xFF38BDF8).copy(alpha = 0.24f),
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onImportSave() }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF38BDF8),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = "Import emulator save",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Import emulator save",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Copy Pokémon from a Gen 6-9 save without changing the original file",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Import",
                            color = Color(0xFFB8A4FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFB8A4FF),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        // 2. Trade Center (Prominent Featured Action - Preserves exact PIN flow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF231B38),
                            Color(0xFF161528),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.40f),
                            Color(0xFFEC4899).copy(alpha = 0.30f),
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onOpenTradeCenter() }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEC4899),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cable,
                            contentDescription = "Trade Center",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Trade Center",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Real-time peer trading with nearby trainers via PIN pairing code",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Trade",
                            color = Color(0xFFC084FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2A2418),
                            Color(0xFF171827),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.48f),
                            Color(0xFF00E5FF).copy(alpha = 0.24f),
                        )
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable { onOpenBadgeCase() }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFF59E0B),
                                        Color(0xFF38BDF8),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Badge Case",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Badge Case",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Showcase gym badges earned across linked cartridge saves",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "View",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF17251F),
                            Color(0xFF111827),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF34D399).copy(alpha = 0.46f),
                            Color(0xFFFBBF24).copy(alpha = 0.26f),
                        )
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable { onOpenDiplomaCase() }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF34D399),
                                        Color(0xFFFBBF24),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Diploma Case",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Diploma Case",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Archive completed Pokédex certificates and proof screenshots",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF34D399).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "View",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun VaultActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161926))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = title,
                color = VantafynColors.Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun VaultConnectedGamesRail(
    games: List<GameSummary>,
    detectedSaves: List<PokemonGameSaveDto>,
    onSelectGame: (GameSummary) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Connected Games",
                color = VantafynColors.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${games.size} Available",
                color = VantafynColors.Muted,
                fontSize = 11.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            games.forEach { game ->
                val save = detectedSaves.firstOrNull { it.gameId == game.id }
                val hasSave = save != null && (save.saveFound || save.party.isNotEmpty() || save.boxes.any { it.entries.isNotEmpty() })
                val partyCount = save?.party?.size ?: 0
                val pcCount = save?.boxes?.sumOf { it.entries.size } ?: 0

                VaultConnectedGameCard(
                    game = game,
                    hasSave = hasSave,
                    partyCount = partyCount,
                    pcCount = pcCount,
                    onClick = { onSelectGame(game) },
                )
            }
        }
    }
}

@Composable
private fun VaultConnectedGameCard(
    game: GameSummary,
    hasSave: Boolean,
    partyCount: Int,
    pcCount: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val effectiveBoxart = remember(game.id, game.boxartUrl) {
        val local = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
            .getString("boxart_${game.id}", null)
        GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
    }

    Box(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161926))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Box Art Poster Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(12.dp))
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

                // Platform Badge (top-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0B0E17).copy(alpha = 0.85f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = game.systemId.ifBlank { "ROM" }.uppercase(),
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // SRAM Save status badge (top-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (hasSave) Color(0xFF10B981).copy(alpha = 0.90f)
                            else Color(0xFF0B0E17).copy(alpha = 0.75f)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (hasSave) "SRAM" else "NO SAVE",
                        color = if (hasSave) Color.White else Color(0xFF94A3B8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Game details below poster
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = game.pokemon?.canonicalTitle ?: game.cleanTitle,
                    color = VantafynColors.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (hasSave) {
                    Text(
                        text = "Party: $partyCount · PC: $pcCount",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = "Ready to Link",
                        color = VantafynColors.Muted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultRecentPokemonRail(
    candidates: List<VaultFeaturedCandidate>,
    availableGames: List<GameSummary>,
    onInspectPokemon: (VaultFeaturedCandidate) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Recent Pokémon",
                color = VantafynColors.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tap to inspect",
                color = VantafynColors.Muted,
                fontSize = 11.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            candidates.forEach { candidate ->
                val pkm = candidate.summary
                val speciesName = PokemonSpeciesCatalog.resolveSpeciesName(pkm.speciesId)
                val isFemale = pkm.gender?.equals("Female", ignoreCase = true) == true ||
                    pkm.gender?.equals("Girl", ignoreCase = true) == true ||
                    pkm.gender?.equals("F", ignoreCase = true) == true
                val spriteUrl = getPokemonSpriteUrl(pkm.speciesId, pkm.isShiny, isFemale = isFemale)

                val matchedGame = remember(candidate.originGameId, candidate.originGameTitle, availableGames) {
                    if (candidate.originGameId != null) {
                        availableGames.firstOrNull { it.id == candidate.originGameId }
                    } else {
                        availableGames.firstOrNull { candidate.originGameTitle.contains(it.cleanTitle, ignoreCase = true) }
                    }
                }

                val boxartUrl = remember(matchedGame) {
                    matchedGame?.let { game ->
                        val local = context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                            .getString("boxart_${game.id}", null)
                        GameBoxartScraper.convertToCdnUrl(local ?: game.boxartUrl)
                    }
                }

                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161926))
                        .border(
                            1.dp,
                            if (pkm.isShiny) Color(0xFFF59E0B).copy(alpha = 0.45f)
                            else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onInspectPokemon(candidate) },
                    contentAlignment = Alignment.Center,
                ) {
                    // Box art backdrop with dark scrim if available
                    if (!boxartUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = boxartUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF090B13).copy(alpha = 0.76f),
                                            Color(0xFF06070B).copy(alpha = 0.90f),
                                        )
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = spriteUrl,
                                contentDescription = speciesName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(50.dp),
                            )
                        }

                        Text(
                            text = if (pkm.nickname.isNotBlank() && !pkm.nickname.equals(speciesName, ignoreCase = true)) pkm.nickname else speciesName,
                            color = VantafynColors.Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (pkm.isShiny) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Shiny",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(9.dp),
                                )
                            }
                            Text(
                                text = "Lv. ${pkm.level}",
                                color = if (pkm.isShiny) Color(0xFFF59E0B) else VantafynColors.Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultEmptyGuideBanner(
    onOpenMove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161926))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Ready to Store Pokémon",
                    color = VantafynColors.Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Transfer Pokémon from your emulator saves into the 30 cloud boxes for safe-keeping and cross-generational trading.",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
