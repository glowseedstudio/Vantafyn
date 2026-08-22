package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableStateOf(SocialTab.Messages) }
    var searchQuery by remember { mutableStateOf("") }
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
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    tint = VantafynColors.Ink.copy(alpha = 0.85f),
                )
            }
        }

        // Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SocialTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val badgeCount = when (tab) {
                    SocialTab.Messages -> totalUnreadMessages
                    SocialTab.Requests -> incomingRequests.size
                    else -> 0
                }
                VantafynGlassChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else VantafynColors.Muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else VantafynColors.Muted,
                        )
                        if (badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3366))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = badgeCount.toString(),
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

        // Main Tab Content
        if (isLoading) {
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
                                onAction = { selectedTab = SocialTab.Friends },
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
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
                                onAction = { selectedTab = SocialTab.Find },
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                            ) {
                                items(friends, key = { it.userId.toString() }) { friend ->
                                    FriendCard(
                                        friend = friend,
                                        onChat = { onOpenChat(friend) },
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
                                onAction = { selectedTab = SocialTab.Find },
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
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
                        val existingRequestUserIds = remember(requests) {
                            (requests.map { it.senderId } + requests.map { it.receiverId }).toSet()
                        }
                        val availableProfiles = remember(discoverableUsers, existingFriendUserIds, existingRequestUserIds) {
                            discoverableUsers.filter { it.userId !in existingFriendUserIds && it.userId !in existingRequestUserIds }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            item {
                                VantafynGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(16.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Add Friend by Username",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VantafynColors.Ink,
                                        )
                                        Text(
                                            text = "Send a friend request to any registered user on this Jellyfin server.",
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
                                        VantafynButton(
                                            text = "Send Friend Request",
                                            enabled = searchQuery.isNotBlank(),
                                            onClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    onSendRequest(searchQuery.trim())
                                                    searchQuery = ""
                                                    focusManager.clearFocus()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }

                            if (availableProfiles.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SERVER PROFILES (${availableProfiles.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = VantafynColors.Muted,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    )
                                }
                                items(availableProfiles, key = { it.userId.toString() }) { user ->
                                    DiscoverUserCard(
                                        user = user,
                                        onAdd = { onSendRequest(user.username) },
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
private fun DiscoverUserCard(
    user: JellyfinFriend,
    onAdd: () -> Unit,
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
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                AvatarCircle(
                    avatarUrl = user.avatarUrl,
                    name = user.displayName,
                    size = 48.dp,
                )
                if (user.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF55F0C0))
                            .border(2.dp, VantafynColors.Surface, CircleShape),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (user.isOnline) "Active Now" else "Server Member",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.isOnline) Color(0xFF55F0C0) else VantafynColors.Muted,
                )
            }
            VantafynGlassChip(
                selected = true,
                onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
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
