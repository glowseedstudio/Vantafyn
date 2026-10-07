package dev.vantafyn.feature.player.games

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.vantafyn.core.emulator.NativeEmulatorEngine

/**
 * Hardware Secondary Display Presentation for dual-screen Nintendo DS gaming.
 * Renders the DS bottom (touch) or top screen directly on an attached physical screen
 * (such as the Retroid Pocket dual-screen attachment or USB-C secondary monitor).
 */
class NdsSecondaryDisplayPresentation(
    context: Context,
    display: Display,
    private val engine: NativeEmulatorEngine,
    private val isTouchScreen: Boolean = true,
) : Presentation(context, display) {

    private var surfaceView: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val sv = SurfaceView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    engine.setSecondarySurface(holder.surface)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    engine.setSecondarySurface(holder.surface)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    engine.setSecondarySurface(null)
                }
            })

            // Full-screen touch tracking for Nintendo DS stylus input
            if (isTouchScreen) {
                setOnTouchListener { _, event ->
                    val w = width.toFloat()
                    val h = height.toFloat()
                    if (w <= 0f || h <= 0f) return@setOnTouchListener false

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            val relX = (event.x / w).coerceIn(0f, 1f)
                            val relY = (event.y / h).coerceIn(0f, 1f)
                            val retroX = ((relX * 2f - 1f) * 0x7fff).toInt().toShort()
                            val retroY = ((relY * 2f - 1f) * 0x7fff).toInt().toShort()
                            engine.setTouch(retroX, retroY, true)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            engine.setTouch(0, 0, false)
                            true
                        }
                        else -> false
                    }
                }
            }
        }

        surfaceView = sv
        setContentView(sv)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        engine.setSecondarySurface(null)
    }
}

/**
 * Remembers and observes attached physical displays, managing the secondary
 * display presentation life-cycle for dual-screen Nintendo DS emulation.
 */
@Composable
fun rememberNdsDualDisplayManager(
    engine: NativeEmulatorEngine?,
    isNativeMode: Boolean,
    swapDualScreens: Boolean = false,
): Boolean {
    val context = LocalContext.current
    var hasSecondaryDisplay by remember { mutableStateOf(false) }

    DisposableEffect(context, engine, isNativeMode, swapDualScreens) {
        if (!isNativeMode || engine == null) {
            hasSecondaryDisplay = false
            return@DisposableEffect onDispose {}
        }

        engine.setDualScreenSwap(swapDualScreens)
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

        var presentation: NdsSecondaryDisplayPresentation? = null

        fun attachPresentation(display: Display) {
            try {
                presentation?.dismiss()
                val pres = NdsSecondaryDisplayPresentation(
                    context = context,
                    display = display,
                    engine = engine,
                    isTouchScreen = !swapDualScreens,
                )
                pres.show()
                presentation = pres
                hasSecondaryDisplay = true
                android.util.Log.i("NdsDualDisplay", "Attached secondary presentation to display: ${display.name}")
            } catch (e: Exception) {
                android.util.Log.w("NdsDualDisplay", "Failed to show secondary presentation: ${e.message}")
            }
        }

        fun detachPresentation() {
            try {
                presentation?.dismiss()
            } catch (_: Exception) {}
            presentation = null
            engine.setSecondarySurface(null)
            hasSecondaryDisplay = false
        }

        fun checkDisplays() {
            if (displayManager == null) return
            val presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)?.toList().orEmpty()
            val externalDisplays: List<Display> = if (presentationDisplays.isNotEmpty()) {
                presentationDisplays
            } else {
                displayManager.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
            }

            if (externalDisplays.isNotEmpty()) {
                attachPresentation(externalDisplays.first())
            } else {
                detachPresentation()
            }
        }

        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = checkDisplays()
            override fun onDisplayRemoved(displayId: Int) = checkDisplays()
            override fun onDisplayChanged(displayId: Int) = checkDisplays()
        }

        displayManager?.registerDisplayListener(listener, null)
        checkDisplays()

        onDispose {
            displayManager?.unregisterDisplayListener(listener)
            detachPresentation()
        }
    }

    return hasSecondaryDisplay
}
