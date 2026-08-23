package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.vantafyn.core.jellyfin.JellyfinFriend
import dev.vantafyn.core.jellyfin.JellyfinFriendRequest
import dev.vantafyn.core.jellyfin.JellyfinSocialConversation
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassChip
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class SocialTab(val label: String, val icon: ImageVector) {
    Messages("Messages", Icons.Rounded.ChatBubbleOutline),
    Friends("Friends", Icons.Rounded.Group),
    Requests("Requests", Icons.Rounded.MailOutline),
    Find("Add Friend", Icons.Rounded.PersonAdd),
}

@Composable
fun SocialScreen(
    friends: List<JellyfinFriend>,
    requests: List<JellyfinFriendRequest>,
    conversations: List<JellyfinSocialConversation>,
    discoverableUsers: List<JellyfinFriend> = emptyList(),
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenChat: (JellyfinFriend) -> Unit,
    onOpenConversation: (JellyfinSocialConversation) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onRemoveFriend: (JellyfinFriend) -> Unit = {},
    selectedTab: SocialTab = SocialTab.Messages,
    onSelectTab: (SocialTab) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    val invitedUserIds = remember { mutableStateListOf<UUID>() }
    var showManualUsernameDialog by remember { mutableStateOf(false) }
    var activeFriendActionTarget by remember { mutableStateOf<JellyfinFriend?>(null) }
    var showRemoveConfirmationFor by remember { mutableStateOf<JellyfinFriend?>(null) }
    val focusManager = LocalFocusManager.current

    val incomingRequests = remember(requests) { requests.filter { it.isIncoming } }
    val outgoingRequests = remember(requests) { requests.filter { !it.isIncoming } }
    val totalUnreadMessages = remember(conversations) { conversations.sumOf { it.unreadCount } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = VantafynSpacing.md),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactBackButton(onClick = onBack)
                Text(
                    text = "Friends & Messages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                )
            }
            var isRefreshing by remember { mutableStateOf(false) }
            LaunchedEffect(isLoading) {
                if (!isLoading) isRefreshing = false
            }
            val refreshRotation by animateFloatAsState(
                targetValue = if (isRefreshing || isLoading) 360f else 0f,
                animationSpec = if (isRefreshing || isLoading) infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ) else tween(300),
                label = "refreshRotation",
            )
            IconButton(onClick = {
                isRefreshing = true
                onRefresh()
            }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    tint = if (isRefreshing || isLoading) VantafynColors.Primary else VantafynColors.Ink.copy(alpha = 0.85f),
                    modifier = Modifier.rotate(refreshRotation),
                )
            }
        }

        // Main Tab Content
        val showFullScreenLoading = isLoading &&
            friends.isEmpty() &&
            conversations.isEmpty() &&
            incomingRequests.isEmpty() &&
            outgoingRequests.isEmpty() &&
            discoverableUsers.isEmpty()

        if (showFullScreenLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = VantafynColors.Primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (selectedTab) {
                    SocialTab.Messages -> {
                        if (conversations.isEmpty()) {
                            SocialEmptyView(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                title = "No Messages Yet",
                                description = "Start a chat with one of your friends to share watch recommendations!",
                                actionText = "View Friends",
                                onAction = { onSelectTab(SocialTab.Friends) },
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 110.dp),
                            ) {
                                items(conversations, key = { it.conversationId }) { conv ->
                                    ConversationCard(
                                        conversation = conv,
                                        onClick = { onOpenConversation(conv) },
                                    )
                                }
                            }
                        }
                    }
                    SocialTab.Friends -> {
                        if (friends.isEmpty()) {
                            SocialEmptyView(
                                icon = Icons.Rounded.Group,
                                title = "No Friends Added",
                                description = "Connect with friends on this server to see their achievement ranks and send messages.",
                                actionText = "Add a Friend",
                                onAction = { onSelectTab(SocialTab.Find) },
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 110.dp),
                            ) {
                                items(friends, key = { it.userId.toString() }) { friend ->
                                    FriendCard(
                                        friend = friend,
                                        onChat = { onOpenChat(friend) },
                                        onLongPress = { activeFriendActionTarget = friend },
                                    )
                                }
                            }
                        }
                    }
                    SocialTab.Requests -> {
                        if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                            SocialEmptyView(
                                icon = Icons.Rounded.MailOutline,
                                title = "No Friend Requests",
                                description = "You have no pending incoming or outgoing friend requests.",
                                actionText = "Find Friends",
                                onAction = { onSelectTab(SocialTab.Find) },
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 110.dp),
                            ) {
                                if (incomingRequests.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "INCOMING (${incomingRequests.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = VantafynColors.Primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
                                    items(incomingRequests, key = { it.id }) { req ->
                                        FriendRequestCard(
                                            request = req,
                                            onAccept = { onAcceptRequest(req.id) },
                                            onDecline = { onDeclineRequest(req.id) },
                                        )
                                    }
                                }
                                if (outgoingRequests.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "OUTGOING (${outgoingRequests.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = VantafynColors.Muted,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
                                    items(outgoingRequests, key = { it.id }) { req ->
                                        OutgoingRequestCard(request = req)
                                    }
                                }
                            }
                        }
                    }
                    SocialTab.Find -> {
                        val existingFriendUserIds = remember(friends) { friends.map { it.userId }.toSet() }
                        val incomingRequestMap = remember(incomingRequests) { incomingRequests.associateBy { it.senderId } }
                        val outgoingRequestUserIds = remember(outgoingRequests) { outgoingRequests.map { it.receiverId }.toSet() }

                        val onlineProfiles = remember(discoverableUsers) { discoverableUsers.filter { it.isOnline } }
                        val offlineProfiles = remember(discoverableUsers) { discoverableUsers.filter { !it.isOnline } }

                        if (discoverableUsers.isEmpty()) {
                            SocialEmptyView(
                                icon = Icons.Rounded.PersonAdd,
                                title = "No Server Members Found",
                                description = "Search and connect with friends on your Jellyfin server by username.",
                                actionText = "Invite by Username",
                                onAction = { showManualUsernameDialog = true },
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 110.dp),
                            ) {
                                if (onlineProfiles.isNotEmpty()) {
                                    item {
                                        VantafynGlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            cornerRadius = 24.dp,
                                            contentPadding = PaddingValues(14.dp),
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 2.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF55F0C0)),
                                                    )
                                                    Text(
                                                        text = "ONLINE USERS (${onlineProfiles.size})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.sp,
                                                        color = Color(0xFF55F0C0),
                                                    )
                                                }

                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                ) {
                                                    onlineProfiles.forEach { user ->
                                                        val isFriend = user.userId in existingFriendUserIds
                                                        val incomingReq = incomingRequestMap[user.userId]
                                                        val isOutgoing = user.userId in outgoingRequestUserIds || user.userId in invitedUserIds

                                                        OnlineUserTile(
                                                            user = user,
                                                            isFriend = isFriend,
                                                            incomingRequestId = incomingReq?.id,
                                                            isOutgoingPending = isOutgoing,
                                                            onAdd = {
                                                                invitedUserIds.add(user.userId)
                                                                onSendRequest(user.userId.toString())
                                                            },
                                                            onAccept = { reqId -> onAcceptRequest(reqId) },
                                                            onOpenChat = { onOpenChat(user) },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (offlineProfiles.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "OTHER SERVER MEMBERS (${offlineProfiles.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = VantafynColors.Muted,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
                                    items(offlineProfiles, key = { it.userId.toString() }) { user ->
                                        val isFriend = user.userId in existingFriendUserIds
                                        val incomingReq = incomingRequestMap[user.userId]
                                        val isOutgoing = user.userId in outgoingRequestUserIds || user.userId in invitedUserIds

                                        DiscoverUserCard(
                                            user = user,
                                            isFriend = isFriend,
                                            incomingRequestId = incomingReq?.id,
                                            isOutgoingPending = isOutgoing,
                                            onAdd = {
                                                invitedUserIds.add(user.userId)
                                                onSendRequest(user.userId.toString())
                                            },
                                            onAccept = { reqId -> onAcceptRequest(reqId) },
                                            onOpenChat = { onOpenChat(user) },
                                        )
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { showManualUsernameDialog = true }
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = null,
                                                tint = VantafynColors.Muted,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = "Can't find someone? Invite by username",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VantafynColors.Muted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualUsernameDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualUsernameDialog = false
                searchQuery = ""
            },
            containerColor = Color(0xFF0D1322),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp),
            title = {
                Text(
                    text = "Invite Friend by Username",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the Jellyfin username of the person you'd like to invite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VantafynColors.Muted,
                    )
                    VantafynTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Username",
                        placeholder = "Enter Jellyfin username",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                VantafynButton(
                    text = "Send Invite",
                    enabled = searchQuery.isNotBlank(),
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onSendRequest(searchQuery.trim())
                            searchQuery = ""
                            showManualUsernameDialog = false
                            focusManager.clearFocus()
                        }
                    },
                )
            },
            dismissButton = {
                IconButton(onClick = {
                    showManualUsernameDialog = false
                    searchQuery = ""
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = VantafynColors.Muted,
                    )
                }
            },
        )
    }

    if (activeFriendActionTarget != null) {
        val target = activeFriendActionTarget!!
        FriendActionBottomSheet(
            friend = target,
            onDismiss = { activeFriendActionTarget = null },
            onOpenChat = {
                activeFriendActionTarget = null
                onOpenChat(target)
            },
            onRequestRemove = {
                activeFriendActionTarget = null
                showRemoveConfirmationFor = target
            },
        )
    }

    if (showRemoveConfirmationFor != null) {
        val friendToRemove = showRemoveConfirmationFor!!
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp),
            onDismissRequest = { showRemoveConfirmationFor = null },
            containerColor = Color(0xFF0D1322),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Remove Friend",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${friendToRemove.displayName} from your friends list? This will remove the friendship for both of you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VantafynColors.Muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toRemove = showRemoveConfirmationFor
                        showRemoveConfirmationFor = null
                        if (toRemove != null) {
                            onRemoveFriend(toRemove)
                        }
                    },
                ) {
                    Text(
                        text = "Remove",
                        color = Color(0xFFFF3366),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmationFor = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
        )
    }
}

