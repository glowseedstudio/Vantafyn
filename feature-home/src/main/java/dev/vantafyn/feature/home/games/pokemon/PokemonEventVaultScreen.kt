package dev.vantafyn.feature.home.games.pokemon

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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonEventUnlockDto
import dev.vantafyn.core.jellyfin.PokemonEventUnlockStatusDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.cleanGameTitle
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal
import kotlinx.coroutines.launch

private data class EventVaultGame(
    val save: PokemonGameSaveDto,
    val title: String,
    val subtitle: String,
)

@Composable
fun PokemonEventVaultScreen(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    availableGames: List<GameSummary>,
    detectedSaves: List<PokemonGameSaveDto>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var catalog by remember { mutableStateOf<List<PokemonEventUnlockDto>>(emptyList()) }
    var statuses by remember { mutableStateOf<Map<String, List<PokemonEventUnlockStatusDto>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingUnlock by remember { mutableStateOf<Pair<EventVaultGame, PokemonEventUnlockDto>?>(null) }

    val games = remember(availableGames, detectedSaves) {
        detectedSaves
            .filter { it.saveFound && it.providerAvailable && it.generation in 2..5 }
            .map { save ->
                val game = availableGames.firstOrNull { it.id == save.gameId }
                val title = game?.pokemon?.canonicalTitle
                    ?: game?.cleanTitle
                    ?: cleanGameTitle(save.title).ifBlank { save.title.ifBlank { "Pokémon Save" } }
                EventVaultGame(
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

    fun loadEvents() {
        val active = session ?: return
        scope.launch {
            isLoading = true
            val loadedCatalog = pokemonRepository.getEventUnlockCatalog(active)
                .onFailure { message = it.message ?: "Could not load event unlocks." }
                .getOrDefault(emptyList())
            val loadedStatuses = mutableMapOf<String, List<PokemonEventUnlockStatusDto>>()
            games.forEach { game ->
                pokemonRepository.getEventUnlockStatus(active, game.save.gameId)
                    .onSuccess { loadedStatuses[game.save.gameId] = it }
                    .onFailure { message = it.message ?: "Could not check ${game.title}." }
            }
            catalog = loadedCatalog
            statuses = loadedStatuses
            isLoading = false
        }
    }

    LaunchedEffect(session, games.map { it.save.gameId }.joinToString("|")) { loadEvents() }

    GameScreenReveal(key = "pokemon_event_vault", modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EventVaultHeader(
                isLoading = isLoading,
                onBack = onBack,
                onRefresh = {
                    onRefresh()
                    loadEvents()
                },
            )

            if (games.isEmpty()) {
                EventVaultEmpty()
            } else {
                EventVaultSelector(games, selectedIndex) { selectedIndex = it }
                val game = games[selectedIndex]
                val compatibleStatuses = statuses[game.save.gameId].orEmpty()
                val compatible = catalog.filter { event ->
                    compatibleStatuses.any { it.eventId == event.id }
                }
                AnimatedContent(
                    targetState = game.save.gameId,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "EventVaultGame",
                ) {
                    EventVaultStage(
                        game = game,
                        events = compatible,
                        statuses = compatibleStatuses.associateBy { it.eventId },
                        isBusy = isLoading,
                        onUnlock = { event -> pendingUnlock = game to event },
                    )
                }
            }

            val gen45 = games.any { it.save.generation in 4..5 }
            if (gen45) {
                EventVaultComingSoon()
            }

            message?.let {
                Text(
                    text = it,
                    color = Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    pendingUnlock?.let { (game, event) ->
        AlertDialog(
            onDismissRequest = { pendingUnlock = null },
            containerColor = Color(0xFF111827),
            titleContentColor = VantafynColors.Ink,
            textContentColor = VantafynColors.Muted,
            title = { Text("Unlock ${event.title}?") },
            text = { Text("A backup will be created first. Close the game before unlocking, then reload the battery save to play the event.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val active = session ?: return@TextButton
                        pendingUnlock = null
                        scope.launch {
                            isLoading = true
                            pokemonRepository.unlockEvent(active, game.save.gameId, event.id).fold(
                                onSuccess = { response ->
                                    message = response.message
                                    loadEvents()
                                },
                                onFailure = { message = it.message ?: "Event unlock failed." },
                            )
                            isLoading = false
                        }
                    },
                ) { Text("Unlock", color = Color(0xFF34D399), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnlock = null }) {
                    Text("Cancel", color = VantafynColors.Muted)
                }
            },
        )
    }
}

@Composable
private fun EventVaultHeader(isLoading: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactBackButton(onClick = onBack)
            Column {
                Text("Event Vault", color = VantafynColors.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Unlock preserved mythical events", color = VantafynColors.Muted, fontSize = 11.sp)
            }
        }
        IconButton(onClick = onRefresh, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF34D399), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = VantafynColors.Muted)
            }
        }
    }
}

@Composable
private fun EventVaultSelector(games: List<EventVaultGame>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        games.forEachIndexed { index, game ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .width(184.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (selected) Color(0xFF1D273D) else Color(0xFF151A28),
                                Color(0xFF111827),
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (selected) Color(0xFF34D399).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(18.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = if (selected) Color(0xFF34D399) else VantafynColors.Muted, modifier = Modifier.size(14.dp))
                        Text(game.subtitle, color = if (selected) Color(0xFF34D399) else VantafynColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(game.title, color = VantafynColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EventVaultStage(
    game: EventVaultGame,
    events: List<PokemonEventUnlockDto>,
    statuses: Map<String, PokemonEventUnlockStatusDto>,
    isBusy: Boolean,
    onUnlock: (PokemonEventUnlockDto) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF16261F),
                            Color(0xFF17162A),
                            Color(0xFF0F172A),
                        )
                    )
                )
                .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF34D399).copy(alpha = 0.5f), Color(0xFFFBBF24).copy(alpha = 0.22f))), RoundedCornerShape(24.dp))
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFFFBBF24)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(game.title, color = VantafynColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(game.subtitle, color = VantafynColors.Muted, fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventVaultMetric("Events", events.size.toString(), Color(0xFF34D399), Modifier.weight(1f))
                    EventVaultMetric("Unlocked", statuses.values.count { it.unlocked }.toString(), Color(0xFFFBBF24), Modifier.weight(1f))
                }
            }
        }

        if (events.isEmpty()) {
            EventVaultEmpty("No curated event unlocks are ready for this save yet.")
        } else {
            events.forEach { event ->
                val status = statuses[event.id]
                EventUnlockCard(
                    event = event,
                    status = status,
                    isBusy = isBusy,
                    onUnlock = { onUnlock(event) },
                )
            }
        }
    }
}

