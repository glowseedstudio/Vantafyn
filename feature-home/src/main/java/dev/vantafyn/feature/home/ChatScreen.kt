package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import java.util.UUID
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private const val MAX_MESSAGE_LENGTH = 500
private const val CHAR_COUNTER_THRESHOLD = 400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peer: JellyfinFriend,
    messages: List<JellyfinSocialMessage>,
    isSending: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRefresh: () -> Unit,
    availableMedia: List<JellyfinMediaCard> = emptyList(),
    onOpenMedia: (UUID) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val context = androidx.compose.ui.platform.LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showActionSheet by remember { mutableStateOf(false) }
    var activeReactionTargetMessage by remember { mutableStateOf<JellyfinSocialMessage?>(null) }

    val parsedReactions = remember(messages) {
        val map = mutableMapOf<String, MutableList<String>>()
        for (msg in messages) {
            val reaction = parseReaction(msg.content)
            if (reaction != null) {
                val (targetId, emoji) = reaction
                map.getOrPut(targetId) { mutableListOf() }.add(emoji)
            }
        }
        map
    }

    val visibleMessages = remember(messages) {
        messages.filter { parseReaction(it.content) == null }
    }
    var prevMessageCount by remember { mutableStateOf(visibleMessages.size) }

    // Auto scroll and sound cue when a new message arrives
    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
            if (visibleMessages.size > prevMessageCount) {
                val latest = visibleMessages.lastOrNull()
                if (latest != null && !latest.isFromSelf) {
                    dev.vantafyn.core.ui.VantafynSoundEffects.playNewMessageAlert(context)
                }
            }
        }
        prevMessageCount = visibleMessages.size
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // Dark at the top, fading smoothly to transparent towards the bottom
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07090F).copy(alpha = 0.96f),
                        Color(0xFF07090F).copy(alpha = 0.88f),
                        Color(0xFF07090F).copy(alpha = 0.55f),
                        Color(0xFF07090F).copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                ),
            )
            .drawBehind {
                // Subtle ambient glow in upper-left and lower-right for cinematic depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.10f),
                        radius = size.width * 0.70f,
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF).copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.65f),
                        radius = size.width * 0.75f,
                    ),
                )
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Top Glass Header (Pinned at top with status bar padding) ────────
            ChatHeader(
                peer = peer,
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.statusBars.union(WindowInsets.displayCutout),
                ),
            )

            // ── Conversation Content (Resizes when keyboard opens) ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = VantafynSpacing.md),
            ) {
                if (visibleMessages.isEmpty()) {
                    ChatEmptyState(
                        peer = peer,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    // Grouped message rendering in reverseLayout
                    val reversedMessages = remember(visibleMessages) { visibleMessages.asReversed() }

                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = reversedMessages,
                            key = { _, msg -> msg.messageId },
                        ) { index, msg ->
                            val isSelf = msg.isFromSelf
                            val nextMsg = if (index > 0) reversedMessages[index - 1] else null
                            val prevMsg = if (index < reversedMessages.size - 1) reversedMessages[index + 1] else null

                            val isFirstInGroup = prevMsg?.isFromSelf != isSelf
                            val isLastInGroup = nextMsg?.isFromSelf != isSelf
                            val isLatestOverall = index == 0
                            val messageReactions = parsedReactions[msg.messageId] ?: emptyList()

                            ChatMessageItem(
                                message = msg,
                                peer = peer,
                                reactions = messageReactions,
                                isFirstInGroup = isFirstInGroup,
                                isLastInGroup = isLastInGroup,
                                showTimestamp = isLastInGroup || isLatestOverall,
                                isLatestOverall = isLatestOverall,
                                onLongPress = { activeReactionTargetMessage = msg },
                                onOpenMedia = onOpenMedia,
                            )

                            if (isLastInGroup && index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // ── Error Banner ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !errorMessage.isNullOrBlank(),
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VantafynSpacing.md, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF3B1220))
                        .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF88AA),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // ── Fixed Bottom Glass Composer (Attached right above keyboard) ─────
            ChatComposer(
                textInput = textInput,
                onTextChanged = { if (it.length <= MAX_MESSAGE_LENGTH) textInput = it },
                isSending = isSending,
                onSend = {
                    if (textInput.isNotBlank() && !isSending) {
                        val text = textInput.trim()
                        textInput = ""
                        dev.vantafyn.core.ui.VantafynSoundEffects.playMessageSent(context)
                        onSendMessage(text)
                    }
                },
                onOpenActionSheet = { showActionSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.ime.union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                    )
                    .padding(horizontal = VantafynSpacing.md, vertical = 8.dp),
            )
        }

        // ── Action Sheet Modal (Quick Reactions + Recommend Movies/Shows) ───────
        if (showActionSheet) {
            ChatActionBottomSheet(
                availableMedia = availableMedia,
                onDismiss = { showActionSheet = false },
                onSendQuickReaction = { emoji ->
                    showActionSheet = false
                    dev.vantafyn.core.ui.VantafynSoundEffects.playMessageSent(context)
                    onSendMessage(emoji)
                },
                onSendMediaRecommendation = { mediaItem ->
                    showActionSheet = false
                    val encoded = encodeMediaRecommendation(mediaItem)
                    dev.vantafyn.core.ui.VantafynSoundEffects.playMessageSent(context)
                    onSendMessage(encoded)
                },
            )
        }

        if (activeReactionTargetMessage != null) {
            val targetMsg = activeReactionTargetMessage!!
            QuickReactionModal(
                targetMessage = targetMsg,
                onDismiss = { activeReactionTargetMessage = null },
                onSelectReaction = { emoji ->
                    activeReactionTargetMessage = null
                    dev.vantafyn.core.ui.VantafynSoundEffects.playMessageSent(context)
                    onSendMessage("[reaction|${targetMsg.messageId}|$emoji]")
                },
            )
        }
    }
}