@Composable
private fun ConversationCard(
    conversation: JellyfinSocialConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with Online dot
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                AvatarCircle(
                    avatarUrl = conversation.peerAvatarUrl,
                    name = conversation.peerName,
                    size = 50.dp,
                )
                if (conversation.peerIsOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF55F0C0))
                            .border(2.dp, VantafynColors.Surface, CircleShape),
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.peerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    conversation.lastMessageTimestamp?.let { ts ->
                        Text(
                            text = formatConversationDate(ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = VantafynColors.Muted,
                        )
                    }
                }
                Text(
                    text = conversation.lastMessageText ?: "Tap to start chatting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0) VantafynColors.Ink else VantafynColors.Muted,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Unread Badge
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFF3366))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: JellyfinFriend,
    onChat: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    VantafynGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onChat,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            ),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                AvatarCircle(
                    avatarUrl = friend.avatarUrl,
                    name = friend.displayName,
                    size = 50.dp,
                )
                if (friend.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF55F0C0))
                            .border(2.dp, VantafynColors.Surface, CircleShape),
                    )
                }
            }

            // Friend metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    friend.rankName?.let { rank ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.08f),
                        ) {
                            Text(
                                text = "Lvl ${friend.rankTier} • $rank",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VantafynColors.Primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = when {
                        !friend.currentlyWatching.isNullOrBlank() -> "Watching ${friend.currentlyWatching}"
                        friend.isOnline -> "Online"
                        !friend.lastSeen.isNullOrBlank() -> "Last active ${friend.lastSeen?.take(10)}"
                        else -> "Offline"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!friend.currentlyWatching.isNullOrBlank()) Color(0xFF55F0C0) else VantafynColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Chat action button
            IconButton(
                onClick = onChat,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = "Message ${friend.displayName}",
                    tint = VantafynColors.Ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendActionBottomSheet(
    friend: JellyfinFriend,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit,
    onRequestRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .padding(horizontal = 18.dp, vertical = 16.dp),
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

                // Header with friend profile preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarCircle(
                        avatarUrl = friend.avatarUrl,
                        name = friend.displayName,
                        size = 54.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = friend.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = VantafynColors.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            friend.rankName?.let { rank ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                ) {
                                    Text(
                                        text = "Lvl ${friend.rankTier} • $rank",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VantafynColors.Primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = when {
                                !friend.currentlyWatching.isNullOrBlank() -> "Watching ${friend.currentlyWatching}"
                                friend.isOnline -> "Online"
                                !friend.lastSeen.isNullOrBlank() -> "Last active ${friend.lastSeen?.take(10)}"
                                else -> "Offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!friend.currentlyWatching.isNullOrBlank()) Color(0xFF55F0C0) else VantafynColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Action 1: Send Message
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable {
                            onDismiss()
                            onOpenChat()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Send Message",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                // Action 2: Remove Friend (Danger)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF3366).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            onRequestRemove()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonRemove,
                        contentDescription = null,
                        tint = Color(0xFFFF4D6D),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Remove Friend",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF4D6D),
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: JellyfinFriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarCircle(
                avatarUrl = request.senderAvatarUrl,
                name = request.senderName,
                size = 48.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = request.senderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                )
                Text(
                    text = "Wants to be friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = VantafynColors.Muted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3366).copy(alpha = 0.15f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Decline",
                        tint = Color(0xFFFF6688),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF55F0C0).copy(alpha = 0.20f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Accept",
                        tint = Color(0xFF55F0C0),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(
    request: JellyfinFriendRequest,
    modifier: Modifier = Modifier,
) {
    VantafynGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarCircle(
                avatarUrl = request.senderAvatarUrl,
                name = request.receiverName,
                size = 44.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = request.receiverName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                )
                Text(
                    text = "Request pending...",
                    style = MaterialTheme.typography.bodySmall,
                    color = VantafynColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun AvatarCircle(
    avatarUrl: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VantafynColors.Ink,
            )
        }
    }
}

@Composable
private fun SocialEmptyView(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4000),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VantafynColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                VantafynButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OnlineUserTile(
    user: JellyfinFriend,
    isFriend: Boolean,
    incomingRequestId: String?,
    isOutgoingPending: Boolean,
    onAdd: () -> Unit,
    onAccept: (String) -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Avatar with Live Presence dot
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarCircle(
                avatarUrl = user.avatarUrl,
                name = user.displayName,
                size = 52.dp,
            )
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF55F0C0))
                    .border(2.dp, Color(0xFF0F1420), CircleShape),
            )
        }

        // Display Name
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        // Activity / Status
        Text(
            text = when {
                !user.currentlyWatching.isNullOrBlank() -> user.currentlyWatching.orEmpty()
                user.isOnline -> "Active now"
                else -> "Online"
            },
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.5.sp,
            color = Color(0xFF55F0C0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Action Button
        when {
            isFriend -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenChat)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "Chat",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
            incomingRequestId != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8B5CF6))
                        .clickable { onAccept(incomingRequestId) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Accept",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            isOutgoingPending -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = VantafynColors.Muted,
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VantafynGradients.accentHorizontal())
                        .clickable(onClick = onAdd)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "Invite",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverUserCard(
    user: JellyfinFriend,
    isFriend: Boolean,
    incomingRequestId: String?,
    isOutgoingPending: Boolean,
    onAdd: () -> Unit,
    onAccept: (String) -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                AvatarCircle(
                    avatarUrl = user.avatarUrl,
                    name = user.displayName,
                    size = 50.dp,
                )
                if (user.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF55F0C0))
                            .border(2.dp, VantafynColors.Surface, CircleShape),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    user.rankName?.let { rank ->
                        if (rank.isNotBlank() && rank != "Rookie" && rank != "Server Member") {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.08f),
                            ) {
                                Text(
                                    text = "Lvl ${user.rankTier}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = when {
                        !user.currentlyWatching.isNullOrBlank() -> "Watching ${user.currentlyWatching}"
                        user.isOnline -> "Active now"
                        else -> "Server member"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.isOnline) Color(0xFF55F0C0) else VantafynColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            when {
                isFriend -> {
                    IconButton(
                        onClick = onOpenChat,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Message",
                            tint = VantafynColors.Ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                incomingRequestId != null -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6))
                            .clickable { onAccept(incomingRequestId) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                isOutgoingPending -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                    ) {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.labelMedium,
                            color = VantafynColors.Muted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VantafynGradients.accentHorizontal())
                            .clickable(onClick = onAdd)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = "Invite",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Invite",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatConversationDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
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
            val nowCal = Calendar.getInstance()
            val msgCal = Calendar.getInstance().apply { time = parsedDate }
            when {
                nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> {
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(parsedDate)
                }
                nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> {
                    "Yesterday"
                }
                nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(parsedDate)
                }
                else -> {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsedDate)
                }
            }
        } else {
            trimmed.take(10)
        }
    } catch (_: Exception) {
        isoString.take(10)
    }
}
