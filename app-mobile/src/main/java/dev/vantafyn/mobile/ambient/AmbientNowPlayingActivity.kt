package dev.vantafyn.mobile.ambient

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vantafyn.core.media.MusicPlaybackController
import dev.vantafyn.core.ui.VantafynSurface
import dev.vantafyn.core.ui.VantafynTheme
import dev.vantafyn.feature.music.MusicViewModel
import dev.vantafyn.feature.music.ambient.AmbientNowPlayingScreen

class AmbientNowPlayingActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLockScreenWindowFlags()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        hideSystemBars()

        setContent {
            VantafynTheme {
                VantafynSurface {
                    val playbackController = MusicPlaybackController.get(this)
                    val playbackState by playbackController.state.collectAsStateWithLifecycle()
                    val musicState by musicViewModel.state.collectAsStateWithLifecycle()

                    // Proactively load lyrics for ambient display
                    LaunchedEffect(playbackState.currentTrack?.id) {
                        playbackState.currentTrack?.let { track ->
                            musicViewModel.loadLyrics(track.id)
                        }
                    }

                    AmbientNowPlayingScreen(
                        playbackState = playbackState,
                        lyrics = musicState.lyrics,
                        isLyricsLoading = musicState.isLyricsLoading,
                        currentPositionMs = musicViewModel::currentPlaybackPositionMs,
                        onPlayPause = musicViewModel::togglePlayPause,
                        onNext = musicViewModel::next,
                        onPrevious = musicViewModel::previous,
                        onSeek = musicViewModel::seekTo,
                        onToggleShuffle = musicViewModel::toggleShuffle,
                        onCycleRepeat = musicViewModel::cycleRepeat,
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyLockScreenWindowFlags()
    }

    override fun onResume() {
        super.onResume()
        applyLockScreenWindowFlags()
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun applyLockScreenWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
    }
}
