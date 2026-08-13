package dev.vantafyn.core.ui

import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

enum class VantafynGlassVariant {
    Panel,
    Card,
    Dock,
    Chip,
    Modal,
    Button,
}

enum class VantafynPermissionStatus {
    Granted,
    NotRequested,
    Denied,
    PermanentlyDenied,
    Unsupported,
}

object VantafynGradients {
    val AccentColors = listOf(
        Color(0xFF31D7FF),
        Color(0xFF5B8CFF),
        Color(0xFF8B5CFF),
        Color(0xFFC05CFF),
    )

    fun accentHorizontal(): Brush = Brush.horizontalGradient(AccentColors)

    fun accentLinear(
        start: Offset = Offset.Zero,
        end: Offset = Offset(1f, 1f),
        tileMode: TileMode = TileMode.Clamp,
    ): Brush = Brush.linearGradient(
        colors = AccentColors,
        start = start,
        end = end,
        tileMode = tileMode,
    )
}

fun VantafynNavDockBrush(enabled: Boolean = true): Brush {
    val alpha = if (enabled) 1f else 0.54f
    return Brush.linearGradient(
        listOf(
            Color(0xFF101525).copy(alpha = 0.91f * alpha),
            Color(0xFF141A2B).copy(alpha = 0.88f * alpha),
            Color(0xFF0B1020).copy(alpha = 0.93f * alpha),
        ),
    )
}

fun VantafynNavDockBorder(enabled: Boolean = true): Brush {
    val alpha = if (enabled) 1f else 0.54f
    return Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.16f * alpha),
            Color(0xFF5B8CFF).copy(alpha = 0.12f * alpha),
            Color(0xFF8B5CFF).copy(alpha = 0.10f * alpha),
            Color.White.copy(alpha = 0.08f * alpha),
        ),
    )
}

fun VantafynNavSelectedBrush(): Brush = VantafynGradients.accentHorizontal()

fun VantafynBottomScrim(): Brush =
    Brush.verticalGradient(
        listOf(
            Color.Transparent,
            Color(0xFF050812).copy(alpha = 0.28f),
            Color(0xFF050812).copy(alpha = 0.56f),
        ),
    )

data class VantafynPermissionUiState(
    val status: VantafynPermissionStatus = VantafynPermissionStatus.Unsupported,
    val dismissed: Boolean = false,
) {
    val statusLabel: String
        get() = when (status) {
            VantafynPermissionStatus.Granted -> "Allowed"
            VantafynPermissionStatus.NotRequested -> if (dismissed) "Ask when needed" else "Ask when needed"
            VantafynPermissionStatus.Denied -> "Not allowed"
            VantafynPermissionStatus.PermanentlyDenied -> "Not allowed"
            VantafynPermissionStatus.Unsupported -> "Allowed"
        }

    val needsSettings: Boolean
        get() = status == VantafynPermissionStatus.PermanentlyDenied
}

object VantafynGlassPalette {
    val NavyCore = Color(0xFF10182A)
    val NavyLift = Color(0xFF17233B)
    val IndigoLift = Color(0xFF252A4F)
    val VioletLift = Color(0xFF332456)
    val CyanSpecular = Color(0xFF67DCFF)
    val BlueSpecular = Color(0xFF5B8CFF)
    val VioletSpecular = Color(0xFF9B62FF)
    val EdgeWhite = Color(0xFFF3F8FF)
}

object VantafynGlassElevation {
    val Panel = 0.24f
    val Card = 0.20f
    val Chip = 0.12f
    val Modal = 0.30f
    val Button = 0.18f
}

private fun VantafynGlassVariant.defaultRadius() = when (this) {
    VantafynGlassVariant.Panel -> 24.dp
    VantafynGlassVariant.Card -> 20.dp
    VantafynGlassVariant.Dock -> 30.dp
    VantafynGlassVariant.Chip -> 999.dp
    VantafynGlassVariant.Modal -> 28.dp
    VantafynGlassVariant.Button -> 18.dp
}

