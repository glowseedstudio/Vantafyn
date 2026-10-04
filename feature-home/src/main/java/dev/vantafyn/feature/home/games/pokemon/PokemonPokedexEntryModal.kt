package dev.vantafyn.feature.home.games.pokemon

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.DefaultJellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonBoxDto
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonPokedexEntryDto
import dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.jellyfin.openAuthenticatedConnection
import dev.vantafyn.core.media.games.GameHubSoundManager
import dev.vantafyn.core.media.games.PokedexVoiceAudioProcessor
import dev.vantafyn.core.media.VantafynExoPlayerFactory
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Authentic, encyclopedic Pokédex Entry screen presenting Pokémon species lore, physical stats,
 * canonical base stats, typing, and personal PKVault save lineage.
 * Features full-page presentation, left/right chevrons, and unregistered silhouette display.
 */
@Composable
fun PokemonPokedexEntryModal(
    entry: PokemonPokedexEntryDto,
    matchedSpecimen: PokemonSummaryDto? = null,
    matchedDetails: PokemonDetailsDto? = null,
    totalOwnedCount: Int = 0,
    session: JellyfinSession? = null,
    allEntries: List<PokemonPokedexEntryDto> = emptyList(),
    localPokemonList: List<Pair<PokemonSummaryDto, String>> = emptyList(),
    localSaves: List<PokemonGameSaveDto> = emptyList(),
    vaultBoxes: List<PokemonBoxDto> = emptyList(),
    onInspectSpecimen: ((PokemonSummaryDto, PokemonDetailsDto?) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val metadataRepository = remember { DefaultJellyfinPokemonRepository() }
    val narrationScope = rememberCoroutineScope()

    // Duck ambient background music so audio cries and Pokédex narration are heard clearly.
    DisposableEffect(Unit) {
        GameHubSoundManager.duck(context, duckFactor = 0.20f, durationMs = 400L)
        onDispose {
            GameHubSoundManager.unduck(context, durationMs = 400L)
        }
    }

    val prefs = remember { context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE) }
    var autoPlayCries by remember {
        mutableStateOf(prefs.getBoolean("pokemon_cry_autoplay", true))
    }
    var cryStyle by remember {
        mutableStateOf(prefs.getString("pokemon_cry_style", "latest") ?: "latest")
    }
    var autoPlayNarration by remember {
        mutableStateOf(prefs.getBoolean("pokemon_narration_autoplay", false))
    }

    var currentSpeciesId by remember(entry.speciesId) {
        mutableIntStateOf(entry.speciesId)
    }

    val currentEntry = remember(currentSpeciesId, allEntries, entry) {
        allEntries.firstOrNull { it.speciesId == currentSpeciesId }
            ?: if (currentSpeciesId == entry.speciesId) entry else null
    }

    val isRegistered = currentEntry != null && (currentEntry.isCaught || currentEntry.isSeen)
    val isCaught = currentEntry?.isCaught == true
    val isSeen = currentEntry?.isSeen == true || isCaught
    val hasShiny = currentEntry?.hasShiny == true

    val fallbackDexData = remember(currentSpeciesId) {
        PokemonPokedexCatalog.getPokedexData(currentSpeciesId)
    }
    var fetchedDexMetadata by remember(currentSpeciesId) { mutableStateOf<PokedexMetadata?>(null) }
    LaunchedEffect(currentSpeciesId) {
        fetchedDexMetadata = PokeApiPokedexRepository.load(context, currentSpeciesId, fallbackDexData, session, metadataRepository)
    }
    val dexData = fetchedDexMetadata?.applyTo(fallbackDexData) ?: fallbackDexData
    var selectedFormKey by remember(currentSpeciesId) { mutableStateOf<String?>(null) }
    val alternateForms = remember(currentSpeciesId) {
        PokemonFormsCatalog.getForms(currentSpeciesId)
    }
    val activeForm = remember(selectedFormKey, alternateForms) {
        alternateForms.firstOrNull { it.spriteKey == selectedFormKey }
            ?: alternateForms.firstOrNull()
    }
    val (fallbackPrimaryType, fallbackSecondaryType) = remember(currentSpeciesId, dexData.name, selectedFormKey, activeForm?.name) {
        PokemonTypeCatalog.getTypes(
            speciesId = currentSpeciesId,
            speciesName = dexData.name,
            formKey = selectedFormKey,
            formName = activeForm?.name,
        )
    }
    val primaryType = if (selectedFormKey != null) fallbackPrimaryType else fetchedDexMetadata?.primaryType ?: fallbackPrimaryType
    val secondaryType = if (selectedFormKey != null) fallbackSecondaryType else fetchedDexMetadata?.secondaryType ?: fallbackSecondaryType

    val cardBorderBrush = remember(primaryType, secondaryType, isRegistered) {
        if (isRegistered) {
            Brush.horizontalGradient(
                listOf(
                    primaryType.accentColor.copy(alpha = 0.35f),
                    (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.06f),
                )
            )
        } else {
            Brush.horizontalGradient(
                listOf(
                    primaryType.accentColor.copy(alpha = 0.22f),
                    Color(0xFF2E354B).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.04f),
                )
            )
        }
    }

    val matchingOwnedPokemon = remember(currentSpeciesId, localPokemonList) {
        localPokemonList.filter { it.first.speciesId == currentSpeciesId }
    }
    val activeOwnedCount = if (matchingOwnedPokemon.isNotEmpty()) matchingOwnedPokemon.size
        else if (currentSpeciesId == entry.speciesId) totalOwnedCount else 0

    val localMatch = matchingOwnedPokemon.firstOrNull()?.first
    val currentSummary = localMatch ?: if (currentSpeciesId == entry.speciesId) matchedSpecimen else null
    val matchingSave = localSaves.firstOrNull { save ->
        currentSummary != null && (save.party.any { it.id == currentSummary.id } ||
            save.boxes.any { b -> b.entries.any { it.id == currentSummary.id } })
    }
    val saveDetails = matchingSave?.pokemonDetails?.get(currentSummary?.id ?: "")
        ?: matchingSave?.pokemonDetails?.values?.firstOrNull { it.summary.speciesId == currentSpeciesId }
    val isVault = vaultBoxes.any { box -> box.entries.any { it.id == currentSummary?.id } }
    val currentDetails = saveDetails ?: if (currentSpeciesId == entry.speciesId) matchedDetails
        else currentSummary?.let { PokemonSpeciesCatalog.generateCanonicalDetails(it) }

    var isPlayingCry by remember { mutableStateOf(false) }
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingNarration by remember { mutableStateOf(false) }
    var narrationPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isNarrationLoading by remember { mutableStateOf(false) }
    var narrationRequestId by remember { mutableIntStateOf(0) }

    val stopNarration: () -> Unit = {
        narrationRequestId += 1
        narrationPlayer?.let { player ->
            try { player.stop() } catch (_: Throwable) {}
            try { player.release() } catch (_: Throwable) {}
        }
        narrationPlayer = null
        isPlayingNarration = false
        isNarrationLoading = false
    }

    val playNarration: () -> Unit = playNarration@{
        val targetSession = session ?: return@playNarration
        if (!isRegistered || isNarrationLoading) return@playNarration

        stopNarration()
        val requestId = narrationRequestId
        val speciesId = currentSpeciesId
        isNarrationLoading = true
        narrationScope.launch {
            val narrationFile = runCatching {
                withContext(Dispatchers.IO) {
                    val connection = targetSession.openAuthenticatedConnection(
                        "Vantafyn/Pokemon/Narration/$speciesId",
                    )
                    connection.connectTimeout = 20_000
                    connection.readTimeout = 70_000
                    try {
                        val status = connection.responseCode
                        check(status in 200..299) {
                            "Narration request returned HTTP $status"
                        }
                        val cacheDirectory = File(context.cacheDir, "pokedex-narration")
                        check(cacheDirectory.exists() || cacheDirectory.mkdirs()) {
                            "Could not create narration cache directory"
                        }
                        val destination = File(cacheDirectory, "$speciesId.mp3")
                        val temporaryFile = File(cacheDirectory, "$speciesId-${System.nanoTime()}.tmp")
                        connection.inputStream.use { input ->
                            temporaryFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        check(temporaryFile.length() > 0L) { "Narration response was empty" }
                        if (!temporaryFile.renameTo(destination)) {
                            temporaryFile.copyTo(destination, overwrite = true)
                            temporaryFile.delete()
                        }
                        destination
                    } finally {
                        connection.disconnect()
                    }
                }
            }.getOrElse { error ->
                if (requestId == narrationRequestId) {
                    isNarrationLoading = false
                    Log.w("VantafynNarration", "Could not download Pokédex narration for species $speciesId", error)
                }
                return@launch
            }

            if (requestId != narrationRequestId || currentSpeciesId != speciesId) return@launch
            try {
                val player = VantafynExoPlayerFactory.musicBuilder(
                    context,
                    audioProcessors = arrayOf(
                        PokedexVoiceAudioProcessor(
                            debugDirectory = if (
                                context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
                            ) {
                                File(context.cacheDir, "pokedex-voice-debug/$speciesId")
                            } else {
                                null
                            },
                        ),
                    ),
                ).build().apply {
                    volume = 0.90f
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY && requestId == narrationRequestId) {
                                isNarrationLoading = false
                                isPlayingNarration = true
                            } else if (state == Player.STATE_ENDED) {
                                isNarrationLoading = false
                                isPlayingNarration = false
                                release()
                                if (narrationPlayer === this@apply) narrationPlayer = null
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.w("VantafynNarration", "Pokédex narration playback failed for species $speciesId", error)
                            isNarrationLoading = false
                            isPlayingNarration = false
                            release()
                            if (narrationPlayer === this@apply) narrationPlayer = null
                        }
                    })
                    setMediaItem(MediaItem.fromUri(narrationFile.toURI().toString()))
                    prepare()
                    playWhenReady = true
                }
                narrationPlayer = player
            } catch (error: Throwable) {
                Log.w("VantafynNarration", "Could not prepare Pokédex narration for species $speciesId", error)
                isNarrationLoading = false
                isPlayingNarration = false
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    GameHubSoundManager.pause()
                    try {
                        activePlayer?.let { mp ->
                            if (mp.isPlaying) mp.stop()
                            mp.release()
                        }
                    } catch (_: Throwable) {}
                    activePlayer = null
                    isPlayingCry = false
                    stopNarration()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> GameHubSoundManager.resume(context)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val playCry: (String, Boolean) -> Unit = { style, narrateAfter ->
        try {
            stopNarration()
            activePlayer?.let { mp ->
                try { if (mp.isPlaying) mp.stop() } catch (_: Throwable) {}
                try { mp.release() } catch (_: Throwable) {}
            }
            activePlayer = null
            isPlayingCry = false

            val cryUrl = JellyfinPokemonRepository.getPokemonCryUrl(session, currentSpeciesId, style)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(cryUrl)
                setVolume(0.70f, 0.70f)
                isLooping = false
                setOnPreparedListener { mp ->
                    try {
                        mp.setVolume(0.70f, 0.70f)
                        mp.start()
                        isPlayingCry = true
                    } catch (_: Throwable) {
                        isPlayingCry = false
                    }
                }
                setOnCompletionListener { mp ->
                    isPlayingCry = false
                    try { mp.release() } catch (_: Throwable) {}
                    if (activePlayer === mp) activePlayer = null
                    if (narrateAfter && autoPlayNarration && isRegistered) playNarration()
                }
                setOnErrorListener { mp, _, _ ->
                    isPlayingCry = false
                    try { mp.release() } catch (_: Throwable) {}
                    if (activePlayer === mp) activePlayer = null
                    if (narrateAfter && autoPlayNarration && isRegistered) playNarration()
                    true
                }
                prepareAsync()
            }
            activePlayer = player
        } catch (error: Throwable) {
            Log.w("VantafynNarration", "Could not start Pokémon cry for species $currentSpeciesId", error)
            isPlayingCry = false
            if (narrateAfter && autoPlayNarration && isRegistered) playNarration()
        }
    }

    DisposableEffect(currentSpeciesId) {
        if (autoPlayCries && isRegistered) {
            playCry(cryStyle, true)
        } else if (autoPlayNarration && isRegistered) {
            playNarration()
        }
        onDispose {
            try {
                activePlayer?.let { mp ->
                    try { if (mp.isPlaying) mp.stop() } catch (_: Throwable) {}
                    try { mp.release() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
            activePlayer = null
            isPlayingCry = false
            stopNarration()
        }
    }

    val initialFemale = remember(currentSpeciesId, currentSummary) {
        currentSummary?.gender?.equals("Female", ignoreCase = true) == true ||
            currentSummary?.gender?.equals("Girl", ignoreCase = true) == true ||
            currentSummary?.gender?.equals("F", ignoreCase = true) == true
    }
    var showShinyArtwork by remember(currentSpeciesId, hasShiny) { mutableStateOf(hasShiny) }
    var showFemaleArtwork by remember(currentSpeciesId) { mutableStateOf(initialFemale) }

    val megaForms = remember(currentSpeciesId) {
        PokemonFormsCatalog.getMegaForms(currentSpeciesId)
    }
    val selectedMegaIndex = remember(selectedFormKey, megaForms) {
        megaForms.indexOfFirst { it.spriteKey == selectedFormKey }
    }
    val isMegaArtwork = selectedMegaIndex >= 0
    val activeMegaName = megaForms.getOrNull(selectedMegaIndex)?.name ?: "Mega"
    val megaSymbolUrl = remember(session) {
        session?.let {
            "${it.server.url.trimEnd('/')}/Vantafyn/Pokemon/MegaEvolution/Symbol?api_key=${it.accessToken}"
        }
    }

    val hasGenderDiff = remember(currentSpeciesId, selectedFormKey) {
        if (selectedFormKey != null) false else PokemonGenderCatalog.hasGenderDifferences(currentSpeciesId)
    }

    val artworkUrl = remember(currentSpeciesId, showShinyArtwork, showFemaleArtwork, selectedFormKey) {
        PokemonGenderCatalog.getPokedexArtworkUrl(
            speciesId = currentSpeciesId,
            isShiny = showShinyArtwork,
            isFemale = showFemaleArtwork,
            formKey = selectedFormKey,
        )
    }

    BackHandler(onBack = onDismiss)

    GameScreenReveal(
        key = "pokemon_pokedex_entry_modal",
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C14))
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
        val customBg = LocalPokemonModalBackground.current
        if (!customBg.isNullOrBlank()) {
            AsyncImage(
                model = customBg,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0C14).copy(alpha = 0.88f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // 1. Top Navigation Bar: Back, Chevrons, Dex Number, Region
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CompactBackButton(onClick = onDismiss)

                // Navigation Chevrons & Center Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Prev Chevron
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentSpeciesId > 1) Color(0xFF181C2B) else Color(0xFF12141F))
                            .border(
                                1.dp,
                                if (currentSpeciesId > 1) Color(0xFF2E354B) else Color(0xFF1E2232),
                                CircleShape
                            )
                            .clickable(enabled = currentSpeciesId > 1) {
                                currentSpeciesId--
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = "Previous Pokémon",
                            tint = if (currentSpeciesId > 1) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No. %04d".format(currentSpeciesId),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = if (isRegistered) dexData.name else "???",
                            color = if (isRegistered) primaryType.accentColor else VantafynColors.Muted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }

                    // Next Chevron
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentSpeciesId < 1025) Color(0xFF181C2B) else Color(0xFF12141F))
                            .border(
                                1.dp,
                                if (currentSpeciesId < 1025) Color(0xFF2E354B) else Color(0xFF1E2232),
                                CircleShape
                            )
                            .clickable(enabled = currentSpeciesId < 1025) {
                                currentSpeciesId++
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "Next Pokémon",
                            tint = if (currentSpeciesId < 1025) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Region Tag Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161928))
                        .border(1.dp, Color(0xFF282F48), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dexData.regionName,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 2. Hero Artwork & Backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isRegistered) {
                            // Registered species own the entire hero card: the type atmosphere
                            // is a full surface treatment, not a small glow behind the artwork.
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryType.accentColor.copy(alpha = 0.70f),
                                    (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.52f),
                                    primaryType.glowColor.copy(alpha = 0.34f),
                                    Color(0xFF111522).copy(alpha = 0.92f),
                                ),
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    primaryType.glowColor.copy(alpha = 0.30f),
                                    Color(0xFF1E2638).copy(alpha = 0.45f),
                                    Color(0xFF101320),
                                    Color(0xFF0B0D16),
                                ),
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isRegistered) primaryType.accentColor.copy(alpha = 0.25f) else primaryType.accentColor.copy(alpha = 0.20f),
                        RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isRegistered) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = dexData.name,
                        modifier = Modifier
                            .size(195.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )

                    if (megaForms.isNotEmpty()) {
                        MegaEvolutionQuickToggle(
                            symbolUrl = megaSymbolUrl,
                            isActive = isMegaArtwork,
                            label = activeMegaName,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(
                                    top = if (hasShiny) 48.dp else 10.dp,
                                    end = 10.dp,
                                ),
                            onClick = {
                                selectedFormKey = when {
                                    selectedMegaIndex < 0 -> megaForms.first().spriteKey
                                    selectedMegaIndex < megaForms.lastIndex -> megaForms[selectedMegaIndex + 1].spriteKey
                                    else -> null
                                }
                                showFemaleArtwork = false
                            },
                        )
                    }

                    // Gender toggle chip (for species with visual gender differences)
                    if (hasGenderDiff) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161928).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF282F48), RoundedCornerShape(12.dp))
                                .padding(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                // Male
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (!showFemaleArtwork) Color(0xFF0284C7).copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (!showFemaleArtwork) Color(0xFF38BDF8) else Color.Transparent,
                                            RoundedCornerShape(9.dp)
                                        )
                                        .clickable { showFemaleArtwork = false }
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text(
                                            text = "♂",
                                            color = if (!showFemaleArtwork) Color(0xFF38BDF8) else Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                        Text(
                                            text = "Male",
                                            color = if (!showFemaleArtwork) Color.White else Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                // Female
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (showFemaleArtwork) Color(0xFFE11D48).copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (showFemaleArtwork) Color(0xFFFB7185) else Color.Transparent,
                                            RoundedCornerShape(9.dp)
                                        )
                                        .clickable { showFemaleArtwork = true }
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text(
                                            text = "♀",
                                            color = if (showFemaleArtwork) Color(0xFFFB7185) else Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                        Text(
                                            text = "Female",
                                            color = if (showFemaleArtwork) Color.White else Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        // Difference note at bottom of artwork if female selected
                        if (showFemaleArtwork) {
                            val diffDesc = remember(currentSpeciesId) {
                                PokemonGenderCatalog.getGenderDifferenceDescription(currentSpeciesId)
                            }
                            if (!diffDesc.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp, start = 12.dp, end = 12.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F121E).copy(alpha = 0.90f))
                                        .border(1.dp, Color(0xFFFB7185).copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = diffDesc,
                                        color = Color(0xFFFDA4AF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // Shiny toggle chip (if user has unlocked shiny in their save archive)
                    if (hasShiny) {
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
                } else {
                    // Strong Ambient Backlight Glow behind the unregistered silhouette
                    Box(
                        modifier = Modifier
                            .size(185.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primaryType.glowColor.copy(alpha = 0.75f),
                                        (secondaryType?.accentColor ?: primaryType.accentColor).copy(alpha = 0.40f),
                                        Color(0xFF38BDF8).copy(alpha = 0.15f),
                                        Color.Transparent,
                                    )
                                )
                            )
                    )

                    // Grounded drop shadow beneath
                    Canvas(
                        modifier = Modifier
                            .size(width = 130.dp, height = 28.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF020408).copy(alpha = 0.70f),
                                    Color(0xFF020408).copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width / 2f,
                            )
                        )
                    }

                    // Unregistered Silhouette / Shadow Outline
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "Unregistered Pokémon",
                        colorFilter = ColorFilter.tint(Color(0xFF04060E).copy(alpha = 0.98f), BlendMode.SrcIn),
                        modifier = Modifier
                            .size(195.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2b. Alternate Forms & Regional Variants Carousel / Spinda Lore
            val isSpinda = remember(currentSpeciesId) {
                PokemonFormsCatalog.isSpinda(currentSpeciesId)
            }

            if (alternateForms.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (alternateForms.any { it.isMega }) "FORMS & MEGA EVOLUTION" else "FORMS & VARIATIONS",
                            color = if (alternateForms.any { it.isMega }) Color(0xFFFF6FE5) else VantafynColors.Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        if (activeForm != null && !activeForm.description.isNullOrBlank()) {
                            Text(
                                text = activeForm.description,
                                color = primaryType.accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        alternateForms.forEach { form ->
                            val isSelected = (selectedFormKey == form.spriteKey) ||
                                (selectedFormKey == null && form == alternateForms.first())
                            val formAccent = if (form.isMega) Color(0xFFFF4FD8) else primaryType.accentColor

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) formAccent.copy(alpha = if (form.isMega) 0.30f else 0.25f)
                                        else Color(0xFF141726)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) formAccent
                                        else if (form.isMega) Color(0xFFFF4FD8).copy(alpha = 0.45f)
                                        else Color(0xFF282F48),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        selectedFormKey = if (form == alternateForms.first()) null else form.spriteKey
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = form.name,
                                    color = if (isSelected) Color.White else if (form.isMega) Color(0xFFF0ABFC) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }

                    if (activeForm?.isMega == true && !activeForm.description.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF4FD8).copy(alpha = 0.18f),
                                            Color(0xFF7C3AED).copy(alpha = 0.12f),
                                            Color(0xFF22D3EE).copy(alpha = 0.10f),
                                            Color(0xFF121625),
                                        ),
                                    ),
                                )
                                .border(1.dp, Color(0xFFFF4FD8).copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.24f))
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFFF7AD),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "MEGA EVOLUTION",
                                        color = Color(0xFFFF6FE5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                    )
                                    Text(
                                        text = activeForm.description,
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 16.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            } else if (isSpinda) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161928))
                        .border(1.dp, Color(0xFFFB7185).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "🌀",
                            fontSize = 20.sp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "4,294,967,296 Spot Variations",
                                color = Color(0xFFFB7185),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Spinda's 4 facial spot coordinates are calculated from a 32-bit personality value, making every specimen mathematically unique in Pokémon history.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isRegistered) {
                // Registered Species View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = dexData.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )

                        // Pokémon Cry Replay / Speaker Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlayingCry) primaryType.accentColor.copy(alpha = 0.25f)
                                    else Color(0xFF1B1E2E)
                                )
                                .border(
                                    1.dp,
                                    if (isPlayingCry) primaryType.accentColor else Color(0xFF38405E),
                                    CircleShape
                                )
                                .clickable { playCry(cryStyle, false) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Replay Pokémon Cry",
                                tint = if (isPlayingCry) primaryType.accentColor else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        // Manual narration control also makes the feature available when
                        // auto-play is off, and provides an immediate, visible retry path.
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlayingNarration || isNarrationLoading) Color(0xFFA855F7).copy(alpha = 0.26f)
                                    else Color(0xFF1B1E2E),
                                )
                                .border(
                                    1.dp,
                                    if (isPlayingNarration || isNarrationLoading) Color(0xFFC084FC) else Color(0xFF38405E),
                                    CircleShape,
                                )
                                .clickable {
                                    if (isPlayingNarration || isNarrationLoading) stopNarration() else playNarration()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = if (isPlayingNarration || isNarrationLoading) "Stop Pokédex Narration" else "Play Pokédex Narration",
                                tint = if (isPlayingNarration || isNarrationLoading) Color(0xFFC084FC) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

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
                    if (isCaught) {
                        StatusPill(
                            label = "CAUGHT IN ARCHIVE",
                            tint = Color(0xFF10B981),
                            icon = Icons.Rounded.CatchingPokemon,
                        )
                    } else if (isSeen) {
                        StatusPill(
                            label = "SEEN IN BATTLE",
                            tint = Color(0xFF00E5FF),
                            icon = Icons.Rounded.Visibility,
                        )
                    }

                    if (hasShiny) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(
                            label = "SHINY DISCOVERED",
                            tint = Color(0xFFFFD700),
                            icon = Icons.Rounded.AutoAwesome,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cry Options Strip (Auto-play ON/OFF & Modern / Retro style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Auto-play Toggle Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (autoPlayCries) Color(0xFF132A20) else Color(0xFF181B26))
                            .border(
                                1.dp,
                                if (autoPlayCries) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF2E354B),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val newVal = !autoPlayCries
                                autoPlayCries = newVal
                                prefs.edit().putBoolean("pokemon_cry_autoplay", newVal).apply()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = if (autoPlayCries) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = null,
                                tint = if (autoPlayCries) Color(0xFF34D399) else VantafynColors.Muted,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (autoPlayCries) "Cry: Auto" else "Cry: Muted",
                                color = if (autoPlayCries) Color(0xFFE2E8F0) else VantafynColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Modern / Retro Style Toggle Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (cryStyle == "legacy") Color(0xFF2B1D3D) else Color(0xFF181B26))
                            .border(
                                1.dp,
                                if (cryStyle == "legacy") Color(0xFFA855F7).copy(alpha = 0.6f) else Color(0xFF2E354B),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val nextStyle = if (cryStyle == "latest") "legacy" else "latest"
                                cryStyle = nextStyle
                                prefs.edit().putString("pokemon_cry_style", nextStyle).apply()
                                playCry(nextStyle, false)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = if (cryStyle == "legacy") Color(0xFFC084FC) else Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (cryStyle == "legacy") "Style: Retro" else "Style: Modern",
                                color = if (cryStyle == "legacy") Color(0xFFF3E8FF) else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Physical Attributes Card (Height / Weight / Archetype)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pokemonCardAtmosphereBrush(primaryType.accentColor))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
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
                        imperialValue = dexData.generationName,
                        icon = Icons.Rounded.Public,
                        accentColor = Color(0xFFA855F7),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Encyclopedic Lore Description Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pokemonCardAtmosphereBrush(primaryType.accentColor))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = primaryType.accentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "POKÉDEX LORE ENTRY",
                            color = primaryType.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                    }

                    Text(
                        text = dexData.flavorText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canonical Base Stats Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pokemonCardAtmosphereBrush(primaryType.accentColor))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "BASE STATS",
                            color = primaryType.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            text = "TOTAL: ${dexData.bst}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    StatBarRow(label = "HP", value = dexData.hp, barColor = Color(0xFFFF5252))
                    StatBarRow(label = "Attack", value = dexData.attack, barColor = Color(0xFFFF7A00))
                    StatBarRow(label = "Defense", value = dexData.defense, barColor = Color(0xFFFFD600))
                    StatBarRow(label = "Sp. Atk", value = dexData.spAtk, barColor = Color(0xFF00E5FF))
                    StatBarRow(label = "Sp. Def", value = dexData.spDef, barColor = Color(0xFF00E676))
                    StatBarRow(label = "Speed", value = dexData.speed, barColor = Color(0xFFFF4081))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Biological & Canonical Traits (Abilities, Classification, Archetype, Held Item)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pokemonCardAtmosphereBrush(primaryType.accentColor))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "BIOLOGICAL & CANONICAL TRAITS",
                        color = primaryType.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )

                    // Ability
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Canonical Ability", color = VantafynColors.Muted, fontSize = 12.sp)
                        Text(
                            text = currentDetails?.ability ?: PokemonSpeciesCatalog.getCanonicalAbility(currentSpeciesId),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Classification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Classification", color = VantafynColors.Muted, fontSize = 12.sp)
                        Text(
                            text = dexData.category,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Battle Archetype
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Battle Archetype", color = VantafynColors.Muted, fontSize = 12.sp)
                        Text(
                            text = dexData.archetype,
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Characteristic / Held Item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Signature Item", color = VantafynColors.Muted, fontSize = 12.sp)
                        Text(
                            text = currentDetails?.heldItem?.ifBlank { "None" } ?: "None",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Personal PKVault Save Archive Lineage Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pokemonCardAtmosphereBrush(Color(0xFF00E5FF)))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "SAVE ARCHIVE & LINEAGE",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )

                    // Origin Game
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("First Encounter", color = VantafynColors.Muted, fontSize = 12.sp)
                        Text(
                            text = currentEntry?.firstEncounteredGame ?: "PKVault Save Archive",
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
                                    text = "${(currentEntry?.encounterCount ?: 1).coerceAtLeast(1)} across saves",
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
                                    if (activeOwnedCount > 0) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF1E2336),
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
                                    text = if (activeOwnedCount > 0) "$activeOwnedCount in saves" else "None in saves",
                                    color = if (activeOwnedCount > 0) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Optional Button to Inspect Caught Specimen
                if (currentSummary != null && onInspectSpecimen != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(VantafynGradients.accentHorizontal())
                            .clickable { onInspectSpecimen(currentSummary, currentDetails) }
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
                                text = "Inspect Caught Specimen (Lv. ${currentSummary.level})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                // Unregistered Pokémon View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "???",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )

                    Text(
                        text = "UNREGISTERED SPECIES",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    StatusPill(
                        label = "NOT YET ENCOUNTERED",
                        tint = Color(0xFF64748B),
                        icon = Icons.Rounded.Visibility,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(pokemonCardAtmosphereBrush(Color(0xFF64748B), accentLift = 0.035f))
                            .border(1.dp, cardBorderBrush, RoundedCornerShape(18.dp))
                            .padding(20.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Text(
                                text = "This species hasn't been registered yet",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )

                            Text(
                                text = "Encounter this Pokémon in battle or catch it in your game cartridge saves or PKVault cloud storage to register its Pokédex entry and unlock its canonical lore, base stats, and audio cry.",
                                color = VantafynColors.Muted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "Native to ${dexData.regionName} (${dexData.generationName})",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
    }
}

@Composable
private fun MegaEvolutionQuickToggle(
    symbolUrl: String?,
    isActive: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shellBrush = Brush.linearGradient(
        colors = if (isActive) {
            listOf(Color(0xFFFF4FD8), Color(0xFF7C3AED), Color(0xFF22D3EE))
        } else {
            listOf(Color(0xFF26213D), Color(0xFF181B2C), Color(0xFF123247))
        },
    )
    val text = if (isActive) label else "Mega"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(shellBrush)
            .border(
                1.dp,
                if (isActive) Color.White.copy(alpha = 0.46f) else Color(0xFFFF4FD8).copy(alpha = 0.55f),
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 9.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFF7AD),
                modifier = Modifier.size(14.dp),
            )
            if (!symbolUrl.isNullOrBlank()) {
                AsyncImage(
                    model = symbolUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
