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
            Color(0xFF101525).copy(alpha = 0.84f * alpha),
            Color(0xFF141A2B).copy(alpha = 0.80f * alpha),
            Color(0xFF0B1020).copy(alpha = 0.86f * alpha),
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

private fun VantafynGlassVariant.defaultRadius() = when (this) {
    VantafynGlassVariant.Panel -> 24.dp
    VantafynGlassVariant.Card -> 20.dp
    VantafynGlassVariant.Dock -> 30.dp
    VantafynGlassVariant.Chip -> 999.dp
    VantafynGlassVariant.Modal -> 28.dp
    VantafynGlassVariant.Button -> 18.dp
}

private fun VantafynGlassVariant.surfaceBrush(selected: Boolean, enabled: Boolean): Brush {
    val enabledAlpha = if (enabled) 1f else 0.54f
    val primaryLift = if (selected) 0.12f else 0.04f
    if (this == VantafynGlassVariant.Dock) {
        return VantafynNavDockBrush(enabled)
    }
    return Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.105f * enabledAlpha),
            VantafynColors.Primary.copy(alpha = primaryLift * enabledAlpha),
            VantafynColors.SurfaceHigh.copy(alpha = when (this) {
                VantafynGlassVariant.Dock -> 0.82f
                VantafynGlassVariant.Modal -> 0.90f
                VantafynGlassVariant.Chip -> 0.48f
                VantafynGlassVariant.Button -> 0.62f
                else -> 0.60f
            } * enabledAlpha),
            VantafynColors.Graphite.copy(alpha = when (this) {
                VantafynGlassVariant.Dock -> 0.72f
                VantafynGlassVariant.Modal -> 0.82f
                else -> 0.50f
            } * enabledAlpha),
        ),
    )
}

private fun VantafynGlassVariant.borderBrush(selected: Boolean, focused: Boolean, enabled: Boolean): Brush {
    val lift = when {
        !enabled -> 0.42f
        focused -> 1.0f
        selected -> 0.82f
        this == VantafynGlassVariant.Dock -> 1.0f
        else -> 0.56f
    }
    if (this == VantafynGlassVariant.Dock) {
        return VantafynNavDockBorder(enabled)
    }
    return Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.24f * lift),
            VantafynColors.Secondary.copy(alpha = 0.16f * lift),
            VantafynColors.Primary.copy(alpha = 0.20f * lift),
            Color.White.copy(alpha = 0.07f * lift),
        ),
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
    Box(
        modifier = modifier
            .drawBehind {
                val radius = cornerRadius.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = if (variant == VantafynGlassVariant.Dock) 0.46f else 0.22f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, if (variant == VantafynGlassVariant.Dock) 7.dp.toPx() else 5.dp.toPx()),
                )
                if (selected || focused) {
                    drawRoundRect(
                        color = VantafynColors.Primary.copy(alpha = if (focused) 0.20f else 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    )
                }
            }
            .clip(shape)
            .background(variant.surfaceBrush(selected = selected, enabled = enabled))
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
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (enabled) 0.075f else 0.035f),
                                Color.Transparent,
                                VantafynColors.Secondary.copy(alpha = if (selected || focused) 0.055f else 0.020f),
                                Color.Transparent,
                            ),
                        )
                    },
                ),
        )
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
fun Modifier.vantafynAnimatedModalBorder(
    cornerRadius: Dp = 28.dp,
    strokeWidth: Dp = 2.dp,
    durationMillis: Int = 5200,
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
                colors = VantafynGradients.AccentColors + VantafynGradients.AccentColors.first(),
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val textFieldModifier = modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .onFocusChanged {
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
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        enabled = enabled,
        readOnly = tvKeyboardRequiresClick && !editing,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(VantafynRadii.sheet),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = VantafynColors.Ink,
            unfocusedTextColor = VantafynColors.Ink,
            disabledTextColor = VantafynColors.Muted,
            focusedContainerColor = VantafynColors.Surface.copy(alpha = 0.58f),
            unfocusedContainerColor = VantafynColors.Surface.copy(alpha = 0.46f),
            disabledContainerColor = VantafynColors.Surface.copy(alpha = 0.34f),
            cursorColor = VantafynColors.Primary,
            focusedBorderColor = VantafynColors.Primary,
            unfocusedBorderColor = VantafynColors.Border,
            disabledBorderColor = VantafynColors.Border.copy(alpha = 0.56f),
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
                .size(if (tv) 58.dp else 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(14.dp))
                .padding(7.dp),
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
        VantafynGlassPanel(
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
