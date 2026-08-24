package dev.vantafyn.feature.home.remoteinput

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.feature.home.CompactBackButton
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.pairing.DiscoveredTv
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MobileSendTextToTvScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var textInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(true) }
    var discoveredTvs by remember { mutableStateOf<List<DiscoveredTv>>(emptyList()) }
    var selectedTv by remember { mutableStateOf<DiscoveredTv?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSensitiveTarget by remember { mutableStateOf(false) }
    var targetFieldName by remember { mutableStateOf<String?>(null) }

    fun refreshTvs() {
        isDiscovering = true
        scope.launch {
            val tvs = MobileTvInputClient.discoverNearbyTvs()
            discoveredTvs = tvs
            if (selectedTv == null || tvs.none { it.ipAddress == selectedTv?.ipAddress }) {
                selectedTv = tvs.firstOrNull()
            }
            isDiscovering = false

            selectedTv?.let { tv ->
                val fieldStatus = MobileTvInputClient.getTvFieldStatus(tv.ipAddress, tv.port)
                isSensitiveTarget = fieldStatus.isSensitive
                targetFieldName = fieldStatus.fieldName
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTvs()
    }

    // Periodically sync active TV focus state (e.g. detect password field focus change)
    LaunchedEffect(selectedTv) {
        val tv = selectedTv ?: return@LaunchedEffect
        while (isActive) {
            val fieldStatus = MobileTvInputClient.getTvFieldStatus(tv.ipAddress, tv.port)
            isSensitiveTarget = fieldStatus.isSensitive
            targetFieldName = fieldStatus.fieldName
            delay(1_500L)
        }
    }

    fun sendText() {
        if (textInput.isBlank()) {
            errorMessage = "Please enter text to send."
            return
        }

        isSending = true
        errorMessage = null

        scope.launch {
            var targetTv = selectedTv
            if (targetTv == null) {
                val tvs = MobileTvInputClient.discoverNearbyTvs(timeoutMs = 1_500L)
                discoveredTvs = tvs
                targetTv = tvs.firstOrNull()
                selectedTv = targetTv
            }

            if (targetTv == null) {
                isSending = false
                errorMessage = "No Android TV found. Make sure Vantafyn is open on your TV."
                return@launch
            }

            val response = MobileTvInputClient.sendTextToTv(
                targetIp = targetTv.ipAddress,
                port = targetTv.port,
                text = textInput,
            )

            isSending = false
            if (response.success) {
                // Clear text immediately without showing success card
                textInput = ""
                errorMessage = null
            } else {
                errorMessage = response.message ?: "Could not send text to TV."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactBackButton(onClick = onBack)

            IconButton(
                onClick = { refreshTvs() },
                enabled = !isDiscovering,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            ) {
                if (isDiscovering) {
                    CircularProgressIndicator(
                        color = VantafynColors.Primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh TVs",
                        tint = VantafynColors.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Vertically & Horizontally Centered Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Header Logo Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = CoreUiR.drawable.vantafyn_logo),
                        contentDescription = "Vantafyn",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Send text to TV",
                    color = VantafynColors.Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Type on your phone, then send it to the focused field on your TV.",
                    color = VantafynColors.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Active TV Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = if (selectedTv != null) Color(0xFF21D8FF) else VantafynColors.Muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = selectedTv?.let { "${it.deviceName} • Ready" }
                                ?: if (isDiscovering) "Searching for TV..." else "No TV detected on LAN",
                            color = if (selectedTv != null) Color.White else VantafynColors.Muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (isSensitiveTarget) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22A78BFA))
                            .border(1.dp, Color(0x44A78BFA), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "TV password field focused. Input is masked with ***.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Standard Vantafyn Text Field (Masked with PasswordVisualTransformation if TV field is sensitive)
                VantafynTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        errorMessage = null
                    },
                    label = if (isSensitiveTarget) (targetFieldName ?: "Password") else (targetFieldName?.let { "Text for $it" } ?: "Text to send"),
                    placeholder = if (isSensitiveTarget) "Enter password..." else "Type text to send to TV...",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isSensitiveTarget) KeyboardType.Password else KeyboardType.Text,
                    ),
                    visualTransformation = if (isSensitiveTarget) PasswordVisualTransformation() else VisualTransformation.None,
                )

                // Error Message Card (Only shown if error occurs)
                AnimatedVisibility(
                    visible = errorMessage != null,
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    DeviceInputErrorCard(
                        title = "Could not send",
                        message = errorMessage.orEmpty(),
                        icon = Icons.Rounded.ErrorOutline,
                        tint = Color(0xFFFFB5BE),
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons Row
                val isTextEntered = textInput.isNotBlank()
                val isSendEnabled = isTextEntered && !isSending

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Clear Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .clickable(enabled = isTextEntered) {
                                textInput = ""
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Clear",
                            color = if (isTextEntered) Color.White else VantafynColors.Muted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Send Button
                    val sendShape = RoundedCornerShape(16.dp)
                    val sendGradient = if (isSendEnabled) {
                        VantafynGradients.accentHorizontal()
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f)),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .drawBehind {
                                if (isSendEnabled) {
                                    drawRoundRect(
                                        color = Color(0xFF7B8DFF).copy(alpha = 0.25f),
                                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                                    )
                                }
                            }
                            .clip(sendShape)
                            .background(sendGradient)
                            .border(
                                1.dp,
                                if (isSendEnabled) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
                                sendShape,
                            )
                            .clickable(enabled = isSendEnabled) { sendText() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    tint = if (isSendEnabled) Color.White else VantafynColors.Muted,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = "Send to TV",
                                    color = if (isSendEnabled) Color.White else VantafynColors.Muted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInputErrorCard(
    title: String,
    message: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(message, color = VantafynColors.Muted, fontSize = 12.sp)
        }
    }
}
