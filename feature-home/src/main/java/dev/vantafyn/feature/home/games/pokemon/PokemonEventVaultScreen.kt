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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonEventUnlockDto
import dev.vantafyn.core.jellyfin.PokemonEventUnlockStatusDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonMysteryGiftDto
import dev.vantafyn.core.jellyfin.PokemonMysteryGiftRedeemResponse
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.vantafyn.core.jellyfin.cleanGameTitle
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.feature.home.games.GameScreenReveal
import kotlinx.coroutines.launch

private data class EventVaultGame(
    val save: PokemonGameSaveDto,
    val title: String,
    val subtitle: String,
    val boxartUrl: String? = null,
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var catalog by remember { mutableStateOf<List<PokemonEventUnlockDto>>(emptyList()) }
    var statuses by remember { mutableStateOf<Map<String, List<PokemonEventUnlockStatusDto>>>(emptyMap()) }
    var mysteryGifts by remember { mutableStateOf<Map<String, List<PokemonMysteryGiftDto>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingUnlock by remember { mutableStateOf<Pair<EventVaultGame, PokemonEventUnlockDto>?>(null) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var celebrationGift by remember { mutableStateOf<PokemonMysteryGiftRedeemResponse?>(null) }

    val games = remember(availableGames, detectedSaves) {
        detectedSaves
            .filter { it.saveFound && it.providerAvailable && it.generation in 2..7 }
            .map { save ->
                val game = availableGames.firstOrNull { it.id == save.gameId }
                val title = game?.pokemon?.canonicalTitle
                    ?: game?.cleanTitle
                    ?: cleanGameTitle(save.title).ifBlank { save.title.ifBlank { "Pokémon Save" } }
                val localBoxart = game?.let {
                    context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                        .getString("boxart_${it.id}", null)
                }
                val boxartUrl = GameBoxartScraper.convertToCdnUrl(localBoxart ?: game?.boxartUrl)
                EventVaultGame(
                    save = save,
                    title = title,
                    subtitle = buildString {
                        append("Gen ${save.generation.takeIf { it > 0 } ?: "?"}")
                        if (!save.trainerName.isNullOrBlank()) append(" • ${save.trainerName}")
                    },
                    boxartUrl = boxartUrl,
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
            val loadedMysteryGifts = mutableMapOf<String, List<PokemonMysteryGiftDto>>()
            games.forEach { game ->
                pokemonRepository.getEventUnlockStatus(active, game.save.gameId)
                    .onSuccess { loadedStatuses[game.save.gameId] = it }
                    .onFailure { message = it.message ?: "Could not check ${game.title}." }
                pokemonRepository.getMysteryGiftCodesForGame(active, game.save.gameId)
                    .onSuccess { loadedMysteryGifts[game.save.gameId] = it }
            }
            catalog = loadedCatalog
            statuses = loadedStatuses
            mysteryGifts = loadedMysteryGifts
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
                hasGames = games.isNotEmpty(),
                onBack = onBack,
                onRefresh = {
                    onRefresh()
                    loadEvents()
                },
                onRedeemMysteryGift = { showRedeemDialog = true },
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
                        mysteryGifts = mysteryGifts[game.save.gameId].orEmpty(),
                        isBusy = isLoading,
                        onUnlock = { event -> pendingUnlock = game to event },
                        onOpenMysteryGift = { showRedeemDialog = true },
                    )
                }
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

    if (showRedeemDialog && games.isNotEmpty()) {
        val activeGame = games.getOrNull(selectedIndex) ?: games[0]
        val activeCodes = mysteryGifts[activeGame.save.gameId].orEmpty()
        RedeemMysteryGiftDialog(
            game = activeGame,
            availableCodes = activeCodes,
            isBusy = isLoading,
            onRedeem = { code ->
                val active = session ?: return@RedeemMysteryGiftDialog
                scope.launch {
                    isLoading = true
                    pokemonRepository.redeemMysteryGift(active, activeGame.save.gameId, code).fold(
                        onSuccess = { response ->
                            showRedeemDialog = false
                            celebrationGift = response
                            loadEvents()
                        },
                        onFailure = { err ->
                            message = err.message ?: "Failed to redeem code."
                        },
                    )
                    isLoading = false
                }
            },
            onDismiss = { showRedeemDialog = false },
        )
    }

    celebrationGift?.let { gift ->
        MysteryGiftCelebrationDialog(
            gift = gift,
            onDismiss = { celebrationGift = null },
        )
    }

    pendingUnlock?.let { (game, event) ->
        val accent = parseHexColor(event.accent)
        AlertDialog(
            onDismissRequest = { pendingUnlock = null },
            containerColor = Color(0xFF111827),
            titleContentColor = VantafynColors.Ink,
            textContentColor = VantafynColors.Muted,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("Unlock ${event.title}?")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(event.description, fontSize = 12.sp, color = VantafynColors.Muted)
                    if (event.inGameLocation.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.12f))
                                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "In-Game Destination:",
                                    color = accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    event.inGameLocation,
                                    color = VantafynColors.Ink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Text(
                        "An automatic backup of your save will be created before applying this unlock. Make sure to close the game first, then reload the battery save to play.",
                        fontSize = 11.sp,
                        color = VantafynColors.Muted,
                    )
                }
            },
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
                ) { Text("Unlock Now", color = Color(0xFF34D399), fontWeight = FontWeight.Bold) }
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
private fun EventVaultHeader(
    isLoading: Boolean,
    hasGames: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRedeemMysteryGift: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactBackButton(onClick = onBack)
            Text("Event Vault", color = VantafynColors.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (hasGames) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFFEC4899).copy(alpha = 0.20f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFFA855F7).copy(alpha = 0.6f), Color(0xFFEC4899).copy(alpha = 0.5f))
                            ),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(enabled = !isLoading) { onRedeemMysteryGift() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            "Mystery Code",
                            color = VantafynColors.Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
                    .clickable { onSelect(index) },
            ) {
                if (!game.boxartUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = game.boxartUrl,
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
                                        Color(0xFF090B13).copy(alpha = if (selected) 0.80f else 0.88f),
                                        Color(0xFF06070B).copy(alpha = if (selected) 0.90f else 0.95f),
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
    mysteryGifts: List<PokemonMysteryGiftDto>,
    isBusy: Boolean,
    onUnlock: (PokemonEventUnlockDto) -> Unit,
    onOpenMysteryGift: () -> Unit,
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
                .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF34D399).copy(alpha = 0.5f), Color(0xFFFBBF24).copy(alpha = 0.22f))), RoundedCornerShape(24.dp)),
        ) {
            if (!game.boxartUrl.isNullOrBlank()) {
                AsyncImage(
                    model = game.boxartUrl,
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
                                    Color(0xFF090B13).copy(alpha = 0.78f),
                                    Color(0xFF06070B).copy(alpha = 0.92f),
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                    if (mysteryGifts.isNotEmpty()) {
                        EventVaultMetric("Codes", "${mysteryGifts.count { it.isRedeemed }}/${mysteryGifts.size}", Color(0xFFA855F7), Modifier.weight(1f))
                    }
                }
            }
        }

        MysteryGiftBanner(
            mysteryGifts = mysteryGifts,
            isBusy = isBusy,
            onClick = onOpenMysteryGift,
        )

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
private fun MysteryGiftBanner(
    mysteryGifts: List<PokemonMysteryGiftDto>,
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    val redeemedCount = mysteryGifts.count { it.isRedeemed }
    val totalCount = mysteryGifts.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2E1065).copy(alpha = 0.55f),
                        Color(0xFF1E1B4B).copy(alpha = 0.65f),
                        Color(0xFF0F172A).copy(alpha = 0.85f),
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFA855F7).copy(alpha = 0.6f),
                        Color(0xFFEC4899).copy(alpha = 0.4f),
                        Color(0xFF3B82F6).copy(alpha = 0.3f),
                    )
                ),
                RoundedCornerShape(20.dp),
            )
            .clickable(enabled = !isBusy) { onClick() }
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFA855F7), Color(0xFFEC4899))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.CardGiftcard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Redeem Mystery Gift",
                        color = VantafynColors.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (totalCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFA855F7).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "$redeemedCount/$totalCount CLAIMED",
                                color = Color(0xFFE9D5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    "Enter Wonder Card codes & historical passwords",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(20.dp),
            )
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
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = if (unlocked) 0.18f else 0.12f),
                        Color(0xFF0F172A).copy(alpha = 0.95f),
                        Color(0xFF090D16),
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = if (unlocked) 0.75f else 0.40f),
                        Color.White.copy(alpha = 0.08f),
                        accent.copy(alpha = if (unlocked) 0.40f else 0.20f),
                    )
                ),
                RoundedCornerShape(22.dp),
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Badge Row: Wonder Card Tag + Generation/Region Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        "WONDER CARD EVENT",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "GEN ${event.generation} • ${event.region.uppercase()}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            // Middle Section: Left info & Right 3D Pokémon Render
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        event.title,
                        color = VantafynColors.Ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        event.subtitle,
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        event.description,
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )

                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            "Target: ${event.legendary}",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // 3D Pokémon Render
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accent.copy(alpha = 0.28f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (event.targetSpeciesId > 0) {
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/${event.targetSpeciesId}.png",
                            contentDescription = event.legendary,
                            modifier = Modifier.size(76.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            // In-Game Destination Pill (if present)
            if (event.inGameLocation.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.40f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Destination: ${event.inGameLocation}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp,
                    )
                }
            }

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (unlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "Ready to play in-game",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "✓ Unlocked",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            if (available) "Event locked in save" else (status.reason ?: "Unavailable"),
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (available && !isBusy) {
                                    Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.82f)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                                }
                            )
                            .clickable(enabled = available && !unlocked && !isBusy) { onUnlock() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Unlock Event",
                                color = if (available) Color(0xFF0F172A) else VantafynColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = if (available) Color(0xFF0F172A) else VantafynColors.Muted,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
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
private fun EventVaultEmpty(text: String = "No supported Gen 2-7 saves found yet.") {
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

@Composable
private fun RedeemMysteryGiftDialog(
    game: EventVaultGame,
    availableCodes: List<PokemonMysteryGiftDto>,
    isBusy: Boolean,
    onRedeem: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var enteredCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = maxHeight * 0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.98f))
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B35FF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF8B35FF).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CardGiftcard,
                                contentDescription = null,
                                tint = Color(0xFF21D8FF),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column {
                            Text(
                                "Redeem Mystery Gift",
                                color = VantafynColors.Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                            )
                            Text(
                                "${game.title} • ${game.subtitle}",
                                color = VantafynColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = !isBusy, onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Text(
                    "Enter a promotional code to inject preserved Wonder Cards and events directly into your battery save.",
                    fontSize = 12.sp,
                    color = VantafynColors.Muted,
                )

                // Standard VantafynTextField with signature animated gradient focus glow & border
                VantafynTextField(
                    value = enteredCode,
                    onValueChange = {
                        enteredCode = it.uppercase().trim()
                        localError = null
                    },
                    label = "Mystery Gift Code",
                    placeholder = "e.g. LIBERTY-PASS, MEMBER-CARD",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    trailingIcon = {
                        if (enteredCode.isNotBlank()) {
                            IconButton(onClick = { enteredCode = "" }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = VantafynColors.Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                )

                localError?.let {
                    Text(it, color = Color(0xFFFF8A8A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (availableCodes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "PRESERVED HISTORICAL CODES",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            "${availableCodes.count { it.isRedeemed }}/${availableCodes.size} Claimed",
                            color = Color(0xFF21D8FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableCodes.forEach { gift ->
                            val giftAccent = parseHexColor(gift.accent)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (gift.isRedeemed) Color(0xFF1E293B).copy(alpha = 0.5f)
                                        else Color(0xFF1F2937).copy(alpha = 0.8f)
                                    )
                                    .border(
                                        1.dp,
                                        if (gift.isRedeemed) Color(0xFF10B981).copy(alpha = 0.35f)
                                        else giftAccent.copy(alpha = 0.35f),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable(enabled = !gift.isRedeemed) {
                                        enteredCode = gift.code
                                    }
                                    .padding(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(giftAccent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (gift.targetSpeciesId > 0) {
                                            AsyncImage(
                                                model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${gift.targetSpeciesId}.png",
                                                contentDescription = gift.targetSpeciesName,
                                                modifier = Modifier.size(34.dp),
                                                contentScale = ContentScale.Fit,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.CardGiftcard,
                                                contentDescription = null,
                                                tint = giftAccent,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                gift.code,
                                                color = if (gift.isRedeemed) VantafynColors.Muted else VantafynColors.Ink,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(giftAccent.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                            ) {
                                                Text(
                                                    gift.rewardType,
                                                    color = giftAccent,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                        Text(
                                            gift.title,
                                            color = VantafynColors.Muted,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    if (gift.isRedeemed) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                "CLAIMED",
                                                color = Color(0xFF10B981),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF8B35FF).copy(alpha = 0.2f))
                                                .border(1.dp, Color(0xFF8B35FF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                "USE",
                                                color = Color(0xFF21D8FF),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action buttons: Cancel + VantafynButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isBusy) {
                        Text("Cancel", color = VantafynColors.Muted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    VantafynButton(
                        text = if (isBusy) "Redeeming..." else "Redeem Gift",
                        onClick = {
                            if (enteredCode.isBlank()) {
                                localError = "Please enter a Mystery Gift code."
                            } else {
                                onRedeem(enteredCode)
                            }
                        },
                        enabled = enteredCode.isNotBlank() && !isBusy,
                    )
                }
            }
        }
    }
}

@Composable
private fun MysteryGiftCelebrationDialog(
    gift: PokemonMysteryGiftRedeemResponse,
    onDismiss: () -> Unit,
) {
    val accent = parseHexColor(gift.accent)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.98f))
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFFA855F7))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Celebration,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Text(
                    "WONDER CARD RECEIVED!",
                    color = Color(0xFFFBBF24),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    gift.title,
                    color = VantafynColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (gift.subtitle.isNotBlank()) {
                    Text(
                        gift.subtitle,
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }

                // Sprite preview
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accent.copy(alpha = 0.35f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (gift.targetSpeciesId > 0) {
                        AsyncImage(
                            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/${gift.targetSpeciesId}.png",
                            contentDescription = gift.targetSpeciesName,
                            modifier = Modifier.size(88.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.CardGiftcard,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                // Reward badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        if (gift.targetSpeciesName.isNotBlank()) {
                            "${gift.targetSpeciesName} • ${gift.rewardType}"
                        } else {
                            gift.rewardType
                        },
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (gift.inGameInstructions.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    "HOW TO RECEIVE IN-GAME:",
                                    color = Color(0xFF34D399),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                            Text(
                                gift.inGameInstructions,
                                color = VantafynColors.Ink,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }

                Text(
                    "Your battery save file was safely patched and backed up automatically.",
                    color = VantafynColors.Muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )

                VantafynButton(
                    text = "Awesome, Let's Play!",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
