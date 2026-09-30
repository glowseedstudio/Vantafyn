package dev.vantafyn.feature.home.games

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import dev.vantafyn.core.media.games.GameHubSoundManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import dev.vantafyn.core.ui.VantafynSwitch
import dev.vantafyn.core.ui.VantafynGradientProgressBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import dev.vantafyn.core.jellyfin.GameBoxartScraper
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.GameSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton
import java.io.File

@Composable
fun GamesSettingsScreen(
    onBack: () -> Unit,
    games: List<GameSummary> = emptyList(),
    allGames: List<GameSummary> = games,
    systems: List<GameSystem> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    val fullGamesList = remember(allGames, games) {
        if (allGames.isNotEmpty()) allGames else games
    }
    var selectedScrapeSystemId by remember { mutableStateOf<String?>(null) }
    val targetGames = remember(fullGamesList, selectedScrapeSystemId) {
        if (selectedScrapeSystemId == null) fullGamesList
        else fullGamesList.filter { it.systemId == selectedScrapeSystemId }
    }

    var isScraping by remember { mutableStateOf(false) }
    var scrapeProgress by remember { mutableStateOf(0f) }
    var currentScrapingTitle by remember { mutableStateOf("") }
    var scrapeMatchedCount by remember { mutableStateOf(0) }
    var scrapeCompletedMessage by remember { mutableStateOf<String?>(null) }

    var hapticsEnabled by remember {
        mutableStateOf(prefs.getBoolean("haptics_enabled", true))
    }
    var bgmEnabled by remember {
        mutableStateOf(GameHubSoundManager.isMusicEnabled(context))
    }
    var selectedAspectRatio by remember {
        mutableStateOf(prefs.getString("default_aspect_ratio", "4:3") ?: "4:3")
    }
    var fastForwardSpeed by remember {
        mutableStateOf(prefs.getString("fast_forward_speed", "2x") ?: "2x")
    }
    var romCacheSize by remember { mutableLongStateOf(0L) }
    var romFileCount by remember { mutableStateOf(0) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    fun calculateRomCache() {
        val romDir = File(context.cacheDir, "retro_roms")
        if (romDir.exists() && romDir.isDirectory) {
            val files = romDir.listFiles().orEmpty()
            romFileCount = files.size
            romCacheSize = files.sumOf { it.length() }
        } else {
            romFileCount = 0
            romCacheSize = 0L
        }
    }

    LaunchedEffect(Unit) {
        calculateRomCache()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompactBackButton(onClick = onBack)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Retro Settings",
                        color = VantafynColors.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Emulation, controls & storage",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 1. Media & Boxart Scraper Card (Placed at the top)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "MEDIA & BOXART SCRAPING",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ImageSearch,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp),
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scan & Scrape Library",
                                    color = VantafynColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${fullGamesList.size} titles in library across ${systems.size} ${if (systems.size == 1) "system" else "systems"}",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        if (systems.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                item {
                                    val isSelected = selectedScrapeSystemId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.06f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f),
                                                RoundedCornerShape(999.dp)
                                            )
                                            .clickable { selectedScrapeSystemId = null }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "All Systems (${fullGamesList.size})",
                                            color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                                items(systems) { sys ->
                                    val isSelected = selectedScrapeSystemId == sys.id
                                    val sysCount = fullGamesList.count { it.systemId == sys.id }.takeIf { it > 0 } ?: sys.gameCount
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.06f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f),
                                                RoundedCornerShape(999.dp)
                                            )
                                            .clickable { selectedScrapeSystemId = sys.id }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "${sys.displayName} ($sysCount)",
                                            color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }

                        if (isScraping) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VantafynGradientProgressBar(
                                    progress = scrapeProgress,
                                    height = 7.dp,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = currentScrapingTitle,
                                        color = VantafynColors.Ink,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "${(scrapeProgress * 100).toInt()}%",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        if (scrapeCompletedMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = scrapeCompletedMessage.orEmpty(),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isScraping) SolidColor(Color.White.copy(alpha = 0.08f))
                                    else VantafynGradients.accentHorizontal(),
                                )
                                .clickable(enabled = !isScraping && targetGames.isNotEmpty()) {
                                    isScraping = true
                                    scrapeProgress = 0f
                                    scrapeMatchedCount = 0
                                    scrapeCompletedMessage = null

                                    coroutineScope.launch(Dispatchers.IO) {
                                        val total = targetGames.size
                                        val gamesByPlatform = targetGames.groupBy { GameBoxartScraper.resolvePlatform(it.systemId) }
                                        var processedCount = 0

                                        for ((platform, platformGames) in gamesByPlatform) {
                                            if (platform == null) {
                                                processedCount += platformGames.size
                                                withContext(Dispatchers.Main) {
                                                    scrapeProgress = processedCount.toFloat() / total.coerceAtLeast(1)
                                                }
                                                continue
                                            }

                                            withContext(Dispatchers.Main) {
                                                currentScrapingTitle = "Fetching catalog for ${platform.libretroName}..."
                                            }

                                            val catalog = GameBoxartScraper.getSystemIndex(platform)

                                            for (game in platformGames) {
                                                processedCount++
                                                withContext(Dispatchers.Main) {
                                                    scrapeProgress = processedCount.toFloat() / total.coerceAtLeast(1)
                                                    currentScrapingTitle = "Matching: ${game.cleanTitle.ifEmpty { game.title }}"
                                                }

                                                val matchedFilename = GameBoxartScraper.matchGame(game.cleanTitle, game.filename, catalog)
                                                if (matchedFilename != null) {
                                                    val cdnUrl = GameBoxartScraper.buildCdnUrl(platform, matchedFilename)
                                                    prefs.edit().putString("boxart_${game.id}", cdnUrl).apply()
                                                    withContext(Dispatchers.Main) {
                                                        scrapeMatchedCount++
                                                    }
                                                }
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
                                            isScraping = false
                                            scrapeProgress = 1f
                                            scrapeCompletedMessage = "Scan complete! $total scanned • $scrapeMatchedCount covers updated."
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (isScraping) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text(
                                        text = "Scanning & Scraping...",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = if (selectedScrapeSystemId == null) "Start Scan & Scrape (${targetGames.size} titles)" else "Scrape ${systems.firstOrNull { it.id == selectedScrapeSystemId }?.displayName ?: "Selected System"} (${targetGames.size})",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Display & Aspect Ratio
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "DISPLAY & VIDEO",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Default Aspect Ratio",
                                color = VantafynColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("4:3", "16:9", "1:1").forEach { ratio ->
                                val selected = selectedAspectRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) VantafynGradients.accentHorizontal()
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                        )
                                        .clickable {
                                            selectedAspectRatio = ratio
                                            prefs.edit().putString("default_aspect_ratio", ratio).apply()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = ratio,
                                        color = if (selected) Color.White else VantafynColors.Muted,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Controls & Haptics
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "CONTROLS & INPUT",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable {
                            val newVal = !hapticsEnabled
                            hapticsEnabled = newVal
                            prefs.edit().putBoolean("haptics_enabled", newVal).apply()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Vibration,
                        contentDescription = null,
                        tint = Color(0xFF9D00FF),
                        modifier = Modifier.size(22.dp),
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Vibration",
                            color = VantafynColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Tactile feedback on on-screen button tap",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }

                    VantafynSwitch(
                        checked = hapticsEnabled,
                        onCheckedChange = { checked ->
                            hapticsEnabled = checked
                            prefs.edit().putBoolean("haptics_enabled", checked).apply()
                        },
                    )
                }

                // Ambient Hub Music
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable {
                            val newVal = !bgmEnabled
                            bgmEnabled = newVal
                            GameHubSoundManager.setMusicEnabled(context, newVal)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp),
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ambient Hub Music",
                            color = VantafynColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Play relaxing soundtrack while in the Game Hub (65% volume)",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }

                    VantafynSwitch(
                        checked = bgmEnabled,
                        onCheckedChange = { checked ->
                            bgmEnabled = checked
                            GameHubSoundManager.setMusicEnabled(context, checked)
                        },
                    )
                }

                // Fast Forward Speed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = Color(0xFFFF2A85),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Fast Forward Speed",
                                color = VantafynColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("2x", "3x", "4x").forEach { speed ->
                                val selected = fastForwardSpeed == speed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) VantafynGradients.accentHorizontal()
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                        )
                                        .clickable {
                                            fastForwardSpeed = speed
                                            prefs.edit().putString("fast_forward_speed", speed).apply()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = speed,
                                        color = if (selected) Color.White else VantafynColors.Muted,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. ROM Storage & Cache Cleaner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "STORAGE & ROM CACHE",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(22.dp),
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cached Game Files",
                                    color = VantafynColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val mb = String.format(java.util.Locale.US, "%.1f MB", romCacheSize / (1024f * 1024f))
                                Text(
                                    text = "$romFileCount ROMs stored locally ($mb)",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        if (cacheClearedMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = cacheClearedMessage.orEmpty(),
                                    color = Color(0xFF00E676),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF3366).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable {
                                    val romDir = File(context.cacheDir, "retro_roms")
                                    if (romDir.exists()) {
                                        romDir.listFiles()?.forEach { it.delete() }
                                    }
                                    calculateRomCache()
                                    cacheClearedMessage = "ROM storage cleared! Cloud saves remain safe."
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5277),
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Clear ROM Cache",
                                    color = Color(0xFFFF5277),
                                    fontSize = 13.sp,
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

