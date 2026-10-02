package dev.vantafyn.feature.home.games.pokemon

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonBoxDto
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonPokedexEntryDto
import dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.media.games.GameHubSoundManager
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.games.GameScreenReveal

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

    // Duck ambient background music so audio cries are heard with clarity
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

    val dexData = remember(currentSpeciesId) {
        PokemonPokedexCatalog.getPokedexData(currentSpeciesId)
    }
    val (primaryType, secondaryType) = remember(currentSpeciesId, dexData.name) {
        PokemonTypeCatalog.getTypes(currentSpeciesId, dexData.name)
    }

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

    val playCry: (String) -> Unit = { style ->
        try {
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
                }
                setOnErrorListener { mp, _, _ ->
                    isPlayingCry = false
                    try { mp.release() } catch (_: Throwable) {}
                    if (activePlayer === mp) activePlayer = null
                    true
                }
                prepareAsync()
            }
            activePlayer = player
        } catch (_: Throwable) {
            isPlayingCry = false
        }
    }

    DisposableEffect(currentSpeciesId) {
        if (autoPlayCries && isRegistered) {
            playCry(cryStyle)
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
        }
    }

    val initialFemale = remember(currentSpeciesId, currentSummary) {
        currentSummary?.gender?.equals("Female", ignoreCase = true) == true ||
            currentSummary?.gender?.equals("Girl", ignoreCase = true) == true ||
            currentSummary?.gender?.equals("F", ignoreCase = true) == true
    }
    var showShinyArtwork by remember(currentSpeciesId, hasShiny) { mutableStateOf(hasShiny) }
    var showFemaleArtwork by remember(currentSpeciesId) { mutableStateOf(initialFemale) }

    var selectedFormKey by remember(currentSpeciesId) { mutableStateOf<String?>(null) }

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
                            Brush.radialGradient(
                                colors = listOf(
                                    primaryType.accentColor.copy(alpha = 0.35f),
                                    (secondaryType?.accentColor ?: primaryType.secondaryAccent).copy(alpha = 0.15f),
                                    Color(0xFF121420),
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
            val alternateForms = remember(currentSpeciesId) {
                PokemonFormsCatalog.getForms(currentSpeciesId)
            }
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
                            text = "FORMS & VARIATIONS",
                            color = VantafynColors.Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        val activeForm = alternateForms.firstOrNull { it.spriteKey == selectedFormKey }
                            ?: alternateForms.firstOrNull()
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

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) primaryType.accentColor.copy(alpha = 0.25f)
                                        else Color(0xFF141726)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) primaryType.accentColor else Color(0xFF282F48),
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
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
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
                                .clickable { playCry(cryStyle) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Replay Pokémon Cry",
                                tint = if (isPlayingCry) primaryType.accentColor else Color(0xFFCBD5E1),
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
                                playCry(nextStyle)
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
                        .background(Color(0xFF141724))
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
                        .background(Color(0xFF141724))
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
                        .background(Color(0xFF141724))
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
                        .background(Color(0xFF141724))
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
                        .background(Color(0xFF141724))
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
                            .background(Color(0xFF141724))
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
