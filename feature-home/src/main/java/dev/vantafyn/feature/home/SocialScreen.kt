package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.DeleteOutline
import dev.vantafyn.core.ui.rememberLifecycleAwareMarquee
import dev.vantafyn.core.ui.VantafynSkeletonBlock
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
    blockedUsers: List<dev.vantafyn.core.jellyfin.JellyfinBlockedUser> = emptyList(),
    onBlockUser: (java.util.UUID, String, String, String?) -> Unit = { _, _, _, _ -> },
    onUnblockUser: (java.util.UUID) -> Unit = {},
    onDeleteConversation: (JellyfinSocialConversation) -> Unit = {},
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
    var showBlockConfirmationFor by remember { mutableStateOf<Triple<UUID, String, String>?>(null) }
    var showDeleteConversationConfirmationFor by remember { mutableStateOf<JellyfinSocialConversation?>(null) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vantafyn_social_prefs", Context.MODE_PRIVATE) }
    var starredConversationIds by remember {
        mutableStateOf(prefs.getStringSet("starred_conversations", emptySet())?.toSet() ?: emptySet())
    }
    var activeConversationActionTarget by remember { mutableStateOf<JellyfinSocialConversation?>(null) }

    fun toggleStarConversation(convId: String) {
        val updated = if (convId in starredConversationIds) {
            starredConversationIds - convId
        } else {
            starredConversationIds + convId
        }
        starredConversationIds = updated
        prefs.edit().putStringSet("starred_conversations", updated).apply()
    }

    val sortedConversations = remember(conversations, starredConversationIds) {
        conversations.sortedWith(
            compareByDescending<JellyfinSocialConversation> { it.conversationId in starredConversationIds }
                .thenByDescending { it.lastMessageTimestamp }
        )
    }

    val incomingRequests = remember(requests) { requests.filter { it.isIncoming } }
    val outgoingRequests = remember(requests) { requests.filter { !it.isIncoming } }
    val totalUnreadMessages = remember(conversations) { conversations.sumOf { it.unreadCount } }

    val reducedMotion = rememberReducedMotionPreference()
    var revealProgress by remember { mutableFloatStateOf(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            val anim = Animatable(0f)
            anim.animateTo(1f, animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)) {
                revealProgress = value
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = VantafynSpacing.md)
            .graphicsLayer {
                alpha = revealProgress
                translationY = (1f - revealProgress) * 28.dp.toPx()
            },
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (blockedUsers.isNotEmpty()) {
                    IconButton(onClick = { showBlockedUsersSheet = true }) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "Blocked Users",
                                tint = Color(0xFFFF4D6D),
                                modifier = Modifier.size(22.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3366)),
                            )
                        }
                    }
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
        }

        // Main Tab Content
        val showFullScreenLoading = isLoading &&
            friends.isEmpty() &&
            conversations.isEmpty() &&
            incomingRequests.isEmpty() &&
            outgoingRequests.isEmpty() &&
            discoverableUsers.isEmpty()

        if (showFullScreenLoading) {
            when (selectedTab) {
                SocialTab.Messages -> SocialMessagesSkeleton()
                SocialTab.Friends -> SocialFriendsSkeleton()
                SocialTab.Requests -> SocialRequestsSkeleton()
                SocialTab.Find -> SocialFindSkeleton()
            }
        } else {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(160)).togetherWith(fadeOut(tween(120))).using(SizeTransform(clip = false))
                    } else {
                        (
                            fadeIn(animationSpec = tween(220, delayMillis = 20, easing = FastOutSlowInEasing)) +
                                slideInVertically(animationSpec = tween(220, delayMillis = 20, easing = FastOutSlowInEasing)) { it / 14 }
                        ).togetherWith(
                            fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                        ).using(SizeTransform(clip = false))
                    }
                },
                label = "socialTabContentTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { currentTab ->
                when (currentTab) {
                    SocialTab.Messages -> {
                        if (isLoading && sortedConversations.isEmpty()) {
                            SocialMessagesSkeleton()
                        } else if (sortedConversations.isEmpty()) {
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
                                items(sortedConversations, key = { it.conversationId }) { conv ->
                                    val isStarred = conv.conversationId in starredConversationIds
                                    ConversationCard(
                                        conversation = conv,
                                        isStarred = isStarred,
                                        onClick = { onOpenConversation(conv) },
                                        onLongClick = { activeConversationActionTarget = conv },
                                    )
                                }
                            }
                        }
                    }
                    SocialTab.Friends -> {
                        if (isLoading && friends.isEmpty()) {
                            SocialFriendsSkeleton()
                        } else if (friends.isEmpty()) {
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
                        if (isLoading && incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                            SocialRequestsSkeleton()
                        } else if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
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

                        if (isLoading && discoverableUsers.isEmpty()) {
                            SocialFindSkeleton()
                        } else if (discoverableUsers.isEmpty()) {
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
            onRequestBlock = {
                activeFriendActionTarget = null
                showBlockConfirmationFor = Triple(target.userId, target.displayName, target.username)
            },
        )
    }

    if (activeConversationActionTarget != null) {
        val target = activeConversationActionTarget!!
        val isStarred = target.conversationId in starredConversationIds
        ConversationActionBottomSheet(
            conversation = target,
            isStarred = isStarred,
            onDismiss = { activeConversationActionTarget = null },
            onToggleStar = { toggleStarConversation(target.conversationId) },
            onOpenChat = {
                activeConversationActionTarget = null
                onOpenConversation(target)
            },
            onRequestDelete = {
                activeConversationActionTarget = null
                showDeleteConversationConfirmationFor = target
            },
            onRequestBlock = {
                activeConversationActionTarget = null
                showBlockConfirmationFor = Triple(target.peerUserId, target.peerName, target.peerName)
            },
        )
    }

    if (showDeleteConversationConfirmationFor != null) {
        val conv = showDeleteConversationConfirmationFor!!
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp),
            onDismissRequest = { showDeleteConversationConfirmationFor = null },
            containerColor = Color(0xFF0D1322),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3366).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF4D6D),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "Clear Chat?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to clear your conversation history with ${conv.peerName}? Past messages will be removed from your chat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VantafynColors.Muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = showDeleteConversationConfirmationFor
                        showDeleteConversationConfirmationFor = null
                        if (toDelete != null) {
                            onDeleteConversation(toDelete)
                        }
                    },
                ) {
                    Text(
                        text = "Clear Chat",
                        color = Color(0xFFFF3366),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConversationConfirmationFor = null }) {
                    Text("Cancel", color = Color.White)
                }
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

    if (showBlockConfirmationFor != null) {
        val target = showBlockConfirmationFor!!
        val targetDisplayName = target.second
        AlertDialog(
            modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 24.dp, strokeWidth = 1.3.dp),
            onDismissRequest = { showBlockConfirmationFor = null },
            containerColor = Color(0xFF0D1322),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3366).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = Color(0xFFFF3366),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "Block $targetDisplayName?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            },
            text = {
                Text(
                    text = "They will be removed from your friends list, their messages and invites will be blocked, and they won't be able to contact you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VantafynColors.Muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = showBlockConfirmationFor
                        showBlockConfirmationFor = null
                        if (current != null) {
                            onBlockUser(current.first, current.second, current.third, null)
                        }
                    },
                ) {
                    Text(
                        text = "Block",
                        color = Color(0xFFFF3366),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmationFor = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
        )
    }

    if (showBlockedUsersSheet) {
        BlockedUsersBottomSheet(
            blockedUsers = blockedUsers,
            onDismiss = { showBlockedUsersSheet = false },
            onUnblock = { uId ->
                onUnblockUser(uId)
            },
        )
    }
}

