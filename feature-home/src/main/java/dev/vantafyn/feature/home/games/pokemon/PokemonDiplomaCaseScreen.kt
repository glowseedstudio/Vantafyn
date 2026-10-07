package dev.vantafyn.feature.home.games.pokemon

import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonDiplomaProofDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.cleanGameTitle
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal
import kotlinx.coroutines.launch

private data class DiplomaCaseItem(
    val save: PokemonGameSaveDto,
    val gameTitle: String,
    val certificateId: String,
    val title: String,
    val region: String,
    val requiredCaught: Int,
    val accent: Color,
    val boxartUrl: String?,
)

private data class DiplomaProgress(
    val registered: Int,
    val isStrictSaveDex: Boolean,
)

@Composable
fun PokemonDiplomaCaseScreen(
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
    var proofs by remember { mutableStateOf<List<PokemonDiplomaProofDto>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    fun loadProofs() {
        val active = session ?: return
        scope.launch {
            pokemonRepository.getDiplomaProofs(active).fold(
                onSuccess = { proofs = it },
                onFailure = { message = it.message ?: "Could not load diploma proofs." },
            )
        }
    }

    LaunchedEffect(session, pokemonRepository) { loadProofs() }

    val items = remember(availableGames, detectedSaves) {
        detectedSaves
            .filter { save ->
                save.saveFound && save.providerAvailable &&
                    (save.party.isNotEmpty() ||
                     save.boxes.any { it.entries.isNotEmpty() } ||
                     save.totalPokemonCount > 0 ||
                     (save.pokedexCaught ?: 0) > 0 ||
                     save.gymBadges.any { r -> r.badges.any { it.isEarned } })
            }
            .flatMap { save ->
                val game = availableGames.firstOrNull { it.id == save.gameId }
                val expectedGen = game?.pokemon?.generation ?: 0
                if (expectedGen != 0 && save.generation != 0 && expectedGen != save.generation) {
                    return@flatMap emptyList()
                }
                val title = game?.pokemon?.canonicalTitle
                    ?: game?.cleanTitle
                    ?: cleanGameTitle(save.title).ifBlank { save.title.ifBlank { "Pokemon Save" } }
                val localBoxart = game?.let {
                    context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE)
                        .getString("boxart_${it.id}", null)
                }
                val boxartUrl = GameBoxartScraper.convertToCdnUrl(localBoxart ?: game?.boxartUrl)
                diplomaDefinitionsFor(save, title, boxartUrl)
            }
            .sortedWith(compareBy({ it.save.generation }, { it.gameTitle }, { it.requiredCaught }))
    }

    LaunchedEffect(items.size) {
        if (selectedIndex > items.lastIndex) selectedIndex = 0
    }

    var pendingUpload by remember { mutableStateOf<DiplomaCaseItem?>(null) }
    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val active = session ?: return@rememberLauncherForActivityResult
        val item = pendingUpload ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = "Sealing diploma proof..."
            pokemonRepository.uploadDiplomaProof(
                active,
                context.contentResolver,
                uri,
                item.save.gameId,
                item.certificateId,
                item.title,
            ).fold(
                onSuccess = {
                    message = "Diploma proof added."
                    loadProofs()
                },
                onFailure = { message = it.message ?: "Could not upload that diploma screenshot." },
            )
        }
    }

    GameScreenReveal(key = "pokemon_diploma_case_screen", modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DiplomaHeader(onBack = onBack, onRefresh = {
                onRefresh()
                loadProofs()
            })

            if (items.isEmpty()) {
                DiplomaEmptyState()
            } else {
                DiplomaSelector(items, selectedIndex) { selectedIndex = it }
                AnimatedContent(
                    targetState = items[selectedIndex],
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "DiplomaCaseSelected",
                ) { item ->
                    val proof = proofs.firstOrNull { it.gameId == item.save.gameId && it.certificateId == item.certificateId }
                    DiplomaStage(
                        item = item,
                        proof = proof,
                        imageUrl = proof?.imageUrl?.let { session?.toDiplomaImageUrl(it) },
                        onUpload = {
                            pendingUpload = item
                            uploadLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp"))
                        },
                    )
                }
                message?.let {
                    Text(
                        text = it,
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun DiplomaHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactBackButton(onClick = onBack)
            Column {
                Text("Diploma Case", color = VantafynColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Pokédex completion certificates", color = VantafynColors.Muted, fontSize = 11.sp)
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, null, tint = VantafynColors.Muted)
        }
    }
}

@Composable
private fun DiplomaSelector(items: List<DiplomaCaseItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .width(190.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color(0xFF1D2335) else Color(0xFF131722))
                    .border(
                        1.dp,
                        if (selected) item.accent.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(18.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Diamond, null, tint = if (selected) item.accent else VantafynColors.Muted, modifier = Modifier.size(16.dp))
                        Text(item.region, color = if (selected) item.accent else VantafynColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(item.gameTitle, color = VantafynColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    DiplomaMiniProgress(item)
                }
            }
        }
    }
}

@Composable
private fun DiplomaStage(
    item: DiplomaCaseItem,
    proof: PokemonDiplomaProofDto?,
    imageUrl: String?,
    onUpload: () -> Unit,
) {
    val progress = item.diplomaProgress()
    val verifiedBySave = progress.registered >= item.requiredCaught
    val earned = verifiedBySave || proof != null
    val canUploadProof = verifiedBySave || proof != null || !progress.isStrictSaveDex
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        item.accent.copy(alpha = 0.24f),
                        Color(0xFF121622),
                        Color(0xFF080A11),
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(item.accent.copy(alpha = 0.78f), Color(0xFF00E5FF).copy(alpha = 0.28f), Color.White.copy(alpha = 0.12f))),
                RoundedCornerShape(28.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0B0E16))
                    .border(1.dp, item.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.boxartUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.boxartUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.48f),
                                        Color.Black.copy(alpha = 0.72f),
                                        Color.Black.copy(alpha = 0.9f),
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(item.accent.copy(alpha = 0.08f))
                    )
                }
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = proof?.title ?: item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                    )
                } else {
                    GeneratedDiploma(item = item, earned = earned)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (earned) Icons.Rounded.Verified else Icons.Rounded.Lock, null, tint = if (earned) Color(0xFF34D399) else VantafynColors.Muted, modifier = Modifier.size(18.dp))
                    Text(if (earned) "Earned" else "Locked", color = if (earned) Color(0xFF34D399) else VantafynColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(item.title, color = VantafynColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(
                    text = "${progress.registered.coerceAtMost(item.requiredCaught)} / ${item.requiredCaught} registered",
                    color = VantafynColors.Muted,
                    fontSize = 12.sp,
                )
                if (!progress.isStrictSaveDex) {
                    Text(
                        text = "Waiting for verified Pokédex data from this save",
                        color = VantafynColors.Muted.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (canUploadProof) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(item.accent.copy(alpha = 0.16f))
                        .border(1.dp, item.accent.copy(alpha = 0.48f), RoundedCornerShape(999.dp))
                        .clickable { onUpload() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CloudUpload, null, tint = item.accent, modifier = Modifier.size(18.dp))
                        Text(if (proof == null) "Add screenshot proof" else "Replace screenshot proof", color = item.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedDiploma(item: DiplomaCaseItem, earned: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("VANTAFYN ARCHIVE", color = item.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(item.accent.copy(alpha = 0.45f), Color(0xFF111827))))
                    .border(1.dp, item.accent.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (earned) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock, null, tint = if (earned) item.accent else VantafynColors.Muted, modifier = Modifier.size(46.dp))
            }
            Text(item.region.uppercase(), color = VantafynColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Pokédex Diploma", color = VantafynColors.Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(item.gameTitle, color = item.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Text(
            text = if (earned) "Completion verified from linked save data" else "Complete this Pokédex to unlock the certificate",
            color = VantafynColors.Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DiplomaMiniProgress(item: DiplomaCaseItem) {
    val progress = (item.diplomaProgress().registered.toFloat() / item.requiredCaught.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(999.dp)).background(item.accent))
    }
}

private fun DiplomaCaseItem.diplomaProgress(): DiplomaProgress {
    val caughtCount = save.pokedexCaught
    val storageDerivedSpeciesCount = save.caughtSpeciesIds.size
    val nativeNdsStorageFallback = save.generation in 4..5 &&
        save.platform.isBlank() &&
        caughtCount != null &&
        caughtCount == storageDerivedSpeciesCount

    return when {
        caughtCount != null && !nativeNdsStorageFallback ->
            DiplomaProgress(caughtCount, isStrictSaveDex = true)
        save.generation in 1..3 && storageDerivedSpeciesCount > 0 ->
            DiplomaProgress(storageDerivedSpeciesCount, isStrictSaveDex = true)
        else ->
            DiplomaProgress(0, isStrictSaveDex = false)
    }
}

@Composable
private fun DiplomaEmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF141824)).padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(42.dp))
            Text("No diploma data yet", color = VantafynColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Refresh after a Pokémon save with Pokédex data is synced.", color = VantafynColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun diplomaDefinitionsFor(save: PokemonGameSaveDto, gameTitle: String, boxartUrl: String?): List<DiplomaCaseItem> {
    val key = "${save.gameId} ${save.title} $gameTitle".lowercase()
    fun item(id: String, title: String, region: String, required: Int, accent: Color) =
        DiplomaCaseItem(save, gameTitle, id, title, region, required, accent, boxartUrl)

    val gen = save.generation
    return when {
        gen == 1 || ((key.contains("firered") || key.contains("leafgreen")) && gen == 3) ->
            listOf(item("kanto-diploma", "Kanto Pokédex Diploma", "Kanto", 150, Color(0xFFFBBF24)))
        (gen == 2 && (key.contains("gold") || key.contains("silver") || key.contains("crystal"))) ||
        (gen == 4 && (key.contains("heartgold") || key.contains("soulsilver") || key.contains("hgss"))) ->
            listOf(item("johto-diploma", "Johto Pokédex Diploma", "Johto", 256, Color(0xFFA78BFA)))
        gen == 3 && (key.contains("ruby") || key.contains("sapphire") || key.contains("emerald")) ->
            listOf(item("hoenn-diploma", "Hoenn Pokédex Diploma", "Hoenn", 200, Color(0xFF38BDF8)))
        gen == 4 && (key.contains("diamond") || key.contains("pearl") || key.contains("platinum")) ->
            listOf(item("sinnoh-diploma", "Sinnoh Pokédex Diploma", "Sinnoh", 210, Color(0xFFF472B6)))
        gen == 5 && (key.contains("black") || key.contains("white") || key.contains("b2w2")) ->
            listOf(item("unova-diploma", "Unova Pokédex Diploma", "Unova", 156, Color(0xFF34D399)))
        else -> emptyList()
    }
}

private fun JellyfinSession.toDiplomaImageUrl(rawImageUrl: String): String {
    val base = server.url.trimEnd('/')
    val url = if (rawImageUrl.startsWith("http://", true) || rawImageUrl.startsWith("https://", true)) rawImageUrl else "$base/${rawImageUrl.trimStart('/')}"
    if (accessToken.isBlank()) return url
    val params = buildList {
        if (!url.contains("api_key=", true)) add("api_key=$accessToken")
        if (!url.contains("X-Emby-Token=", true)) add("X-Emby-Token=$accessToken")
    }
    return if (params.isEmpty()) url else "$url${if (url.contains("?")) "&" else "?"}${params.joinToString("&")}"
}
