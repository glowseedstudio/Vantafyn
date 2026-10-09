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
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Vibration
import dev.vantafyn.core.emulator.net.LinkSessionManager
import dev.vantafyn.core.emulator.net.LinkTransportMode
import dev.vantafyn.core.media.games.GameHubSoundManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import android.net.Uri
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
    var selectedVideoFilter by remember {
        mutableStateOf(prefs.getString("video_filter", "crisp") ?: "crisp")
    }
    var fastForwardSpeed by remember {
        mutableStateOf(prefs.getString("fast_forward_speed", "2x") ?: "2x")
    }
    var pokemonCryAutoplay by remember {
        mutableStateOf(prefs.getBoolean("pokemon_cry_autoplay", true))
    }
    var pokemonCryStyle by remember {
        mutableStateOf(prefs.getString("pokemon_cry_style", "latest") ?: "latest")
    }
    var pokemonNarrationAutoplay by remember {
        mutableStateOf(prefs.getBoolean("pokemon_narration_autoplay", false))
    }
    val linkManager = remember { LinkSessionManager.getInstance(context) }
    var linkTransportMode by remember { mutableStateOf(linkManager.transportMode) }
    var linkAutoDiscovery by remember { mutableStateOf(linkManager.autoDiscoveryEnabled) }
    var linkPlayerHandle by remember { mutableStateOf(linkManager.playerHandle) }
    val localIp = remember { linkManager.getLocalIpAddress() ?: "Not connected" }
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

    fun normalizeDirectoryPath(raw: String): File {
        var trimmed = raw.trim().trimEnd('/')
        // Fix common mobile keyboard typo: letter 'O' instead of digit '0' in /storage/emulated/0
        trimmed = trimmed
            .replace("/emulated/O/", "/emulated/0/")
            .replace("/emulated/o/", "/emulated/0/")
            .replace("/emulated/O", "/emulated/0")
            .replace("/emulated/o", "/emulated/0")

        val candidate = when {
            trimmed.startsWith("/storage/") || trimmed.startsWith("/sdcard/") -> File(trimmed)
            trimmed.startsWith("/sdcard") -> File(trimmed)
            trimmed.startsWith("/") -> {
                val direct = File(trimmed)
                if (direct.exists()) direct else File("/sdcard$trimmed")
            }
            else -> File("/sdcard/$trimmed")
        }
        if (candidate.exists()) return candidate

        if (candidate.absolutePath.startsWith("/storage/emulated/0")) {
            val alt = File(candidate.absolutePath.replaceFirst("/storage/emulated/0", "/sdcard"))
            if (alt.exists()) return alt
        } else if (candidate.absolutePath.startsWith("/sdcard")) {
            val alt = File(candidate.absolutePath.replaceFirst("/sdcard", "/storage/emulated/0"))
            if (alt.exists()) return alt
        }

        val parent = candidate.parentFile
        if (parent != null && parent.exists()) {
            val match = parent.listFiles()?.firstOrNull { it.name.equals(candidate.name, ignoreCase = true) }
            if (match != null) return match
        }
        return candidate
    }

    val default3dsDir = remember {
        val candidates = listOf(
            File("/sdcard/Roms/3DS"),
            File("/sdcard/ROMs/3ds"),
            File("/sdcard/ROMs/3DS"),
            File("/sdcard/Roms/3ds"),
            File("/sdcard/Download/3ds"),
            File("/sdcard/Download"),
            File(context.getExternalFilesDir(null), "3ds"),
        )
        candidates.firstOrNull { it.exists() && it.isDirectory }?.absolutePath
            ?: File(context.getExternalFilesDir(null), "3ds").absolutePath
    }
    var local3dsPath by remember {
        val saved = prefs.getString("local_3ds_rom_directory", null)?.takeIf { it.isNotBlank() }
        val effective = if (saved != null) {
            val norm = normalizeDirectoryPath(saved)
            if (norm.exists() && norm.isDirectory) norm.absolutePath else default3dsDir
        } else {
            default3dsDir
        }
        mutableStateOf(effective)
    }
    var detected3dsFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showCustom3dsPathDialog by remember { mutableStateOf(false) }
    var showDetected3dsDialog by remember { mutableStateOf(false) }
    var customPathInput by remember { mutableStateOf(local3dsPath) }

    fun scan3dsRoms(dirPath: String): List<File> {
        val resolved = normalizeDirectoryPath(dirPath)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        android.util.Log.i(
            "Vantafyn3DS",
            "scan3dsRoms for '$dirPath' -> resolved='${resolved.absolutePath}' (exists=${resolved.exists()}, isDir=${resolved.isDirectory}, allFilesAccess=$hasPermission)"
        )
        if (!resolved.exists() || !resolved.isDirectory) {
            android.util.Log.w("Vantafyn3DS", "Directory does not exist or is not a directory: ${resolved.absolutePath}")
            return emptyList()
        }
        val validExts = setOf("3ds", "cci", "cia", "cxi", "3dsx")
        return try {
            val list = resolved.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile && it.length() > 0L && it.extension.lowercase() in validExts }
                .sortedBy { it.name.lowercase() }
                .toList()
            android.util.Log.i("Vantafyn3DS", "scan3dsRoms found ${list.size} 3DS ROMs in ${resolved.absolutePath}: ${list.map { it.name }}")
            list
        } catch (e: Exception) {
            android.util.Log.e("Vantafyn3DS", "Exception in scan3dsRoms: ${e.message}", e)
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        calculateRomCache()
    }

    LaunchedEffect(local3dsPath) {
        detected3dsFiles = scan3dsRoms(local3dsPath)
    }

    GameScreenReveal(
        key = "games_settings_screen",
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
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

                        // Retro Video Filter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tv,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Default Video Filter",
                                color = VantafynColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "crisp" to "Crisp Pixels",
                                "crt" to "CRT Scanlines",
                                "smooth" to "Smooth Filter"
                            ).forEach { (filterId, label) ->
                                val selected = selectedVideoFilter == filterId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) VantafynGradients.accentHorizontal()
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                        )
                                        .clickable {
                                            selectedVideoFilter = filterId
                                            prefs.edit().putString("video_filter", filterId).apply()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.White else VantafynColors.Muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Wireless Communication & Link Play
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "WIRELESS COMMUNICATION & LINK PLAY",
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
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cable,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Multiplayer Transport Mode",
                                    color = VantafynColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Link cable & wireless communication for trades & battles",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // Transport Mode selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                LinkTransportMode.LOCAL_WIFI to "Local Wi-Fi",
                                LinkTransportMode.SERVER_RELAY to "Server Relay",
                                LinkTransportMode.OFFLINE to "Offline",
                            ).forEach { (mode, label) ->
                                val selected = linkTransportMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) VantafynGradients.accentHorizontal()
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                        )
                                        .clickable {
                                            linkTransportMode = mode
                                            linkManager.transportMode = mode
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.White else VantafynColors.Muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }

                        // Explanatory badge for selected mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(10.dp),
                        ) {
                            Text(
                                text = when (linkTransportMode) {
                                    LinkTransportMode.LOCAL_WIFI ->
                                        "⚡ Ultra-low latency (<2ms) peer discovery via mDNS. Perfect for Pokémon battles and trades with nearby handhelds on the same Wi-Fi or Wi-Fi Direct."
                                    LinkTransportMode.SERVER_RELAY ->
                                        "🌐 Relays link packets over your Vantafyn Jellyfin server. Allows trading and battling with friends anywhere in the world."
                                    LinkTransportMode.OFFLINE ->
                                        "🔒 Multiplayer link features disabled. Cores run in isolated single-player mode."
                                },
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }

                        // Auto-Discovery Switch
                        if (linkTransportMode != LinkTransportMode.OFFLINE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .clickable {
                                        val newVal = !linkAutoDiscovery
                                        linkAutoDiscovery = newVal
                                        linkManager.autoDiscoveryEnabled = newVal
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sensors,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nearby Peer Auto-Discovery",
                                        color = VantafynColors.Ink,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Announce and detect nearby Vantafyn rooms automatically",
                                        color = VantafynColors.Muted,
                                        fontSize = 11.sp,
                                    )
                                }
                                VantafynSwitch(
                                    checked = linkAutoDiscovery,
                                    onCheckedChange = { checked ->
                                        linkAutoDiscovery = checked
                                        linkManager.autoDiscoveryEnabled = checked
                                    },
                                )
                            }

                            // Device info / IP status pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Host Tag: $linkPlayerHandle",
                                        color = VantafynColors.Ink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Text(
                                    text = "IP: $localIp",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Controls & Haptics
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
                            text = "Play relaxing soundtrack while in the Game Hub (40% volume)",
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

                // Pokédex Cry Auto-Play
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable {
                            val newVal = !pokemonCryAutoplay
                            pokemonCryAutoplay = newVal
                            prefs.edit().putBoolean("pokemon_cry_autoplay", newVal).apply()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp),
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pokédex Cry Auto-Play",
                            color = VantafynColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Play Pokémon cry automatically when opening Pokédex details",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }

                    VantafynSwitch(
                        checked = pokemonCryAutoplay,
                        onCheckedChange = { checked ->
                            pokemonCryAutoplay = checked
                            prefs.edit().putBoolean("pokemon_cry_autoplay", checked).apply()
                        },
                    )
                }

                // Pokédex Cry Style
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
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Pokédex Cry Style",
                                color = VantafynColors.Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("latest" to "Modern (Remastered)", "legacy" to "Retro (Gen 1-5)").forEach { (style, label) ->
                                val selected = pokemonCryStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) VantafynGradients.accentHorizontal()
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                        )
                                        .clickable {
                                            pokemonCryStyle = style
                                            prefs.edit().putString("pokemon_cry_style", style).apply()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.White else VantafynColors.Muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }

                // Pokédex Narration Auto-Play
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable {
                            val newVal = !pokemonNarrationAutoplay
                            pokemonNarrationAutoplay = newVal
                            prefs.edit().putBoolean("pokemon_narration_autoplay", newVal).apply()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pokédex Auto-Narration", color = VantafynColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Read the name, classification, and entry after its cry. Requires a configured Companion TTS service.", color = VantafynColors.Muted, fontSize = 12.sp)
                    }
                    VantafynSwitch(
                        checked = pokemonNarrationAutoplay,
                        onCheckedChange = { checked ->
                            pokemonNarrationAutoplay = checked
                            prefs.edit().putBoolean("pokemon_narration_autoplay", checked).apply()
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

        // 5. Local 3DS ROM Storage
        item {
            val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "LOCAL 3DS ROM STORAGE",
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
                        if (!hasAllFilesAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ACTION REQUIRED: Grant All Files Access",
                                            color = Color(0xFFFFB300),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "Android 11+ blocks reading .3ds/.cci files until this permission is enabled. Tap here to allow.",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE60012).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(dev.vantafyn.feature.home.R.drawable.system_logo_3ds),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(18.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nintendo 3DS ROM Directory",
                                    color = VantafynColors.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp),
                                ) {
                                    Text(
                                        text = "${detected3dsFiles.size} 3DS games detected",
                                        color = if (detected3dsFiles.isNotEmpty()) Color(0xFF00E676) else VantafynColors.Muted,
                                        fontSize = 12.sp,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable {
                                                detected3dsFiles = scan3dsRoms(local3dsPath)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = "Rescan",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Because 3DS ROMs are 1–4 GB, they are loaded directly from your device storage instead of streaming or downloading from the server.",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )

                        // Active path box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "ACTIVE PATH",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                )
                                Text(
                                    text = local3dsPath,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        // Presets
                        Text(
                            text = "Quick Presets:",
                            color = VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val presets = listOf(
                                "Roms/3DS" to "/sdcard/Roms/3DS",
                                "ROMs/3ds" to "/sdcard/ROMs/3ds",
                                "Downloads" to "/sdcard/Download",
                                "App Storage" to File(context.getExternalFilesDir(null), "3ds").absolutePath,
                            )
                            presets.forEach { (label, path) ->
                                val isSelected = local3dsPath == path
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) VantafynGradients.accentHorizontal()
                                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                        )
                                        .clickable {
                                            local3dsPath = path
                                            prefs.edit().putString("local_3ds_rom_directory", path).apply()
                                            detected3dsFiles = scan3dsRoms(path)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else VantafynColors.Muted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }

                        // Custom path & view ROMs buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        customPathInput = local3dsPath
                                        showCustom3dsPathDialog = true
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Set Custom Folder",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            if (detected3dsFiles.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00E676).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            showDetected3dsDialog = true
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "View ROMs (${detected3dsFiles.size})",
                                            color = Color(0xFF00E676),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }

                        if (!hasAllFilesAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Grant All Files Access",
                                            color = Color(0xFFFFB300),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "Required on Android 11+ to read /sdcard/ROMs or Downloads folders",
                                            color = VantafynColors.Muted,
                                            fontSize = 11.sp,
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

    if (showCustom3dsPathDialog) {
        AlertDialog(
            onDismissRequest = { showCustom3dsPathDialog = false },
            containerColor = Color(0xFF14141E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "3DS ROM Directory",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the absolute folder path where your .3ds or .cci ROM files are stored on this device:",
                        color = VantafynColors.Muted,
                        fontSize = 13.sp,
                    )
                    OutlinedTextField(
                        value = customPathInput,
                        onValueChange = { customPathInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("/sdcard/ROMs/3ds", color = Color.Gray) },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val resolved = normalizeDirectoryPath(customPathInput)
                        val clean = resolved.absolutePath
                        local3dsPath = clean
                        prefs.edit().putString("local_3ds_rom_directory", clean).apply()
                        detected3dsFiles = scan3dsRoms(clean)
                        showCustom3dsPathDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustom3dsPathDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
        )
    }

    if (showDetected3dsDialog) {
        AlertDialog(
            onDismissRequest = { showDetected3dsDialog = false },
            containerColor = Color(0xFF14141E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Detected 3DS ROMs (${detected3dsFiles.size})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(detected3dsFiles) { file ->
                        val mb = String.format(java.util.Locale.US, "%.1f MB", file.length() / (1024f * 1024f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = file.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "$mb • ${file.parentFile?.name ?: ""}",
                                    color = VantafynColors.Muted,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetected3dsDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
    }
}
