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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.tv.components.VantafynLogoBadge
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvTextField
import kotlinx.coroutines.delay

@Composable
fun TvLoginScreen(
    username: String,
    password: String,
    serverName: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    onQuickConnect: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var isUsernameFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }
    val isAnyFieldFocused = isUsernameFocused || isPasswordFocused

    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            if (username.isNotBlank()) {
                passwordFocusRequester.requestFocus()
            } else {
                usernameFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    val verticalOffsetPx by animateFloatAsState(
        targetValue = if (isAnyFieldFocused) -68f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "TvLoginLift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
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
                text = "Sign in to Jellyfin",
                color = VantafynColors.Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (!serverName.isNullOrBlank()) "Connecting to $serverName" else "Enter your username and password",
                color = VantafynColors.Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- 1. USERNAME INPUT FIELD ---
            VantafynTvTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = "Username",
                enabled = !isLoading,
                focusRequester = usernameFocusRequester,
                onFocusChange = { isUsernameFocused = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. PASSWORD INPUT FIELD ---
            VantafynTvTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "Password (leave blank if passwordless)",
                enabled = !isLoading,
                focusRequester = passwordFocusRequester,
                onFocusChange = { isPasswordFocused = it },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (username.isNotBlank() && !isLoading) {
                            onLogin()
                        }
                    }
                ),
                trailingContent = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = if (isPasswordVisible) Color(0xFF21D8FF) else VantafynColors.Muted,
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
                    text = if (isLoading) "Signing in..." else "Sign In",
                    icon = Icons.Rounded.Check,
                    isPrimary = true,
                    enabled = username.isNotBlank() && !isLoading,
                    onClick = {
                        if (username.isNotBlank() && !isLoading) {
                            onLogin()
                        }
                    },
                )

                if (onQuickConnect != null) {
                    VantafynTvGlassButton(
                        text = "Quick Connect",
                        icon = Icons.Rounded.Bolt,
                        isPrimary = false,
                        enabled = !isLoading,
                        onClick = onQuickConnect,
                    )
                }

                VantafynTvGlassButton(
                    text = "Back",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    isPrimary = false,
                    onClick = onBack,
                )
            }

            // Bottom clearance so scaled buttons & glowing focus outlines are never clipped
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
