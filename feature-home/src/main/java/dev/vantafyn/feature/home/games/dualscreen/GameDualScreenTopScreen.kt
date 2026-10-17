package dev.vantafyn.feature.home.games.dualscreen

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.games.grid.ConsoleStatusTray
import dev.vantafyn.feature.home.games.grid.GridTileType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Date

/**
 * State model representing content currently displayed on the Nintendo 3DS-style Top Screen.
 */
data class DualScreenTopContent(
    val title: String = "Vantafyn Games Hub",
    val subtitle: String = "Ready to Play",
    val landscapeArtUrl: String? = null,
    val logoUrl: String? = null,
    val boxartUrl: String? = null,
    val isGame: Boolean = false,
    val systemDisplayName: String? = null,
    val tileType: GridTileType? = null,
    val game: GameSummary? = null,
)

/**
 * Nintendo 3DS-style Top Screen for external displays in Games-Only mode.
 *
 * Renders full-screen dark nebula backdrop with subtle scrim, dynamic cross-fading
 * in-game landscape snap art, slow Ken Burns motion, and floating 3D Clear Logo
 * for selected titles, with themed consoles for system and tool channels.
 */
@Composable
fun GameDualScreenTopScreen(
    state: DualScreenTopContent,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Subtle Ken Burns motion for landscape game art
    val infiniteTransition = rememberInfiniteTransition(label = "topScreenMotion")
    val kenScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "topScreenKenScale",
    )
    val kenPan by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "topScreenKenPan",
    )
    val logoFloatY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoFloatY",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 1. Dark Nebula Fullscreen Base (Matching Bottom Screen)
        Image(
            painter = painterResource(id = dev.vantafyn.core.ui.R.drawable.vantafyn_onboarding_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Cosmic Nebula Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.70f),
                        )
                    )
                ),
        )

        // 3. Dynamic Content (Game Landscape Art vs Themed App Screen)
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(650, easing = LinearEasing)) togetherWith
                    fadeOut(animationSpec = tween(650, easing = LinearEasing))
            },
            modifier = Modifier.fillMaxSize(),
            label = "topScreenContentTransition",
        ) { currentContent ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentContent.isGame) {
                    var landscapeError by remember(currentContent.landscapeArtUrl) { mutableStateOf(false) }
                    val effectiveLandscape = if (!landscapeError && !currentContent.landscapeArtUrl.isNullOrBlank()) {
                        currentContent.landscapeArtUrl
                    } else null

                    val fallbackArt = currentContent.boxartUrl ?: currentContent.game?.boxartUrl ?: currentContent.landscapeArtUrl

                    // 1. In-game landscape snap or title screen with Ken Burns Motion
                    if (effectiveLandscape != null) {
                        AsyncImage(
                            model = effectiveLandscape,
                            contentDescription = currentContent.title,
                            contentScale = ContentScale.Crop,
                            onError = { landscapeError = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = kenScale
                                    scaleY = kenScale
                                    translationX = kenPan
                                },
                        )
                    } else if (!fallbackArt.isNullOrBlank()) {
                        // Ambient blurred backdrop of boxart
                        AsyncImage(
                            model = fallbackArt,
                            contentDescription = currentContent.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(24.dp)
                                .graphicsLayer { alpha = 0.55f },
                        )
                    }

                    // Cinematic Radial Vignette
                    val radialVignette = remember {
                        object : ShaderBrush() {
                            override fun createShader(size: Size): Shader {
                                val maxDim = maxOf(size.height, size.width)
                                return RadialGradientShader(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                                    center = size.center,
                                    radius = maxDim / 1.35f,
                                    colorStops = listOf(0f, 0.95f),
                                )
                            }
                        }
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = radialVignette, blendMode = BlendMode.Multiply)
                    }

                    // Bottom Scrim for Title / Logo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Black.copy(alpha = 0.95f),
                                    )
                                )
                            ),
                    )

                    // Floating 3D Logo / Game Title in Lower Section
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        var logoLoadError by remember(currentContent.logoUrl) { mutableStateOf(false) }
                        if (!logoLoadError && !currentContent.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentContent.logoUrl,
                                contentDescription = currentContent.title,
                                contentScale = ContentScale.Fit,
                                onError = { logoLoadError = true },
                                modifier = Modifier
                                    .size(width = 340.dp, height = 140.dp)
                                    .graphicsLayer {
                                        translationY = logoFloatY
                                    },
                            )
                        } else {
                            // 3DS-style floating showcase: 3D boxart card + bold typography
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.graphicsLayer { translationY = logoFloatY },
                            ) {
                                val boxart = currentContent.boxartUrl ?: currentContent.game?.boxartUrl ?: currentContent.landscapeArtUrl
                                if (!boxart.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 96.dp, height = 128.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                                            .shadow(18.dp, RoundedCornerShape(10.dp), spotColor = Color(0xFF00E5FF)),
                                    ) {
                                        AsyncImage(
                                            model = boxart,
                                            contentDescription = currentContent.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = if (!boxart.isNullOrBlank()) Alignment.Start else Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = currentContent.title,
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = if (!boxart.isNullOrBlank()) TextAlign.Start else TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    if (!currentContent.systemDisplayName.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0xFF00E5FF).copy(alpha = 0.22f))
                                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(999.dp))
                                                .padding(horizontal = 14.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = currentContent.systemDisplayName.uppercase(),
                                                color = Color(0xFF8FE7FF),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // App / Tool Themed Screen
                    TopScreenThemedAppBackdrop(content = currentContent, logoFloatY = logoFloatY)
                }
            }
        }

        // 4. Top Bar Scrim & Status Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.85f),
                            0.5f to Color.Black.copy(alpha = 0.40f),
                            1.0f to Color.Transparent,
                        )
                    )
                ),
        )

        // 5. Console Status & 3DS Top Screen Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: 3DS Dual Screen Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                    )
                    Text(
                        text = "3DS TOP SCREEN",
                        color = Color(0xFF8FE7FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            // Right: Console Hardware Tray
            ConsoleStatusTray()
        }
    }
}

