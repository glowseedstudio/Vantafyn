package dev.vantafyn.feature.player.games

import android.annotation.SuppressLint
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import dev.vantafyn.core.jellyfin.GameSaveKind
import dev.vantafyn.core.jellyfin.JellyfinGamesRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Intercept hardware Back press to toggle Pause HUD instead of exiting immediately
    BackHandler(enabled = true) {
        if (!isPaused) {
            isPaused = true
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.pause();", null)
        } else {
            isPaused = false
            webViewInstance?.evaluateJavascript("window.VantafynEmulator?.resume();", null)
        }
    }

    // Step 1: Download / Cache ROM
    LaunchedEffect(game.id) {
        isDownloading = true
        statusMessage = "Downloading ROM from Jellyfin..."
        try {
            val file = storageManager.downloadRomIfNeeded(session, libraryId, game) { progress ->
                downloadProgress = progress
                statusMessage = "Downloading ROM: ${(progress * 100).toInt()}%"
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
                                    mediaPlaybackRequiresUserGesture = false
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }

                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onGameReady() {
                                            scope.launch {
                                                // Check for existing save state and load it
                                                val stateData = storageManager.loadSaveState(session, game.id)
                                                if (stateData != null && stateData.isNotEmpty()) {
                                                    val base64 = Base64.encodeToString(stateData, Base64.NO_WRAP)
                                                    evaluateJavascript("window.VantafynEmulator?.loadState('$base64');", null)
                                                }
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onStateSaved(base64Data: String) {
                                            scope.launch {
                                                isSavingState = true
                                                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                val result = storageManager.saveState(session, game.id, bytes, GameSaveKind.State)
                                                isSavingState = false
                                                saveStateSuccess = result.isSuccess
                                                delay(2500)
                                                saveStateSuccess = false
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onRequestMenu() {
                                            isPaused = true
                                        }
                                    },
                                    "VantafynBridge",
                                )

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): WebResourceResponse? {
                                        val url = request?.url?.toString() ?: return null
                                        if (url.endsWith("current_game.rom")) {
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

                                val html = generateEmulatorHtml(game.systemId, game.core)
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
                onButtonPress = { btn, isDown ->
                    val js = "window.VantafynEmulator?.setButton('${btn.id}', $isDown);"
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
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.requestSaveState();", null)
            },
            onQuickLoad = {
                scope.launch {
                    val stateData = storageManager.loadSaveState(session, game.id)
                    if (stateData != null && stateData.isNotEmpty()) {
                        val base64 = Base64.encodeToString(stateData, Base64.NO_WRAP)
                        webViewInstance?.evaluateJavascript("window.VantafynEmulator?.loadState('$base64');", null)
                        isPaused = false
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
                webViewInstance?.evaluateJavascript("window.VantafynEmulator?.destroy();", null)
                onExit()
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}

/**
 * Generates the self-contained HTML/JS emulator runner that interfaces with the WebAssembly Libretro core.
 */
private fun generateEmulatorHtml(systemId: String, core: String): String {
    val systemCoreName = when (systemId.lowercase()) {
        "nes" -> "nes"
        "snes" -> "snes"
        "gb", "gbc" -> "gb"
        "gba" -> "gba"
        "sega", "genesis", "megadrive", "segamd" -> "segaMD"
        "n64" -> "n64"
        "nds" -> "nds"
        "psx", "ps1" -> "psx"
        else -> systemId.lowercase()
    }

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
            </style>
        </head>
        <body>
            <div id="game-container"></div>
            <script>
                window.EJS_player = '#game-container';
                window.EJS_core = '$systemCoreName';
                window.EJS_gameUrl = 'https://vantafyn.emulator/current_game.rom';
                window.EJS_pathtodata = 'https://cdn.emulatorjs.org/stable/data/';
                window.EJS_startOnLoaded = true;
                window.EJS_color = '#21D8FF';
                window.EJS_backgroundColor = '#000000';
                window.EJS_disableUI = true;

                window.EJS_onGameStart = function() {
                    if (window.VantafynBridge) {
                        window.VantafynBridge.onGameReady();
                    }
                };

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
                        if (window.EJS_emulator && window.EJS_emulator.restart) {
                            window.EJS_emulator.restart();
                        }
                    },
                    setSpeed: function(speed) {
                        if (window.EJS_emulator && window.EJS_emulator.setSpeed) {
                            window.EJS_emulator.setSpeed(speed);
                        }
                    },
                    requestSaveState: function() {
                        if (window.EJS_emulator && window.EJS_emulator.saveState) {
                            var state = window.EJS_emulator.saveState();
                            if (state && window.VantafynBridge) {
                                // Convert Uint8Array to base64
                                var binary = '';
                                var bytes = new Uint8Array(state);
                                for (var i = 0; i < bytes.byteLength; i++) {
                                    binary += String.fromCharCode(bytes[i]);
                                }
                                var base64 = btoa(binary);
                                window.VantafynBridge.onStateSaved(base64);
                            }
                        }
                    },
                    loadState: function(base64) {
                        if (window.EJS_emulator && window.EJS_emulator.loadState) {
                            var binary_string = atob(base64);
                            var len = binary_string.length;
                            var bytes = new Uint8Array(len);
                            for (var i = 0; i < len; i++) {
                                bytes[i] = binary_string.charCodeAt(i);
                            }
                            window.EJS_emulator.loadState(bytes.buffer);
                        }
                    },
                    setButton: function(btn, isDown) {
                        if (window.EJS_emulator && window.EJS_emulator.pressButton) {
                            window.EJS_emulator.pressButton(btn, isDown);
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
