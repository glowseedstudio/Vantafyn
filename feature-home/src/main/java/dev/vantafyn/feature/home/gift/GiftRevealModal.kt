package dev.vantafyn.feature.home.gift

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynSoundEffects
import dev.vantafyn.core.ui.rememberLifecycleAwareMarquee
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder

@Composable
fun GiftRevealModal(
    gift: VantafynCodeGift,
    queueCount: Int = 1,
    queueIndex: Int = 1,
    onClaim: (VantafynCodeGift) -> Unit,
    onDismiss: () -> Unit,
    onDismissAll: () -> Unit = onDismiss,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val accentColor = remember(gift.accentColorHex) { Color(gift.accentColorHex) }

    // Gated Animation Sequence (keyed to each gift)
    val descendAnim = remember { Animatable(0f) }
    val unfoldAnim = remember { Animatable(0f) }
    val glowBurstAnim = remember { Animatable(0f) }

    LaunchedEffect(gift.id) {
        descendAnim.snapTo(0f)
        unfoldAnim.snapTo(0f)
        glowBurstAnim.snapTo(0f)

        // Play unlock guide sound celebration
        try {
            VantafynSoundEffects.playUnlockGuide(context)
        } catch (_: Exception) {}

        // Step 1: Descend from top
        descendAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        )

        // Step 2: 3D Unfold
        unfoldAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        )

        // Step 3: Volumetric glow & highlight
        glowBurstAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        )
    }

    // Shimmer highlight for the unlock card
    val infiniteTransition = rememberInfiniteTransition(label = "giftCardShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    val descendVal = descendAnim.value
    val unfoldVal = unfoldAnim.value

    val translationYPx = with(density) { -((1f - descendVal) * 500.dp.toPx()) }
    val rotationX = -((1f - unfoldVal) * 85f)
    val scaleX = 0.82f + (unfoldVal * 0.18f)
    val scaleY = 0.65f + (unfoldVal * 0.35f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            val maxHeightPx = maxHeight

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = maxHeightPx * 0.92f)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .graphicsLayer {
                        this.translationY = translationYPx
                        this.rotationX = rotationX
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.transformOrigin = TransformOrigin(0.5f, 0.1f)
                        this.cameraDistance = 18f * density.density
                    }
                    .clickable(enabled = false) {} // Prevent click-through
            ) {
                // Volumetric ambient glow behind the modal
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha = (glowBurstAnim.value * 0.6f)
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.45f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(VantafynColors.Graphite.copy(alpha = 0.98f))
                        .border(
                            2.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = shimmerAlpha),
                                    accentColor.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f),
                                ),
                            ),
                            RoundedCornerShape(32.dp),
                        )
                        .vantafynAnimatedModalBorder(cornerRadius = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("✨", fontSize = 16.sp)
                            Text(
                                "SERVER SPECIAL GIFT",
                                color = accentColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                            )
                            if (queueCount > 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.22f))
                                        .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "$queueIndex OF $queueCount",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(onClick = onDismissAll),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close All",
                                tint = VantafynColors.Ink,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Sender Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!gift.senderAvatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = gift.senderAvatarUrl,
                                    contentDescription = gift.senderName,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = "Gift from ${gift.senderName}",
                                color = VantafynColors.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        }

                        // Franchise Poster Card
                        val resolvedPosterUrl = remember(gift.posterUrl, gift.franchiseCode) {
                            gift.posterUrl?.takeIf { it.isNotBlank() }
                                ?: VantafynGiftFranchises.all.firstOrNull { it.code.equals(gift.franchiseCode, ignoreCase = true) }?.posterUrl
                        }

                        if (!resolvedPosterUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 160.dp)
                                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(14.dp), spotColor = accentColor)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(2.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                                    .background(VantafynColors.SurfaceHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = resolvedPosterUrl,
                                    contentDescription = gift.franchiseTitle,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(42.dp),
                                )
                            }
                        }

                        // Franchise Titles
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Franchise badge pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = gift.franchiseBadge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = gift.franchiseTitle,
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = rememberLifecycleAwareMarquee(iterations = 2),
                            )
                        }

                        // Personal Note if provided
                        if (!gift.note.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = "\"${gift.note}\"",
                                    color = VantafynColors.Ink.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Golden Code Box with Shimmer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.22f),
                                            VantafynColors.SurfaceHigh.copy(alpha = 0.7f),
                                        ),
                                    ),
                                )
                                .border(1.5.dp, accentColor.copy(alpha = shimmerAlpha), RoundedCornerShape(20.dp))
                                .padding(vertical = 16.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "OFFICIAL UNLOCK CODE",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                            )
                            Text(
                                text = gift.franchiseCode,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp,
                            )
                            Text(
                                "Instantly unlocks the full interactive timeline & watch guide",
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Primary Claim Action
                        VantafynButton(
                            text = if (queueCount > 1) "Claim & Unlock Guide Now ($queueIndex of $queueCount)" else "Claim & Unlock Guide Now",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onClaim(gift) },
                        )

                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = VantafynColors.Muted),
                        ) {
                            Text(
                                text = if (queueCount > 1) "Save for Later (Show Next)" else "Save for Later",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
