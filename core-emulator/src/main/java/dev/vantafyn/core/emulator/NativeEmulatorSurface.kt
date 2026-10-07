package dev.vantafyn.core.emulator

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

enum class NdsScreenLayout(val label: String, val coreValue: String) {
    TopBottom("Vertical (Top/Bottom)", "Top/Bottom"),
    LeftRight("Side by Side (Left/Right)", "Left/Right"),
    TopOnly("Focus Top Screen", "Top Only"),
    BottomOnly("Focus Touch Screen", "Bottom Only");

    fun next(): NdsScreenLayout = when (this) {
        TopBottom -> LeftRight
        LeftRight -> TopOnly
        TopOnly -> BottomOnly
        BottomOnly -> TopBottom
    }
}

/**
 * Jetpack Compose SurfaceView container for Native Libretro Rendering.
 * Maps touches on the touch screen partition to Nintendo DS pointer coordinates.
 */
@Composable
fun NativeEmulatorSurface(
    engine: NativeEmulatorEngine,
    modifier: Modifier = Modifier,
    isDualScreen: Boolean = true,
    layout: NdsScreenLayout = NdsScreenLayout.TopBottom,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            engine.setSurface(holder.surface)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            engine.setSurface(holder.surface)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            engine.setSurface(null)
                        }
                    })
                }
            },
        )

        // DS Touchscreen input interceptor (maps touches based on current screen layout)
        if (isDualScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(layout) {
                        detectTapGestures(
                            onPress = { offset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val retroCoords = mapTouchToRetro(offset, viewW, viewH, layout)

                                if (retroCoords != null) {
                                    engine.setTouch(retroCoords.first, retroCoords.second, true)
                                    tryAwaitRelease()
                                    engine.setTouch(retroCoords.first, retroCoords.second, false)
                                }
                            },
                        )
                    }
                    .pointerInput(layout) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val retroCoords = mapTouchToRetro(offset, viewW, viewH, layout)
                                if (retroCoords != null) {
                                    engine.setTouch(retroCoords.first, retroCoords.second, true)
                                }
                            },
                            onDragEnd = {
                                engine.setTouch(0, 0, false)
                            },
                            onDragCancel = {
                                engine.setTouch(0, 0, false)
                            },
                            onDrag = { change, _ ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val retroCoords = mapTouchToRetro(change.position, viewW, viewH, layout)
                                if (retroCoords != null) {
                                    engine.setTouch(retroCoords.first, retroCoords.second, true)
                                }
                            },
                        )
                    },
            )
        }
    }
}

private fun mapTouchToRetro(
    offset: androidx.compose.ui.geometry.Offset,
    viewW: Float,
    viewH: Float,
    layout: NdsScreenLayout,
): Pair<Short, Short>? {
    val relX: Float
    val relY: Float
    when (layout) {
        NdsScreenLayout.TopBottom -> {
            val topScreenBottom = viewH * 0.5f
            if (offset.y < topScreenBottom) return null
            relX = (offset.x / viewW).coerceIn(0f, 1f)
            relY = ((offset.y - topScreenBottom) / (viewH - topScreenBottom)).coerceIn(0f, 1f)
        }
        NdsScreenLayout.LeftRight -> {
            val leftScreenRight = viewW * 0.5f
            if (offset.x < leftScreenRight) return null
            relX = ((offset.x - leftScreenRight) / (viewW - leftScreenRight)).coerceIn(0f, 1f)
            relY = (offset.y / viewH).coerceIn(0f, 1f)
        }
        NdsScreenLayout.BottomOnly -> {
            relX = (offset.x / viewW).coerceIn(0f, 1f)
            relY = (offset.y / viewH).coerceIn(0f, 1f)
        }
        NdsScreenLayout.TopOnly -> {
            return null
        }
    }
    val retroX = ((relX * 2f - 1f) * 0x7fff).toInt().toShort()
    val retroY = ((relY * 2f - 1f) * 0x7fff).toInt().toShort()
    return Pair(retroX, retroY)
}
