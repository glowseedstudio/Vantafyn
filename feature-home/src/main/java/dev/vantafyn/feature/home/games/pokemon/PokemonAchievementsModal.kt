package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonAchievementDto
import dev.vantafyn.core.jellyfin.PokemonAchievementsSummaryDto
import dev.vantafyn.core.jellyfin.PokemonSocialActivityEvent
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonAchievementsModal(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Achievements, 1 = Social Activity
    var summary by remember { mutableStateOf<PokemonAchievementsSummaryDto?>(null) }
    var activityFeed by remember { mutableStateOf<List<PokemonSocialActivityEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            val achResult = pokemonRepository.getAchievements(session)
            val feedResult = pokemonRepository.getSocialActivity(session, limit = 50)

            if (achResult.isSuccess) {
                summary = achResult.getOrNull()
            } else {
                errorMessage = achResult.exceptionOrNull()?.message ?: "Failed to load achievements"
            }

            if (feedResult.isSuccess) {
                activityFeed = feedResult.getOrNull() ?: emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .height(680.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151722))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Trainer Hub & Badges",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val unlocked = summary?.unlockedCount ?: 0
                        val total = summary?.totalCount ?: 10
                        val score = summary?.totalScore ?: 0
                        Text(
                            text = "$unlocked / $total Unlocked • $score Trainer Points",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { loadData() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF94A3B8),
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161926))
                    .padding(3.dp),
            ) {
                // Achievements Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedTab == 0) Brush.horizontalGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Achievements (${summary?.unlockedCount ?: 0}/${summary?.totalCount ?: 10})",
                            color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }

                // Social Activity Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedTab == 1) Brush.horizontalGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Trainer Feed (${activityFeed.size})",
                            color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4F46E5),
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                    )
                }
            } else {
                if (selectedTab == 0) {
                    // Achievements List
                    val achievements = summary?.achievements ?: emptyList()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(achievements, key = { it.id }) { ach ->
                            PokemonAchievementCard(achievement = ach)
                        }
                    }
                } else {
                    // Community Activity Feed
                    if (activityFeed.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No trainer activity recorded yet.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            items(activityFeed, key = { it.id }) { event ->
                                PokemonSocialActivityCard(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonAchievementCard(achievement: PokemonAchievementDto) {
    val isUnlocked = achievement.isUnlocked
    val rarityColor = when (achievement.rarity.lowercase()) {
        "common" -> Color(0xFF94A3B8)
        "uncommon" -> Color(0xFF10B981)
        "rare" -> Color(0xFF38BDF8)
        "epic" -> Color(0xFFA855F7)
        "legendary" -> Color(0xFFF59E0B)
        "mythic" -> Color(0xFFEC4899)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUnlocked) Color(0xFF1E2235) else Color(0xFF151824))
            .border(
                width = 1.dp,
                color = if (isUnlocked) rarityColor.copy(alpha = 0.5f) else Color(0xFF262C3D),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) rarityColor.copy(alpha = 0.18f) else Color(0xFF1F2433)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) rarityColor else Color(0xFF64748B),
                    modifier = Modifier.size(22.dp),
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = achievement.title,
                        color = if (isUnlocked) Color.White else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(rarityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = achievement.rarity.uppercase(),
                                color = rarityColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "+${achievement.score} pts",
                            color = Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = achievement.description,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )

                if (achievement.maxProgress > 1 && !isUnlocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { (achievement.currentProgress.toFloat() / achievement.maxProgress.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = rarityColor,
                            trackColor = Color(0xFF262C3D),
                        )
                        Text(
                            text = "${achievement.currentProgress}/${achievement.maxProgress}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonSocialActivityCard(event: PokemonSocialActivityEvent) {
    val eventColor = when (event.eventType) {
        "ShinyAdded" -> Color(0xFFF59E0B)
        "CrossGenerationTransferCompleted" -> Color(0xFFA855F7)
        "TradeCompleted" -> Color(0xFF10B981)
        "AchievementUnlocked" -> Color(0xFFEC4899)
        "PokemonDeposited" -> Color(0xFF38BDF8)
        "PokemonWithdrawn" -> Color(0xFF6366F1)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF181B28))
            .border(1.dp, Color(0xFF272D3F), RoundedCornerShape(12.dp))
            .padding(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(eventColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (event.eventType) {
                        "ShinyAdded" -> Icons.Rounded.AutoAwesome
                        "TradeCompleted" -> Icons.Rounded.SwapHoriz
                        "AchievementUnlocked" -> Icons.Rounded.Star
                        else -> Icons.Rounded.Person
                    },
                    contentDescription = null,
                    tint = eventColor,
                    modifier = Modifier.size(17.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (event.isShiny) {
                        Text(
                            text = "✨ SHINY",
                            color = Color(0xFFFBBF24),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