@Composable
private fun EventUnlockCard(
    event: PokemonEventUnlockDto,
    status: PokemonEventUnlockStatusDto?,
    isBusy: Boolean,
    onUnlock: () -> Unit,
) {
    val accent = parseHexColor(event.accent)
    val unlocked = status?.unlocked == true
    val available = status?.available != false
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), Color(0xFF111827))))
            .border(1.dp, accent.copy(alpha = if (unlocked) 0.66f else 0.34f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (unlocked) Icons.Rounded.Verified else Icons.Rounded.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, color = VantafynColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(event.subtitle, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(event.description, color = VantafynColors.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                Text("Target: ${event.legendary}", color = VantafynColors.Ink.copy(alpha = 0.72f), fontSize = 10.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (unlocked) 0.10f else 0.18f))
                    .clickable(enabled = available && !unlocked && !isBusy) { onUnlock() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (unlocked) "Ready" else "Unlock",
                        color = if (available) accent else VantafynColors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!unlocked) Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun EventVaultMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111827).copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Column {
                Text(label, color = VantafynColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = VantafynColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun EventVaultComingSoon() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B2238), Color(0xFF111827))))
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Gen 4/5 Wonder Cards", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Importable Wonder Card support is staged for the next pass, so DS events can use preserved card files safely.", color = VantafynColors.Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun EventVaultEmpty(text: String = "No supported Gen 2-5 saves found yet.") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF111827).copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = VantafynColors.Muted, fontSize = 12.sp)
    }
}

private fun parseHexColor(value: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(value.takeIf { it.startsWith("#") } ?: "#$value"))
    }.getOrDefault(Color(0xFFFBBF24))
}