private fun VantafynGlassVariant.depthAlpha(): Float =
    when (this) {
        VantafynGlassVariant.Panel -> VantafynGlassElevation.Panel
        VantafynGlassVariant.Card -> VantafynGlassElevation.Card
        VantafynGlassVariant.Dock -> 0.46f
        VantafynGlassVariant.Chip -> VantafynGlassElevation.Chip
        VantafynGlassVariant.Modal -> VantafynGlassElevation.Modal
        VantafynGlassVariant.Button -> VantafynGlassElevation.Button
    }

private fun VantafynGlassVariant.surfaceBrush(selected: Boolean, focused: Boolean, enabled: Boolean): Brush {
    val enabledAlpha = if (enabled) 1f else 0.54f
    if (this == VantafynGlassVariant.Dock) {
        return VantafynNavDockBrush(enabled)
    }
    if (this == VantafynGlassVariant.Modal) {
        return Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to VantafynGlassPalette.EdgeWhite.copy(alpha = 0.075f * enabledAlpha),
                0.10f to VantafynColors.Graphite.copy(alpha = 0.96f * enabledAlpha),
                0.46f to Color(0xFF080B14).copy(alpha = 0.97f * enabledAlpha),
                0.74f to Color(0xFF0B0D17).copy(alpha = 0.96f * enabledAlpha),
                1.00f to VantafynColors.Graphite.copy(alpha = 0.98f * enabledAlpha),
            ),
            start = Offset(0f, 0f),
            end = Offset(620f, 860f),
        )
    }
    val activeLift = when {
        focused -> 1f
        selected -> 0.76f
        else -> 0f
    }
    val density = when (this) {
        VantafynGlassVariant.Panel -> 0.86f
        VantafynGlassVariant.Card -> 0.74f
        VantafynGlassVariant.Chip -> 0.54f
        VantafynGlassVariant.Modal -> 0.92f
        VantafynGlassVariant.Button -> 0.66f
        VantafynGlassVariant.Dock -> 0.84f
    }
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to VantafynGlassPalette.EdgeWhite.copy(alpha = (0.115f + activeLift * 0.045f) * enabledAlpha),
            0.12f to VantafynGlassPalette.IndigoLift.copy(alpha = (0.22f + activeLift * 0.12f) * enabledAlpha),
            0.42f to VantafynGlassPalette.NavyLift.copy(alpha = density * enabledAlpha),
            0.72f to VantafynGlassPalette.VioletLift.copy(alpha = (0.18f + activeLift * 0.11f) * enabledAlpha),
            1.00f to VantafynColors.Graphite.copy(alpha = (0.58f + density * 0.16f) * enabledAlpha),
        ),
        start = Offset(0f, 0f),
        end = Offset(520f, 760f),
    )
}

private fun VantafynGlassVariant.borderBrush(selected: Boolean, focused: Boolean, enabled: Boolean): Brush {
    val lift = when {
        !enabled -> 0.34f
        focused -> 1.14f
        selected -> 0.98f
        this == VantafynGlassVariant.Dock -> 1.0f
        else -> 0.64f
    }
    if (this == VantafynGlassVariant.Dock) {
        return VantafynNavDockBorder(enabled)
    }
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.00f to VantafynGlassPalette.EdgeWhite.copy(alpha = 0.30f * lift),
            0.24f to VantafynGlassPalette.CyanSpecular.copy(alpha = 0.18f * lift),
            0.58f to VantafynGlassPalette.BlueSpecular.copy(alpha = 0.18f * lift),
            0.82f to VantafynGlassPalette.VioletSpecular.copy(alpha = 0.14f * lift),
            1.00f to Color.White.copy(alpha = 0.055f * lift),
        ),
        start = Offset.Zero,
        end = Offset(640f, 900f),
    )
}