// ── Header Component ──────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(
    peer: JellyfinFriend,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1019).copy(alpha = 0.95f),
                        Color(0xFF090B12).copy(alpha = 0.90f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.03f),
                    ),
                ),
                shape = RoundedCornerShape(0.dp),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular glass back button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Avatar with presence indicator
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1E2638),
                                        Color(0xFF141926),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.5.dp,
                                brush = if (peer.isOnline) {
                                    VantafynGradients.accentHorizontal()
                                } else {
                                    SolidColor(Color.White.copy(alpha = 0.16f))
                                },
                                shape = CircleShape,
                            ),
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
                                color = Color.White,
                            )
                        }
                    }

                    if (peer.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF55F0C0))
                                .border(2.dp, Color(0xFF090B12), CircleShape),
                        )
                    }
                }

                // Name, rank badge & presence text
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = peer.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Rank Level Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 1.5.dp),
                        ) {
                            Text(
                                text = "Lvl ${peer.rankTier}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                            )
                        }
                    }

                    // Watching status or active state
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!peer.currentlyWatching.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF55F0C0),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Watching ${peer.currentlyWatching}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF55F0C0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = when {
                                    peer.isOnline -> "Active now"
                                    !peer.lastSeen.isNullOrBlank() -> "Last seen ${peer.lastSeen?.take(10)}"
                                    else -> "Offline"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = if (peer.isOnline) Color(0xFF55F0C0) else VantafynColors.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyState(
    peer: JellyfinFriend,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Glowing circular icon
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.15f),
                            Color(0xFF7C4DFF).copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color(0xFF7C4DFF).copy(alpha = 0.25f),
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(30.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Say hello to ${peer.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Send a message or recommend something to watch.",
                style = MaterialTheme.typography.bodySmall,
                color = VantafynColors.Muted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}

// ── Message Item & Bubbles ────────────────────────────────────────────────────

@Composable
private fun ChatMessageItem(
    message: JellyfinSocialMessage,
    peer: JellyfinFriend,
    reactions: List<String> = emptyList(),
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    showTimestamp: Boolean,
    isLatestOverall: Boolean,
    onLongPress: () -> Unit = {},
    onOpenMedia: (UUID) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isSelf = message.isFromSelf
    val formattedTime = remember(message.timestamp) { formatMessageTime(message.timestamp) }
    val mediaRec = remember(message.content) { parseMediaRecommendation(message.content) }
    val haptic = LocalHapticFeedback.current

    // Dynamic Corner Radius for smooth message grouping
    val topStartRadius = if (isSelf) 18.dp else if (isFirstInGroup) 18.dp else 6.dp
    val topEndRadius = if (isSelf) (if (isFirstInGroup) 18.dp else 6.dp) else 18.dp
    val bottomStartRadius = if (isSelf) 18.dp else if (isLastInGroup) 18.dp else 6.dp
    val bottomEndRadius = if (isSelf) (if (isLastInGroup) 18.dp else 6.dp) else 18.dp

    val bubbleShape = RoundedCornerShape(
        topStart = topStartRadius,
        topEnd = topEndRadius,
        bottomStart = bottomStartRadius,
        bottomEnd = bottomEndRadius,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            // Optional mini avatar for received messages (at bottom of cluster)
            if (!isSelf) {
                if (isLastInGroup) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2638))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!peer.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = peer.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = peer.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }

            if (mediaRec != null) {
                MediaRecommendationCard(
                    recommendation = mediaRec,
                    isSelf = isSelf,
                    timestamp = formattedTime,
                    onOpenMedia = onOpenMedia,
                    onLongPress = onLongPress,
                )
            } else {
                // Standard Message Bubble with long-press support
                Box(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .clip(bubbleShape)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            },
                        )
                        .background(
                            if (isSelf) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF162544),
                                        Color(0xFF261942),
                                    ),
                                )
                            } else {
                                SolidColor(Color(0xFF121624).copy(alpha = 0.92f))
                            },
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isSelf) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.45f),
                                        Color(0xFF7C4DFF).copy(alpha = 0.35f),
                                    ),
                                )
                            } else {
                                SolidColor(Color.White.copy(alpha = 0.10f))
                            },
                            shape = bubbleShape,
                        )
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        // ── Floating Reaction Badges ──────────────────────────────────────────
        if (reactions.isNotEmpty()) {
            val reactionCounts = remember(reactions) {
                reactions.groupingBy { it }.eachCount()
            }
            Row(
                modifier = Modifier
                    .padding(
                        start = if (isSelf) 0.dp else 40.dp,
                        end = if (isSelf) 8.dp else 0.dp,
                        top = 2.dp,
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF141928).copy(alpha = 0.94f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = emoji, fontSize = 13.sp)
                        if (count > 1) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }

        // Timestamp & Read Receipt (Seen) for text bubbles
        if (showTimestamp && mediaRec == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = if (isSelf) 0.dp else 40.dp,
                    end = if (isSelf) 8.dp else 0.dp,
                    top = 2.dp,
                ),
            ) {
                if (!formattedTime.isNullOrBlank()) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = VantafynColors.Muted,
                    )
                }

                if (isSelf && isLatestOverall) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = if (message.isRead) "Seen" else "Sent",
                            tint = if (message.isRead) Color(0xFF00E5FF) else VantafynColors.Muted,
                            modifier = Modifier.size(12.dp),
                        )
                        if (message.isRead) {
                            Text(
                                text = "Seen",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Interactive Media Recommendation Bubble ───────────────────────────────────

@Composable
private fun MediaRecommendationCard(
    recommendation: MediaRecommendationPayload,
    isSelf: Boolean,
    timestamp: String?,
    onOpenMedia: (UUID) -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            )
            .background(
                Brush.verticalGradient(
                    if (isSelf) {
                        listOf(
                            Color(0xFF162544).copy(alpha = 0.95f),
                            Color(0xFF261942).copy(alpha = 0.95f),
                        )
                    } else {
                        listOf(
                            Color(0xFF141828).copy(alpha = 0.95f),
                            Color(0xFF0C101C).copy(alpha = 0.95f),
                        )
                    },
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.50f),
                        Color(0xFFA78BFA).copy(alpha = 0.40f),
                    ),
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(11.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            // Header tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "RECOMMENDED TITLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 0.8.sp,
                    )
                }
                if (!timestamp.isNullOrBlank()) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = VantafynColors.Muted,
                    )
                }
            }

            // Poster Artwork + Info Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 66.dp, height = 96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!recommendation.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = recommendation.imageUrl,
                            contentDescription = recommendation.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val details = listOfNotNull(
                        recommendation.year?.toString(),
                        recommendation.itemType?.takeIf { it.isNotBlank() },
                    ).joinToString(" • ")
                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = VantafynColors.Muted,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Watch Now Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VantafynGradients.accentHorizontal())
                    .clickable { onOpenMedia(recommendation.id) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Watch Now",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ── Composer Component ────────────────────────────────────────────────────────

@Composable
private fun ChatComposer(
    textInput: String,
    onTextChanged: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
    onOpenActionSheet: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isComposerFocused by remember { mutableStateOf(false) }
    val composerFocusProgress by animateFloatAsState(
        targetValue = if (isComposerFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "composerFocusProgress",
    )

    val isSendActive = textInput.isNotBlank() && !isSending
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val sendScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "sendScale",
    )

    val sendBgColor by animateColorAsState(
        targetValue = if (isSendActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "sendBgColor",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Character counter when typing a long message
        AnimatedVisibility(
            visible = textInput.length > CHAR_COUNTER_THRESHOLD,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = "${textInput.length}/$MAX_MESSAGE_LENGTH",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (textInput.length >= MAX_MESSAGE_LENGTH) Color(0xFFFF6688) else VantafynColors.Muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.End,
            )
        }

        // Glass Capsule Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF131724).copy(alpha = 0.96f),
                            Color(0xFF0E121E).copy(alpha = 0.96f),
                        ),
                    ),
                )
                .drawWithContent {
                    drawContent()
                    val stroke = 1.35.dp.toPx()
                    val glowStroke = 4.5.dp.toPx()
                    val corner = 26.dp.toPx()
                    val halfStroke = stroke / 2f

                    // Base subtle border when unfocused / transitioning
                    if (composerFocusProgress < 1f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.16f * (1f - composerFocusProgress)),
                                    Color.White.copy(alpha = 0.06f * (1f - composerFocusProgress)),
                                ),
                            ),
                            topLeft = Offset(halfStroke, halfStroke),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(corner, corner),
                            style = Stroke(width = stroke),
                        )
                    }

                    // Smooth animated gradient glow & crisp border when focused
                    if (composerFocusProgress > 0f) {
                        val glowBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF21D8FF).copy(alpha = 0.28f * composerFocusProgress),
                                Color(0xFF3E63FF).copy(alpha = 0.24f * composerFocusProgress),
                                Color(0xFF8B35FF).copy(alpha = 0.26f * composerFocusProgress),
                                Color(0xFFFF36C7).copy(alpha = 0.28f * composerFocusProgress),
                            ),
                            startX = 0f,
                            endX = size.width,
                        )
                        val borderBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF21D8FF).copy(alpha = composerFocusProgress),
                                Color(0xFF3E63FF).copy(alpha = composerFocusProgress),
                                Color(0xFF8B35FF).copy(alpha = composerFocusProgress),
                                Color(0xFFFF36C7).copy(alpha = composerFocusProgress),
                            ),
                            startX = 0f,
                            endX = size.width,
                        )
                        drawRoundRect(
                            brush = glowBrush,
                            topLeft = Offset(glowStroke / 2f, glowStroke / 2f),
                            size = Size(size.width - glowStroke, size.height - glowStroke),
                            cornerRadius = CornerRadius(corner, corner),
                            style = Stroke(width = glowStroke),
                        )
                        drawRoundRect(
                            brush = borderBrush,
                            topLeft = Offset(halfStroke, halfStroke),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(corner, corner),
                            style = Stroke(width = stroke),
                        )
                    }
                }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Contextual Action Button (+ / Quick Actions & Recommend Media)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .clickable(onClick = onOpenActionSheet),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Quick actions and recommend media",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Multiline Text Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 110.dp)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (textInput.isEmpty()) {
                    Text(
                        text = "Write a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VantafynColors.Muted,
                    )
                }

                BasicTextField(
                    value = textInput,
                    onValueChange = onTextChanged,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    cursorBrush = SolidColor(Color(0xFF00E5FF)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isComposerFocused = it.isFocused },
                    maxLines = 5,
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Animated Send Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .scale(sendScale)
                    .clip(CircleShape)
                    .background(
                        if (isSendActive) {
                            VantafynGradients.accentHorizontal()
                        } else {
                            SolidColor(sendBgColor)
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSendActive) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = isSendActive,
                        onClick = onSend,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send message",
                        tint = if (isSendActive) Color.White else VantafynColors.Muted.copy(alpha = 0.60f),
                        modifier = Modifier
                            .size(17.dp)
                            .padding(end = 1.dp),
                    )
                }
            }
        }
    }
}

