package dev.vantafyn.tv.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvTextField

@Composable
fun TvConnectServerScreen(
    serverUrl: String,
    isLoading: Boolean,
    errorMessage: String?,
    onServerUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var isFieldFocused by remember { mutableStateOf(false) }

    val verticalOffsetPx by animateFloatAsState(
        targetValue = if (isFieldFocused) -54f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "TvConnectServerLift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .graphicsLayer {
                    translationY = verticalOffsetPx.dp.toPx()
                }
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VantafynLogoBadge(
                size = 56.dp,
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Connect to Jellyfin",
                color = VantafynColors.Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your server address or IP to get started",
                color = VantafynColors.Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glass Text Input with 4-Color Gradient Focus Border
            VantafynTvTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                placeholder = "https://jellyfin.example.com or 192.168.1.100:8096",
                isError = errorMessage != null,
                enabled = !isLoading,
                onFocusChange = { isFieldFocused = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (serverUrl.isNotBlank() && !isLoading) {
                            onConnect()
                        }
                    }
                ),
                trailingContent = {
                    if (serverUrl.isNotEmpty() && !isLoading) {
                        IconButton(
                            onClick = { onServerUrlChange("") },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = VantafynColors.Muted,
                            )
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF21D8FF),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            )

            // Error Banner
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FF5252))
                        .border(1.dp, Color(0x66FF5252), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VantafynTvGlassButton(
                    text = if (isLoading) "Testing..." else "Connect",
                    icon = Icons.Rounded.Check,
                    isPrimary = true,
                    illuminatedPrimary = true,
                    enabled = serverUrl.isNotBlank() && !isLoading,
                    onClick = {
                        if (serverUrl.isNotBlank() && !isLoading) {
                            onConnect()
                        }
                    },
                )

                VantafynTvGlassButton(
                    text = "Back",
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    isPrimary = true,
                    illuminatedPrimary = true,
                    onClick = onBack,
                )
            }

            // Bottom clearance so scaled buttons & glowing focus outlines are never clipped
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
