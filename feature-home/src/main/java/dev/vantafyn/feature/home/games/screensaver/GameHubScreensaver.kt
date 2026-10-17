package dev.vantafyn.feature.home.games.screensaver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import dev.vantafyn.core.media.games.GameHubSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Date

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Ambient full-screen console screensaver for Games-Only mode.
 *
 * Displays a cinematic landscape in-game screenshot slideshow from the user's game library
 * (showing 2 pictures per game: in-game gameplay snap and title screen), with slow Ken Burns pan/zoom motion,
 * 30% screen dimming, background music fade-out, top gradient scrim, console clock, top-left battery status,
 * and bottom-left clear 3D game logo.
 * Wakes up instantly on any controller button press or touch screen tap, restoring brightness and ambient audio.
 */
@Composable
fun GameHubScreensaver(
    games: List<GameSummary>,
    systems: List<GameSystem>,
    slideDurationMs: Long = 8_000L,
    showClock: Boolean = true,
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // 1. Audio lifecycle: Smoothly fade out ambient music when screensaver enters, fade back in on wake
    DisposableEffect(Unit) {
        GameHubSoundManager.fadeOut(durationMs = 800L)
        onDispose {
            GameHubSoundManager.fadeIn(context, durationMs = 800L)
        }
    }

    // 2. Display brightness lifecycle: Safely dim display by 30% during screensaver, restore on wake
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose {}

        val window = activity.window
        val originalAttributes = window.attributes
        val originalBrightness = originalAttributes.screenBrightness

        try {
            val baseBrightness = if (originalBrightness < 0f) {
                try {
                    Settings.System.getInt(
                        activity.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        128,
                    ) / 255f
                } catch (_: Exception) {
                    0.5f
                }
            } else {
                originalBrightness
            }

            // Dim hardware backlight by 30% (70% of base), bounded safely
            val dimmedBrightness = (baseBrightness * 0.70f).coerceIn(0.05f, 1.0f)
            val lp = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                screenBrightness = dimmedBrightness
            }
            window.attributes = lp
        } catch (e: Exception) {
            Log.w("GameHubScreensaver", "Unable to dim window brightness: ${e.message}")
        }

        onDispose {
            try {
                val lp = WindowManager.LayoutParams().apply {
                    copyFrom(window.attributes)
                    screenBrightness = originalBrightness
                }
                window.attributes = lp
            } catch (_: Exception) {}
        }
    }

    // Prepare prioritized slides (favoring player's active titles & series)
    val slides = remember(games, systems) {
        GameScreensaverResolver.buildSlides(context, games, systems)
    }

    var currentSlideIndex by remember { mutableIntStateOf(0) }
    var currentPicIndex by remember { mutableIntStateOf(0) }
    var currentClockText by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isBatteryCharging by remember { mutableStateOf(false) }

    // Battery status monitor
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100 / scale).coerceIn(0, 100)
                    }
                    isBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        stickyIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100 / scale).coerceIn(0, 100)
            }
            isBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Automatic clock update every 5 seconds
    LaunchedEffect(Unit) {
        while (isActive) {
            val format = DateFormat.getTimeFormat(context)
            currentClockText = format.format(Date())
            delay(5_000L)
        }
    }

    // Slide rotation timer: Shows 2 pictures per game, then advances to next game
    LaunchedEffect(slides.size, slideDurationMs) {
        if (slides.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(slideDurationMs)
            val currentSlide = slides.getOrNull(currentSlideIndex)
            val picCount = currentSlide?.pictureUrls?.size ?: 1
            if (currentPicIndex < picCount - 1) {
                currentPicIndex += 1
            } else {
                currentPicIndex = 0
                currentSlideIndex = (currentSlideIndex + 1) % slides.size
            }
        }
    }

    // Grab focus immediately so any gamepad key wakes from screensaver
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Ken Burns slow zoom and subtle pan
    val infiniteTransition = rememberInfiniteTransition(label = "screensaverMotion")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = slideDurationMs.coerceAtLeast(3000L).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kenBurnsScale",
    )
    val panOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (slideDurationMs * 1.3).toInt().coerceAtLeast(4000), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kenBurnsPan",
    )

    val currentSlide = slides.getOrNull(currentSlideIndex)
    val currentPictureUrl = currentSlide?.pictureUrls?.getOrNull(currentPicIndex)
        ?: currentSlide?.pictureUrls?.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                // Wake up on any controller key
                if (it.type == KeyEventType.KeyDown) {
                    onWake()
                    true
                } else false
            }
            .pointerInput(Unit) {
                // Wake up on any screen touch
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onWake()
                }
            },
    ) {
        if (currentSlide != null && !currentPictureUrl.isNullOrBlank()) {
            // Cinematic Background Slide with Cross-Fade Transition between pictures
            AnimatedContent(
                targetState = currentPictureUrl,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1100, easing = LinearEasing)) togetherWith
                        fadeOut(animationSpec = tween(1100, easing = LinearEasing))
                },
                modifier = Modifier.fillMaxSize(),
                label = "screensaverSlideAnimation",
            ) { picUrl ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = picUrl,
                        contentDescription = currentSlide.game.cleanTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = panOffset
                            },
                        onError = {
                            // If this specific picture fails to load, advance immediately
                            if (currentSlide.pictureUrls.size > 1 && currentPicIndex == 0) {
                                currentPicIndex = 1
                            } else if (slides.size > 1) {
                                currentPicIndex = 0
                                currentSlideIndex = (currentSlideIndex + 1) % slides.size
                            }
                        },
                    )
                }
            }
        } else {
            // Ambient Fallback when no games or snaps are available yet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF14142B), Color.Black),
                            center = Offset.Unspecified,
                            radius = 900f,
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Text(
                        text = "Vantafyn Games Hub",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = "Press any button to return",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Cinematic Vignette & Radial Edge Darkening (Wholphin style)
        val radialVignette = remember {
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val maxDim = maxOf(size.height, size.width)
                    return RadialGradientShader(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        center = size.center,
                        radius = maxDim / 1.35f,
                        colorStops = listOf(0f, 0.95f),
                    )
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = radialVignette,
                blendMode = BlendMode.Multiply,
            )
        }

        // 30% Screen Dimming Scrim Layer (smooth animated tint)
        val dimOverlayAlpha by animateFloatAsState(
            targetValue = 0.30f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing),
            label = "screensaverDimLayer",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimOverlayAlpha))
        )

        // Top Scrim: Fades from top down so battery and clock are crisp & readable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.82f),
                            0.45f to Color.Black.copy(alpha = 0.45f),
                            1.0f to Color.Transparent,
                        )
                    )
                ),
        )

        // Top-Left Battery Indicator (Symbol + %)
        ScreensaverBatteryHeader(
            batteryLevel = batteryLevel,
            isCharging = isBatteryCharging,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 18.dp, start = 24.dp),
        )

        // Top-Right Clock Display
        if (showClock && currentClockText.isNotBlank()) {
            Text(
                text = currentClockText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 24.dp),
            )
        }

        // Bottom-Left Scrim & Game 3D Logo / Clean Title
        if (currentSlide != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.5f to Color.Black.copy(alpha = 0.55f),
                                1.0f to Color.Black.copy(alpha = 0.9f),
                            )
                        )
                    ),
            )

            // Logo animated state attached to the game slide (stable across picture 1 -> 2 transition)
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    fadeIn(animationSpec = tween(700, easing = LinearEasing)) togetherWith
                        fadeOut(animationSpec = tween(500, easing = LinearEasing))
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 28.dp, bottom = 28.dp),
                label = "screensaverLogoAnimation",
            ) { slide ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var logoFailed by remember(slide.game.id) { mutableStateOf(false) }
                    val logoUrl = slide.logoUrl

                    if (!logoFailed && !logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = slide.game.cleanTitle,
                            contentScale = ContentScale.Fit,
                            onError = { logoFailed = true },
                            modifier = Modifier
                                .size(width = 240.dp, height = 110.dp)
                                .graphicsLayer {
                                    shadowElevation = 10f
                                },
                        )
                    } else {
                        Text(
                            text = slide.game.cleanTitle,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = "Press any button to return",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/**
 * Premium console-style battery indicator for the screensaver.
 */
@Composable
private fun ScreensaverBatteryHeader(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Battery Icon Canvas
        Canvas(modifier = Modifier.size(width = 28.dp, height = 14.dp)) {
            val bodyWidth = size.width - 3.5.dp.toPx()
            val bodyHeight = size.height
            val strokeWidth = 1.5.dp.toPx()
            val cornerRadius = 3.dp.toPx()

            // Outer battery pill border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth),
            )

            // Positive terminal nub
            val nubWidth = 2.5.dp.toPx()
            val nubHeight = bodyHeight * 0.42f
            val nubTop = (bodyHeight - nubHeight) / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(bodyWidth + 1.dp.toPx(), nubTop),
                size = Size(nubWidth, nubHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
            )

            // Inner fill bar
            val innerPadding = strokeWidth + 1.5.dp.toPx()
            val maxFillWidth = (bodyWidth - innerPadding * 2f).coerceAtLeast(0f)
            val fillHeight = (bodyHeight - innerPadding * 2f).coerceAtLeast(0f)
            val fillWidth = (maxFillWidth * (batteryLevel / 100f)).coerceIn(0f, maxFillWidth)

            val fillColor = when {
                isCharging -> Color(0xFF00E5FF)
                batteryLevel <= 15 -> Color(0xFFFF5252)
                batteryLevel <= 30 -> Color(0xFFFFD740)
                else -> Color(0xFF00E676)
            }

            if (fillWidth > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(innerPadding, innerPadding),
                    size = Size(fillWidth, fillHeight),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                )
            }
        }

        // Charging lightning indicator if plugged in
        if (isCharging) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Charging",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp),
            )
        }

        // Battery percentage text
        Text(
            text = "$batteryLevel%",
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}
