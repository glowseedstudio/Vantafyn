package dev.vantafyn.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinAchievement
import dev.vantafyn.core.jellyfin.JellyfinAchievementRarity
import dev.vantafyn.core.jellyfin.JellyfinAchievementSummary
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassChip
import dev.vantafyn.core.ui.VantafynGlassSurface
import dev.vantafyn.core.ui.VantafynGlassVariant
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.vantafynAnimatedModalBorder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private enum class AchievementFilter(val label: String) {
    All("All"),
    Unlocked("Unlocked"),
    Locked("Locked"),
}

@Composable
fun AchievementsScreen(
    userName: String,
    userImageUrl: String?,
    summary: JellyfinAchievementSummary?,
    achievements: List<JellyfinAchievement>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedFilter by remember { mutableStateOf(AchievementFilter.All) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var detailAchievement by remember { mutableStateOf<JellyfinAchievement?>(null) }
    val gridState = rememberLazyGridState()

    val categories = remember(achievements) {
        achievements.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredAchievements = remember(achievements, selectedFilter, selectedCategory) {
        achievements.filter { item ->
            val filterMatch = when (selectedFilter) {
                AchievementFilter.All -> true
                AchievementFilter.Unlocked -> item.isUnlocked
                AchievementFilter.Locked -> !item.isUnlocked
            }
            val categoryMatch = selectedCategory == null || item.category == selectedCategory
            filterMatch && categoryMatch
        }
    }

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

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .graphicsLayer {
                    alpha = revealProgress
                    translationY = (1f - revealProgress) * 28.dp.toPx()
                },
        ) {
            // Top action bar with chevron back button and refresh
            AchievementsHeaderBar(
                summary = summary,
                onBack = onBack,
                onRefresh = onRetry,
                isLoading = isLoading,
            )

            if (isLoading && achievements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = VantafynColors.Primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else if (achievements.isEmpty()) {
                AchievementsUnavailableView(
                    message = error ?: "Install and enable the Achievement Badges for Jellyfin plugin to unlock badges, rank tiers, and milestones.",
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = VantafynSpacing.lg,
                        end = VantafynSpacing.lg,
                        top = VantafynSpacing.xs,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    // Centered User Profile Avatar & Name
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        UserProfileHeader(
                            userName = userName,
                            userImageUrl = userImageUrl,
                        )
                    }

                    // Progression summary card with app gradient progress
                    if (summary != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AchievementProgressionHero(summary = summary)
                        }
                    }

                    // Filter controls with Vantafyn animated-border glass pills
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AchievementFilterStrip(
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = {
                                selectedCategory = if (selectedCategory == it) null else it
                            },
                        )
                    }

                    // Empty filter state
                    if (filteredAchievements.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No achievements found in this category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VantafynColors.Muted,
                                )
                            }
                        }
                    } else {
                        items(filteredAchievements, key = { it.id }) { achievement ->
                            AchievementCard(
                                achievement = achievement,
                                onClick = { detailAchievement = achievement },
                            )
                        }
                    }
                }
            }
        }

        detailAchievement?.let { achievement ->
            AchievementDetailDialog(
                achievement = achievement,
                onDismiss = { detailAchievement = null },
            )
        }
    }
}