@Composable
fun VantafynGlassSurface(
    modifier: Modifier = Modifier,
    variant: VantafynGlassVariant = VantafynGlassVariant.Panel,
    selected: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = variant.defaultRadius(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val active = selected || focused
    Box(
        modifier = modifier
            .drawBehind {
                val radius = cornerRadius.toPx()
                val shadowAlpha = variant.depthAlpha() * if (enabled) 1f else 0.46f
                drawRoundRect(
                    color = Color.Black.copy(alpha = shadowAlpha * 0.42f),
                    cornerRadius = CornerRadius(radius, radius),
                    topLeft = Offset(0f, if (variant == VantafynGlassVariant.Dock) 9.dp.toPx() else 7.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFF02040A).copy(alpha = shadowAlpha * 0.30f),
                    cornerRadius = CornerRadius(radius, radius),
                    topLeft = Offset(0f, if (variant == VantafynGlassVariant.Dock) 15.dp.toPx() else 12.dp.toPx()),
                    size = Size(size.width, size.height),
                )
                if (active) {
                    drawRoundRect(
                        color = VantafynGlassPalette.BlueSpecular.copy(alpha = if (focused) 0.16f else 0.10f),
                        cornerRadius = CornerRadius(radius, radius),
                        topLeft = Offset(-3.dp.toPx(), -2.dp.toPx()),
                        size = Size(size.width + 6.dp.toPx(), size.height + 7.dp.toPx()),
                    )
                    drawRoundRect(
                        color = VantafynGlassPalette.VioletSpecular.copy(alpha = if (focused) 0.10f else 0.07f),
                        cornerRadius = CornerRadius(radius, radius),
                        topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
                    )
                }
            }
            .clip(shape)
            .background(variant.surfaceBrush(selected = selected, focused = focused, enabled = enabled))
            .border(BorderStroke(if (focused) 1.4.dp else 1.dp, variant.borderBrush(selected, focused, enabled)), shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (variant == VantafynGlassVariant.Dock) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (enabled) 0.095f else 0.04f),
                                Color.White.copy(alpha = if (enabled) 0.030f else 0.014f),
                                Color.Transparent,
                            ),
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                VantafynGlassPalette.CyanSpecular.copy(
                                    alpha = if (enabled) {
                                        0.075f + if (active) 0.040f else 0f
                                    } else {
                                        0.028f
                                    },
                                ),
                                VantafynGlassPalette.BlueSpecular.copy(
                                    alpha = if (enabled) {
                                        0.040f + if (active) 0.030f else 0f
                                    } else {
                                        0.016f
                                    },
                                ),
                                Color.Transparent,
                            ),
                            center = Offset(90f, 20f),
                            radius = 520f,
                        )
                    },
                ),
        )
        if (variant != VantafynGlassVariant.Dock) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White.copy(alpha = if (enabled) 0.105f else 0.040f),
                                0.18f to Color.White.copy(alpha = if (enabled) 0.030f else 0.012f),
                                0.58f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = if (enabled) 0.055f else 0.030f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White.copy(alpha = if (enabled) 0.030f else 0.010f),
                                0.22f to Color.Transparent,
                                0.78f to VantafynGlassPalette.VioletSpecular.copy(alpha = if (active) 0.060f else 0.020f),
                                1.00f to Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(720f, 520f),
                        ),
                    ),
            )
        }
        if (variant == VantafynGlassVariant.Dock) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF31D7FF).copy(alpha = 0.035f),
                                Color.Transparent,
                                Color(0xFF8B5CFF).copy(alpha = 0.040f),
                            ),
                        ),
                    ),
            )
        }
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun VantafynGlassPanel(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(VantafynSpacing.lg),
    content: @Composable BoxScope.() -> Unit,
) = VantafynGlassSurface(
    modifier = modifier,
    variant = VantafynGlassVariant.Panel,
    selected = selected,
    focused = focused,
    enabled = enabled,
    cornerRadius = cornerRadius,
    contentPadding = contentPadding,
    content = content,
)

@Composable
fun VantafynGlassModalPanel(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(VantafynSpacing.lg),
    content: @Composable BoxScope.() -> Unit,
) = VantafynGlassSurface(
    modifier = modifier,
    variant = VantafynGlassVariant.Modal,
    selected = selected,
    focused = focused,
    enabled = enabled,
    cornerRadius = cornerRadius,
    contentPadding = contentPadding,
    content = content,
)

@Composable
fun VantafynGlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(VantafynSpacing.md),
    content: @Composable BoxScope.() -> Unit,
) = VantafynGlassSurface(
    modifier = modifier,
    variant = VantafynGlassVariant.Card,
    selected = selected,
    focused = focused,
    enabled = enabled,
    cornerRadius = cornerRadius,
    contentPadding = contentPadding,
    content = content,
)