// ── Action Sheet Modal (Quick Reactions + Recommend Movies/Shows) ──────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatActionBottomSheet(
    availableMedia: List<JellyfinMediaCard>,
    onDismiss: () -> Unit,
    onSendQuickReaction: (String) -> Unit,
    onSendMediaRecommendation: (JellyfinMediaCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mediaSearchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.70f),
        dragHandle = null,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .vantafynAnimatedModalBorder(
                    cornerRadius = 28.dp,
                    strokeWidth = 1.35.dp,
                    durationMillis = 4800,
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF141926).copy(alpha = 0.98f),
                            Color(0xFF0F1420).copy(alpha = 0.98f),
                            Color(0xFF080B12).copy(alpha = 0.98f),
                        ),
                    ),
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.2f, size.height * 0.15f),
                            radius = size.width * 0.6f,
                        ),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C4DFF).copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.75f),
                            radius = size.width * 0.6f,
                        ),
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

                // 1. Quick Reactions Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUICK REACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 0.8.sp,
                    )
                    val quickReactions = listOf("🔥", "❤️", "🍿", "🎬", "😂", "👏", "💯", "🚀", "😱", "🎧", "👍", "🎉", "🤩", "⚡")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(quickReactions) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .clickable { onSendQuickReaction(emoji) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // 2. Recommend Movies & Shows
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "RECOMMEND MOVIES & SHOWS",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA),
                            letterSpacing = 0.8.sp,
                        )
                        if (availableMedia.isNotEmpty()) {
                            Text(
                                text = "${availableMedia.size} titles",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = VantafynColors.Muted,
                            )
                        }
                    }

                    // Search Bar
                    var isSearchFocused by remember { mutableStateOf(false) }
                    val searchFocusProgress by animateFloatAsState(
                        targetValue = if (isSearchFocused) 1f else 0f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        label = "searchFocusProgress",
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .drawWithContent {
                                drawContent()
                                val stroke = 1.35.dp.toPx()
                                val glowStroke = 4.dp.toPx()
                                val corner = 16.dp.toPx()
                                val halfStroke = stroke / 2f

                                // Base subtle border when unfocused / transitioning
                                if (searchFocusProgress < 1f) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.12f * (1f - searchFocusProgress)),
                                        topLeft = Offset(halfStroke, halfStroke),
                                        size = Size(size.width - stroke, size.height - stroke),
                                        cornerRadius = CornerRadius(corner, corner),
                                        style = Stroke(width = stroke),
                                    )
                                }

                                // Smooth animated gradient glow & crisp border when focused
                                if (searchFocusProgress > 0f) {
                                    val glowBrush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF21D8FF).copy(alpha = 0.28f * searchFocusProgress),
                                            Color(0xFF3E63FF).copy(alpha = 0.24f * searchFocusProgress),
                                            Color(0xFF8B35FF).copy(alpha = 0.26f * searchFocusProgress),
                                            Color(0xFFFF36C7).copy(alpha = 0.28f * searchFocusProgress),
                                        ),
                                        startX = 0f,
                                        endX = size.width,
                                    )
                                    val borderBrush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF21D8FF).copy(alpha = searchFocusProgress),
                                            Color(0xFF3E63FF).copy(alpha = searchFocusProgress),
                                            Color(0xFF8B35FF).copy(alpha = searchFocusProgress),
                                            Color(0xFFFF36C7).copy(alpha = searchFocusProgress),
                                        ),
                                        startX = 0f,
                                        endX = size.width,
                                    )
                                    drawRoundRect(
                                        brush = glowBrush,
                                        topLeft = Offset(glowStroke / 2f, glowStroke / 2f),
                                        size = Size(size.width - glowStroke, size.height - glowStroke),
                                        cornerRadius = CornerRadius(corner, corner),
                                        style = Stroke(width = glowStroke),
                                    )
                                    drawRoundRect(
                                        brush = borderBrush,
                                        topLeft = Offset(halfStroke, halfStroke),
                                        size = Size(size.width - stroke, size.height - stroke),
                                        cornerRadius = CornerRadius(corner, corner),
                                        style = Stroke(width = stroke),
                                    )
                                }
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = if (searchFocusProgress > 0.5f) Color(0xFF00E5FF) else VantafynColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (mediaSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search server library...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VantafynColors.Muted,
                                    )
                                }
                                BasicTextField(
                                    value = mediaSearchQuery,
                                    onValueChange = { mediaSearchQuery = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF00E5FF)),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isSearchFocused = it.isFocused },
                                )
                            }
                            if (mediaSearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { mediaSearchQuery = "" },
                                    modifier = Modifier.size(22.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = VantafynColors.Muted,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }

                    val filteredMedia = remember(mediaSearchQuery, availableMedia) {
                        if (mediaSearchQuery.isBlank()) availableMedia
                        else availableMedia.filter { it.title.contains(mediaSearchQuery, ignoreCase = true) }
                    }

                    if (filteredMedia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.03f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (mediaSearchQuery.isBlank()) "No titles loaded from server yet" else "No results matching \"$mediaSearchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = VantafynColors.Muted,
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                        ) {
                            items(filteredMedia, key = { it.id.toString() }) { item ->
                                RecommendMediaTile(
                                    item = item,
                                    onSelect = { onSendMediaRecommendation(item) },
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
private fun RecommendMediaTile(
    item: JellyfinMediaCard,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = VantafynColors.Muted,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Media Recommendation Data Model & Serializer ─────────────────────────────

private data class MediaRecommendationPayload(
    val id: UUID,
    val title: String,
    val itemType: String?,
    val year: Int?,
    val imageUrl: String?,
)

private fun encodeMediaRecommendation(item: JellyfinMediaCard): String {
    val cleanTitle = item.title.replace("|", "").replace("\n", " ")
    val cleanType = item.itemType.orEmpty().replace("|", "")
    val cleanYear = item.year?.toString() ?: ""
    val cleanImage = item.imageUrl.orEmpty().replace("|", "")
    return "[media_rec|${item.id}|$cleanTitle|$cleanType|$cleanYear|$cleanImage]"
}

private fun parseMediaRecommendation(raw: String): MediaRecommendationPayload? {
    if (!raw.startsWith("[media_rec|") || !raw.endsWith("]")) return null
    val content = raw.removePrefix("[media_rec|").removeSuffix("]")
    val parts = content.split("|")
    if (parts.isEmpty()) return null
    val idStr = parts[0]
    val id = try { UUID.fromString(idStr) } catch (_: Exception) { return null }
    val title = parts.getOrNull(1).orEmpty()
    val itemType = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
    val year = parts.getOrNull(3)?.toIntOrNull()
    val imageUrl = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
    return MediaRecommendationPayload(id = id, title = title, itemType = itemType, year = year, imageUrl = imageUrl)
}

private fun parseReaction(raw: String): Pair<String, String>? {
    if (!raw.startsWith("[reaction|") || !raw.endsWith("]")) return null
    val content = raw.removePrefix("[reaction|").removeSuffix("]")
    val parts = content.split("|")
    if (parts.size < 2) return null
    val targetId = parts[0].trim()
    val emoji = parts[1].trim()
    if (targetId.isBlank() || emoji.isBlank()) return null
    return targetId to emoji
}

@Composable
private fun QuickReactionModal(
    targetMessage: JellyfinSocialMessage,
    onDismiss: () -> Unit,
    onSelectReaction: (String) -> Unit,
) {
    val emojis = listOf("❤️", "🔥", "😂", "🍿", "👍", "😮", "🎉", "👏")
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        modifier = Modifier
            .vantafynAnimatedModalBorder(cornerRadius = 28.dp, strokeWidth = 1.35.dp, durationMillis = 4000)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF141926).copy(alpha = 0.98f),
                        Color(0xFF0F1420).copy(alpha = 0.98f),
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "React to message",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.70f),
                    letterSpacing = 0.5.sp,
                )

                // Message preview snippet
                val previewText = remember(targetMessage.content) {
                    val mediaRec = parseMediaRecommendation(targetMessage.content)
                    when {
                        mediaRec != null -> "🎬 ${mediaRec.title}"
                        else -> targetMessage.content.take(60)
                    }
                }
                Text(
                    text = "\"$previewText\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = VantafynColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                // Emoji Reaction Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelectReaction(emoji)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

// ── Utility Formatting ────────────────────────────────────────────────────────

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
