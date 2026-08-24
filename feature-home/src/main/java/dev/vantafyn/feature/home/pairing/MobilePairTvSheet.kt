package dev.vantafyn.feature.home.pairing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPublicUser
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val VantafynModalCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

@Composable
fun MobilePairTvSheet(
    state: VantafynHomeUiState,
    viewModel: VantafynHomeViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            easing = VantafynModalCinematicEasing,
        ),
        label = "pairTvSheetProgress",
    )

    fun dismissWithAnimation() {
        if (!isVisible) return
        isVisible = false
        scope.launch {
            delay(380L)
            onDismiss()
        }
    }

    var code by remember { mutableStateOf("") }
    val selectedProfile = remember(state.savedProfiles, state.selectedProfileId) {
        state.savedProfiles.firstOrNull { it.id == state.selectedProfileId }
            ?: state.savedProfiles.firstOrNull()
    }

    // Resolve user avatar image URL
    val userImageUrl = selectedProfile?.imageUrl
        ?: state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl
        ?: state.publicUsers.firstOrNull { it.displayName == (selectedProfile?.displayName ?: state.username) }?.imageUrl

    // Resolve server administrator avatar image URL
    val serverAdminImageUrl = state.publicUsers.firstOrNull { it.isAdministrator && it.imageUrl != null }?.imageUrl
        ?: state.savedProfiles.firstOrNull { it.serverUrl == state.server?.url && it.imageUrl != null }?.imageUrl
        ?: state.publicUsers.firstOrNull { it.imageUrl != null }?.imageUrl

    var isPairing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successTvName by remember { mutableStateOf<String?>(null) }

    fun executePairing() {
        val cleanCode = code.trim().replace("-", "").replace(" ", "").uppercase()
        if (cleanCode.length != 6) {
            errorMessage = "Please enter the 6-character code shown on your TV."
            return
        }

        isPairing = true
        errorMessage = null

        scope.launch {
            val payload = viewModel.createPairingPayload(cleanCode, selectedProfile?.id)
            if (payload == null) {
                isPairing = false
                errorMessage = "Could not create pairing session. Please make sure you are signed in."
                return@launch
            }

            when (val result = MobileTvPairingClient.pairWithCode(payload)) {
                is PairingClientResult.Success -> {
                    isPairing = false
                    successTvName = result.tvName
                }
                is PairingClientResult.Failure -> {
                    isPairing = false
                    errorMessage = result.message
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f * animProgress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation() },
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Dark glass surface with 100% symmetric entrance & exit slide
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - animProgress) * 500f
                        alpha = animProgress
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // Consume click so scrim isn't triggered
                    )
                    .vantafynAnimatedModalBorder(
                        cornerRadius = 28.dp,
                        strokeWidth = 1.35.dp,
                        durationMillis = 4800,
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.96f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .align(Alignment.CenterHorizontally),
                    )

                    // Top Header Row with official branded Vantafyn logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(
                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                                contentDescription = "Vantafyn",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }

                        Column {
                            Text(
                                text = "Pair a TV",
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Set up your Android TV instantly",
                                color = VantafynColors.Muted,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    IconButton(
                        onClick = { dismissWithAnimation() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                if (successTvName != null) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color(0x4421D8FF), RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x2221D8FF))
                                .border(1.dp, Color(0x6621D8FF), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF21D8FF),
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TV Paired Successfully!",
                            color = VantafynColors.Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Your Android TV ($successTvName) is now connected to ${state.server?.name ?: "Jellyfin"}.",
                            color = VantafynColors.Muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        MobileSheetActionButton(
                            text = "Done",
                            enabled = true,
                            isLoading = false,
                            onClick = { dismissWithAnimation() },
                        )
                    }
                } else {
                    // 1. Pairing Code Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TV PAIRING CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 0.8.sp,
                        )

                        var isCodeFocused by remember { mutableStateOf(false) }
                        val codeFocusProgress by animateFloatAsState(
                            targetValue = if (isCodeFocused) 1f else 0f,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            label = "pairCodeFocusProgress",
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .drawWithContent {
                                    drawContent()
                                    val stroke = 1.35.dp.toPx()
                                    val glowStroke = 4.dp.toPx()
                                    val corner = 16.dp.toPx()
                                    val halfStroke = stroke / 2f

                                    // Base subtle border when unfocused / transitioning
                                    if (codeFocusProgress < 1f) {
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.12f * (1f - codeFocusProgress)),
                                            topLeft = Offset(halfStroke, halfStroke),
                                            size = Size(size.width - stroke, size.height - stroke),
                                            cornerRadius = CornerRadius(corner, corner),
                                            style = Stroke(width = stroke),
                                        )
                                    }

                                    // Smooth animated gradient glow & crisp border when focused
                                    if (codeFocusProgress > 0f) {
                                        val glowBrush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF21D8FF).copy(alpha = 0.28f * codeFocusProgress),
                                                Color(0xFF3E63FF).copy(alpha = 0.24f * codeFocusProgress),
                                                Color(0xFF8B35FF).copy(alpha = 0.26f * codeFocusProgress),
                                                Color(0xFFFF36C7).copy(alpha = 0.28f * codeFocusProgress),
                                            ),
                                            startX = 0f,
                                            endX = size.width,
                                        )
                                        val borderBrush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF21D8FF).copy(alpha = codeFocusProgress),
                                                Color(0xFF3E63FF).copy(alpha = codeFocusProgress),
                                                Color(0xFF8B35FF).copy(alpha = codeFocusProgress),
                                                Color(0xFFFF36C7).copy(alpha = codeFocusProgress),
                                            ),
                                            startX = 0f,
                                            endX = size.width,
                                        )
                                        // Soft outer atmospheric glow
                                        drawRoundRect(
                                            brush = glowBrush,
                                            topLeft = Offset(-glowStroke / 2f, -glowStroke / 2f),
                                            size = Size(size.width + glowStroke, size.height + glowStroke),
                                            cornerRadius = CornerRadius(corner + 2.dp.toPx(), corner + 2.dp.toPx()),
                                            style = Stroke(width = glowStroke),
                                        )
                                        // Razor-sharp crisp border
                                        drawRoundRect(
                                            brush = borderBrush,
                                            topLeft = Offset(halfStroke, halfStroke),
                                            size = Size(size.width - stroke, size.height - stroke),
                                            cornerRadius = CornerRadius(corner, corner),
                                            style = Stroke(width = stroke),
                                        )
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (code.isEmpty()) {
                                Text(
                                    text = "Enter 6-digit code (e.g. 482 915)",
                                    color = VantafynColors.Muted.copy(alpha = 0.55f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                )
                            }

                            BasicTextField(
                                value = code,
                                onValueChange = { input ->
                                    if (input.length <= 8) {
                                        code = input.uppercase()
                                        errorMessage = null
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = VantafynColors.Ink,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp,
                                    textAlign = TextAlign.Center,
                                ),
                                cursorBrush = SolidColor(Color(0xFF21D8FF)),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(onDone = { executePairing() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isCodeFocused = it.isFocused },
                            )
                        }
                    }

                    // 2. Profile & Server Section with Avatars
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SERVER & PROFILE TO SHARE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA),
                            letterSpacing = 0.8.sp,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Server Row with Admin Profile Picture
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF3D5266), Color(0xFF756A8A), Color(0xFF20252D)),
                                            ),
                                        )
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (serverAdminImageUrl != null) {
                                        AsyncImage(
                                            model = serverAdminImageUrl,
                                            contentDescription = "Server Admin",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Dns,
                                            contentDescription = null,
                                            tint = VantafynColors.Primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = state.server?.name ?: "Jellyfin Server",
                                        color = VantafynColors.Ink,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = state.server?.url.orEmpty(),
                                        color = VantafynColors.Muted,
                                        fontSize = 12.sp,
                                    )
                                }
                            }

                            // User Profile Row with Active Profile Picture
                            val profileDisplayName = selectedProfile?.displayName ?: state.username
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF21D8FF).copy(alpha = 0.35f), Color(0xFF8B35FF).copy(alpha = 0.35f)),
                                            ),
                                        )
                                        .border(BorderStroke(1.dp, Color(0xFF21D8FF).copy(alpha = 0.40f)), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (userImageUrl != null) {
                                        AsyncImage(
                                            model = userImageUrl,
                                            contentDescription = profileDisplayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        val initial = profileDisplayName.firstOrNull()?.uppercase() ?: "U"
                                        Text(
                                            text = initial,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                        )
                                    }
                                }

                                Text(
                                    text = "Connecting as $profileDisplayName",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    // Error Message
                    if (errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x22FF5C5C))
                                .border(1.dp, Color(0x55FF5C5C), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                color = Color(0xFFFFD4D4),
                                fontSize = 13.sp,
                            )
                        }
                    }

                    // Action Button
                    val isButtonEnabled = code.trim().replace("-", "").replace(" ", "").length == 6 && !isPairing

                    MobileSheetActionButton(
                        text = "Pair TV",
                        enabled = isButtonEnabled,
                        isLoading = isPairing,
                        onClick = { executePairing() },
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
}

@Composable
private fun MobileSheetActionButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        label = "sheetButtonScale",
    )

    val shape = RoundedCornerShape(18.dp)
    val backgroundGradient = if (enabled) {
        VantafynGradients.accentHorizontal()
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.04f),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        color = Color(0xFF7B8DFF).copy(alpha = 0.20f),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    )
                }
            }
            .padding(1.dp)
            .clip(shape)
            .background(backgroundGradient)
            .border(
                BorderStroke(
                    1.dp,
                    if (enabled) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
                ),
                shape,
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Connecting to TV...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        } else {
            Text(
                text = text,
                color = if (enabled) Color.White else VantafynColors.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}