@Composable
fun VantafynGlassDock(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 30.dp,
    contentPadding: PaddingValues = PaddingValues(VantafynSpacing.xs),
    content: @Composable BoxScope.() -> Unit,
) = VantafynGlassSurface(
    modifier = modifier,
    variant = VantafynGlassVariant.Dock,
    selected = selected,
    focused = focused,
    enabled = enabled,
    cornerRadius = cornerRadius,
    contentPadding = contentPadding,
    content = content,
)

@Composable
fun VantafynGlassChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    VantafynInteractiveGlass(
        modifier = modifier.then(
            if (selected && enabled) {
                Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
            } else {
                Modifier
            },
        ),
        variant = VantafynGlassVariant.Chip,
        selected = selected,
        enabled = enabled,
        cornerRadius = 999.dp,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun VantafynGlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    if (onClick == null) {
        VantafynGlassSurface(
            modifier = modifier,
            variant = VantafynGlassVariant.Chip,
            selected = selected,
            enabled = enabled,
            cornerRadius = 999.dp,
            contentPadding = contentPadding,
            content = content,
        )
    } else {
        VantafynGlassChip(
            modifier = modifier,
            selected = selected,
            enabled = enabled,
            onClick = onClick,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

@Composable
fun VantafynGlassTile(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: Dp = 18.dp,
    contentPadding: PaddingValues = PaddingValues(VantafynSpacing.md),
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    VantafynInteractiveGlass(
        modifier = modifier,
        variant = VantafynGlassVariant.Button,
        selected = selected,
        enabled = enabled,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}

@Composable
private fun VantafynInteractiveGlass(
    modifier: Modifier,
    variant: VantafynGlassVariant,
    selected: Boolean,
    enabled: Boolean,
    cornerRadius: Dp,
    contentPadding: PaddingValues,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.985f else 1f,
        animationSpec = tween(durationMillis = VantafynMotion.Quick),
        label = "vantafynGlassPressedScale",
    )
    VantafynGlassSurface(
        modifier = modifier
            .scale(scale)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        variant = variant,
        selected = selected || pressed,
        enabled = enabled,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun Modifier.vantafynAnimatedModalBorder(
    cornerRadius: Dp = 28.dp,
    strokeWidth: Dp = 2.dp,
    durationMillis: Int = 5200,
    alpha: Float = 1f,
): Modifier {
    val transition = rememberInfiniteTransition(label = "vantafynModalBorder")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vantafynModalBorderShift",
    )
    val shape = RoundedCornerShape(cornerRadius)
    return this.clip(shape).drawWithContent {
        drawContent()
        val radius = cornerRadius.toPx()
        val start = Offset(-size.width * shift, -size.height * shift)
        val end = Offset(size.width * (1f - shift), size.height * (1f - shift))
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = (VantafynGradients.AccentColors + VantafynGradients.AccentColors.first()).map {
                    it.copy(alpha = it.alpha * alpha.coerceIn(0f, 1f))
                },
                start = start,
                end = end,
                tileMode = TileMode.Repeated,
            ),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = strokeWidth.toPx()),
        )
    }
}

@Composable
fun VantafynScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF101624), VantafynColors.Graphite, Color(0xFF050812)),
                ),
            )
            .padding(VantafynSpacing.xl),
    ) {
        content()
    }
}

@Composable
fun VantafynOnboardingBackground(
    tv: Boolean,
    modifier: Modifier = Modifier,
    @DrawableRes backgroundResId: Int = R.drawable.vantafyn_onboarding_background,
    content: @Composable () -> Unit,
) {
    val customBackground = backgroundResId != R.drawable.vantafyn_onboarding_background
    val baseScrim = when {
        customBackground && tv -> 0.42f
        customBackground -> 0.38f
        tv -> 0.54f
        else -> 0.52f
    }
    val sideScrimStart = when {
        customBackground && tv -> 0.58f
        customBackground -> 0.52f
        tv -> 0.74f
        else -> 0.70f
    }
    val sideScrimMid = when {
        customBackground && tv -> 0.24f
        customBackground -> 0.20f
        tv -> 0.36f
        else -> 0.34f
    }
    val bottomScrim = when {
        customBackground && tv -> 0.50f
        customBackground -> 0.52f
        tv -> 0.68f
        else -> 0.72f
    }
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = backgroundResId, animationSpec = tween(durationMillis = 420), label = "vantafynBackground") { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = baseScrim)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VantafynColors.Graphite.copy(alpha = sideScrimStart),
                            VantafynColors.Graphite.copy(alpha = sideScrimMid),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VantafynColors.Graphite.copy(alpha = 0.18f),
                            Color.Transparent,
                            VantafynColors.Graphite.copy(alpha = bottomScrim),
                        ),
                        startY = 0f,
                    ),
                ),
        )
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun VantafynButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = VantafynGradientButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
)