@Composable
private fun UserProfileHeader(
    userName: String,
    userImageUrl: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Centered user profile avatar
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .vantafynAnimatedModalBorder(cornerRadius = 43.dp, strokeWidth = 2.dp)
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!userImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userImageUrl,
                    contentDescription = userName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = extractUserInitials(userName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = VantafynColors.Ink,
                )
            }
        }

        // Centered user name
        Text(
            text = userName.ifBlank { "Vantafyn User" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = VantafynColors.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AchievementsHeaderBar(
    summary: JellyfinAchievementSummary?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VantafynSpacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CompactBackButton(onClick = onBack)

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (summary != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.75f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${summary.currentScore} PTS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                        )
                    }
                }
            }
            Spacer(Modifier.width(VantafynSpacing.xs))
            IconButton(onClick = onRefresh) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = VantafynColors.Primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = VantafynColors.Ink.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressionHero(summary: JellyfinAchievementSummary) {
    val progressAnimated by animateFloatAsState(
        targetValue = if (summary.totalCount > 0) summary.unlockedCount.toFloat() / summary.totalCount.toFloat() else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "achievementProgress",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        VantafynColors.SurfaceHigh.copy(alpha = 0.75f),
                        VantafynColors.Surface.copy(alpha = 0.85f),
                    ),
                ),
            )
            .vantafynAnimatedModalBorder(cornerRadius = 20.dp, strokeWidth = 1.2.dp)
            .padding(VantafynSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            text = summary.rankName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = VantafynColors.Ink,
                        )
                        Text(
                            text = "Tier ${summary.rankTier}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VantafynColors.Muted,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.unlockedCount} / ${summary.totalCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                    )
                    Text(
                        text = "${summary.progressPercentage}% Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = VantafynColors.Muted,
                    )
                }
            }

            // Custom Vantafyn gradient progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    if (progressAnimated > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnimated.coerceIn(0.02f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(VantafynGradients.accentHorizontal()),
                        )
                    }
                }
                val nextRankScore = summary.nextRankScore
                if (nextRankScore != null && nextRankScore > summary.currentScore) {
                    val remaining = nextRankScore - summary.currentScore
                    Text(
                        text = "$remaining PTS to next rank",
                        style = MaterialTheme.typography.labelSmall,
                        color = VantafynColors.Muted.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementFilterStrip(
    selectedFilter: AchievementFilter,
    onSelectFilter: (AchievementFilter) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Status filter segmented chip rail (All, Unlocked, Locked) with gradient border highlight
        VantafynGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = VantafynGlassVariant.Chip,
            enabled = true,
            cornerRadius = 999.dp,
            contentPadding = PaddingValues(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AchievementFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val shape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.vantafynAnimatedModalBorder(cornerRadius = 999.dp, strokeWidth = 1.3.dp, durationMillis = 4200)
                                } else {
                                    Modifier.clip(shape)
                                },
                            )
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { onSelectFilter(filter) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Category pills
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    VantafynGlassChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(category) },
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: JellyfinAchievement,
    onClick: () -> Unit,
) {
    val isLocked = !achievement.isUnlocked
    val isHiddenLocked = isLocked && achievement.isHidden
    val rarityColor = achievement.rarity.toColor()
    val vectorIcon = remember(achievement.iconName, achievement.name, achievement.category) {
        resolveAchievementIcon(achievement.iconName, achievement.name, achievement.category)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (achievement.isUnlocked) {
                    VantafynColors.SurfaceHigh.copy(alpha = 0.65f)
                } else {
                    VantafynColors.Surface.copy(alpha = 0.45f)
                },
            )
            .border(
                1.dp,
                if (achievement.isUnlocked) {
                    rarityColor.copy(alpha = 0.35f)
                } else {
                    VantafynColors.Border.copy(alpha = 0.3f)
                },
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(VantafynSpacing.md)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Badge icon container with icon/image rendering
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (achievement.isUnlocked) rarityColor.copy(alpha = 0.15f)
                        else VantafynColors.Surface.copy(alpha = 0.6f),
                    )
                    .border(
                        1.dp,
                        if (achievement.isUnlocked) rarityColor.copy(alpha = 0.4f)
                        else VantafynColors.Border,
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (achievement.iconUrl != null && !isHiddenLocked) {
                    AsyncImage(
                        model = achievement.iconUrl,
                        contentDescription = achievement.name,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        imageVector = if (isHiddenLocked) Icons.Rounded.Lock else vectorIcon,
                        contentDescription = null,
                        tint = if (achievement.isUnlocked) rarityColor else VantafynColors.Muted.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (isHiddenLocked) "Secret Achievement" else achievement.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) VantafynColors.Ink else VantafynColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (achievement.isEquipped) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Equipped",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Text(
                    text = if (isHiddenLocked) "Keep watching to discover and unlock this secret badge."
                    else achievement.description.ifBlank { "Unlocked through viewing activity." },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (achievement.isUnlocked) VantafynColors.Muted else VantafynColors.Muted.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!achievement.isUnlocked && !isHiddenLocked && (achievement.progressRatio != null || achievement.progressText != null)) {
                    val ratio = (achievement.progressRatio ?: 0f).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = VantafynColors.Muted,
                            )
                            Text(
                                text = achievement.progressText ?: "${(ratio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ratio > 0f) rarityColor else VantafynColors.Muted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            if (ratio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    rarityColor.copy(alpha = 0.75f),
                                                    rarityColor,
                                                ),
                                            ),
                                        ),
                                )
                            }
                        }
                    }
                }

                // Bottom tags (Rarity & Points)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = rarityColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Text(
                            text = achievement.rarity.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = rarityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    if (achievement.score > 0) {
                        Text(
                            text = "+${achievement.score} PTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementDetailDialog(
    achievement: JellyfinAchievement,
    onDismiss: () -> Unit,
) {
    val rarityColor = achievement.rarity.toColor()
    val isHiddenLocked = !achievement.isUnlocked && achievement.isHidden
    val vectorIcon = remember(achievement.iconName, achievement.name, achievement.category) {
        resolveAchievementIcon(achievement.iconName, achievement.name, achievement.category)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.vantafynAnimatedModalBorder(cornerRadius = 24.dp),
        containerColor = VantafynColors.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Large Badge Icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (achievement.isUnlocked) rarityColor.copy(alpha = 0.18f)
                            else VantafynColors.Surface.copy(alpha = 0.8f),
                        )
                        .border(
                            1.5.dp,
                            if (achievement.isUnlocked) rarityColor.copy(alpha = 0.5f)
                            else VantafynColors.Border,
                            RoundedCornerShape(20.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (achievement.iconUrl != null && !isHiddenLocked) {
                        AsyncImage(
                            model = achievement.iconUrl,
                            contentDescription = achievement.name,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            imageVector = if (isHiddenLocked) Icons.Rounded.Lock else vectorIcon,
                            contentDescription = null,
                            tint = if (achievement.isUnlocked) rarityColor else VantafynColors.Muted.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                // Title & Category
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isHiddenLocked) "Secret Achievement" else achievement.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VantafynColors.Ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = achievement.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = VantafynColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                }

                // Tags row (Rarity & Points)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rarityColor.copy(alpha = 0.18f),
                    ) {
                        Text(
                            text = achievement.rarity.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = rarityColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    if (achievement.score > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "+${achievement.score} PTS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = if (isHiddenLocked) {
                        "This is a secret achievement. Details and criteria will be revealed once you earn it by watching content on your server."
                    } else {
                        achievement.description.ifBlank { "Earned by watching content and engaging with your media library." }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = VantafynColors.Ink.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                )

                // Unlock status / Progress
                val unlockedAt = achievement.unlockedAt
                val progressRatio = achievement.progressRatio
                val progressText = achievement.progressText
                val localizedUnlockedDate = remember(unlockedAt) { formatLocalizedDate(unlockedAt) }
                if (achievement.isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF55F0C0),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (!localizedUnlockedDate.isNullOrBlank()) {
                                "Unlocked: $localizedUnlockedDate"
                            } else {
                                "Unlocked"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF55F0C0),
                        )
                    }
                } else if (!isHiddenLocked) {
                    val ratio = (progressRatio ?: 0f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null,
                                        tint = if (ratio > 0f) rarityColor else VantafynColors.Muted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (ratio > 0f) "In Progress" else "Not Started",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VantafynColors.Ink,
                                    )
                                }
                                Text(
                                    text = progressText ?: if (ratio > 0f) "${(ratio * 100).toInt()}%" else "0%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ratio > 0f) rarityColor else VantafynColors.Muted,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                            ) {
                                if (ratio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        rarityColor.copy(alpha = 0.75f),
                                                        rarityColor,
                                                        Color(0xFF55F0C0),
                                                    ),
                                                ),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AchievementModalGlassButton(
                text = "Close",
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun AchievementModalGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.55f))
            .vantafynAnimatedModalBorder(cornerRadius = 18.dp, strokeWidth = 1.2.dp, durationMillis = 4200)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = VantafynColors.Ink,
        )
    }
}