@Composable
private fun ConversationCard(
    conversation: JellyfinSocialConversation,
    isStarred: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    VantafynGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isStarred) {
                    Modifier.border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = 0.45f),
                                Color(0xFFFFA000).copy(alpha = 0.20f),
                            )
                        ),
                        RoundedCornerShape(20.dp),
                    )
                } else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = conversation.peerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VantafynColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isStarred) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Starred",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    conversation.lastMessageTimestamp?.let { ts ->
                        Text(
                            text = formatConversationDate(ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isStarred) Color(0xFFFFD700).copy(alpha = 0.85f) else VantafynColors.Muted,
                        )
                    }
                }
                Text(
                    text = dev.vantafyn.core.jellyfin.formatSocialSnippet(conversation.lastMessageText),
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
                        .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3366))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.5.sp,
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
                val isWatching = !friend.currentlyWatching.isNullOrBlank()
                val statusText = when {
                    isWatching -> "Watching ${friend.currentlyWatching}"
                    friend.isOnline -> "Online"
                    !friend.lastSeen.isNullOrBlank() -> "Last active ${friend.lastSeen?.take(10)}"
                    else -> "Offline"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isWatching) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF55F0C0),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWatching) Color(0xFF55F0C0) else VantafynColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = if (isWatching) {
                            rememberLifecycleAwareMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 2000,
                                repeatDelayMillis = 3500,
                            )
                        } else Modifier,
                    )
                }
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
    onRequestBlock: () -> Unit,
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
                        val isWatching = !friend.currentlyWatching.isNullOrBlank()
                        val statusText = when {
                            isWatching -> "Watching ${friend.currentlyWatching}"
                            friend.isOnline -> "Online"
                            !friend.lastSeen.isNullOrBlank() -> "Last active ${friend.lastSeen?.take(10)}"
                            else -> "Offline"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (isWatching) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF55F0C0),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isWatching) Color(0xFF55F0C0) else VantafynColors.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = if (isWatching) {
                                    rememberLifecycleAwareMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 2000,
                                        repeatDelayMillis = 3500,
                                    )
                                } else Modifier,
                            )
                        }
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

                // Action 2: Remove Friend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
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
                        tint = VantafynColors.Ink.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Remove Friend",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                // Action 3: Block User (Danger)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF3366).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            onRequestBlock()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = Color(0xFFFF4D6D),
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Block User",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF4D6D),
                        )
                        Text(
                            text = "Unfriend and prevent future messages & invites",
                            style = MaterialTheme.typography.labelSmall,
                            color = VantafynColors.Muted,
                        )
                    }
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
        val isWatching = !user.currentlyWatching.isNullOrBlank()
        val activityText = when {
            isWatching -> user.currentlyWatching.orEmpty()
            user.isOnline -> "Active now"
            else -> "Online"
        }
        Text(
            text = activityText,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.5.sp,
            color = Color(0xFF55F0C0),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = if (isWatching) {
                rememberLifecycleAwareMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2000,
                    repeatDelayMillis = 3500,
                )
            } else Modifier,
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
                val isWatching = !user.currentlyWatching.isNullOrBlank()
                val statusText = when {
                    isWatching -> "Watching ${user.currentlyWatching}"
                    user.isOnline -> "Active now"
                    else -> "Server member"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isWatching) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF55F0C0),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWatching || user.isOnline) Color(0xFF55F0C0) else VantafynColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = if (isWatching) {
                            rememberLifecycleAwareMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 2000,
                                repeatDelayMillis = 3500,
                            )
                        } else Modifier,
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationActionBottomSheet(
    conversation: JellyfinSocialConversation,
    isStarred: Boolean,
    onDismiss: () -> Unit,
    onToggleStar: () -> Unit,
    onOpenChat: () -> Unit,
    onRequestDelete: () -> Unit,
    onRequestBlock: () -> Unit,
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
                        )
                    )
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header: Avatar + Peer Name
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.BottomEnd) {
                        AvatarCircle(
                            avatarUrl = conversation.peerAvatarUrl,
                            name = conversation.peerName,
                            size = 48.dp,
                        )
                        if (conversation.peerIsOnline) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF55F0C0))
                                    .border(1.5.dp, VantafynColors.Surface, CircleShape),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            if (isStarred) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "Starred",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(
                            text = if (isStarred) "Starred & Pinned to Top" else "Direct Message",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStarred) Color(0xFFFFD700).copy(alpha = 0.90f) else VantafynColors.Muted,
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Star / Unstar
                    VantafynGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleStar()
                                onDismiss()
                            },
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isStarred) Color(0xFFFFD700).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = null,
                                    tint = if (isStarred) Color(0xFFFFD700) else VantafynColors.Ink,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = if (isStarred) "Unstar Conversation" else "Star & Pin to Top",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isStarred) Color(0xFFFFD700) else VantafynColors.Ink,
                                )
                                Text(
                                    text = if (isStarred) "Remove from pinned favorites" else "Pin conversation to the top of your messages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VantafynColors.Muted,
                                )
                            }
                        }
                    }

                    // Open Chat
                    VantafynGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenChat()
                            },
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Open Chat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VantafynColors.Ink,
                                )
                                Text(
                                    text = "View full message history and send messages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VantafynColors.Muted,
                                )
                            }
                        }
                    }

                    // Clear Chat
                    VantafynGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onRequestDelete()
                            },
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3366).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4D6D),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Clear Chat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF4D6D),
                                )
                                Text(
                                    text = "Delete chat history and remove conversation thread",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VantafynColors.Muted,
                                )
                            }
                        }
                    }

                    // Block User
                    VantafynGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onRequestBlock()
                            },
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3366).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4D6D),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Block User",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF4D6D),
                                )
                                Text(
                                    text = "Unfriend and block messages and future invites",
                                    style = MaterialTheme.typography.labelSmall,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedUsersBottomSheet(
    blockedUsers: List<dev.vantafyn.core.jellyfin.JellyfinBlockedUser>,
    onDismiss: () -> Unit,
    onUnblock: (UUID) -> Unit,
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
                // Drag handle
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .align(Alignment.CenterHorizontally),
                )

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3366).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFF4D6D),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Blocked Users",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "${blockedUsers.size} user${if (blockedUsers.size != 1) "s" else ""} blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = VantafynColors.Muted,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = VantafynColors.Ink.copy(alpha = 0.7f),
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                if (blockedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = VantafynColors.Muted.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                text = "No blocked users",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VantafynColors.Muted,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(blockedUsers, key = { it.userId }) { user ->
                            VantafynGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        AvatarCircle(
                                            avatarUrl = user.avatarUrl,
                                            name = user.displayName,
                                            size = 42.dp,
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(
                                                text = user.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "@${user.username}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VantafynColors.Muted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onUnblock(user.userId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.10f),
                                            contentColor = Color(0xFF55F0C0),
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "Unblock",
                                            style = MaterialTheme.typography.labelMedium,
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
    }
}

@Composable
private fun SocialMessagesSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 110.dp),
    ) {
        repeat(4) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VantafynSkeletonBlock(Modifier.size(50.dp), cornerRadius = 999.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VantafynSkeletonBlock(Modifier.width(110.dp).height(16.dp), cornerRadius = 6.dp)
                            VantafynSkeletonBlock(Modifier.width(42.dp).height(10.dp), cornerRadius = 4.dp)
                        }
                        VantafynSkeletonBlock(Modifier.fillMaxWidth(0.72f).height(13.dp), cornerRadius = 4.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialFriendsSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 110.dp),
    ) {
        repeat(4) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VantafynSkeletonBlock(Modifier.size(50.dp), cornerRadius = 999.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VantafynSkeletonBlock(Modifier.width(100.dp).height(16.dp), cornerRadius = 6.dp)
                            VantafynSkeletonBlock(Modifier.width(60.dp).height(18.dp), cornerRadius = 999.dp)
                        }
                        VantafynSkeletonBlock(Modifier.width(80.dp).height(12.dp), cornerRadius = 4.dp)
                    }
                    VantafynSkeletonBlock(Modifier.size(40.dp), cornerRadius = 999.dp)
                }
            }
        }
    }
}