@Composable
fun VantafynGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && enabled) 1.03f else 1f, label = "gradientButtonScale")
    val borderColor by animateColorAsState(
        if (focused && enabled) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.16f),
        label = "gradientButtonBorder",
    )
    val shape = RoundedCornerShape(20.dp)
    val gradient = if (enabled) {
        VantafynGradients.accentHorizontal()
    } else {
        Brush.horizontalGradient(
            listOf(
                VantafynColors.SurfaceHigh.copy(alpha = 0.82f),
                Color(0xFF252B3D).copy(alpha = 0.82f),
            ),
        )
    }
    Box(
        modifier = modifier
            .widthIn(min = 132.dp)
            .height(58.dp)
            .scale(scale)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        color = Color(0xFF7B8DFF).copy(alpha = if (focused) 0.24f else 0.14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    )
                }
            }
            .padding(2.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(gradient)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = VantafynSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color(0xFFF8FAFF) else VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun VantafynTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    tvKeyboardRequiresClick: Boolean = false,
) {
    var editing by remember { mutableStateOf(!tvKeyboardRequiresClick) }
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(VantafynRadii.sheet)
    val textFieldModifier = modifier
        .fillMaxWidth()
        .drawWithContent {
            drawContent()
            if (focused && enabled) {
                val stroke = 3.dp.toPx()
                val glowStroke = 6.dp.toPx()
                val halfStroke = stroke / 2f
                val corner = 16.dp.toPx()
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF21D8FF),
                        Color(0xFF3E63FF),
                        Color(0xFF8B35FF),
                        Color(0xFFFF36C7),
                    ),
                    startX = 0f,
                    endX = size.width,
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF21D8FF).copy(alpha = 0.28f),
                            Color(0xFF3E63FF).copy(alpha = 0.24f),
                            Color(0xFF8B35FF).copy(alpha = 0.26f),
                            Color(0xFFFF36C7).copy(alpha = 0.28f),
                        ),
                        startX = 0f,
                        endX = size.width,
                    ),
                    topLeft = Offset(glowStroke / 2f, glowStroke / 2f),
                    size = Size(size.width - glowStroke, size.height - glowStroke),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = glowStroke),
                )
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = stroke),
                )
            }
        }
        .focusRequester(focusRequester)
        .onFocusChanged {
            focused = it.isFocused
            if (!it.isFocused && tvKeyboardRequiresClick) {
                editing = false
            }
        }
        .onPreviewKeyEvent {
            if (!tvKeyboardRequiresClick || it.type != KeyEventType.KeyUp) {
                false
            } else {
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    -> {
                        editing = true
                        focusRequester.requestFocus()
                        keyboardController?.show()
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (editing) {
                            editing = false
                            keyboardController?.hide()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
        }
        .then(
            if (tvKeyboardRequiresClick) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    editing = true
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            } else {
                Modifier
            },
        )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = textFieldModifier,
        placeholder = { Text(placeholder ?: label) },
        singleLine = true,
        enabled = enabled,
        readOnly = tvKeyboardRequiresClick && !editing,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VantafynColors.Ink,
            unfocusedTextColor = VantafynColors.Ink,
            disabledTextColor = VantafynColors.Muted,
            focusedContainerColor = VantafynGlassPalette.NavyLift.copy(alpha = 0.72f),
            unfocusedContainerColor = VantafynGlassPalette.NavyCore.copy(alpha = 0.58f),
            disabledContainerColor = VantafynGlassPalette.NavyCore.copy(alpha = 0.36f),
            cursorColor = VantafynGlassPalette.CyanSpecular,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.13f),
            disabledBorderColor = Color.White.copy(alpha = 0.065f),
            focusedLabelColor = VantafynColors.Ink,
            unfocusedLabelColor = VantafynColors.Muted,
            focusedPlaceholderColor = VantafynColors.Muted.copy(alpha = 0.78f),
            unfocusedPlaceholderColor = VantafynColors.Muted.copy(alpha = 0.62f),
        ),
    )
}

