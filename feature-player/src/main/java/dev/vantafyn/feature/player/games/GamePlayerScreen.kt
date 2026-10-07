package dev.vantafyn.feature.player.games

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.vantafyn.core.ui.findActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import dev.vantafyn.core.emulator.net.LinkSessionManager
import dev.vantafyn.core.emulator.net.ActiveLinkSession
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GamePlayTracker
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.media.games.GameHubSoundManager
import dev.vantafyn.core.ui.VantafynColors
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.vantafyn.core.emulator.NativeEmulatorEngine
import dev.vantafyn.core.emulator.NativeEmulatorSurface
import dev.vantafyn.core.emulator.NdsScreenLayout
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GamePlayerScreen(
    game: GameDetail,
    libraryId: String,
    session: JellyfinSession?,
    gamesRepository: JellyfinGamesRepository,
    isTv: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { GameStorageManager(context, gamesRepository) }

    LaunchedEffect(Unit) {
        GameHubSoundManager.stop(instant = true)
    }

    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val window = activity?.window
        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val previousBehavior = controller.systemBarsBehavior
            val previousKeepScreenOn = window.decorView.keepScreenOn
            var previousDisplayModeId = 0
            var previousRefreshRate = 0f

            window.decorView.keepScreenOn = true
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Only hide status bar in landscape mode so portrait has safe room for front camera cutouts
            val orientation = context.resources.configuration.orientation
            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val params = window.attributes
                previousDisplayModeId = params.preferredDisplayModeId
                previousRefreshRate = params.preferredRefreshRate
                val display = window.decorView.display ?: activity.windowManager.defaultDisplay
                val stableMode = display.supportedModes
                    .filter { it.refreshRate >= 59f }
                    .minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
                if (stableMode != null) {
                    params.preferredDisplayModeId = stableMode.modeId
                    params.preferredRefreshRate = stableMode.refreshRate
                    window.attributes = params
                }
            }

            onDispose {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val params = window.attributes
                    params.preferredDisplayModeId = previousDisplayModeId
                    params.preferredRefreshRate = previousRefreshRate
                    window.attributes = params
                }
                window.decorView.keepScreenOn = previousKeepScreenOn
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    var isDownloading by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf("Preparing game environment...") }
    var romFile by remember { mutableStateOf<File?>(null) }

    val prefs = remember { context.getSharedPreferences("vantafyn_retro_settings", Context.MODE_PRIVATE) }
    val initialAspect = remember {
        when (prefs.getString("default_aspect_ratio", "4:3")) {
            "16:9" -> GameAspectRatio.Widescreen
            "1:1" -> GameAspectRatio.Square
            else -> GameAspectRatio.Standard
        }
    }
    // Native Libretro Core Engine state & handheld detection
    val isNativeMode = remember(game.systemId, game.core) {
        val s = game.systemId.lowercase().trim()
        val c = game.core.lowercase().trim()
        s in setOf("nds", "ds", "gba", "gb", "gbc") || c.contains("melonds") || c.contains("desmume") || c.contains("gpsp") || c.contains("mgba") || c.contains("gambatte") || c.contains("tgbdual") || c.contains("sameboy")
    }
    val isHandheld = remember(game.systemId, game.core) {
        val s = game.systemId.lowercase().trim()
        val c = game.core.lowercase().trim()
        s in setOf("nds", "ds", "gba", "gb", "gbc", "psp") || c.contains("melonds") || c.contains("desmume") || c.contains("gpsp") || c.contains("mgba") || c.contains("gambatte") || c.contains("tgbdual") || c.contains("sameboy")
    }

    val initialFilter = remember(isHandheld) {
        val saved = prefs.getString("video_filter", "crisp")
        when {
            saved == "crt" && isHandheld -> GameVideoFilter.Crisp // Enforce no CRT scanlines for NDS & handhelds
            saved == "crt" -> GameVideoFilter.Crt
            saved == "lcd" -> GameVideoFilter.LcdGrid
            saved == "smooth" -> GameVideoFilter.Smooth
            else -> GameVideoFilter.Crisp
        }
    }
    val initialFfSpeed = remember {
        when (prefs.getString("fast_forward_speed", "2x")) {
            "3x" -> 3f
            "4x" -> 4f
            else -> 2f
        }
    }

    var isPaused by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(initialAspect) }
    var fastForwardSpeed by remember { mutableFloatStateOf(1f) }
    var configuredFfSpeed by remember { mutableFloatStateOf(initialFfSpeed) }
    var isMuted by remember { mutableStateOf(false) }
    var videoFilter by remember { mutableStateOf(initialFilter) }
    var hasPhysicalGamepad by remember { mutableStateOf(GameInputController.isGamepadConnected()) }
    var showTouchControls by remember { mutableStateOf(!hasPhysicalGamepad) }
    var gbaColorCorrection by remember { mutableStateOf(prefs.getBoolean("gba_color_correction", true)) }
    var gbaAudioFiltering by remember { mutableStateOf(prefs.getBoolean("gba_audio_filtering", true)) }
    var gbcColorCorrection by remember { mutableStateOf(prefs.getBoolean("gbc_color_correction", true)) }
    var gbPalette by remember { mutableStateOf(prefs.getString("gb_palette_${game.id}", "colorized") ?: "colorized") }
    var lcdGhosting by remember { mutableStateOf(prefs.getBoolean("lcd_ghosting", false)) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val defaultNdsLayout = if (isLandscape) NdsScreenLayout.LeftRight else NdsScreenLayout.TopBottom
    var ndsScreenLayout by remember { mutableStateOf(defaultNdsLayout) }
    var swapDualScreens by remember { mutableStateOf(false) }
    var isSyncingSave by remember { mutableStateOf(false) }
    var syncSaveSuccess by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webRendererCrashed by remember { mutableStateOf(false) }
    var webViewReloadKey by remember { mutableIntStateOf(0) }
    var initialSramBase64 by remember { mutableStateOf<String?>(null) }
    var pendingConflict by remember { mutableStateOf<SaveSyncInfo?>(null) }
    var pendingDownloadedRom by remember { mutableStateOf<File?>(null) }
    var nativeEngine by remember { mutableStateOf<NativeEmulatorEngine?>(null) }
    var nativeCoreLoaded by remember { mutableStateOf(false) }

    val hasSecondaryDisplay = rememberNdsDualDisplayManager(
        engine = nativeEngine,
        isNativeMode = isNativeMode,
        swapDualScreens = swapDualScreens,
    )

    // Release the screen-on lock while the pause HUD (or crash screen) is up so the panel can sleep.
    LaunchedEffect(isPaused, activity) {
        activity?.window?.decorView?.keepScreenOn = !isPaused
    }

    val linkManager = remember { dev.vantafyn.core.emulator.net.LinkSessionManager.getInstance(context) }
    val activeLinkSession by linkManager.activeSession.collectAsState()
    LaunchedEffect(activeLinkSession, nativeEngine) {
        val eng = nativeEngine
        val ses = activeLinkSession
        if (eng != null && ses != null) {
            linkManager.configureEngineForLink(eng, ses)
        }
    }

    // Battery: freeze the emulator whenever the app leaves the foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewInstance, nativeEngine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (isNativeMode) {
                        val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                        nativeEngine?.saveSram(sramFile)
                        nativeEngine?.pause()
                    } else {
                        webViewInstance?.evaluateJavascript("window.VantafynEmulator?.flushAllSaves();", null)
                        if (!isPaused) {
                            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.pause();", null)
                        }
                        webViewInstance?.pauseTimers()
                        webViewInstance?.onPause()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (isNativeMode) {
                        if (!isPaused) {
                            nativeEngine?.resume()
                        }
                    } else {
                        webViewInstance?.resumeTimers()
                        webViewInstance?.onResume()
                        if (!isPaused) {
                            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.resumeTimers()
            webViewInstance?.onResume()
        }
    }

    // Battery: only forward axis values to the WebView when they actually change, instead of
    // echoing every gamepad/touch sample (which can be 60-120 Hz) across the JS bridge.
    val lastAxisValues = remember { mutableMapOf<String, Float>() }

    // Sync aspect ratio changes to preferences and WebView
    LaunchedEffect(aspectRatio, webViewInstance) {
        val prefStr = when (aspectRatio) {
            GameAspectRatio.Widescreen -> "16:9"
            GameAspectRatio.Square -> "1:1"
            GameAspectRatio.Standard -> "4:3"
        }
        prefs.edit().putString("default_aspect_ratio", prefStr).apply()
        val mode = when (aspectRatio) {
            GameAspectRatio.Widescreen -> "widescreen"
            GameAspectRatio.Square -> "square"
            GameAspectRatio.Standard -> "standard"
        }
        webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setAspectRatio('$mode');", null)
    }

    // Sync video filter changes to preferences, WebView, and Native Engine
    LaunchedEffect(videoFilter, webViewInstance, nativeEngine) {
        prefs.edit().putString("video_filter", videoFilter.id).apply()
        if (isNativeMode) {
            nativeEngine?.setVideoFilter(videoFilter.id)
        } else {
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setVideoFilter('${videoFilter.id}');", null)
        }
    }

    // Sync speed changes to WebView or Native Engine
    LaunchedEffect(fastForwardSpeed, webViewInstance, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.setFastForward(fastForwardSpeed.toInt())
        } else {
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setSpeed($fastForwardSpeed);", null)
        }
    }

    // Sync audio mute to WebView or Native Engine
    LaunchedEffect(isMuted, webViewInstance, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.isMuted = isMuted
        } else {
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setMute($isMuted);", null)
        }
    }

    // Sync GBA enhancements (color correction & audio anti-aliasing) to native core
    LaunchedEffect(gbaColorCorrection, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.setColorCorrection(gbaColorCorrection)
        }
    }

    LaunchedEffect(gbaAudioFiltering, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.setAudioFiltering(gbaAudioFiltering)
        }
    }

    LaunchedEffect(gbcColorCorrection, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.setGbcColorCorrection(gbcColorCorrection)
        }
    }

    LaunchedEffect(gbPalette, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.setGbPalette(gbPalette)
        }
    }

    LaunchedEffect(lcdGhosting, nativeEngine) {
        if (isNativeMode) {
            nativeEngine?.setLcdGhosting(lcdGhosting)
        }
    }

    // Intercept hardware Back press to flush saves and toggle Pause HUD
    BackHandler(enabled = true) {
        if (!isPaused) {
            if (isNativeMode) {
                val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                nativeEngine?.saveSram(sramFile)
                nativeEngine?.pause()
            } else {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.flushAllSaves(); window.VantafynEmulator?.pause();", null)
            }
            isPaused = true
        } else {
            isPaused = false
            if (isNativeMode) {
                nativeEngine?.resume()
            } else {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
            }
        }
    }

    // Step 1: Download / Cache ROM & Preload Saved Game
    LaunchedEffect(game.id) {
        isDownloading = true
        statusMessage = "Loading game & saves..."
        try {
            val file = storageManager.downloadRomIfNeeded(session, libraryId, game) { progress ->
                downloadProgress = progress
                statusMessage = "Downloading ROM: ${(progress * 100).toInt()}%"
            }

            // Ensure the exact EmulatorJS runtime and core are available before the WebView
            // starts. NDS resolves to melonDS by default; DeSmuME crashes older Android
            // WebView renderers on some Retroid builds.
            statusMessage = "Preparing emulation core..."
            storageManager.preCacheEmulatorCore(game.systemId, game.core)

            // Check save synchronization between cloud and local
            val syncInfo = storageManager.checkSaveSync(session, game.id, GameSaveKind.Sram)
            if (syncInfo.status == SaveSyncStatus.CLOUD_NEWER || syncInfo.status == SaveSyncStatus.CONFLICT) {
                pendingDownloadedRom = file
                pendingConflict = syncInfo
                isDownloading = false
                statusMessage = "Save conflict detected"
            } else {
                if (syncInfo.status == SaveSyncStatus.LOCAL_NEWER && session != null) {
                    android.util.Log.i("GamePlayerScreen", "Syncing newer offline SRAM save to cloud for ${game.id}")
                    storageManager.replaceCloudWithLocalSave(session, game.id, GameSaveKind.Sram)
                } else if (syncInfo.status == SaveSyncStatus.CLOUD_ONLY && session != null) {
                    android.util.Log.i("GamePlayerScreen", "Downloading cloud SRAM save for ${game.id}")
                    storageManager.replaceLocalWithCloudSave(session, game.id, GameSaveKind.Sram, syncInfo.cachedCloudData)
                }

                // Pre-load existing in-game battery save (SRAM) from storage or cloud
                val sramBytes = storageManager.loadSaveState(session, game.id, GameSaveKind.Sram)
                if (sramBytes != null && sramBytes.isNotEmpty()) {
                    initialSramBase64 = Base64.encodeToString(sramBytes, Base64.NO_WRAP)
                    android.util.Log.i("GamePlayerScreen", "Found existing in-game SRAM save (${sramBytes.size} bytes) for ${game.id}")
                } else {
                    initialSramBase64 = null
                    android.util.Log.i("GamePlayerScreen", "No existing SRAM found for ${game.id}, starting clean.")
                }
                if (isNativeMode) {
                    val isNdsSystem = game.systemId.lowercase() in listOf("nds", "ds") || game.core.contains("melonds", ignoreCase = true)
                    val preferredCore = prefs.getString("selected_core_${game.id}", null)
                    val effectiveCore = (preferredCore ?: game.core).trim()
                    val coreId = if (effectiveCore.isNotBlank() && (effectiveCore.contains("gambatte", ignoreCase = true) ||
                            effectiveCore.contains("tgbdual", ignoreCase = true) ||
                            effectiveCore.contains("sameboy", ignoreCase = true) ||
                            effectiveCore.contains("gpsp", ignoreCase = true) ||
                            effectiveCore.contains("mgba", ignoreCase = true) ||
                            effectiveCore.contains("melonds", ignoreCase = true))) {
                        when {
                            effectiveCore.contains("gambatte", ignoreCase = true) -> "gambatte"
                            effectiveCore.contains("tgbdual", ignoreCase = true) -> "tgbdual"
                            effectiveCore.contains("sameboy", ignoreCase = true) -> "sameboy"
                            effectiveCore.contains("gpsp", ignoreCase = true) -> "gpsp"
                            effectiveCore.contains("mgba", ignoreCase = true) -> "mgba"
                            effectiveCore.contains("melonds", ignoreCase = true) -> "melonds"
                            else -> storageManager.nativeCoreManager.getCoreIdForSystem(game.systemId)
                        }
                    } else {
                        storageManager.nativeCoreManager.getCoreIdForSystem(game.systemId)
                    }
                    statusMessage = "Loading native $coreId 64-bit core..."
                    val coreResult = storageManager.nativeCoreManager.ensureCoreInstalled(coreId) { p ->
                        downloadProgress = p
                        statusMessage = "Downloading native core: ${(p * 100).toInt()}%"
                    }
                    val coreFile = coreResult.getOrThrow()
                    
                    val engine = NativeEmulatorEngine(
                        onFatalErrorCallback = { err ->
                            android.util.Log.e("GamePlayerScreen", "Native emulator fatal error: $err")
                        },
                        onCoreShutdownCallback = {
                            android.util.Log.i("GamePlayerScreen", "Native core requested shutdown")
                        },
                    )

                    val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                    val av = withContext(Dispatchers.IO) {
                        engine.loadGame(
                            corePath = coreFile,
                            romPath = file,
                            systemDir = storageManager.nativeCoreManager.getSystemDirectory(),
                            saveDir = storageManager.nativeCoreManager.getSaveDirectory(),
                        )
                    }

                    if (av != null) {
                        if (isNdsSystem) {
                            engine.setOption("melonds_screen_layout", ndsScreenLayout.coreValue)
                        }
                        engine.setColorCorrection(gbaColorCorrection)
                        engine.setAudioFiltering(gbaAudioFiltering)
                        engine.setGbcColorCorrection(gbcColorCorrection)
                        engine.setGbPalette(gbPalette)
                        engine.setLcdGhosting(lcdGhosting)
                        engine.setVideoFilter(videoFilter.id)

                        // Configure Wireless Link Cable networking if active
                        val linkManager = dev.vantafyn.core.emulator.net.LinkSessionManager.getInstance(context)
                        if (linkManager.transportMode != dev.vantafyn.core.emulator.net.LinkTransportMode.OFFLINE) {
                            val activeSession = linkManager.activeSession.value
                            if (activeSession != null) {
                                linkManager.configureEngineForLink(engine, activeSession)
                            } else if (isNdsSystem) {
                                engine.setOption("melonds_nifi", "enabled")
                            }
                        }

                        if (sramFile.exists()) {
                            engine.loadSram(sramFile)
                        }
                        nativeEngine = engine
                        engine.start()
                        nativeCoreLoaded = true
                    } else {
                        throw IllegalStateException("Native core failed to boot game ROM.")
                    }
                }

                romFile = file
                statusMessage = "Starting emulation core..."
                isDownloading = false
            }
        } catch (e: Exception) {
            statusMessage = "Error loading game: ${e.message}"
        }
    }

    // Bitmask for native pad input
    var nativeInputMask by remember { mutableIntStateOf(0) }

    // Input Controller
    val inputController = rememberGameInputController(
        onButtonEvent = { btn, isDown ->
            if (isNativeMode) {
                val bit = when (btn) {
                    RetroButton.B -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_B
                    RetroButton.Y -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_Y
                    RetroButton.Select -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_SELECT
                    RetroButton.Start -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_START
                    RetroButton.Up -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_UP
                    RetroButton.Down -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_DOWN
                    RetroButton.Left -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_LEFT
                    RetroButton.Right -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_RIGHT
                    RetroButton.A -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_A
                    RetroButton.X -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_X
                    RetroButton.L1 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_L
                    RetroButton.R1 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_R
                    RetroButton.L2 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_L2
                    RetroButton.R2 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_R2
                    else -> -1
                }
                if (bit >= 0) {
                    nativeInputMask = if (isDown) (nativeInputMask or (1 shl bit)) else (nativeInputMask and (1 shl bit).inv())
                    nativeEngine?.setInputMask(0, nativeInputMask)
                }
            } else {
                val js = "window.VantafynEmulator?.setButton('${btn.id}', $isDown);"
                webViewInstance?.evaluateJavascript(js, null)
            }
        },
        onMenuTriggered = {
            isPaused = !isPaused
            if (isPaused) {
                if (isNativeMode) {
                    val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                    nativeEngine?.saveSram(sramFile)
                    nativeEngine?.pause()
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.pause();", null)
                }
            } else {
                if (isNativeMode) {
                    nativeEngine?.resume()
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
                }
            }
        },
        onAxisEvent = { axis, value ->
            if (lastAxisValues[axis] != value) {
                lastAxisValues[axis] = value
                val js = "window.VantafynEmulator?.setAxis('$axis', $value);"
                webViewInstance?.evaluateJavascript(js, null)
            }
        },
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .focusable()
            .onKeyEvent { event ->
                inputController.handleKeyEvent(event.nativeKeyEvent)
            },
        contentAlignment = Alignment.Center,
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val isPortraitLayout = totalHeight > (totalWidth * 1.1f) && !isTv
        val isNdsGame = game.systemId.lowercase() in listOf("nds", "ds") || game.core.contains("melonds", ignoreCase = true)
        val isGbaGame = game.systemId.lowercase() == "gba" || game.core.contains("gpsp", ignoreCase = true) || game.core.contains("mgba", ignoreCase = true)
        val isGbGame = game.systemId.lowercase() in listOf("gb", "gbc") || game.core.contains("gambatte", ignoreCase = true) || game.core.contains("tgbdual", ignoreCase = true) || game.core.contains("sameboy", ignoreCase = true)
        val ratioFloat = if (hasSecondaryDisplay && isNdsGame) {
            4f / 3f // On physical dual displays, the primary screen renders a single 256x192 DS screen (4:3)
        } else if (isNdsGame) {
            when (ndsScreenLayout) {
                NdsScreenLayout.LeftRight -> 512f / 192f // 8:3 = ~2.67 widescreen side-by-side
                NdsScreenLayout.TopBottom -> 256f / 384f // 2:3 = ~0.67 vertical stack
                NdsScreenLayout.TopOnly, NdsScreenLayout.BottomOnly -> 256f / 192f // 4:3 = 1.33 single screen
            }
        } else if (isGbaGame) {
            when (aspectRatio) {
                GameAspectRatio.Standard -> 3f / 2f // Authentic 240x160 GBA (1.5:1)
                GameAspectRatio.Widescreen -> 16f / 9f
                GameAspectRatio.Square -> 1f
            }
        } else if (isGbGame) {
            when (aspectRatio) {
                GameAspectRatio.Standard -> 10f / 9f // Authentic 160x144 Game Boy (1.11:1)
                GameAspectRatio.Widescreen -> 4f / 3f // Classic CRT / standard full screen
                GameAspectRatio.Square -> 1f
            }
        } else {
            when (aspectRatio) {
                GameAspectRatio.Standard -> 4f / 3f
                GameAspectRatio.Widescreen -> 16f / 9f
                GameAspectRatio.Square -> 1f
            }
        }
        val gameHeight = if (isPortraitLayout) (totalWidth / ratioFloat).coerceAtMost(totalHeight * 0.65f) else totalHeight
        val controlsHeight = if (isPortraitLayout) (totalHeight - gameHeight).coerceAtLeast(0.dp) else totalHeight

        // Emulator View
        if (romFile != null) {
            val gameContainerModifier = if (isPortraitLayout) {
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(gameHeight)
            } else {
                val screenAspect = if (totalHeight.value > 0f) (totalWidth.value / totalHeight.value) else 1f
                if (aspectRatio == GameAspectRatio.Widescreen && !isNativeMode) {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                } else if (ratioFloat > screenAspect) {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(ratioFloat)
                } else {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .aspectRatio(ratioFloat)
                }
            }

            Box(
                modifier = gameContainerModifier,
                contentAlignment = Alignment.Center,
            ) {
                if (isNativeMode && nativeEngine != null) {
                    NativeEmulatorSurface(
                        engine = nativeEngine!!,
                        modifier = Modifier.fillMaxSize(),
                        isDualScreen = !hasSecondaryDisplay && isNdsGame,
                        layout = if (hasSecondaryDisplay) {
                            if (swapDualScreens) NdsScreenLayout.BottomOnly else NdsScreenLayout.TopOnly
                        } else ndsScreenLayout,
                    )
                } else {
                    key(webViewReloadKey) {
                        AndroidView(
                            factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)
                                isFocusable = true
                                isFocusableInTouchMode = true
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                resumeTimers()

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    allowFileAccessFromFileURLs = true
                                    allowUniversalAccessFromFileURLs = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }

                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onGameReady() {
                                            android.util.Log.i("GamePlayerScreen", "Emulator core started and game ready for ${game.id}")
                                        }

                                        @JavascriptInterface
                                        fun onSramSaved(base64Data: String) {
                                            persistSram(base64Data, forceUpload = false)
                                        }

                                        @JavascriptInterface
                                        fun onSramSavedForced(base64Data: String) {
                                            persistSram(base64Data, forceUpload = true)
                                        }

                                        fun persistSram(base64Data: String, forceUpload: Boolean) {
                                            GameStorageManager.saveScope.launch {
                                                try {
                                                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                    android.util.Log.i("GamePlayerScreen", "Persisting in-game SRAM save (${bytes.size} bytes) for ${game.id} (force=$forceUpload)")
                                                    storageManager.saveState(session, game.id, bytes, GameSaveKind.Sram, forceUpload)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("GamePlayerScreen", "Error persisting SRAM for ${game.id}", e)
                                                }
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onRequestMenu() {
                                            isPaused = true
                                        }
                                    },
                                    "VantafynBridge",
                                )

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        if (consoleMessage != null) {
                                            android.util.Log.d("VantafynEmulatorJS", "${consoleMessage.message()} [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}]")
                                        }
                                        return true
                                    }
                                }

                                val cleanGameName = game.cleanTitle.ifEmpty { game.title }.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim()
                                val ext = game.extension.ifEmpty { romFile?.extension ?: "rom" }.removePrefix(".")
                                val friendlyRomFileName = romFile?.name ?: "$cleanGameName.$ext"

                                val corsHeaders = mapOf(
                                    "Access-Control-Allow-Origin" to "*",
                                    "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                                    "Access-Control-Allow-Headers" to "*",
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        android.util.Log.e(
                                            "GamePlayerScreen",
                                            "WebView renderer crashed while running ${game.id}; didCrash=${detail?.didCrash()} priorityAtExit=${detail?.rendererPriorityAtExit()}",
                                        )
                                        if (view == webViewInstance) {
                                            webViewInstance = null
                                        }
                                        webRendererCrashed = true
                                        isPaused = true
                                        isDownloading = false
                                        statusMessage = "Emulator renderer crashed"
                                        try {
                                            view?.stopLoading()
                                            view?.loadUrl("about:blank")
                                            view?.removeAllViews()
                                            view?.destroy()
                                        } catch (e: Exception) {
                                            android.util.Log.w("GamePlayerScreen", "Error cleaning crashed WebView", e)
                                        }
                                        return true
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): WebResourceResponse? {
                                        val url = request?.url?.toString() ?: return null
                                        val method = request.method ?: "GET"

                                        if (method.equals("OPTIONS", ignoreCase = true)) {
                                            return WebResourceResponse(
                                                "text/plain",
                                                "UTF-8",
                                                200,
                                                "OK",
                                                corsHeaders,
                                                ByteArrayInputStream(ByteArray(0)),
                                            )
                                        }

                                        if (url.startsWith("https://vantafyn.emulator/rom/") || url.endsWith("current_game.rom")) {
                                            val currentRom = romFile ?: return null
                                            return WebResourceResponse(
                                                "application/octet-stream",
                                                "UTF-8",
                                                200,
                                                "OK",
                                                corsHeaders,
                                                FileInputStream(currentRom),
                                            )
                                        }
                                        if (url.startsWith("https://cdn.emulatorjs.org/stable/data/")) {
                                            val relPath = url.removePrefix("https://cdn.emulatorjs.org/stable/data/").substringBefore('?')
                                            val cachedFile = File(storageManager.emulatorCacheDir, relPath)
                                            val mime = when {
                                                url.endsWith(".js") -> "application/javascript"
                                                url.endsWith(".css") -> "text/css"
                                                url.endsWith(".wasm") -> "application/wasm"
                                                url.endsWith(".json") -> "application/json"
                                                url.endsWith(".png") -> "image/png"
                                                url.endsWith(".svg") -> "image/svg+xml"
                                                else -> "application/octet-stream"
                                            }
                                            if (cachedFile.exists() && cachedFile.length() > 0) {
                                                return WebResourceResponse(
                                                    mime,
                                                    "UTF-8",
                                                    200,
                                                    "OK",
                                                    corsHeaders,
                                                    FileInputStream(cachedFile),
                                                )
                                            }
                                            // Transparently fetch and cache locally if connected
                                            try {
                                                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                                                    connectTimeout = 12_000
                                                    readTimeout = 25_000
                                                    instanceFollowRedirects = true
                                                }
                                                if (conn.responseCode in 200..299) {
                                                    cachedFile.parentFile?.mkdirs()
                                                    val tmp = File(cachedFile.parentFile, "${cachedFile.name}.tmp")
                                                    conn.inputStream.use { input ->
                                                        FileOutputStream(tmp).use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    tmp.renameTo(cachedFile)
                                                    return WebResourceResponse(
                                                        mime,
                                                        "UTF-8",
                                                        200,
                                                        "OK",
                                                        corsHeaders,
                                                        FileInputStream(cachedFile),
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.w("GamePlayerScreen", "Offline fetch failed for $url: ${e.message}")
                                            }
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }
                                }

                                val html = generateEmulatorHtml(
                                    systemId = game.systemId,
                                    core = game.core,
                                    gameTitle = game.cleanTitle.ifEmpty { game.title },
                                    romFileName = friendlyRomFileName,
                                    initialSramBase64 = initialSramBase64,
                                    initialFilter = videoFilter.id,
                                    initialAspectRatio = when (aspectRatio) {
                                        GameAspectRatio.Widescreen -> "widescreen"
                                        GameAspectRatio.Square -> "square"
                                        GameAspectRatio.Standard -> "standard"
                                    },
                                )
                                loadDataWithBaseURL("https://vantafyn.emulator/", html, "text/html", "UTF-8", null)
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

                // Video Filter Overlays: Authentic CRT Scanlines (Home Consoles) or LCD Grid (Handhelds)
                if (videoFilter == GameVideoFilter.Crt && !isHandheld) {
                    RetroCrtOverlay(modifier = Modifier.matchParentSize())
                } else if (videoFilter == GameVideoFilter.LcdGrid && isHandheld) {
                    RetroLcdOverlay(modifier = Modifier.matchParentSize())
                }
            }
        }

        // Loading Screen
        AnimatedVisibility(
            visible = isDownloading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0C)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        color = VantafynColors.Primary,
                        strokeWidth = 3.dp,
                    )
                    Text(
                        text = game.cleanTitle.ifEmpty { game.title },
                        color = VantafynColors.Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = statusMessage,
                        color = VantafynColors.Muted,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = webRendererCrashed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE05070D)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF151B2D),
                                    Color(0xFF090B12),
                                ),
                            ),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "Emulator renderer crashed",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "The Android WebView renderer stopped while starting this game. Your app is still running.",
                        color = VantafynColors.Muted,
                        fontSize = 14.sp,
                    )
                    Button(
                        onClick = {
                            webRendererCrashed = false
                            isPaused = false
                            statusMessage = "Restarting emulation core..."
                            webViewReloadKey++
                        },
                    ) {
                        Text("Retry")
                    }
                    Button(onClick = onExit) {
                        Text("Exit game")
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }
        }

        // Divider line between screen and controls in portrait mode
        if (isPortraitLayout) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = gameHeight)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x33FFFFFF)),
            )
        }

        // On-Screen Virtual Touchpad (Mobile/Tablet only, and hidden when physical gamepad is connected, when paused, or when disabled)
        if (!isTv && !isDownloading && !isPaused && showTouchControls) {
            val controlsContainerModifier = if (isPortraitLayout) {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(controlsHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF13131C),
                                Color(0xFF0A0A10),
                            )
                        )
                    )
            } else {
                Modifier.fillMaxSize()
            }

            Box(
                modifier = controlsContainerModifier,
            ) {
                RetroTouchOverlay(
                    visible = true,
                    systemId = game.systemId,
                    core = game.core,
                    isPortrait = isPortraitLayout,
                    onButtonPress = { btn, isDown ->
                        if (isNativeMode) {
                            val bit = when (btn) {
                                RetroButton.B -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_B
                                RetroButton.Y -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_Y
                                RetroButton.Select -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_SELECT
                                RetroButton.Start -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_START
                                RetroButton.Up -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_UP
                                RetroButton.Down -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_DOWN
                                RetroButton.Left -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_LEFT
                                RetroButton.Right -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_RIGHT
                                RetroButton.A -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_A
                                RetroButton.X -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_X
                                RetroButton.L1 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_L
                                RetroButton.R1 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_R
                                RetroButton.L2 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_L2
                                RetroButton.R2 -> NativeEmulatorEngine.RETRO_DEVICE_ID_JOYPAD_R2
                                else -> -1
                            }
                            if (bit >= 0) {
                                nativeInputMask = if (isDown) (nativeInputMask or (1 shl bit)) else (nativeInputMask and (1 shl bit).inv())
                                nativeEngine?.setInputMask(0, nativeInputMask)
                            }
                        } else {
                            val js = "window.VantafynEmulator?.setButton('${btn.id}', $isDown);"
                            webViewInstance?.evaluateJavascript(js, null)
                        }
                    },
                    onAxisChange = { axis, value ->
                        if (lastAxisValues[axis] != value) {
                            lastAxisValues[axis] = value
                            val js = "window.VantafynEmulator?.setAxis('$axis', $value);"
                            webViewInstance?.evaluateJavascript(js, null)
                        }
                    },
                    onMenuClick = {
                        isPaused = true
                        if (isNativeMode) {
                            val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                            nativeEngine?.saveSram(sramFile)
                            nativeEngine?.pause()
                        } else {
                            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.pause();", null)
                        }
                    },
                )
            }
        }

        // In-game Pause HUD Modal
        GamePauseHud(
            visible = isPaused,
            game = game,
            aspectRatio = aspectRatio,
            fastForwardSpeed = fastForwardSpeed,
            isMuted = isMuted,
            onToggleMute = {
                isMuted = !isMuted
                if (isNativeMode) {
                    nativeEngine?.isMuted = isMuted
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setVolume(${if (isMuted) 0 else 1});", null)
                }
            },
            videoFilter = videoFilter,
            onCycleVideoFilter = {
                videoFilter = videoFilter.nextForSystem(isHandheld)
                prefs.edit().putString("video_filter", videoFilter.id).apply()
            },
            showTouchControls = showTouchControls,
            onToggleTouchControls = {
                showTouchControls = !showTouchControls
            },
            hasPhysicalGamepad = hasPhysicalGamepad,
            isTv = isTv,
            isNativeMode = isNativeMode,
            isHandheld = isHandheld,
            ndsLayout = ndsScreenLayout,
            onCycleNdsLayout = {
                ndsScreenLayout = ndsScreenLayout.next()
                if (isNativeMode) {
                    nativeEngine?.setOption("melonds_screen_layout", ndsScreenLayout.coreValue)
                }
            },
            hasSecondaryDisplay = hasSecondaryDisplay,
            swapDualScreens = swapDualScreens,
            onToggleSwapDualScreens = {
                swapDualScreens = !swapDualScreens
            },
            onSyncCloudSave = {
                if (isNativeMode) {
                    isSyncingSave = true
                    syncSaveSuccess = false
                    scope.launch {
                        val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                        nativeEngine?.saveSram(sramFile)
                        if (sramFile.exists() && sramFile.length() > 0 && session != null) {
                            storageManager.saveState(session, game.id, sramFile.readBytes(), GameSaveKind.Sram, forceUpload = true)
                        }
                        delay(300)
                        isSyncingSave = false
                        syncSaveSuccess = true
                        delay(2500)
                        syncSaveSuccess = false
                    }
                }
            },
            isSyncingSave = isSyncingSave,
            syncSaveSuccess = syncSaveSuccess,
            gbaColorCorrection = gbaColorCorrection,
            onToggleGbaColorCorrection = {
                val next = !gbaColorCorrection
                gbaColorCorrection = next
                prefs.edit().putBoolean("gba_color_correction", next).apply()
            },
            gbaAudioFiltering = gbaAudioFiltering,
            onToggleGbaAudioFiltering = {
                val next = !gbaAudioFiltering
                gbaAudioFiltering = next
                prefs.edit().putBoolean("gba_audio_filtering", next).apply()
            },
            gbcColorCorrection = gbcColorCorrection,
            onToggleGbcColorCorrection = {
                val next = !gbcColorCorrection
                gbcColorCorrection = next
                prefs.edit().putBoolean("gbc_color_correction", next).apply()
            },
            gbPalette = gbPalette,
            onCycleGbPalette = {
                val next = when (gbPalette) {
                    "colorized" -> "dmg"
                    "dmg" -> "pocket"
                    else -> "colorized"
                }
                gbPalette = next
                prefs.edit().putString("gb_palette_${game.id}", next).apply()
            },
            lcdGhosting = lcdGhosting,
            onToggleLcdGhosting = {
                val next = !lcdGhosting
                lcdGhosting = next
                prefs.edit().putBoolean("lcd_ghosting", next).apply()
            },
            onResume = {
                isPaused = false
                if (isNativeMode) {
                    nativeEngine?.resume()
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
                }
            },
            onToggleSpeed = {
                fastForwardSpeed = when (fastForwardSpeed) {
                    1f -> 2f
                    2f -> 3f
                    3f -> 4f
                    4f -> 8f
                    else -> 1f
                }
                if (isNativeMode) {
                    nativeEngine?.setFastForward(fastForwardSpeed.toInt())
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setSpeed($fastForwardSpeed);", null)
                }
            },
            onCycleAspectRatio = {
                aspectRatio = when (aspectRatio) {
                    GameAspectRatio.Standard -> GameAspectRatio.Widescreen
                    GameAspectRatio.Widescreen -> GameAspectRatio.Square
                    GameAspectRatio.Square -> GameAspectRatio.Standard
                }
            },
            onReset = {
                if (isNativeMode) {
                    nativeEngine?.reset()
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.reset();", null)
                }
                isPaused = false
            },
            onExit = {
                if (isNativeMode) {
                    val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                    nativeEngine?.saveSram(sramFile)
                    if (sramFile.exists() && sramFile.length() > 0) {
                        GameStorageManager.saveScope.launch {
                            storageManager.saveState(session, game.id, sramFile.readBytes(), GameSaveKind.Sram, forceUpload = true)
                        }
                    }
                    nativeEngine?.destroy()
                    nativeEngine = null
                } else {
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.destroy();", null)
                }
                scope.launch {
                    delay(200)
                    onExit()
                }
            },
        )
    }

    if (pendingConflict != null) {
        SaveConflictDialog(
            gameTitle = game.cleanTitle.ifEmpty { game.title },
            conflict = pendingConflict!!,
            onUseCloud = {
                val conflict = pendingConflict
                val rom = pendingDownloadedRom
                scope.launch(Dispatchers.IO) {
                    if (session != null && conflict != null) {
                        storageManager.replaceLocalWithCloudSave(session, game.id, GameSaveKind.Sram, conflict.cachedCloudData)
                    }
                    val sramBytes = conflict?.cachedCloudData ?: storageManager.loadSaveState(session, game.id, GameSaveKind.Sram)
                    val base64 = sramBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    withContext(Dispatchers.Main) {
                        initialSramBase64 = base64
                        romFile = rom
                        pendingConflict = null
                        pendingDownloadedRom = null
                        statusMessage = "Starting emulation core..."
                    }
                }
            },
            onUseLocal = {
                val rom = pendingDownloadedRom
                scope.launch(Dispatchers.IO) {
                    if (session != null) {
                        storageManager.replaceCloudWithLocalSave(session, game.id, GameSaveKind.Sram)
                    }
                    val sramBytes = storageManager.loadSaveState(session, game.id, GameSaveKind.Sram)
                    val base64 = sramBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    withContext(Dispatchers.Main) {
                        initialSramBase64 = base64
                        romFile = rom
                        pendingConflict = null
                        pendingDownloadedRom = null
                        statusMessage = "Starting emulation core..."
                    }
                }
            },
            onDismiss = {
                pendingConflict = null
                pendingDownloadedRom = null
                onExit()
            },
        )
    }

    DisposableEffect(game.id) {
        val startTime = System.currentTimeMillis()
        val tracker = GamePlayTracker(context)
        tracker.recordGameLaunched(game)
        onDispose {
            val durationMs = System.currentTimeMillis() - startTime
            if (durationMs > 2000L) {
                tracker.recordPlaySession(game, durationMs)
            }
            if (isNativeMode) {
                val sramFile = storageManager.getLocalSaveFile(game.id, GameSaveKind.Sram)
                nativeEngine?.saveSram(sramFile)
                if (sramFile.exists() && sramFile.length() > 0) {
                    GameStorageManager.saveScope.launch {
                        storageManager.saveState(session, game.id, sramFile.readBytes(), GameSaveKind.Sram, forceUpload = true)
                    }
                }
                nativeEngine?.destroy()
                nativeEngine = null
                try {
                    dev.vantafyn.core.emulator.net.LinkSessionManager.getInstance(context).disconnect()
                } catch (e: Exception) {
                    android.util.Log.w("GamePlayerScreen", "Error disconnecting link: ${e.message}")
                }
            }
            val wv = webViewInstance
            webViewInstance = null
            wv?.evaluateJavascript("window.VantafynEmulator?.destroy();", null)
            wv?.stopLoading()
            wv?.postDelayed({
                try {
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeAllViews()
                    wv.destroy()
                } catch (e: Exception) {
                    android.util.Log.w("GamePlayerScreen", "Error destroying webView", e)
                }
            }, 300L)
        }
    }
}

