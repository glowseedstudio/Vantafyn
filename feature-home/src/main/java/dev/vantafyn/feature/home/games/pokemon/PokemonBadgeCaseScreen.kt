package dev.vantafyn.feature.home.games.pokemon

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import coil3.compose.SubcomposeAsyncImage
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.PokemonBadgeArtCatalogDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonGymBadgeDto
import dev.vantafyn.core.jellyfin.PokemonGymBadgeRegionDto
import dev.vantafyn.core.jellyfin.cleanGameTitle
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal

private const val BadgeCaseLogTag = "PokemonBadgeCase"

private data class BadgeCaseGame(
    val save: PokemonGameSaveDto,
    val title: String,
    val subtitle: String,
)

@Composable
fun PokemonBadgeCaseScreen(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    availableGames: List<GameSummary>,
    detectedSaves: List<PokemonGameSaveDto>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val games = remember(availableGames, detectedSaves) {
        detectedSaves
            .filter { save ->
                save.saveFound && save.providerAvailable &&
                    save.gymBadges.isNotEmpty() &&
                    (save.party.isNotEmpty() ||
                     save.boxes.any { it.entries.isNotEmpty() } ||
                     save.totalPokemonCount > 0 ||
                     (save.pokedexCaught ?: 0) > 0 ||
                     save.gymBadges.any { r -> r.badges.any { it.isEarned } })
            }
            .filter { save ->
                val game = availableGames.firstOrNull { it.id == save.gameId }
                val expectedGen = game?.pokemon?.generation ?: 0
                expectedGen == 0 || save.generation == 0 || expectedGen == save.generation
            }
            .map { save ->
                val game = availableGames.firstOrNull { it.id == save.gameId }
                val title = game?.pokemon?.canonicalTitle
                    ?: game?.cleanTitle
                    ?: cleanGameTitle(save.title).ifBlank { save.title.ifBlank { "Pokémon Save" } }
                BadgeCaseGame(
                    save = save,
                    title = title,
                    subtitle = buildString {
                        append("Gen ${save.generation.takeIf { it > 0 } ?: "?"}")
                        if (!save.trainerName.isNullOrBlank()) append(" • ${save.trainerName}")
                    },
                )
            }
            .sortedWith(compareBy({ it.save.generation }, { it.title }))
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(games.size) {
        if (selectedIndex > games.lastIndex) selectedIndex = 0
    }

    var badgeArtCatalog by remember { mutableStateOf<PokemonBadgeArtCatalogDto?>(null) }
    LaunchedEffect(session, pokemonRepository) {
        badgeArtCatalog = session?.let { activeSession ->
            pokemonRepository.getBadgeArtCatalog(activeSession)
                .onSuccess { catalog ->
                    Log.d(
                        BadgeCaseLogTag,
                        "Badge art catalog configured=${catalog.configured} available=${catalog.availableCount}/${catalog.totalCount} " +
                            "regions=${catalog.regions.joinToString { "${it.id}:${it.availableCount}/${it.totalCount}" }}",
                    )
                }
                .onFailure { error ->
                    Log.w(BadgeCaseLogTag, "Badge art catalog failed: ${error.message}", error)
                }
                .getOrNull()
        }
    }
    val badgeImageUrls = remember(session, badgeArtCatalog) {
        buildMap {
            val activeSession = session ?: return@buildMap
            badgeArtCatalog?.regions.orEmpty().forEach { region ->
                region.badges.forEach { badge ->
                    val imageUrl = badge.imageUrl
                    if (badge.available && !imageUrl.isNullOrBlank()) {
                        put(badgeArtKey(badge.region, badge.id), activeSession.toAuthenticatedBadgeArtUrl(imageUrl))
                    }
                }
            }
        }
    }

    GameScreenReveal(
        key = "pokemon_badge_case_screen",
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BadgeCaseHeader(onBack = onBack, onRefresh = onRefresh)

            if (games.isEmpty()) {
                BadgeCaseEmptyState()
            } else {
                val selected = games[selectedIndex]
                BadgeCaseGameSelector(
                    games = games,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                )

                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "BadgeCaseSelectedGame",
                ) { game ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BadgeCaseHero(game = game)
                        game.save.gymBadges.forEach { region ->
                            BadgeRegionCase(
                                region = region,
                                badgeImageUrls = badgeImageUrls,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun BadgeCaseHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
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
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Badge Case",
                    color = VantafynColors.Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Gym badges by save file",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = VantafynColors.Muted,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun BadgeCaseGameSelector(
    games: List<BadgeCaseGame>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        games.forEachIndexed { index, game ->
            val selected = index == selectedIndex
            val earned = game.save.gymBadges.sumOf { region -> region.badges.count { it.isEarned } }
            val total = game.save.gymBadges.sumOf { it.badges.size }.coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .width(188.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Color(0xFF1B2238) else Color(0xFF141824))
                    .border(
                        width = 1.dp,
                        brush = if (selected) {
                            Brush.horizontalGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.64f), Color(0xFF38BDF8).copy(alpha = 0.34f)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.05f)))
                        },
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = if (selected) Color(0xFFFBBF24) else VantafynColors.Muted,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = game.subtitle,
                            color = if (selected) Color(0xFFFBBF24) else VantafynColors.Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = game.title,
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BadgeCaseProgressBar(
                        earned = earned,
                        total = total,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeCaseHero(game: BadgeCaseGame) {
    val earned = game.save.gymBadges.sumOf { region -> region.badges.count { it.isEarned } }
    val total = game.save.gymBadges.sumOf { it.badges.size }.coerceAtLeast(1)
    val completeRegions = game.save.gymBadges.count { region -> region.badges.isNotEmpty() && region.badges.all { it.isEarned } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF211D16),
                        Color(0xFF141927),
                        Color(0xFF10131E),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.44f),
                        Color(0xFF38BDF8).copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.08f),
                    )
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = game.title,
                        color = VantafynColors.Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = game.subtitle,
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.14f))
                        .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            BadgeCaseProgressBar(
                earned = earned,
                total = total,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BadgeCaseMetric("Earned", "$earned", "/$total", Icons.Rounded.AutoAwesome, Color(0xFFFBBF24), Modifier.weight(1f))
                BadgeCaseMetric("Regions", "${game.save.gymBadges.size}", "tracked", Icons.Rounded.Security, Color(0xFF38BDF8), Modifier.weight(1f))
                BadgeCaseMetric("Complete", "$completeRegions", "regions", Icons.Rounded.Star, Color(0xFF10B981), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCaseMetric(
    label: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111827).copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                Text(label, color = VantafynColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(value, color = VantafynColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtext, color = VantafynColors.Muted, fontSize = 9.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun BadgeRegionCase(
    region: PokemonGymBadgeRegionDto,
    badgeImageUrls: Map<String, String>,
) {
    val earned = region.badges.count { it.isEarned }
    val total = region.badges.size.coerceAtLeast(1)
    LaunchedEffect(region.region, region.badges, badgeImageUrls) {
        val missing = region.badges
            .filter { badgeImageUrls[badgeArtKey(it.region, it.id)] == null }
            .joinToString { "${it.region}:${it.id}" }
        if (missing.isNotBlank()) {
            Log.d(
                BadgeCaseLogTag,
                "Missing badge art urls for ${region.region}: $missing; loadedKeys=${badgeImageUrls.keys.joinToString()}",
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141824))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = region.displayName.ifBlank { region.region.replaceFirstChar { it.titlecase() } },
                        color = VantafynColors.Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val earnedUnit = when (region.region) {
                        "alola" -> "stamps earned"
                        "zcrystals" -> "Z-Crystals unlocked"
                        else -> "badges earned"
                    }
                    Text(
                        text = "$earned of $total $earnedUnit",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                    )
                }
                RegionCompletionPill(earned = earned, total = total)
            }

            region.badges.chunked(4).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowBadges.forEach { badge ->
                        BadgeSlot(
                            badge = badge,
                            imageUrl = badgeImageUrls[badgeArtKey(badge.region, badge.id)],
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - rowBadges.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeSlot(
    badge: PokemonGymBadgeDto,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val isZCrystal = badge.region == "zcrystals"
    val earnedAlpha = if (badge.isEarned) 1f else 0.36f

    val backgroundBrush = when {
        badge.isEarned && isZCrystal -> Brush.radialGradient(
            listOf(
                Color(0xFFC084FC).copy(alpha = 0.28f),
                Color(0xFF38BDF8).copy(alpha = 0.12f),
                Color(0xFF111827),
            )
        )
        badge.isEarned -> Brush.radialGradient(
            listOf(
                Color(0xFFFBBF24).copy(alpha = 0.22f),
                Color(0xFF111827),
            )
        )
        else -> Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = 0.05f),
                Color(0xFF0F172A),
            )
        )
    }

    val borderColor = when {
        badge.isEarned && isZCrystal -> Color(0xFFC084FC).copy(alpha = 0.55f)
        badge.isEarned -> Color(0xFFFBBF24).copy(alpha = 0.42f)
        else -> Color.White.copy(alpha = 0.07f)
    }

    val fallbackIcon = if (isZCrystal) Icons.Rounded.AutoAwesome else Icons.Rounded.Star
    val fallbackTint = when {
        badge.isEarned && isZCrystal -> Color(0xFFC084FC)
        badge.isEarned -> Color(0xFFFBBF24)
        else -> VantafynColors.Muted
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(backgroundBrush)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(9.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = badge.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(earnedAlpha),
                    error = {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = badge.name,
                            tint = fallbackTint,
                            modifier = Modifier
                                .size(30.dp)
                                .alpha(earnedAlpha),
                        )
                    },
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = badge.name,
                    tint = fallbackTint,
                    modifier = Modifier
                        .size(30.dp)
                        .alpha(earnedAlpha),
                )
            }
        }
        Text(
            text = badge.name.removeSuffix(" Badge").removeSuffix(" Stamp"),
            color = if (badge.isEarned) VantafynColors.Ink else VantafynColors.Muted,
            fontSize = 10.sp,
            fontWeight = if (badge.isEarned) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RegionCompletionPill(
    earned: Int,
    total: Int,
) {
    val isComplete = earned >= total && total > 0
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isComplete) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.13f))
            .border(
                1.dp,
                if (isComplete) Color(0xFF10B981).copy(alpha = 0.42f) else Color(0xFFF59E0B).copy(alpha = 0.35f),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = if (isComplete) "Complete" else "$earned/$total",
            color = if (isComplete) Color(0xFF34D399) else Color(0xFFFBBF24),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = if (isComplete) Color(0xFF34D399) else Color(0xFFFBBF24),
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun BadgeCaseProgressBar(
    earned: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val progress = (earned.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFBBF24),
                            Color(0xFF38BDF8),
                            Color(0xFF8B5CF6),
                        )
                    )
                ),
        )
    }
}

@Composable
private fun BadgeCaseEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141824))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(VantafynGradients.accentHorizontal()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text(
                text = "No badge data yet",
                color = VantafynColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Refresh after a Gen 1-3 save is synced or available locally.",
                color = VantafynColors.Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun JellyfinSession.toAuthenticatedBadgeArtUrl(rawImageUrl: String): String {
    val base = server.url.trimEnd('/')
    val url = if (rawImageUrl.startsWith("http://", ignoreCase = true) || rawImageUrl.startsWith("https://", ignoreCase = true)) {
        rawImageUrl
    } else {
        "$base/${rawImageUrl.trimStart('/')}"
    }
    if (accessToken.isBlank()) return url

    val queryParams = buildList {
        if (!url.contains("api_key=", ignoreCase = true)) add("api_key=$accessToken")
        if (!url.contains("X-Emby-Token=", ignoreCase = true)) add("X-Emby-Token=$accessToken")
    }

    return if (queryParams.isEmpty()) {
        url
    } else {
        "$url${if (url.contains("?")) "&" else "?"}${queryParams.joinToString("&")}"
    }
}

private fun badgeArtKey(region: String, badgeId: String): String =
    "${region.trim().lowercase()}:${badgeId.trim().lowercase()}"
