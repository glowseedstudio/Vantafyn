package dev.vantafyn.tv.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Smartphone
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.TvRemoteInputTarget
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPalette
import dev.vantafyn.tv.remoteinput.TvRemoteInputManager
import java.util.UUID

val VantafynFocusGradientColors = listOf(
    Color(0xFF21D8FF), // Electric Cyan
    Color(0xFF3E63FF), // Royal Blue
    Color(0xFF8B35FF), // Deep Violet
    Color(0xFFFF36C7), // Hot Magenta
)

@Composable
fun VantafynTvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionFocused by interactionSource.collectIsFocusedAsState()
    val editFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasFieldFocus by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val isFocused = hasFieldFocus || isInteractionFocused
    fun beginEditing() {
        if (enabled) {
            isEditing = true
        }
    }

    val fieldId = remember { UUID.randomUUID().toString() }
    val isSensitive = visualTransformation != VisualTransformation.None

    var remotePulseTrigger by remember { mutableIntStateOf(0) }
    val pulseGlow = remember { Animatable(0f) }

    // Double pulse glow animation when remote text is received:
    // Bright -> Soft -> Bright -> Soft (smooth successive breathing bloom)
    LaunchedEffect(remotePulseTrigger) {
        if (remotePulseTrigger > 0) {
            // Pulse 1: Bloom to bright peak, then ease to soft
            pulseGlow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            )
            pulseGlow.animateTo(
                targetValue = 0.25f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            )
            // Pulse 2: Re-bloom to bright peak, then ease down to settled rest
            pulseGlow.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            )
            pulseGlow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }
    }

    DisposableEffect(isFocused, enabled, placeholder, isSensitive) {
        if (isFocused && enabled) {
            TvRemoteInputManager.registerTarget(
                TvRemoteInputTarget(
                    fieldId = fieldId,
                    fieldName = placeholder,
                    isSensitive = isSensitive,
                    onTextReceived = { incomingText ->
                        onValueChange(incomingText)
                        remotePulseTrigger++
                    },
                ),
            )
        } else {
            TvRemoteInputManager.unregisterTarget(fieldId)
        }
        onDispose {
            TvRemoteInputManager.unregisterTarget(fieldId)
        }
    }

    LaunchedEffect(hasFieldFocus) {
        onFocusChange?.invoke(isFocused)
    }

    LaunchedEffect(isEditing, enabled) {
        if (isEditing && enabled) {
            editFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "TvTextFieldScale",
    )

    // See-through translucent navy glass matching mobile VantafynTextField
    val containerColor by animateColorAsState(
        targetValue = if (isFocused && enabled) {
            val baseAlpha = 0.65f + (0.20f * pulseGlow.value)
            VantafynGlassPalette.NavyLift.copy(alpha = baseAlpha.coerceIn(0f, 1f))
        } else {
            VantafynGlassPalette.NavyCore.copy(alpha = 0.45f)
        },
        label = "TvTextFieldContainer",
    )

    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focusState ->
                    hasFieldFocus = focusState.hasFocus
                    if (!focusState.hasFocus) {
                        isEditing = false
                    }
                }
                .focusable(enabled = enabled, interactionSource = interactionSource)
                .onPreviewKeyEvent { event ->
                    val isSelect = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.NumPadEnter
                    if (enabled && !isEditing && isSelect && event.type == KeyEventType.KeyDown) {
                        beginEditing()
                        true
                    } else {
                        false
                    }
                }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    beginEditing()
                }
                .scale(scale)
                .clip(shape)
                .background(containerColor)
                .drawWithContent {
                    drawContent()
                    val p = pulseGlow.value

                    if (isFocused && enabled) {
                        val stroke = (2.dp + (1.2.dp * p)).toPx()
                        val glowStroke = (4.dp + (8.dp * p)).toPx()
                        val corner = 16.dp.toPx()
                        val halfStroke = stroke / 2f
                        val glowAlpha = (0.25f + (0.65f * p)).coerceIn(0f, 1f)

                        // Outer blooming halo on pulse
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF21D8FF).copy(alpha = 0.35f * glowAlpha),
                                    Color(0xFF3E63FF).copy(alpha = 0.30f * glowAlpha),
                                    Color(0xFF8B35FF).copy(alpha = 0.32f * glowAlpha),
                                    Color(0xFFFF36C7).copy(alpha = 0.35f * glowAlpha),
                                ),
                                startX = 0f,
                                endX = size.width,
                            ),
                            topLeft = Offset(-glowStroke / 2f, -glowStroke / 2f),
                            size = Size(size.width + glowStroke, size.height + glowStroke),
                            cornerRadius = CornerRadius(corner + 2.dp.toPx(), corner + 2.dp.toPx()),
                            style = Stroke(width = glowStroke),
                        )

                        // Dynamic glowing border
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF21D8FF).copy(alpha = (0.88f + 0.12f * p).coerceIn(0f, 1f)),
                                    Color(0xFF3E63FF).copy(alpha = (0.88f + 0.12f * p).coerceIn(0f, 1f)),
                                    Color(0xFF8B35FF).copy(alpha = (0.88f + 0.12f * p).coerceIn(0f, 1f)),
                                    Color(0xFFFF36C7).copy(alpha = (0.88f + 0.12f * p).coerceIn(0f, 1f)),
                                ),
                                startX = 0f,
                                endX = size.width,
                            ),
                            topLeft = Offset(halfStroke, halfStroke),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(corner, corner),
                            style = Stroke(width = stroke),
                        )
                    } else {
                        val stroke = 1.dp.toPx()
                        val halfStroke = stroke / 2f
                        val corner = 16.dp.toPx()
                        drawRoundRect(
                            color = if (isError) Color(0xFFFF5252) else Color.White.copy(alpha = 0.14f),
                            topLeft = Offset(halfStroke, halfStroke),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(corner, corner),
                            style = Stroke(width = stroke),
                        )
                    }
                }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) Color(0xFF21D8FF) else VantafynColors.Primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(editFocusRequester),
                    textStyle = TextStyle(
                        color = VantafynColors.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(Color(0xFF21D8FF)),
                    singleLine = true,
                    enabled = enabled && isEditing,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = VantafynColors.Muted.copy(alpha = 0.75f),
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    },
                )

                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }

        // Subtle Living Room Helper Badge
        AnimatedVisibility(
            visible = isFocused && enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = Color(0xFF21D8FF),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "Use your phone to type",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