/**
 * Generates the self-contained HTML/JS emulator runner that interfaces with the WebAssembly Libretro core.
 */
private fun generateEmulatorHtml(
    systemId: String,
    core: String,
    gameTitle: String,
    romFileName: String,
    initialSramBase64: String?,
    initialFilter: String = "crisp",
    initialAspectRatio: String = "standard",
): String {
    val key = core.ifBlank { systemId }.lowercase().trim()
    val ext = romFileName.substringAfterLast('.', "").lowercase().trim()
    val systemCoreName = when {
        key == "gba" || key.contains("advance") || ext == "gba" || ext == "agb" -> "gba"
        key == "gb" || key == "gbc" || key.contains("color") || key.contains("gameboy") || key.contains("game boy") || ext == "gb" || ext == "gbc" -> "gb"
        key == "snes" || key.contains("super nintendo") || key == "sfc" || ext == "sfc" || ext == "smc" -> "snes"
        key == "nes" || key.contains("famicom") || ext == "nes" -> "nes"
        key == "segamd" || key.contains("genesis") || key.contains("sega") || key.contains("megadrive") || ext == "gen" || ext == "smd" || ext == "md" -> "segaMD"
        key == "n64" || key.contains("nintendo 64") || ext == "z64" || ext == "n64" || ext == "v64" -> "n64"
        key.contains("desmume") -> "desmume"
        key == "nds" || key == "ds" || key.contains("nintendo ds") || key.contains("melonds") || ext == "nds" -> "melonds"
        key == "psx" || key == "ps1" || key.contains("playstation") || ext == "chd" || ext == "pbp" || ext == "cue" -> "psx"
        else -> key
    }

    val safeRomFileName = romFileName.replace("'", "").replace("\"", "").replace("/", "_")
    val safeRomStem = safeRomFileName.substringBeforeLast('.').replace("'", "\\'").replace("\"", "\\\"")
    val safeGameTitle = gameTitle.replace("'", "\\'").replace("\"", "\\\"")
    val safeGameName = safeRomStem.ifBlank { safeGameTitle }
    val gameIdHash = Math.abs(gameTitle.hashCode()).coerceAtLeast(1)
    val dsLayoutOption = when (systemCoreName) {
        "desmume" -> "'desmume_screens_layout': (window.innerWidth > window.innerHeight ? 'left/right' : 'top/bottom'),"
        "melonds" -> "'melonds_screen_layout': (window.innerWidth > window.innerHeight ? 'Left/Right' : 'Top/Bottom'),"
        else -> ""
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body, html { width: 100%; height: 100%; overflow: hidden; background-color: #000; position: relative; }
                #game-container { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
                canvas { width: 100% !important; height: 100% !important; object-fit: contain; image-rendering: pixelated; }
                canvas.widescreen-fill { object-fit: fill !important; }

                /* CRT Scanline and Vignette Layer */
                #vantafyn-crt-overlay {
                    position: absolute;
                    top: 0; left: 0; right: 0; bottom: 0;
                    width: 100%; height: 100%;
                    pointer-events: none;
                    z-index: 9999;
                    display: ${if (initialFilter == "crt") "block" else "none"};
                    background: linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.35) 50%), linear-gradient(90deg, rgba(255, 0, 0, 0.04), rgba(0, 255, 0, 0.02), rgba(0, 0, 255, 0.04));
                    background-size: 100% 4px, 6px 100%;
                    box-shadow: inset 0 0 70px rgba(0, 0, 0, 0.65);
                }

                /* Completely hide and disable EmulatorJS built-in menus, settings dialogs, popups and touch controls */
                .ejs_virtualGamepad_parent,
                .ejs_virtualGamepad_open,
                [class*="ejs_virtualGamepad"],
                .ejs_dpad_main,
                .b_speed_fast,
                .b_speed_slow,
                .b_speed_rewind,
                .ejs_menu,
                #ejs_menu,
                .ejs_menu_bar,
                #ejs_menu_bar,
                .ejs_menu_button,
                .ejs_modal,
                [class*="ejs_modal"],
                .ejs_settings,
                .ejs_dialog,
                .ejs_popup,
                .ejs_overlay,
                #ejs_overlay,
                .ejs_controls_btn {
                    display: none !important;
                    visibility: hidden !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                    width: 0 !important;
                    height: 0 !important;
                    position: absolute !important;
                    top: -9999px !important;
                    left: -9999px !important;
                }
            </style>
        </head>
        <body>
            <div id="game-container"></div>
            <div id="vantafyn-crt-overlay"></div>
            <script>
                window.EJS_player = '#game-container';
                window.EJS_core = '$systemCoreName';
                window.EJS_gameName = '$safeGameName';
                window.EJS_gameID = $gameIdHash;
                window.EJS_gameUrl = 'https://vantafyn.emulator/rom/' + encodeURIComponent('$safeRomFileName');
                window.EJS_pathtodata = 'https://cdn.emulatorjs.org/stable/data/';
                window.EJS_startOnLoaded = true;
                window.EJS_color = '#21D8FF';
                window.EJS_backgroundColor = '#000000';
                window.EJS_disableUI = true;
                window.VantafynIsNintendoDsCore = ${if (systemCoreName == "melonds" || systemCoreName == "desmume") "true" else "false"};
                window.EJS_defaultOptions = {
                    'virtual-gamepad': 'disabled',
                    'menu-bar-button': 'hidden',
                    'save-save-interval': '15',
                    'save-state-location': 'browser',
                    'fps-limit': '60',
                    $dsLayoutOption
                };

                // Battery: only keep the compositor warm while the page is visible and the emulator
                // is running. Cleared on hide/pause so it can't spin in the background.
                var vsyncKeepAliveInterval = null;
                function startVsyncKeepAlive() {
                    if (vsyncKeepAliveInterval || document.hidden || window.VantafynEmulatorPaused) return;
                    vsyncKeepAliveInterval = setInterval(function() {
                        if (window.requestAnimationFrame) {
                            window.requestAnimationFrame(function() {});
                        }
                    }, 250);
                }
                function stopVsyncKeepAlive() {
                    if (vsyncKeepAliveInterval) {
                        clearInterval(vsyncKeepAliveInterval);
                        vsyncKeepAliveInterval = null;
                    }
                }
                document.addEventListener('visibilitychange', function() {
                    if (document.hidden) { stopVsyncKeepAlive(); } else { startVsyncKeepAlive(); }
                });
                startVsyncKeepAlive();

                window.VantafynInitialSram = ${if (initialSramBase64 != null) "\"$initialSramBase64\"" else "null"};
                window._lastSramHash = window.VantafynInitialSram;

                var cleanUpBuiltinControls = function() {
                    var elems = document.querySelectorAll('.ejs_virtualGamepad_parent, .ejs_virtualGamepad_open, [class*="ejs_virtualGamepad"], .ejs_dpad_main, .b_speed_fast, .b_speed_slow, .ejs_menu, #ejs_menu, .ejs_modal, [class*="ejs_modal"], .ejs_settings, .ejs_popup, .ejs_dialog, .ejs_overlay, #ejs_overlay, .ejs_menu_bar, #ejs_menu_bar');
                    for (var i = 0; i < elems.length; i++) {
                        elems[i].style.display = 'none';
                        try { elems[i].remove(); } catch(e) {}
                    }
                    if (window.EJS_emulator) {
                        if (window.EJS_emulator.closeMenu) {
                            try { window.EJS_emulator.closeMenu(); } catch(e) {}
                        }
                        if (window.EJS_emulator.closeModal) {
                            try { window.EJS_emulator.closeModal(); } catch(e) {}
                        }
                        if (window.EJS_emulator.toggleVirtualGamepad) {
                            try { window.EJS_emulator.toggleVirtualGamepad(false); } catch(e) {}
                        }
                        if (window.EJS_emulator.virtualGamepad) {
                            window.EJS_emulator.virtualGamepad.style.display = 'none';
                            try { window.EJS_emulator.virtualGamepad.remove(); } catch(e) {}
                        }
                        if (window.EJS_emulator.elements && window.EJS_emulator.elements.menuToggle) {
                            window.EJS_emulator.elements.menuToggle.style.display = 'none';
                            try { window.EJS_emulator.elements.menuToggle.remove(); } catch(e) {}
                        }
                    }
                    try {
                        var cv = document.querySelector('canvas');
                        if (cv && window._currentAspectRatioMode === 'widescreen') {
                            if (!cv.classList.contains('widescreen-fill')) {
                                cv.classList.add('widescreen-fill');
                                cv.style.setProperty('object-fit', 'fill', 'important');
                            }
                        }
                    } catch(e) {}
                };

                var cleanUpInterval = setInterval(cleanUpBuiltinControls, 400);

                function uint8ToBase64(bytes) {
                    var binary = '';
                    var len = bytes.byteLength;
                    var chunkSize = 0x8000;
                    for (var i = 0; i < len; i += chunkSize) {
                        var chunk = bytes.subarray(i, Math.min(i + chunkSize, len));
                        binary += String.fromCharCode.apply(null, chunk);
                    }
                    return btoa(binary);
                }

                function base64ToUint8(b64) {
                    var binary = atob(b64);
                    var len = binary.length;
                    var bytes = new Uint8Array(len);
                    for (var i = 0; i < len; i++) {
                        bytes[i] = binary.charCodeAt(i);
                    }
                    return bytes;
                }

                function extractSramData(gm) {
                    if (!gm) return null;
                    var savePath = "";
                    try {
                        if (typeof gm.getSaveFilePath === 'function') savePath = gm.getSaveFilePath();
                    } catch(e) {}
                    try {
                        gm.saveSaveFiles();
                    } catch(e) {}
                    try {
                        var s = gm.getSaveFile(false);
                        if (s && s.length > 0) {
                            console.log("Vantafyn: extracted SRAM from " + savePath + " (" + s.length + " bytes)");
                            return (s instanceof Uint8Array) ? s : new Uint8Array(s);
                        }
                    } catch(e) {}
                    try {
                        if (savePath && gm.FS && gm.FS.analyzePath(savePath).exists) {
                            var d = gm.FS.readFile(savePath);
                            if (d && d.length > 0) {
                                console.log("Vantafyn: extracted SRAM from fallback " + savePath + " (" + d.length + " bytes)");
                                return (d instanceof Uint8Array) ? d : new Uint8Array(d);
                            }
                        }
                    } catch(e) {}
                    console.warn("Vantafyn: no SRAM produced, core save path was '" + savePath + "'");
                    return null;
                }

                function writeSramFiles(fs) {
                    if (!window.VantafynInitialSram || !fs) return;
                    try {
                        var bytes = base64ToUint8(window.VantafynInitialSram);
                        var dirs = ["/data", "/data/saves"];
                        dirs.forEach(function(d) {
                            try { if (!fs.analyzePath(d).exists) fs.mkdir(d); } catch(e) {}
                        });
                        var candidateStems = ["$safeGameName"];
                        var candidatePaths = [];
                        try {
                            if (window.EJS_emulator && window.EJS_emulator.gameManager && typeof window.EJS_emulator.gameManager.getSaveFilePath === 'function') {
                                var sfp = window.EJS_emulator.gameManager.getSaveFilePath();
                                if (sfp) candidatePaths.push(sfp);
                            }
                        } catch(e) {}
                        if (window.EJS_emulator) {
                            if (window.EJS_emulator.fileName) {
                                var fn = window.EJS_emulator.fileName.replace(/\.[^/.]+$/, "");
                                candidateStems.push(fn);
                            }
                            if (typeof window.EJS_emulator.getBaseFileName === 'function') {
                                var b = window.EJS_emulator.getBaseFileName(true);
                                if (b) {
                                    candidateStems.push(b.replace(/\.[^/.]+$/, ""));
                                }
                            }
                        }
                        candidateStems.forEach(function(stem) {
                            if (!stem) return;
                            candidatePaths.push("/data/saves/" + stem + ".srm");
                            candidatePaths.push("/data/saves/" + stem + ".sav");
                            candidatePaths.push("/data/saves/" + stem);
                        });
                        if (window.VantafynIsNintendoDsCore && candidatePaths.length === 0) {
                            console.warn("Vantafyn: Skipping DS SRAM preload because core save path is not ready yet");
                            return;
                        }

                        var seen = {};
                        var writes = 0;
                        candidatePaths.forEach(function(p) {
                            if (seen[p]) return;
                            if (writes >= (window.VantafynIsNintendoDsCore ? 1 : 8)) return;
                            seen[p] = true;
                            try {
                                if (fs.analyzePath(p).exists) fs.unlink(p);
                                fs.writeFile(p, bytes);
                                writes++;
                                console.log("Vantafyn: Preloaded SRAM into " + p + " (" + bytes.length + " bytes)");
                            } catch(e) {
                                console.warn("Vantafyn: Could not write SRAM to " + p, e);
                            }
                        });
                    } catch(err) {
                        console.error("Vantafyn: Failed to inject initial SRAM", err);
                    }
                }

                window.EJS_ready = function() {
                    console.log("Vantafyn: EJS_ready fired");
                    if (window.EJS_emulator) {
                        window.EJS_emulator.on("saveDatabaseLoaded", function(fs) {
                            console.log("Vantafyn: saveDatabaseLoaded fired!");
                            writeSramFiles(fs || (window.EJS_emulator.gameManager && window.EJS_emulator.gameManager.FS));
                        });
                        window.EJS_emulator.on("saveSaveFiles", function(data) {
                            var sram = data || (window.EJS_emulator.gameManager && extractSramData(window.EJS_emulator.gameManager));
                            if (sram && sram.length > 0 && window.VantafynBridge) {
                                var bytes = (sram instanceof Uint8Array) ? sram : new Uint8Array(sram);
                                var b64 = uint8ToBase64(bytes);
                                if (b64 !== window._lastSramHash) {
                                    window._lastSramHash = b64;
                                    console.log("Vantafyn: In-game SRAM updated via saveSaveFiles event (" + bytes.length + " bytes)");
                                    window.VantafynBridge.onSramSaved(b64);
                                }
                            }
                        });
                    }
                };

                window.EJS_onGameStart = function() {
                    cleanUpBuiltinControls();
                    if (window.EJS_emulator && window.EJS_emulator.gameManager && window.EJS_emulator.gameManager.FS) {
                        var gm = window.EJS_emulator.gameManager;
                        writeSramFiles(gm.FS);
                        if (typeof gm.loadSaveFiles === 'function') {
                            try {
                                gm.loadSaveFiles();
                                console.log("Vantafyn: Refreshed save files in Libretro core via loadSaveFiles()");
                            } catch(e) {
                                console.warn("Vantafyn: loadSaveFiles error", e);
                            }
                        }
                    }
                    if (window.VantafynBridge) {
                        window.VantafynBridge.onGameReady();
                    }
                    if (window.VantafynEmulator) {
                        window.VantafynEmulator.setVideoFilter('$initialFilter');
                        window.VantafynEmulator.setAspectRatio('$initialAspectRatio');
                    }
                };

                window.EJS_onSaveUpdate = function(e) {
                    if (e && e.save && window.VantafynBridge) {
                        var bytes = (e.save instanceof Uint8Array) ? e.save : new Uint8Array(e.save);
                        var b64 = uint8ToBase64(bytes);
                        if (b64 !== window._lastSramHash) {
                            window._lastSramHash = b64;
                            console.log("Vantafyn: In-game SRAM updated via EJS_onSaveUpdate (" + bytes.length + " bytes)");
                            window.VantafynBridge.onSramSaved(b64);
                        }
                    }
                };

                window.VantafynEmulator = {
                    setMute: function(muted) {
                        try {
                            var emu = window.EJS_emulator;
                            if (emu) {
                                if (typeof emu.setVolume === 'function') emu.setVolume(muted ? 0 : 1);
                                if (typeof emu.changeSettingOption === 'function') emu.changeSettingOption('volume', muted ? '0' : '100');
                                emu.muted = !!muted;
                            }
                            var audios = document.querySelectorAll('audio, video');
                            for (var i = 0; i < audios.length; i++) {
                                audios[i].muted = muted;
                            }
                        } catch(e) {
                            console.warn("Vantafyn: setMute error", e);
                        }
                    },
                    setVideoFilter: function(filterId) {
                        try {
                            var cv = document.querySelector('canvas');
                            var crtOverlay = document.getElementById('vantafyn-crt-overlay');
                            var emu = window.EJS_emulator;
                            if (filterId === 'crt') {
                                if (crtOverlay) crtOverlay.style.display = 'block';
                                if (cv) {
                                    cv.style.filter = 'contrast(1.10) brightness(1.04)';
                                    cv.style.imageRendering = 'pixelated';
                                }
                                if (emu && typeof emu.changeSettingOption === 'function') {
                                    emu.changeSettingOption('shader', 'crt-aperture');
                                }
                            } else if (filterId === 'smooth') {
                                if (crtOverlay) crtOverlay.style.display = 'none';
                                if (cv) {
                                    cv.style.filter = 'blur(0.4px)';
                                    cv.style.imageRendering = 'auto';
                                }
                                if (emu && typeof emu.changeSettingOption === 'function') {
                                    emu.changeSettingOption('shader', 'bicubic');
                                }
                            } else {
                                // crisp pixel
                                if (crtOverlay) crtOverlay.style.display = 'none';
                                if (cv) {
                                    cv.style.filter = 'none';
                                    cv.style.imageRendering = 'pixelated';
                                }
                                if (emu && typeof emu.changeSettingOption === 'function') {
                                    emu.changeSettingOption('shader', 'disabled');
                                }
                            }
                        } catch(e) {
                            console.warn("Vantafyn: setVideoFilter error", e);
                        }
                    },
                    setAspectRatio: function(mode) {
                        try {
                            window._currentAspectRatioMode = mode;
                            var cv = document.querySelector('canvas');
                            if (!cv) return;
                            if (mode === 'widescreen') {
                                cv.classList.add('widescreen-fill');
                                cv.style.setProperty('object-fit', 'fill', 'important');
                            } else {
                                cv.classList.remove('widescreen-fill');
                                cv.style.setProperty('object-fit', 'contain', 'important');
                            }
                        } catch(e) {
                            console.warn("Vantafyn: setAspectRatio error", e);
                        }
                    },
                    pause: function() {
                        try {
                            window.VantafynEmulatorPaused = true;
                            stopVsyncKeepAlive();
                            var emu = window.EJS_emulator;
                            if (!emu) return;
                            if (typeof emu.pause === 'function') emu.pause();
                            if (emu.gameManager && typeof emu.gameManager.toggleMainLoop === 'function') {
                                emu.gameManager.toggleMainLoop(0);
                            }
                        } catch(e) { console.warn("Vantafyn: pause error", e); }
                    },
                    resume: function() {
                        try {
                            window.VantafynEmulatorPaused = false;
                            startVsyncKeepAlive();
                            var emu = window.EJS_emulator;
                            if (!emu) return;
                            if (typeof emu.play === 'function') emu.play();
                            if (typeof emu.resume === 'function') emu.resume();
                            if (emu.gameManager && typeof emu.gameManager.toggleMainLoop === 'function') {
                                emu.gameManager.toggleMainLoop(1);
                            }
                        } catch(e) { console.warn("Vantafyn: resume error", e); }
                    },
                    reset: function() {
                        var gm = window.EJS_emulator && window.EJS_emulator.gameManager;
                        if (gm && typeof gm.restart === 'function') {
                            gm.restart();
                        } else if (window.EJS_emulator && window.EJS_emulator.restart) {
                            window.EJS_emulator.restart();
                        }
                    },
                    setSpeed: function(speed) {
                        try {
                            var emu = window.EJS_emulator;
                            if (!emu) return;
                            var ratioStr = (speed > 1) ? speed.toFixed(1) : '1.0';
                            if (typeof emu.changeSettingOption === 'function') {
                                emu.changeSettingOption('ff-ratio', ratioStr);
                            }
                            if (emu.gameManager && typeof emu.gameManager.toggleFastForward === 'function') {
                                emu.gameManager.toggleFastForward(speed > 1 ? 1 : 0);
                            }
                            if (typeof emu.setSpeed === 'function') {
                                emu.setSpeed(speed);
                            }
                        } catch(e) {
                            console.warn("Vantafyn: setSpeed error", e);
                        }
                    },
                    flushAllSaves: function() {
                        if (!window.EJS_emulator || !window.EJS_emulator.gameManager) return;
                        var gm = window.EJS_emulator.gameManager;
                        try {
                            var sram = extractSramData(gm);
                            if (sram && sram.length > 0 && window.VantafynBridge) {
                                var b64 = uint8ToBase64(sram);
                                window._lastSramHash = b64;
                                console.log("Vantafyn: flushAllSaves saved SRAM (" + sram.length + " bytes)");
                                window.VantafynBridge.onSramSavedForced(b64);
                            }
                        } catch(e) {
                            console.error("Vantafyn: flushAllSaves SRAM error", e);
                        }
                    },
                    requestSaveSram: function() {
                        var emu = window.EJS_emulator;
                        var gm = emu && emu.gameManager;
                        if (!gm) return;
                        try {
                            var sram = extractSramData(gm);
                            if (sram && sram.length > 0 && window.VantafynBridge) {
                                var b64 = uint8ToBase64(sram);
                                window._lastSramHash = b64;
                                window.VantafynBridge.onSramSavedForced(b64);
                            }
                        } catch(e) {
                            console.error("Vantafyn: Failed to get SRAM", e);
                        }
                    },
                    loadSram: function(base64) {
                        if (!base64) return;
                        window.VantafynInitialSram = base64;
                        var emu = window.EJS_emulator;
                        var gm = emu && emu.gameManager;
                        if (gm && gm.FS) {
                            writeSramFiles(gm.FS);
                            if (typeof gm.loadSaveFiles === 'function') {
                                try { gm.loadSaveFiles(); } catch(e) {}
                            }
                        }
                    },

                    setAxis: function(axis, value) {
                        if (!window.EJS_emulator || !window.EJS_emulator.gameManager) return;
                        var gm = window.EJS_emulator.gameManager;
                        var fn = (typeof gm.simulateInput === 'function') ? gm.simulateInput.bind(gm) : (gm.functions && typeof gm.functions.simulateInput === 'function' ? gm.functions.simulateInput.bind(gm.functions) : null);
                        if (!fn) return;
                        var maxVal = 0x7fff;
                        if (axis === 'left_x') {
                            if (value > 0.05) {
                                fn(0, 16, Math.round(maxVal * value));
                                fn(0, 17, 0);
                            } else if (value < -0.05) {
                                fn(0, 17, Math.round(maxVal * -value));
                                fn(0, 16, 0);
                            } else {
                                fn(0, 16, 0);
                                fn(0, 17, 0);
                            }
                        } else if (axis === 'left_y') {
                            if (value > 0.05) {
                                fn(0, 18, Math.round(maxVal * value));
                                fn(0, 19, 0);
                            } else if (value < -0.05) {
                                fn(0, 19, Math.round(maxVal * -value));
                                fn(0, 18, 0);
                            } else {
                                fn(0, 18, 0);
                                fn(0, 19, 0);
                            }
                        }
                    },
                    setButton: function(btn, isDown) {
                        var btnLower = btn.toLowerCase();
                        var isN64 = '$systemCoreName' === 'n64';
                        var btnMap = {
                            'b': isN64 ? 1 : 0,
                            'y': 1,
                            'select': 2,
                            'start': 3,
                            'up': 4,
                            'down': 5,
                            'left': 6,
                            'right': 7,
                            'a': isN64 ? 0 : 8,
                            'x': 9,
                            'l1': 10,
                            'l': 10,
                            'r1': 11,
                            'r': 11,
                            'l2': 12,
                            'r2': 13,
                            'z': 12,
                            'c_right': 20,
                            'c_left': 21,
                            'c_down': 22,
                            'c_up': 23,
                            'thumbl': 14,
                            'thumbr': 15
                        };
                        var idx = btnMap[btnLower];
                        if (idx !== undefined && window.EJS_emulator) {
                            var isSpecial = (idx >= 16 && idx <= 23);
                            var val = isDown ? (isSpecial ? 0x7fff : 1) : 0;
                            try {
                                if (window.EJS_emulator.gameManager && typeof window.EJS_emulator.gameManager.simulateInput === 'function') {
                                    window.EJS_emulator.gameManager.simulateInput(0, idx, val);
                                } else if (window.EJS_emulator.gameManager && window.EJS_emulator.gameManager.functions && typeof window.EJS_emulator.gameManager.functions.simulateInput === 'function') {
                                    window.EJS_emulator.gameManager.functions.simulateInput(0, idx, val);
                                }
                            } catch(e) {
                                console.error('Error simulating input:', e);
                            }
                        }
                    },
                    destroy: function() {
                        try {
                            if (typeof cleanUpInterval !== 'undefined') clearInterval(cleanUpInterval);
                            stopVsyncKeepAlive();
                        } catch(e) {}
                        try {
                            if (window.VantafynEmulator && window.VantafynEmulator.flushAllSaves) {
                                window.VantafynEmulator.flushAllSaves();
                            }
                        } catch(e) {}
                        try {
                            if (window.EJS_emulator && window.EJS_emulator.stop) {
                                window.EJS_emulator.stop();
                            }
                        } catch(e) {}
                    }
                };

                // Load EmulatorJS loader script
                var script = document.createElement('script');
                script.src = 'https://cdn.emulatorjs.org/stable/data/loader.js';
                document.body.appendChild(script);
            </script>
        </body>
        </html>
    """.trimIndent()
}