@Composable
private fun SocialRequestsSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 110.dp),
    ) {
        repeat(3) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VantafynSkeletonBlock(Modifier.size(46.dp), cornerRadius = 999.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VantafynSkeletonBlock(Modifier.width(110.dp).height(15.dp), cornerRadius = 6.dp)
                        VantafynSkeletonBlock(Modifier.width(80.dp).height(11.dp), cornerRadius = 4.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VantafynSkeletonBlock(Modifier.size(36.dp), cornerRadius = 999.dp)
                        VantafynSkeletonBlock(Modifier.size(36.dp), cornerRadius = 999.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialFindSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 110.dp),
    ) {
        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VantafynSkeletonBlock(Modifier.width(120.dp).height(14.dp), cornerRadius = 4.dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(3) {
                        VantafynSkeletonBlock(Modifier.weight(1f).height(100.dp), cornerRadius = 18.dp)
                    }
                }
            }
        }
        repeat(3) {
            VantafynGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VantafynSkeletonBlock(Modifier.size(46.dp), cornerRadius = 999.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VantafynSkeletonBlock(Modifier.width(120.dp).height(15.dp), cornerRadius = 6.dp)
                        VantafynSkeletonBlock(Modifier.width(70.dp).height(11.dp), cornerRadius = 4.dp)
                    }
                    VantafynSkeletonBlock(Modifier.width(70.dp).height(34.dp), cornerRadius = 999.dp)
                }
            }
        }
    }
}