@Composable
private fun AchievementsUnavailableView(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = VantafynSpacing.lg, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        VantafynGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.28f),
                                    Color(0xFFFF9100).copy(alpha = 0.24f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Achievements Unavailable",
                        color = VantafynColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = message ?: "Install and enable the Achievement Badges for Jellyfin plugin to unlock badges, rank tiers, and milestones.",
                        color = VantafynColors.Muted.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    )
                }
            }
        }
    }
}

private fun resolveAchievementIcon(
    iconName: String?,
    name: String,
    category: String,
): ImageVector {
    val key = iconName?.trim()?.lowercase()
    if (!key.isNullOrBlank()) {
        when {
            key.contains("fire") || key.contains("whatshot") || key.contains("flame") || key.contains("hot") -> return Icons.Rounded.LocalFireDepartment
            key.contains("visibility") || key.contains("eye") || key.contains("watch") || key.contains("view") -> return Icons.Rounded.Visibility
            key.contains("movie") || key.contains("theaters") || key.contains("film") || key.contains("cinema") -> return Icons.Rounded.Movie
            key.contains("tv") || key.contains("television") || key.contains("screen") -> return Icons.Rounded.Tv
            key.contains("star") -> return Icons.Rounded.Star
            key.contains("trophy") || key.contains("badge") || key.contains("emoji") || key.contains("reward") || key.contains("medal") || key.contains("rank") -> return Icons.Rounded.EmojiEvents
            key.contains("time") || key.contains("timer") || key.contains("schedule") || key.contains("hour") || key.contains("clock") -> return Icons.Rounded.Timer
            key.contains("favorite") || key.contains("heart") || key.contains("thumb") || key.contains("like") -> return Icons.Rounded.Favorite
            key.contains("music") || key.contains("audio") || key.contains("song") || key.contains("track") -> return Icons.Rounded.MusicNote
            key.contains("explore") || key.contains("map") || key.contains("compass") || key.contains("flag") || key.contains("quest") -> return Icons.Rounded.Explore
            key.contains("repeat") || key.contains("replay") || key.contains("loop") || key.contains("sync") -> return Icons.Rounded.Repeat
            key.contains("bolt") || key.contains("flash") || key.contains("speed") || key.contains("fast") -> return Icons.Rounded.Bolt
            key.contains("night") || key.contains("bedtime") || key.contains("dark") || key.contains("moon") -> return Icons.Rounded.Nightlight
            key.contains("weekend") || key.contains("couch") || key.contains("chair") || key.contains("home") -> return Icons.Rounded.Weekend
            key.contains("celebrat") || key.contains("party") || key.contains("cake") -> return Icons.Rounded.Celebration
            key.contains("light") || key.contains("idea") || key.contains("learn") || key.contains("smart") -> return Icons.Rounded.Lightbulb
            key.contains("bookmark") || key.contains("save") || key.contains("collect") -> return Icons.Rounded.Bookmark
            key.contains("video") || key.contains("cam") -> return Icons.Rounded.Videocam
            key.contains("magic") || key.contains("auto") || key.contains("spark") -> return Icons.Rounded.AutoAwesome
            key.contains("play") -> return Icons.Rounded.PlayArrow
            key.contains("check") || key.contains("done") || key.contains("finish") -> return Icons.Rounded.CheckCircle
        }
    }

    val lowerName = name.lowercase()
    val lowerCat = category.lowercase()

    return when {
        lowerName.contains("binge") || lowerCat.contains("binge") -> Icons.Rounded.LocalFireDepartment
        lowerName.contains("first contact") || lowerName.contains("explore") || lowerCat.contains("started") -> Icons.Rounded.Explore
        lowerName.contains("resident") || lowerName.contains("settling") || lowerName.contains("comfort") -> Icons.Rounded.Visibility
        lowerCat.contains("film") || lowerCat.contains("movie") || lowerName.contains("movie") || lowerName.contains("film") -> Icons.Rounded.Movie
        lowerCat.contains("series") || lowerCat.contains("show") || lowerCat.contains("tv") || lowerName.contains("episode") || lowerName.contains("season") -> Icons.Rounded.Tv
        lowerCat.contains("music") || lowerCat.contains("audio") || lowerName.contains("music") || lowerName.contains("track") -> Icons.Rounded.MusicNote
        lowerCat.contains("night") || lowerName.contains("midnight") || lowerName.contains("night") -> Icons.Rounded.Nightlight
        lowerCat.contains("weekend") || lowerName.contains("marathon") || lowerName.contains("hour") -> Icons.Rounded.Timer
        lowerCat.contains("favorite") || lowerName.contains("favorite") || lowerName.contains("loved") -> Icons.Rounded.Favorite
        else -> Icons.Rounded.EmojiEvents
    }
}

private fun formatLocalizedDate(isoString: String?): String? {
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
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(parsedDate)
        } else {
            trimmed
        }
    } catch (_: Exception) {
        isoString
    }
}

private fun extractUserInitials(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "V" }

private fun JellyfinAchievementRarity.toColor(): Color =
    when (this) {
        JellyfinAchievementRarity.Common -> Color(0xFF81C784)
        JellyfinAchievementRarity.Uncommon -> Color(0xFF00E676)
        JellyfinAchievementRarity.Rare -> Color(0xFF2979FF)
        JellyfinAchievementRarity.Epic -> Color(0xFFAA00FF)
        JellyfinAchievementRarity.Legendary -> Color(0xFFFFD700)
        JellyfinAchievementRarity.Mythic -> Color(0xFFFF1744)
        JellyfinAchievementRarity.Unknown -> Color(0xFF9E9E9E)
    }
