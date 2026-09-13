package dev.vantafyn.feature.home.mcu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.vantafyn.feature.home.gift.VantafynGiftFranchises
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.util.UUID

private val VantafynModalContainerColor: Color
    get() = VantafynColors.Graphite.copy(alpha = 0.96f)

@Composable
fun EnterCodeDialog(
    onDismiss: () -> Unit,
    onSubmitCode: (String) -> Unit,
    onOpenMcuGuide: () -> Unit,
    onOpenSawGuide: () -> Unit = {},
    onOpenResidentEvilGuide: () -> Unit = {},
    onOpenHarryPotterGuide: () -> Unit = {},
    onOpenHungerGamesGuide: () -> Unit = {},
    onOpenScreamGuide: () -> Unit = {},
    onOpenMatrixGuide: () -> Unit = {},
    onOpenJumanjiGuide: () -> Unit = {},
    onOpenJurassicGuide: () -> Unit = {},
    onOpenPiratesGuide: () -> Unit = {},
    onOpenPokemonGuide: () -> Unit = {},
    onOpenScaryMovieGuide: () -> Unit = {},
    onOpenTwilightGuide: () -> Unit = {},
    onOpenUnderworldGuide: () -> Unit = {},
    onOpenXMenGuide: () -> Unit = {},
    onOpenMiddleEarthGuide: () -> Unit = {},
    isMcuUnlocked: Boolean = false,
    isSawUnlocked: Boolean = false,
    isResidentEvilUnlocked: Boolean = false,
    isHarryPotterUnlocked: Boolean = false,
    isHungerGamesUnlocked: Boolean = false,
    isScreamUnlocked: Boolean = false,
    isMatrixUnlocked: Boolean = false,
    isJumanjiUnlocked: Boolean = false,
    isJurassicUnlocked: Boolean = false,
    isPiratesUnlocked: Boolean = false,
    isPokemonUnlocked: Boolean = false,
    isScaryMovieUnlocked: Boolean = false,
    isTwilightUnlocked: Boolean = false,
    isUnderworldUnlocked: Boolean = false,
    isXMenUnlocked: Boolean = false,
    isMiddleEarthUnlocked: Boolean = false,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    var codeInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(max = this@BoxWithConstraints.maxHeight * 0.9f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynModalContainerColor)
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
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
                                .background(VantafynColors.Primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                tint = VantafynColors.Primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column {
                            Text(
                                "Enter Code",
                                color = VantafynColors.Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                            )
                            Text(
                                "Unlock special guides & features",
                                color = VantafynColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("✕", color = VantafynColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Text field
                VantafynTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = "Unlock Code (e.g. MCU)",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                )

                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF8A8A),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }

                // Submit button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    VantafynButton(
                        text = if (isSubmitting) "Unlocking..." else "Unlock",
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSubmitCode(codeInput)
                        },
                        enabled = codeInput.isNotBlank() && !isSubmitting,
                    )
                }

                // Unlocked Collections section
                if (isMcuUnlocked || isSawUnlocked || isResidentEvilUnlocked || isHarryPotterUnlocked ||
                    isHungerGamesUnlocked || isScreamUnlocked || isMatrixUnlocked || isJumanjiUnlocked ||
                    isJurassicUnlocked || isPiratesUnlocked || isPokemonUnlocked || isScaryMovieUnlocked ||
                    isTwilightUnlocked || isUnderworldUnlocked || isXMenUnlocked || isMiddleEarthUnlocked) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                    val franchiseMap = remember { VantafynGiftFranchises.all.associateBy { it.code } }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Unlocked Collections",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isMcuUnlocked) {
                            val f = franchiseMap["MCU"]
                            UnlockedCollectionCard(
                                title = "MCU: Road to Doomsday",
                                subtitle = "Official Timeline Watch Guide",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: VantafynColors.Primary,
                                onClick = {
                                    onDismiss()
                                    onOpenMcuGuide()
                                },
                            )
                        }
                        if (isSawUnlocked) {
                            val f = franchiseMap["SAW"]
                            UnlockedCollectionCard(
                                title = "SAW: The Complete Guide",
                                subtitle = "10-film franchise watch guide",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFF8B0000),
                                onClick = {
                                    onDismiss()
                                    onOpenSawGuide()
                                },
                            )
                        }
                        if (isResidentEvilUnlocked) {
                            val f = franchiseMap["R-EVIL"]
                            UnlockedCollectionCard(
                                title = "Resident Evil: Alice Saga",
                                subtitle = "All 6 Milla Jovovich films",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFB01C2E),
                                onClick = {
                                    onDismiss()
                                    onOpenResidentEvilGuide()
                                },
                            )
                        }
                        if (isHarryPotterUnlocked) {
                            val f = franchiseMap["POTTER"]
                            UnlockedCollectionCard(
                                title = "Harry Potter: Complete Guide",
                                subtitle = "11-film Hogwarts & Fantastic Beasts guide",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFD4AF37),
                                onClick = {
                                    onDismiss()
                                    onOpenHarryPotterGuide()
                                },
                            )
                        }
                        if (isHungerGamesUnlocked) {
                            val f = franchiseMap["HUNGER"]
                            UnlockedCollectionCard(
                                title = "The Hunger Games: Complete Saga",
                                subtitle = "All 5 films — prequel & Panem rebellion",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFE67E22),
                                onClick = {
                                    onDismiss()
                                    onOpenHungerGamesGuide()
                                },
                            )
                        }
                        if (isScreamUnlocked) {
                            val f = franchiseMap["SCREAM"]
                            UnlockedCollectionCard(
                                title = "Scream: Complete Slasher Guide",
                                subtitle = "All 7 films — Ghostface through the decades",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFC0392B),
                                onClick = {
                                    onDismiss()
                                    onOpenScreamGuide()
                                },
                            )
                        }
                        if (isMatrixUnlocked) {
                            val f = franchiseMap["MATRIX"]
                            UnlockedCollectionCard(
                                title = "The Matrix: Digital Reality Guide",
                                subtitle = "All 4 films — Zion to Machine City",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFF00AA33),
                                onClick = {
                                    onDismiss()
                                    onOpenMatrixGuide()
                                },
                            )
                        }
                        if (isJumanjiUnlocked) {
                            val f = franchiseMap["JUMANJI"]
                            UnlockedCollectionCard(
                                title = "Jumanji: Complete Adventure Guide",
                                subtitle = "All 3 films — board & video games",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFF27AE60),
                                onClick = {
                                    onDismiss()
                                    onOpenJumanjiGuide()
                                },
                            )
                        }
                        if (isJurassicUnlocked) {
                            val f = franchiseMap["JURASSIC"]
                            UnlockedCollectionCard(
                                title = "Jurassic Park & World: Reborn",
                                subtitle = "All 7 films — Isla Nublar to Rebirth",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFE74C3C),
                                onClick = {
                                    onDismiss()
                                    onOpenJurassicGuide()
                                },
                            )
                        }
                        if (isPiratesUnlocked) {
                            val f = franchiseMap["PIRATES"]
                            UnlockedCollectionCard(
                                title = "Pirates of the Caribbean",
                                subtitle = "All 5 films — Jack Sparrow's voyages",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFD4AF37),
                                onClick = {
                                    onDismiss()
                                    onOpenPiratesGuide()
                                },
                            )
                        }
                        if (isPokemonUnlocked) {
                            val f = franchiseMap["POKEMON"]
                            UnlockedCollectionCard(
                                title = "Pokémon: The Complete Collection",
                                subtitle = "All 24 films — Kanto to Detective Pikachu",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFE3350D),
                                onClick = {
                                    onDismiss()
                                    onOpenPokemonGuide()
                                },
                            )
                        }
                        if (isScaryMovieUnlocked) {
                            val f = franchiseMap["SCARY"]
                            UnlockedCollectionCard(
                                title = "Scary Movie: Complete Spoof Guide",
                                subtitle = "All 6 films — spoofing every horror classic",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFF00E676),
                                onClick = {
                                    onDismiss()
                                    onOpenScaryMovieGuide()
                                },
                            )
                        }
                        if (isTwilightUnlocked) {
                            val f = franchiseMap["TWILIGHT"]
                            UnlockedCollectionCard(
                                title = "The Twilight Saga: Complete Romance",
                                subtitle = "All 5 films — Bella, Edward & Jacob",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFF9C27B0),
                                onClick = {
                                    onDismiss()
                                    onOpenTwilightGuide()
                                },
                            )
                        }
                        if (isUnderworldUnlocked) {
                            val f = franchiseMap["UNDERWORLD"]
                            UnlockedCollectionCard(
                                title = "Underworld: Blood War Chronicles",
                                subtitle = "All 5 films — Vampires vs Lycans",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFF1E88E5),
                                onClick = {
                                    onDismiss()
                                    onOpenUnderworldGuide()
                                },
                            )
                        }
                        if (isXMenUnlocked) {
                            val f = franchiseMap["X-MEN"]
                            UnlockedCollectionCard(
                                title = "X-Men: The Complete Mutant Saga",
                                subtitle = "All 13 films — First Class to Logan",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFF59E0B),
                                onClick = {
                                    onDismiss()
                                    onOpenXMenGuide()
                                },
                            )
                        }
                        if (isMiddleEarthUnlocked) {
                            val f = franchiseMap["MORDOR"]
                            UnlockedCollectionCard(
                                title = "Middle-earth: The Complete Legendarium",
                                subtitle = "All 7 films — Rohirrim, Hobbit & LOTR",
                                posterUrl = f?.posterUrl.orEmpty(),
                                accentColor = f?.accentColor ?: Color(0xFFD4AF37),
                                onClick = {
                                    onDismiss()
                                    onOpenMiddleEarthGuide()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlockedCollectionCard(
    title: String,
    subtitle: String,
    posterUrl: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(VantafynColors.SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = VantafynColors.Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Open →",
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun McuWatchGuideDialog(
    movies: List<McuWatchItemUi>,
    isLoading: Boolean,
    errorMessage: String?,
    sortMode: McuSortMode,
    filterMode: McuFilterMode,
    onToggleSort: (McuSortMode) -> Unit,
    onSelectFilter: (McuFilterMode) -> Unit,
    onOpenMovie: (UUID) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Compute stats
    val totalMovies = movies.size
    val watchedCount = movies.count { it.isPlayed }
    val onServerCount = movies.count { it.isOnServer }
    val notOnServerCount = movies.count { !it.isOnServer }
    val progressFraction = if (totalMovies > 0) watchedCount.toFloat() / totalMovies.toFloat() else 0f
    val progressPercent = (progressFraction * 100).toInt()

    // Filter and sort items
    val sortedMovies = remember(movies, sortMode) {
        when (sortMode) {
            McuSortMode.Timeline -> movies.sortedBy { it.movie.timelineOrder }
            McuSortMode.Release -> movies.sortedBy { it.movie.releaseOrder }
        }
    }

    val displayMovies = remember(sortedMovies, filterMode) {
        when (filterMode) {
            McuFilterMode.All -> sortedMovies
            McuFilterMode.Watched -> sortedMovies.filter { it.isPlayed }
            McuFilterMode.Unwatched -> sortedMovies.filter { !it.isPlayed }
            McuFilterMode.Missing -> sortedMovies.filter { !it.isOnServer }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxHeightPx = maxHeight
            val headerArtworkUrl = remember(movies) {
                val serverAvengers = movies.firstOrNull {
                    it.isOnServer && it.movie.id.contains("avengers") && !it.serverBackdropUrl.isNullOrBlank()
                }?.serverBackdropUrl
                val anyServer = movies.firstOrNull {
                    it.isOnServer && !it.serverBackdropUrl.isNullOrBlank()
                }?.serverBackdropUrl
                serverAvengers ?: anyServer ?: "https://image.tmdb.org/t/p/w780/7RyHsO4yDXtBv1zUU3mTpHeQ0d5.jpg"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .heightIn(max = maxHeightPx * 0.94f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynModalContainerColor)
                    .vantafynAnimatedModalBorder(cornerRadius = 28.dp),
            ) {
                // Header Artwork Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                ) {
                    AsyncImage(
                        model = headerArtworkUrl,
                        contentDescription = "Marvel Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Dark Gradient Overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.50f),
                                        Color.Black.copy(alpha = 0.15f),
                                        VantafynModalContainerColor.copy(alpha = 0.85f),
                                        VantafynModalContainerColor,
                                    ),
                                ),
                            ),
                    )

                    // Top Action Bar: Marvel Studios Badge + Refresh & Close Icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Horizontal Marvel Studios badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE23636))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "MARVEL",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(10.dp)
                                    .background(Color.White.copy(alpha = 0.6f)),
                            )
                            Text(
                                "STUDIOS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.3.sp,
                            )
                        }

                        // Refresh and Close circular buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable(onClick = onRefresh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Title & Subtitle at bottom of header banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "MCU: Road to Doomsday",
                            color = VantafynColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.titleLarge.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.9f),
                                    blurRadius = 10f,
                                ),
                            ),
                        )
                        Text(
                            "All 62 films — Sacred Timeline & Canon Multiverse",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Inner content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Progress Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    "Watched $watchedCount of $totalMovies films",
                                    color = VantafynColors.Ink,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                            }
                            Text(
                                "$progressPercent%",
                                color = Color(0xFF4ADE80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4ADE80),
                            trackColor = Color.White.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Round,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Available on server: $onServerCount",
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                            )
                            if (notOnServerCount > 0) {
                                Text(
                                    "Not on server: $notOnServerCount",
                                    color = Color(0xFFFFB86C),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                // Controls Row: Sort & Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Sort toggle
                    val isTimeline = sortMode == McuSortMode.Timeline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VantafynColors.Primary.copy(alpha = 0.18f))
                            .border(1.dp, VantafynColors.Primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .clickable {
                                onToggleSort(if (isTimeline) McuSortMode.Release else McuSortMode.Timeline)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (isTimeline) "📅 Timeline Order" else "🎬 Release Order",
                            color = VantafynColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }

                    // Filter chips
                    McuFilterMode.entries.forEach { mode ->
                        val isSelected = filterMode == mode
                        val label = when (mode) {
                            McuFilterMode.All -> "All ($totalMovies)"
                            McuFilterMode.Watched -> "Watched ($watchedCount)"
                            McuFilterMode.Unwatched -> "Unwatched (${totalMovies - watchedCount})"
                            McuFilterMode.Missing -> "Missing ($notOnServerCount)"
                        }
                        val bgColor = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
                        val borderColor = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
                        val textColor = if (isSelected) VantafynColors.Ink else VantafynColors.Muted

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable { onSelectFilter(mode) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                // Error message banner
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4A1818))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFFB4B4),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Content List or Loading
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                ) {
                    when {
                        isLoading && movies.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                VantafynLoadingIndicator("Syncing with your Jellyfin library...")
                            }
                        }

                        displayMovies.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No movies match this filter.",
                                    color = VantafynColors.Muted,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp),
                            ) {
                                itemsIndexed(
                                    items = displayMovies,
                                    key = { _, item -> item.movie.id },
                                ) { index, item ->
                                    McuMovieCard(
                                        index = if (sortMode == McuSortMode.Timeline) item.movie.timelineOrder else item.movie.releaseOrder,
                                        item = item,
                                        onOpen = {
                                            if (item.isOnServer && item.mediaItemId != null) {
                                                onDismiss()
                                                onOpenMovie(item.mediaItemId)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun McuMovieCard(
    index: Int,
    item: McuWatchItemUi,
    onOpen: () -> Unit,
) {
    val isClickable = item.isOnServer && item.mediaItemId != null
    val borderColor = when {
        item.isPlayed -> Color(0xFF4ADE80).copy(alpha = 0.28f)
        item.isOnServer -> VantafynColors.Primary.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val backgroundColor = when {
        item.isPlayed -> Color(0xFF4ADE80).copy(alpha = 0.04f)
        item.isOnServer -> Color.White.copy(alpha = 0.04f)
        else -> Color.Black.copy(alpha = 0.25f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = isClickable, onClick = onOpen)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Order badge
        Box(
            modifier = Modifier
                .width(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = String.format("%02d", index),
                color = VantafynColors.Muted.copy(alpha = 0.75f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }

        // Poster
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.displayPosterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.displayPosterUrl,
                    contentDescription = item.movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = VantafynColors.Muted,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Movie info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Phase badge & timeline tag
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (badgeBg, badgeColor) = when (item.movie.phase) {
                    "Fantastic Four" -> Color(0xFF0284C7).copy(alpha = 0.25f) to Color(0xFF38BDF8)
                    "X-Men Universe" -> Color(0xFFD97706).copy(alpha = 0.25f) to Color(0xFFFBBF24)
                    "Spider-Man Legacy" -> Color(0xFFDC2626).copy(alpha = 0.25f) to Color(0xFFF87171)
                    "Sony Universe" -> Color(0xFF7C3AED).copy(alpha = 0.25f) to Color(0xFFA78BFA)
                    else -> Color.White.copy(alpha = 0.08f) to VantafynColors.Muted
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = item.movie.phase,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = item.movie.timelineSetting,
                    color = VantafynColors.Muted.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Title
            Text(
                text = item.movie.title,
                color = VantafynColors.Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Year
            Text(
                text = item.movie.releaseYear.toString(),
                color = VantafynColors.Muted,
                fontSize = 11.sp,
            )
        }

        // Status badge
        Box(
            modifier = Modifier.padding(start = 4.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            when {
                item.isPlayed -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF4ADE80).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Watched",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }

                item.isOnServer -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(VantafynColors.Primary.copy(alpha = 0.15f))
                            .border(1.dp, VantafynColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = VantafynColors.Primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "On Server",
                            color = VantafynColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE28743).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFE28743).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Not on server",
                            color = Color(0xFFE28743),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
