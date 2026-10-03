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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
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

import dev.vantafyn.core.jellyfin.PokemonBoxDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto

private data class LocalAchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val rarity: String,
    val score: Int,
    val max: Int,
    val current: Int,
)

private fun computeLocalAchievements(
    localSaves: List<PokemonGameSaveDto>,
    vaultBoxes: List<PokemonBoxDto>,
): PokemonAchievementsSummaryDto {
    val vaultEntries = vaultBoxes.flatMap { it.entries }
    val vaultCount = vaultEntries.size

    val caughtSet = mutableSetOf<Int>()
    for (save in localSaves) {
        // Sanity guard: Only trust caughtSpeciesIds if save actually has active pokemon and isn't uninitialized SRAM
        if (save.totalPokemonCount > 0 && save.caughtSpeciesIds.size <= (save.totalPokemonCount + 40)) {
            caughtSet.addAll(save.caughtSpeciesIds)
        }
        save.party.forEach { if (it.speciesId > 0) caughtSet.add(it.speciesId) }
        save.boxes.forEach { b -> b.entries.forEach { if (it.speciesId > 0) caughtSet.add(it.speciesId) } }
    }
    for (p in vaultEntries) {
        if (p.speciesId > 0) caughtSet.add(p.speciesId)
    }

    val hasShiny = vaultEntries.any { it.isShiny } || localSaves.any { save ->
        save.party.any { it.isShiny } || save.boxes.any { b -> b.entries.any { it.isShiny } }
    }

    val totalOwnedPokemon = vaultCount + localSaves.sumOf { it.totalPokemonCount }
    val kantoCaught = (1..151).count { caughtSet.contains(it) }
    val isLegitKantoMaster = kantoCaught >= 151 && caughtSet.size >= 151 && totalOwnedPokemon >= 25
    val kantoProgress = if (isLegitKantoMaster) 151 else minOf(kantoCaught, minOf(totalOwnedPokemon + 5, 150))

    val defs = listOf(
        LocalAchievementDef("pk-vault-first-deposit", "Vault Initiate", "Deposited your first Pokémon into the personal cloud vault.", "Common", 100, 1, if (vaultCount >= 1) 1 else 0),
        LocalAchievementDef("pk-vault-50", "Pokemon Collector", "Stored 50 or more Pokémon in your personal vault.", "Rare", 250, 50, minOf(50, vaultCount)),
        LocalAchievementDef("pk-vault-withdraw", "Ready for Battle", "Withdrew a Pokémon from the cloud vault into an active game save.", "Common", 100, 1, 0),
        LocalAchievementDef("pk-transfer-first", "Cross-Game Traveler", "Transferred a Pokémon directly from one game save to another.", "Uncommon", 150, 1, 0),
        LocalAchievementDef("pk-crossgen-transfer", "Time Traveler", "Successfully performed a cross-generation migration across historical eras.", "Epic", 300, 1, 0),
        LocalAchievementDef("pk-shiny-first", "Gotta Gleam 'Em All", "Registered a Shiny Pokémon in your vault or Pokédex.", "Legendary", 500, 1, if (hasShiny) 1 else 0),
        LocalAchievementDef("pk-pokedex-10", "Research Assistant", "Registered 10 unique species in the National Pokédex.", "Common", 100, 10, minOf(10, caughtSet.size)),
        LocalAchievementDef("pk-pokedex-50", "Field Researcher", "Registered 50 unique species in the National Pokédex.", "Rare", 300, 50, minOf(50, caughtSet.size)),
        LocalAchievementDef("pk-pokedex-kanto-master", "Kanto Master", "Completed the Generation I Pokédex (all 151 species).", "Mythic", 1000, 151, kantoProgress),
        LocalAchievementDef("pk-trade-first", "Link Cable Connection", "Completed a Pokémon trade with another trainer.", "Rare", 250, 1, 0),
    )

    val dtoList = defs.map { d ->
        val unlocked = d.current >= d.max
        PokemonAchievementDto(
            id = d.id,
            title = d.title,
            description = d.description,
            category = "Pokemon",
            rarity = d.rarity,
            score = d.score,
            iconName = "star",
            isUnlocked = unlocked,
            unlockedAtUtc = null,
            currentProgress = d.current,
            maxProgress = d.max,
        )
    }

    return PokemonAchievementsSummaryDto(
        userId = java.util.UUID.randomUUID().toString(),
        totalScore = dtoList.filter { it.isUnlocked }.sumOf { it.score },
        unlockedCount = dtoList.count { it.isUnlocked },
        totalCount = dtoList.size,
        achievements = dtoList,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonAchievementsModal(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    localSaves: List<PokemonGameSaveDto> = emptyList(),
    vaultBoxes: List<PokemonBoxDto> = emptyList(),
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Achievements, 1 = Social Activity
    val localBaseline = remember(localSaves, vaultBoxes) {
        computeLocalAchievements(localSaves, vaultBoxes)
    }
    var summary by remember { mutableStateOf<PokemonAchievementsSummaryDto?>(localBaseline) }
    var activityFeed by remember { mutableStateOf<List<PokemonSocialActivityEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(session != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        val currentLocal = computeLocalAchievements(localSaves, vaultBoxes)
        if (session == null) {
            summary = currentLocal
            isLoading = false
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            val achResult = pokemonRepository.getAchievements(session)
            val feedResult = pokemonRepository.getSocialActivity(session, limit = 50)

            if (achResult.isSuccess) {
                val netSummary = achResult.getOrNull()
                if (netSummary != null && netSummary.achievements.isNotEmpty()) {
                    val totalOwned = vaultBoxes.sumOf { it.entries.size } + localSaves.sumOf { it.totalPokemonCount }
                    val mergedList = netSummary.achievements.map { netAch ->
                        val localMatch = currentLocal.achievements.firstOrNull { it.id.equals(netAch.id, ignoreCase = true) }
                        if (localMatch != null) {
                            val isPokedexMilestone = netAch.id.startsWith("pk-pokedex-", ignoreCase = true)
                            // Pokédex milestone achievements must strictly reflect legitimate collection progress
                            val isUnlocked = if (isPokedexMilestone) {
                                localMatch.isUnlocked
                            } else {
                                netAch.isUnlocked || localMatch.isUnlocked
                            }
                            val maxP = maxOf(netAch.maxProgress, localMatch.maxProgress)
                            val curP = if (isUnlocked) maxP else if (isPokedexMilestone) localMatch.currentProgress else minOf(netAch.currentProgress, localMatch.currentProgress)
                            netAch.copy(
                                isUnlocked = isUnlocked,
                                currentProgress = curP,
                                maxProgress = maxP,
                            )
                        } else {
                            netAch
                        }
                    }
                    val unlockedCount = mergedList.count { it.isUnlocked }
                    val totalScore = mergedList.filter { it.isUnlocked }.sumOf { it.score }
                    summary = netSummary.copy(
                        unlockedCount = unlockedCount,
                        totalScore = totalScore,
                        achievements = mergedList,
                    )
                } else {
                    summary = currentLocal
                }
            } else {
                summary = currentLocal
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

    BasicAlertDialog(onDismissRequest = onDismiss) {
        PokemonModalContainer(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(680.dp),
            shape = RoundedCornerShape(20.dp),
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
                            text = "Trainer Hub & Achievements",
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
                    .background(Color(0xFF161926).copy(alpha = 0.85f))
                    .padding(3.dp),
            ) {
                // Achievements Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedTab == 0) Brush.horizontalGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedTab = 0 }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Social Activity Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedTab == 1) Brush.horizontalGradient(
                                listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { selectedTab = 1 }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        itemsIndexed(
                            items = achievements,
                            key = { index, ach -> if (ach.id.isNotBlank()) ach.id else "ach_$index" }
                        ) { _, ach ->
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
                            itemsIndexed(
                                items = activityFeed,
                                key = { index, event ->
                                    if (event.id.isNotBlank()) event.id else "${event.eventType}_${event.timestampUtc}_$index"
                                }
                            ) { _, event ->
                                PokemonSocialActivityCard(event = event)
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
    val fallbackTitle = when (event.eventType) {
        "ShinyAdded" -> event.speciesName?.let { "Shiny $it discovered" } ?: "Shiny Pokémon discovered"
        "CrossGenerationTransferCompleted" -> event.speciesName?.let { "$it crossed generations" } ?: "Cross-generation transfer"
        "PokemonTransferred" -> event.speciesName?.let { "$it transferred" } ?: "Pokémon transferred"
        "TradeCompleted" -> "Trade completed"
        "AchievementUnlocked" -> "Achievement unlocked"
        "PokemonDeposited" -> event.speciesName?.let { "$it deposited" } ?: "Pokémon deposited"
        "PokemonWithdrawn" -> event.speciesName?.let { "$it withdrawn" } ?: "Pokémon withdrawn"
        else -> "Trainer activity"
    }
    val title = event.title.ifBlank { fallbackTitle }
    val description = event.description.ifBlank {
        event.userName.ifBlank { "A trainer" } + " recorded ${title.lowercase()}."
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
                        text = title,
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
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
