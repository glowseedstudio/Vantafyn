package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinFriend
import dev.vantafyn.core.jellyfin.JellyfinSocialMessage
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSpacing
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private const val MAX_MESSAGE_LENGTH = 500
private const val CHAR_COUNTER_THRESHOLD = 400

@Composable
fun ChatScreen(
    peer: JellyfinFriend,
    messages: List<JellyfinSocialMessage>,
    isSending: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var textInput by remember { mutableStateOf("") }
    // reverseLayout = true means index 0 is at the bottom; we pass items in natural order
    // and LazyColumn shows newest (last) first visually at the bottom — no manual scroll needed.
    val listState = rememberLazyListState()

    // Scroll to top of reversed list (= newest message) when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VantafynColors.Surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.md, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactBackButton(onClick = onBack)

                // Peer avatar with online dot
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!peer.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = peer.avatarUrl,
                                contentDescription = peer.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = peer.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VantafynColors.Ink,
                            )
                        }
                    }
                    if (peer.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF55F0C0))
                                .border(2.dp, VantafynColors.Surface, CircleShape),
                        )
                    }
                }

                // Peer name & status
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = peer.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VantafynColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        peer.rankName?.let {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = "Lvl ${peer.rankTier}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Primary,
                                )
                            }
                        }
                    }
                    Text(
                        text = when {
                            !peer.currentlyWatching.isNullOrBlank() -> "Watching ${peer.currentlyWatching}"
                            peer.isOnline -> "Online"
                            !peer.lastSeen.isNullOrBlank() -> "Last seen ${peer.lastSeen?.take(10)}"
                            else -> "Offline"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!peer.currentlyWatching.isNullOrBlank()) Color(0xFF55F0C0) else VantafynColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh messages",
                    tint = VantafynColors.Ink.copy(alpha = 0.85f),
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // ── Messages list ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = VantafynSpacing.md),
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    VantafynGlassCard(
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(20.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Start a conversation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VantafynColors.Ink,
                            )
                            Text(
                                text = "Send ${peer.displayName} a message or recommend something to watch!",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                // reverseLayout=true: index 0 is visually at the bottom.
                // We pass messages in reverse so newest (last) appears at bottom.
                val reversed = remember(messages) { messages.asReversed() }
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(reversed, key = { it.messageId }) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }
            }
        }

        // ── Error banner ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !errorMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VantafynSpacing.md, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF3366).copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF88AA),
                )
            }
        }

        // ── Composer ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.md, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Character counter (shown when approaching limit)
            AnimatedVisibility(visible = textInput.length > CHAR_COUNTER_THRESHOLD) {
                Text(
                    text = "${textInput.length}/$MAX_MESSAGE_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (textInput.length >= MAX_MESSAGE_LENGTH) Color(0xFFFF6688) else VantafynColors.Muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.End,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(VantafynColors.SurfaceHigh.copy(alpha = 0.88f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) textInput = it },
                    placeholder = { Text("Write a message...", color = VantafynColors.Muted) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank() && !isSending) {
                            onSendMessage(textInput.trim())
                            textInput = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = VantafynColors.Ink,
                        unfocusedTextColor = VantafynColors.Ink,
                        focusedPlaceholderColor = VantafynColors.Muted,
                        unfocusedPlaceholderColor = VantafynColors.Muted,
                        cursorColor = VantafynColors.Primary,
                    ),
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank() && !isSending) {
                            onSendMessage(textInput.trim())
                            textInput = ""
                        }
                    },
                    enabled = textInput.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (textInput.isNotBlank() && !isSending) {
                                VantafynGradients.accentHorizontal()
                            } else {
                                androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.08f))
                            },
                        ),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank()) Color.White else VantafynColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: JellyfinSocialMessage,
    modifier: Modifier = Modifier,
) {
    val isSelf = message.isFromSelf
    val formattedTime = remember(message.timestamp) { formatMessageTime(message.timestamp) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSelf) 18.dp else 4.dp,
                        bottomEnd = if (isSelf) 4.dp else 18.dp,
                    ),
                )
                .background(
                    if (isSelf) {
                        VantafynGradients.accentHorizontal()
                    } else {
                        androidx.compose.ui.graphics.SolidColor(VantafynColors.SurfaceHigh.copy(alpha = 0.82f))
                    },
                )
                .border(
                    width = 1.dp,
                    color = if (isSelf) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSelf) 18.dp else 4.dp,
                        bottomEnd = if (isSelf) 4.dp else 18.dp,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }

        if (!formattedTime.isNullOrBlank()) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = VantafynColors.Muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

private fun formatMessageTime(isoString: String?): String? {
    if (isoString.isNullOrBlank()) return null
    return try {
        val trimmed = isoString.trim()
        val parsedDate = if (trimmed.contains("T")) {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(trimmed.substringBefore(".").substringBefore("Z"))
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(trimmed)
        }
        if (parsedDate != null) {
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(parsedDate)
        } else {
            trimmed
        }
    } catch (_: Exception) {
        isoString
    }
}