@Composable
private fun TopScreenThemedAppBackdrop(
    content: DualScreenTopContent,
    logoFloatY: Float,
) {
    val (primaryColor, accentColor, iconVector) = when (content.tileType) {
        GridTileType.POKEMON_VAULT -> Triple(Color(0xFFFF2A85), Color(0xFF9D00FF), Icons.Rounded.CatchingPokemon)
        GridTileType.CLOUD_SAVES -> Triple(Color(0xFF00E676), Color(0xFF00B0FF), Icons.Rounded.CloudDone)
        GridTileType.SETTINGS -> Triple(Color(0xFFA78BFA), Color(0xFF00E5FF), Icons.Rounded.Settings)
        GridTileType.LIBRARY -> Triple(Color(0xFF00E5FF), Color(0xFF9D00FF), Icons.Rounded.SportsEsports)
        GridTileType.SEARCH -> Triple(Color(0xFF00E5FF), Color(0xFF00B0FF), Icons.Rounded.Search)
        GridTileType.MAIN_HOME -> Triple(Color(0xFFFF2A85), Color(0xFF9D00FF), Icons.Rounded.Home)
        GridTileType.SYSTEM -> Triple(Color(0xFF9D00FF), Color(0xFF00E5FF), Icons.Rounded.Dns)
        GridTileType.PROFILE_STATS -> Triple(Color(0xFF00E5FF), Color(0xFFBD00FF), Icons.Rounded.Person)
        else -> Triple(Color(0xFF00E5FF), Color(0xFF9D00FF), Icons.Rounded.SportsEsports)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient Radial Glow
        Box(
            modifier = Modifier
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.28f),
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.graphicsLayer { translationY = logoFloatY },
        ) {
            if (content.tileType == GridTileType.SYSTEM && !content.landscapeArtUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .height(96.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = content.landscapeArtUrl,
                        contentDescription = content.title,
                        modifier = Modifier.fillMaxHeight(),
                        contentScale = ContentScale.Fit,
                    )
                }
            } else {
                // Glowing Halo Hero Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(2.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }

            Text(
                text = content.title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )

            Text(
                text = content.subtitle,
                color = VantafynColors.Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
