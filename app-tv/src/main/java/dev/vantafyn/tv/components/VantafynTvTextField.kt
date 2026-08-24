package dev.vantafyn.tv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPalette

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
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChange?.invoke(isFocused)
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "TvTextFieldScale",
    )

    // See-through translucent navy glass matching mobile VantafynTextField
    val containerColor by animateColorAsState(
        targetValue = if (isFocused && enabled) {
            VantafynGlassPalette.NavyLift.copy(alpha = 0.65f)
        } else {
            VantafynGlassPalette.NavyCore.copy(alpha = 0.45f)
        },
        label = "TvTextFieldContainer",
    )

    val shape = RoundedCornerShape(16.dp)
    val accentGradient = Brush.horizontalGradient(VantafynFocusGradientColors)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .then(
                if (isFocused && enabled) {
                    Modifier.border(
                        border = BorderStroke(2.dp, accentGradient),
                        shape = shape,
                    )
                } else {
                    Modifier.border(
                        border = BorderStroke(
                            1.dp,
                            if (isError) Color(0xFFFF5252) else Color.White.copy(alpha = 0.14f)
                        ),
                        shape = shape,
                    )
                }
            )
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
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                textStyle = TextStyle(
                    color = VantafynColors.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(Color(0xFF21D8FF)),
                singleLine = true,
                enabled = enabled,
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
                }
            )

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