@Composable
fun VantafynCard(
    modifier: Modifier = Modifier,
    focusedScale: Boolean = false,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && focusedScale) 1.04f else 1f, label = "cardScale")
    Box(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        VantafynGlassPanel(
            focused = focused,
            cornerRadius = VantafynRadii.sheet,
            contentPadding = PaddingValues(VantafynSpacing.lg),
        ) {
            content()
        }
    }
}

@Composable
fun VantafynLoadingIndicator(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(color = VantafynColors.Primary, modifier = Modifier.size(24.dp))
        Text(text, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun VantafynErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    VantafynCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
            Text("Something needs attention", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge)
            Text(message, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            if (action != null) {
                Spacer(Modifier.height(VantafynSpacing.xs))
                Row(content = action)
            }
        }
    }
}

@Composable
fun VantafynServerCard(
    name: String,
    url: String,
    version: String?,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    VantafynGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = VantafynSpacing.lg, vertical = 22.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4F657D), Color(0xFF756A8A), Color(0xFF20252D)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (leadingContent != null) {
                    leadingContent()
                } else {
                    Text(
                        name.take(1).uppercase(),
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        name,
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF76D9C8).copy(alpha = 0.86f)),
                    )
                }
                Text(url, color = VantafynColors.Muted.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                version?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = VantafynSpacing.sm, vertical = VantafynSpacing.xxs),
                    ) {
                        Text(
                            "Jellyfin $it",
                            color = VantafynColors.Muted.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VantafynProfileCard(
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    manageMode: Boolean = false,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    avatar: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "profileCardScale")
    VantafynGlassCard(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        focused = focused,
        cornerRadius = VantafynRadii.sheet,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF273557), Color(0xFF35324E), VantafynColors.SurfaceHigh),
                        ),
                    ),
                contentAlignment = Alignment.Center,
                content = avatar,
            )
            Text(
                label,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            subtitle?.let {
                Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
            if (manageMode && onRemove != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VantafynRadii.button))
                        .background(VantafynColors.Destructive.copy(alpha = 0.22f))
                        .border(BorderStroke(1.dp, VantafynColors.Destructive.copy(alpha = 0.38f)), RoundedCornerShape(VantafynRadii.button))
                        .clickable(onClick = onRemove)
                        .padding(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.xs),
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
fun VantafynLogoHeader(
    title: String,
    tagline: String?,
    tv: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tv) VantafynSpacing.md else VantafynSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(if (tv) 92.dp else 76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(20.dp))
                .padding(if (tv) 12.dp else 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.vantafyn_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            title,
            color = VantafynColors.Ink,
            style = if (tv) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        tagline?.let {
            Text(
                it,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun VantafynSetupHeader(
    title: String,
    subtitle: String?,
    tv: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tv) VantafynSpacing.md else VantafynSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(if (tv) 92.dp else 76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(20.dp))
                .padding(if (tv) 12.dp else 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.vantafyn_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        subtitle?.let {
            Text(
                it,
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun VantafynPermissionSheet(
    title: String,
    body: String,
    primaryAction: String,
    secondaryAction: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    trustNote: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.54f))
            .padding(VantafynSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        VantafynGlassModalPanel(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .vantafynAnimatedModalBorder(),
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(VantafynSpacing.lg),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
                trustNote?.let {
                    Text(
                        it,
                        color = VantafynColors.Muted.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(Modifier.height(VantafynSpacing.xs))
                VantafynButton(
                    text = primaryAction,
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = onSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(secondaryAction, color = VantafynColors.Muted)
                }
            }
        }
    }
}

@Composable
fun VantafynPermissionExplainerScreen(
    title: String,
    body: String,
    primaryAction: String,
    secondaryAction: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    trustNote: String? = null,
) = VantafynPermissionSheet(
    title = title,
    body = body,
    primaryAction = primaryAction,
    secondaryAction = secondaryAction,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    modifier = modifier,
    trustNote = trustNote,
)
