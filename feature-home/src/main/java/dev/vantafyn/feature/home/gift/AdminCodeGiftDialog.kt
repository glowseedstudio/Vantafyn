package dev.vantafyn.feature.home.gift

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinAdminUser
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynSoundEffects
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.rememberLifecycleAwareMarquee
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AdminCodeGiftDialog(
    users: List<JellyfinAdminUser>,
    isLoadingUsers: Boolean,
    currentUserId: UUID?,
    onSendGift: (recipientId: UUID, recipientName: String, franchiseCode: String, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUser by remember { mutableStateOf<JellyfinAdminUser?>(null) }
    var selectedFranchise by remember { mutableStateOf<VantafynGiftFranchise?>(null) }
    var giftNote by remember { mutableStateOf("") }
    var isDispatching by remember { mutableStateOf(false) }

    // Pre-select first non-self user if available
    LaunchedEffect(users, currentUserId) {
        if (selectedUser == null) {
            selectedUser = users.firstOrNull { it.id != currentUserId } ?: users.firstOrNull()
        }
    }

    // 3D Origami Folding & Upward Woosh Animation States
    val foldAnim = remember { Animatable(0f) }
    val wooshAnim = remember { Animatable(0f) }

    fun triggerDispatch() {
        val user = selectedUser ?: return
        val franchise = selectedFranchise ?: return
        if (isDispatching) return
        isDispatching = true

        coroutineScope.launch {
            // Play message sent audio
            try {
                VantafynSoundEffects.preload(context)
            } catch (_: Exception) {}

            // Phase 1: 3D Origami Fold in place
            foldAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )

            // Phase 2: High velocity Woosh Upwards off top of screen
            wooshAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutLinearInEasing),
            )

            // Dispatch and dismiss
            onSendGift(
                user.id,
                user.name,
                franchise.code,
                giftNote.trim().takeIf { it.isNotBlank() },
            )
            onDismiss()
        }
    }

    val foldVal = foldAnim.value
    val wooshVal = wooshAnim.value

    // 3D perspective calculation
    val scaleX = 1f - (foldVal * 0.32f) - (wooshVal * 0.15f)
    val scaleY = (1f - (foldVal * 0.45f)) * (1f + (wooshVal * 0.35f)) // Stretches on woosh
    val rotationX = -(foldVal * 42f)
    val translationYPx = with(density) { -(wooshVal * 1800.dp.toPx()) }
    val alphaVal = (1f - (wooshVal * 1.5f)).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = { if (!isDispatching) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxHeightPx = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .heightIn(max = maxHeightPx * 0.94f)
                    .graphicsLayer {
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.rotationX = rotationX
                        this.translationY = translationYPx
                        this.alpha = alphaVal
                        this.transformOrigin = TransformOrigin(0.5f, 0.4f)
                        this.cameraDistance = 16f * density.density
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(VantafynColors.Graphite.copy(alpha = 0.98f))
                    .vantafynAnimatedModalBorder(cornerRadius = 32.dp),
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFD4AF37).copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD4AF37).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFD4AF37))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        "ADMIN SERVICES",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                    )
                                    Text(
                                        "|",
                                        color = Color.Black.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Light,
                                        fontSize = 10.sp,
                                    )
                                    Text(
                                        "GIFT DISPATCH",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp,
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    "Send Franchise Unlock Gift",
                                    color = VantafynColors.Ink,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(enabled = !isDispatching, onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VantafynColors.Ink,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding(),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Section 1: Choose Recipient
                    item {
                        Text(
                            "1. SELECT SERVER RECIPIENT",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isLoadingUsers && users.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFD4AF37),
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else if (users.isEmpty()) {
                            Text(
                                "No other server users found.",
                                color = VantafynColors.Muted,
                                fontSize = 13.sp,
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(users, key = { it.id }) { user ->
                                    val isSelected = selectedUser?.id == user.id
                                    val isSelf = user.id == currentUserId

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) Color(0xFFD4AF37).copy(alpha = 0.18f)
                                                else VantafynColors.SurfaceHigh.copy(alpha = 0.5f),
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) Color(0xFFD4AF37)
                                                else Color.White.copy(alpha = 0.08f),
                                                RoundedCornerShape(14.dp),
                                            )
                                            .clickable { selectedUser = user }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier.size(44.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!user.imageUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user.imageUrl,
                                                    contentDescription = user.name,
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(VantafynColors.SurfaceHigh),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Person,
                                                        contentDescription = null,
                                                        tint = VantafynColors.Muted,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                            }

                                            if (user.isAdministrator) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFD4AF37)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.AdminPanelSettings,
                                                        contentDescription = "Admin",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp),
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        key(user.id, isSelected) {
                                            Text(
                                                text = if (isSelf) "${user.name} (You)" else user.name,
                                                color = if (isSelected) Color(0xFFD4AF37) else VantafynColors.Ink,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                modifier = rememberLifecycleAwareMarquee(iterations = 2),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Choose Franchise to Gift
                    item {
                        Text(
                            "2. SELECT WATCH GUIDE TO GIFT",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VantafynGiftFranchises.all.forEach { franchise ->
                                val isSelected = selectedFranchise?.code == franchise.code
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) franchise.accentColor.copy(alpha = 0.22f)
                                            else VantafynColors.SurfaceHigh.copy(alpha = 0.45f),
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) franchise.accentColor
                                            else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable { selectedFranchise = franchise }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(VantafynColors.SurfaceHigh),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (franchise.posterUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = franchise.posterUrl,
                                                contentDescription = franchise.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Movie,
                                                contentDescription = null,
                                                tint = franchise.accentColor,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = franchise.title,
                                            color = if (isSelected) franchise.accentColor else VantafynColors.Ink,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${franchise.movieCount} films • ${franchise.eraSubtitle} • Code: ${franchise.code}",
                                            color = VantafynColors.Muted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = franchise.accentColor,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Optional Personal Note
                    item {
                        Text(
                            "3. PERSONAL GIFT NOTE (OPTIONAL)",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        VantafynTextField(
                            value = giftNote,
                            onValueChange = { giftNote = it },
                            label = "Personal Note (Optional)",
                            placeholder = "e.g., Enjoy the marathon over the weekend! - Admin",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Bottom Dispatch Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                ) {
                    val canSend = selectedUser != null && selectedFranchise != null && !isDispatching
                    val recipientName = selectedUser?.name ?: "Recipient"
                    val franchiseName = selectedFranchise?.title ?: "Franchise"

                    VantafynButton(
                        text = if (isDispatching) "Dispatching Gift..." else "Send Gift to $recipientName",
                        enabled = canSend,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { triggerDispatch() },
                    )
                }
            }
        }
    }
}
