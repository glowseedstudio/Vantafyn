package dev.vantafyn.feature.player.games

import android.annotation.SuppressLint
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.vantafyn.core.jellyfin.GameDetail
import dev.vantafyn.core.jellyfin.GamePlayTracker
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GamePlayerScreen(
    game: GameDetail,
    libraryId: String,
    session: JellyfinSession,
    gamesRepository: JellyfinGamesRepository,
    isTv: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { GameStorageManager(context, gamesRepository) }

    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val window = activity?.window
        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val previousBehavior = controller.systemBarsBehavior
            val previousKeepScreenOn = window.decorView.keepScreenOn

            window.decorView.keepScreenOn = true
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())

            onDispose {
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

    var isPaused by remember { mutableStateOf(false) }
    var isSavingState by remember { mutableStateOf(false) }
    var saveStateSuccess by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(GameAspectRatio.Standard) }
    var fastForwardSpeed by remember { mutableFloatStateOf(1f) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var hasPhysicalGamepad by remember { mutableStateOf(GameInputController.isGamepadConnected()) }
    var initialSramBase64 by remember { mutableStateOf<String?>(null) }

    // Intercept hardware Back press to flush saves and toggle Pause HUD
    BackHandler(enabled = true) {
        if (!isPaused) {
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.flushAllSaves(); window.VantafynEmulator?.pause();", null)
            isPaused = true
        } else {
            isPaused = false
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
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
            // Pre-load existing in-game battery save (SRAM) from storage or cloud
            val sramBytes = storageManager.loadSaveState(session, game.id, GameSaveKind.Sram)
            if (sramBytes != null && sramBytes.isNotEmpty()) {
                initialSramBase64 = Base64.encodeToString(sramBytes, Base64.NO_WRAP)
                android.util.Log.i("GamePlayerScreen", "Found existing in-game SRAM save (${sramBytes.size} bytes) for ${game.id}")
            } else {
                initialSramBase64 = null
                android.util.Log.i("GamePlayerScreen", "No existing SRAM found for ${game.id}, starting clean.")
            }
            romFile = file
            statusMessage = "Starting emulation core..."
            isDownloading = false
        } catch (e: Exception) {
            statusMessage = "Error loading game: ${e.message}"
        }
    }

    // Input Controller
    val inputController = rememberGameInputController(
        onButtonEvent = { btn, isDown ->
            val js = "window.VantafynEmulator?.setButton('${btn.id}', $isDown);"
            webViewInstance?.evaluateJavascript(js, null)
        },
        onMenuTriggered = {
            isPaused = !isPaused
            if (isPaused) {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.pause();", null)
            } else {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
            }
        },
        onAxisEvent = { axis, value ->
            val js = "window.VantafynEmulator?.setAxis('$axis', $value);"
            webViewInstance?.evaluateJavascript(js, null)
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onKeyEvent { event ->
                inputController.handleKeyEvent(event.nativeKeyEvent)
            },
        contentAlignment = Alignment.Center,
    ) {
        // Emulator View
        if (romFile != null) {
            val ratioFloat = when (aspectRatio) {
                GameAspectRatio.Standard -> 4f / 3f
                GameAspectRatio.Widescreen -> 16f / 9f
                GameAspectRatio.Square -> 1f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(ratioFloat, matchHeightConstraintsFirst = true),
                ) {
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

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    allowFileAccess = true
                                    allowContentAccess = true
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
                                        fun onStateSaved(base64Data: String) {
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    isSavingState = true
                                                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                    android.util.Log.i("GamePlayerScreen", "Writing State save (${bytes.size} bytes) for ${game.id}")
                                                    val result = storageManager.saveState(session, game.id, bytes, GameSaveKind.State)
                                                    isSavingState = false
                                                    saveStateSuccess = result.isSuccess
                                                    delay(2500)
                                                    saveStateSuccess = false
                                                } catch (e: Exception) {
                                                    android.util.Log.e("GamePlayerScreen", "Error saving State for ${game.id}", e)
                                                    isSavingState = false
                                                }
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onSramSaved(base64Data: String) {
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                    android.util.Log.i("GamePlayerScreen", "Persisting in-game SRAM save (${bytes.size} bytes) for ${game.id}")
                                                    storageManager.saveState(session, game.id, bytes, GameSaveKind.Sram)
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
                                val friendlyRomFileName = "$cleanGameName.$ext"

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): WebResourceResponse? {
                                        val url = request?.url?.toString() ?: return null
                                        if (url.startsWith("https://vantafyn.emulator/rom/") || url.endsWith("current_game.rom")) {
                                            val currentRom = romFile ?: return null
                                            return WebResourceResponse(
                                                "application/octet-stream",
                                                "UTF-8",
                                                FileInputStream(currentRom),
                                            )
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

        // On-Screen Virtual Touchpad (Mobile/Tablet only, and hidden when physical gamepad is connected or when paused)
        if (!isTv && !isDownloading && !isPaused && !hasPhysicalGamepad) {
            RetroTouchOverlay(
                visible = true,
                systemId = game.systemId,
                core = game.core,
                onButtonPress = { btn, isDown ->
                    val js = "window.VantafynEmulator?.setButton('${btn.id}', $isDown);"
                    webViewInstance?.evaluateJavascript(js, null)
                },
                onAxisChange = { axis, value ->
                    val js = "window.VantafynEmulator?.setAxis('$axis', $value);"
                    webViewInstance?.evaluateJavascript(js, null)
                },
                onMenuClick = {
                    isPaused = true
                    webViewInstance?.evaluateJavascript("window.VantafynEmulator?.pause();", null)
                },
            )
        }

        // In-game Pause HUD Modal
        GamePauseHud(
            visible = isPaused,
            game = game,
            aspectRatio = aspectRatio,
            fastForwardSpeed = fastForwardSpeed,
            isSavingState = isSavingState,
            saveStateSuccess = saveStateSuccess,
            onResume = {
                isPaused = false
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
            },
            onQuickSave = {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.requestSaveState(); window.VantafynEmulator?.requestSaveSram();", null)
            },
            onQuickLoad = {
                scope.launch(Dispatchers.IO) {
                    val stateData = storageManager.loadSaveState(session, game.id, GameSaveKind.State)
                    if (stateData != null && stateData.isNotEmpty()) {
                        val base64 = Base64.encodeToString(stateData, Base64.NO_WRAP)
                        withContext(Dispatchers.Main) {
                            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.loadState('$base64');", null)
                            isPaused = false
                        }
                    }
                }
            },
            onToggleSpeed = {
                fastForwardSpeed = when (fastForwardSpeed) {
                    1f -> 2f
                    2f -> 4f
                    else -> 1f
                }
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.setSpeed($fastForwardSpeed);", null)
            },
            onCycleAspectRatio = {
                aspectRatio = when (aspectRatio) {
                    GameAspectRatio.Standard -> GameAspectRatio.Widescreen
                    GameAspectRatio.Widescreen -> GameAspectRatio.Square
                    GameAspectRatio.Square -> GameAspectRatio.Standard
                }
            },
            onReset = {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.reset();", null)
                isPaused = false
            },
            onExit = {
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.flushAllSaves();", null)
                scope.launch {
                    delay(350)
                    onExit()
                }
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
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.flushAllSaves();", null)
            val wv = webViewInstance
            webViewInstance = null
            wv?.postDelayed({
                try {
                    wv.destroy()
                } catch (e: Exception) {
                    android.util.Log.w("GamePlayerScreen", "Error destroying webView", e)
                }
            }, 1000L)
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
): String {
    val key = core.ifBlank { systemId }.lowercase().trim()
    val systemCoreName = when {
        key == "gba" || key.contains("advance") -> "gba"
        key == "gb" || key == "gbc" || key.contains("color") -> "gb"
        key == "snes" || key.contains("super nintendo") -> "snes"
        key == "nes" || key.contains("famicom") -> "nes"
        key == "segamd" || key.contains("genesis") || key.contains("sega") || key.contains("megadrive") -> "segaMD"
        key == "n64" || key.contains("nintendo 64") -> "n64"
        key == "nds" || key.contains("ds") -> "nds"
        key == "psx" || key == "ps1" || key.contains("playstation") -> "psx"
        else -> key
    }

    val safeRomFileName = romFileName.replace("'", "").replace("\"", "").replace("/", "_")
    val safeRomStem = safeRomFileName.substringBeforeLast('.').replace("'", "\\'").replace("\"", "\\\"")
    val safeGameTitle = gameTitle.replace("'", "\\'").replace("\"", "\\\"")
    val safeGameName = safeRomStem.ifBlank { safeGameTitle }
    val gameIdHash = Math.abs(gameTitle.hashCode()).coerceAtLeast(1)

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body, html { width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                #game-container { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
                canvas { width: 100% !important; height: 100% !important; object-fit: contain; image-rendering: pixelated; }

                /* Completely hide and disable EmulatorJS built-in virtual touch controls */
                .ejs_virtualGamepad_parent,
                .ejs_virtualGamepad_open,
                [class*="ejs_virtualGamepad"],
                .ejs_dpad_main,
                .b_speed_fast,
                .b_speed_slow,
                .b_speed_rewind {
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
                window.EJS_fixedSaveInterval = 1000;
                window.EJS_defaultOptions = {
                    'virtual-gamepad': 'disabled',
                    'menu-bar-button': 'hidden',
                    'save-save-interval': '1',
                    'save-state-location': 'browser'
                };

                window.VantafynInitialSram = ${if (initialSramBase64 != null) "\"$initialSramBase64\"" else "null"};
                window._lastSramHash = window.VantafynInitialSram;

                var cleanUpBuiltinControls = function() {
                    var elems = document.querySelectorAll('.ejs_virtualGamepad_parent, .ejs_virtualGamepad_open, [class*="ejs_virtualGamepad"], .ejs_dpad_main, .b_speed_fast, .b_speed_slow');
                    for (var i = 0; i < elems.length; i++) {
                        elems[i].style.display = 'none';
                        try { elems[i].remove(); } catch(e) {}
                    }
                    if (window.EJS_emulator) {
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
                };

                setInterval(cleanUpBuiltinControls, 300);

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
                    try {
                        gm.saveSaveFiles();
                    } catch(e) {}
                    try {
                        var s = gm.getSaveFile(false);
                        if (s && s.length > 0) return (s instanceof Uint8Array) ? s : new Uint8Array(s);
                    } catch(e) {}
                    try {
                        if (typeof gm.getSaveFilePath === 'function') {
                            var p = gm.getSaveFilePath();
                            if (p && gm.FS && gm.FS.analyzePath(p).exists) {
                                var d = gm.FS.readFile(p);
                                if (d && d.length > 0) return (d instanceof Uint8Array) ? d : new Uint8Array(d);
                            }
                        }
                    } catch(e) {}
                    try {
                        if (gm.FS && gm.FS.analyzePath("/data/saves").exists) {
                            var list = gm.FS.readdir("/data/saves");
                            for (var i = 0; i < list.length; i++) {
                                var item = list[i];
                                if (item !== "." && item !== ".." && (item.endsWith(".srm") || item.endsWith(".sav"))) {
                                    var full = "/data/saves/" + item;
                                    var d = gm.FS.readFile(full);
                                    if (d && d.length > 0) return (d instanceof Uint8Array) ? d : new Uint8Array(d);
                                }
                            }
                        }
                    } catch(e) {}
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
                        var candidateStems = [
                            "current_game",
                            "game",
                            "$safeGameName",
                            "$safeRomStem",
                            "$safeGameTitle"
                        ];
                        if (window.EJS_emulator) {
                            if (window.EJS_emulator.fileName) {
                                var fn = window.EJS_emulator.fileName.replace(/\.[^/.]+$/, "");
                                candidateStems.push(fn);
                                candidateStems.push(window.EJS_emulator.fileName);
                            }
                            if (typeof window.EJS_emulator.getBaseFileName === 'function') {
                                var b = window.EJS_emulator.getBaseFileName(true);
                                if (b) {
                                    candidateStems.push(b.replace(/\.[^/.]+$/, ""));
                                    candidateStems.push(b);
                                }
                            }
                        }
                        var candidatePaths = [];
                        candidateStems.forEach(function(stem) {
                            if (!stem) return;
                            candidatePaths.push("/data/saves/" + stem + ".srm");
                            candidatePaths.push("/data/saves/" + stem + ".sav");
                            candidatePaths.push("/data/saves/" + stem);
                        });
                        try {
                            if (window.EJS_emulator && window.EJS_emulator.gameManager && typeof window.EJS_emulator.gameManager.getSaveFilePath === 'function') {
                                var sfp = window.EJS_emulator.gameManager.getSaveFilePath();
                                if (sfp) candidatePaths.push(sfp);
                            }
                        } catch(e) {}
                        try {
                            if (fs.analyzePath("/data/saves").exists) {
                                var existing = fs.readdir("/data/saves");
                                for (var i = 0; i < existing.length; i++) {
                                    var ex = existing[i];
                                    if (ex !== "." && ex !== ".." && (ex.endsWith(".srm") || ex.endsWith(".sav"))) {
                                        candidatePaths.push("/data/saves/" + ex);
                                    }
                                }
                            }
                        } catch(e) {}

                        var seen = {};
                        candidatePaths.forEach(function(p) {
                            if (seen[p]) return;
                            seen[p] = true;
                            try {
                                if (fs.analyzePath(p).exists) fs.unlink(p);
                                fs.writeFile(p, bytes);
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

                // Periodic SRAM persistence checker (every 1.2s)
                setInterval(function() {
                    if (window.EJS_emulator && window.EJS_emulator.gameManager && window.EJS_emulator.started) {
                        try {
                            var gm = window.EJS_emulator.gameManager;
                            var sram = extractSramData(gm);
                            if (sram && sram.length > 0) {
                                var b64 = uint8ToBase64(sram);
                                if (b64 && b64 !== window._lastSramHash) {
                                    window._lastSramHash = b64;
                                    console.log("Vantafyn: In-game SRAM changed! (" + sram.length + " bytes). Persisting to storage...");
                                    if (window.VantafynBridge && window.VantafynBridge.onSramSaved) {
                                        window.VantafynBridge.onSramSaved(b64);
                                    }
                                }
                            }
                        } catch(e) {
                            console.warn("Vantafyn: periodic SRAM check error", e);
                        }
                    }
                }, 1200);

                window.VantafynEmulator = {
                    pause: function() {
                        if (window.EJS_emulator && window.EJS_emulator.pause) {
                            window.EJS_emulator.pause();
                        }
                    },
                    resume: function() {
                        if (window.EJS_emulator && window.EJS_emulator.play) {
                            window.EJS_emulator.play();
                        }
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
                        if (window.EJS_emulator && window.EJS_emulator.setSpeed) {
                            window.EJS_emulator.setSpeed(speed);
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
                                window.VantafynBridge.onSramSaved(b64);
                            }
                        } catch(e) {
                            console.error("Vantafyn: flushAllSaves SRAM error", e);
                        }
                        try {
                            var state = null;
                            if (typeof gm.getState === 'function') {
                                state = gm.getState();
                            } else if (gm.functions && typeof gm.functions.saveStateInfo === 'function') {
                                var info = gm.functions.saveStateInfo().split("|");
                                if (info[2] === "1") {
                                    var size = parseInt(info[0]);
                                    var start = parseInt(info[1]);
                                    state = new Uint8Array(gm.Module.HEAPU8.subarray(start, start + size));
                                }
                            }
                            if (state && state.length > 0 && window.VantafynBridge) {
                                console.log("Vantafyn: flushAllSaves saved State (" + state.length + " bytes)");
                                window.VantafynBridge.onStateSaved(uint8ToBase64(state));
                            }
                        } catch(e) {
                            console.error("Vantafyn: flushAllSaves State error", e);
                        }
                    },
                    requestSaveState: function() {
                        var emu = window.EJS_emulator;
                        var gm = emu && emu.gameManager;
                        if (!gm) {
                            console.error("Vantafyn: GameManager not ready");
                            return;
                        }
                        try {
                            var state = null;
                            if (typeof gm.getState === 'function') {
                                state = gm.getState();
                            } else if (gm.functions && typeof gm.functions.saveStateInfo === 'function') {
                                var info = gm.functions.saveStateInfo().split("|");
                                if (info[2] === "1") {
                                    var size = parseInt(info[0]);
                                    var start = parseInt(info[1]);
                                    state = new Uint8Array(gm.Module.HEAPU8.subarray(start, start + size));
                                }
                            }
                            if (state && window.VantafynBridge) {
                                var bytes = (state instanceof Uint8Array) ? state : new Uint8Array(state);
                                var base64 = uint8ToBase64(bytes);
                                window.VantafynBridge.onStateSaved(base64);
                            }
                        } catch(e) {
                            console.error("Vantafyn: Failed to save state", e);
                        }
                    },
                    loadState: function(base64) {
                        var emu = window.EJS_emulator;
                        var gm = emu && emu.gameManager;
                        if (!gm || typeof gm.loadState !== 'function') {
                            console.error("Vantafyn: GameManager not ready for loadState");
                            return;
                        }
                        try {
                            var binary_string = atob(base64);
                            var len = binary_string.length;
                            var bytes = new Uint8Array(len);
                            for (var i = 0; i < len; i++) {
                                bytes[i] = binary_string.charCodeAt(i);
                            }
                            gm.loadState(bytes);
                        } catch(e) {
                            console.error("Vantafyn: Failed to load state", e);
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
                                window.VantafynBridge.onSramSaved(b64);
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
                    destroy: function() {
                        try {
                            if (window.VantafynEmulator && window.VantafynEmulator.flushAllSaves) {
                                window.VantafynEmulator.flushAllSaves();
                            }
                        } catch(e) {}
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
                        var btnMap = {
                            'b': 0,
                            'y': 1,
                            'select': 2,
                            'start': 3,
                            'up': 4,
                            'down': 5,
                            'left': 6,
                            'right': 7,
                            'a': 8,
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
                        var idx = btnMap[btn.toLowerCase()];
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
                        if (window.EJS_emulator && window.EJS_emulator.stop) {
                            window.EJS_emulator.stop();
                        }
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
