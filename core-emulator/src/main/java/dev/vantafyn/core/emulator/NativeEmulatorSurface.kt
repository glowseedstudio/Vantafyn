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
    is3ds: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                SurfaceView(context).apply {
                    holder.setFormat(android.graphics.PixelFormat.RGBA_8888)
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

        // DS / 3DS Touchscreen input interceptor (maps touches based on current screen layout)
        if (isDualScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(layout, is3ds) {
                        detectTapGestures(
                            onPress = { offset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val retroCoords = mapTouchToRetro(offset, viewW, viewH, layout, is3ds)

                                if (retroCoords != null) {
                                    engine.setTouch(retroCoords.first, retroCoords.second, true)
                                    tryAwaitRelease()
                                    engine.setTouch(retroCoords.first, retroCoords.second, false)
                                }
                            },
                        )
                    }
                    .pointerInput(layout, is3ds) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val viewW = size.width.toFloat()
                                val viewH = size.height.toFloat()
                                val retroCoords = mapTouchToRetro(offset, viewW, viewH, layout, is3ds)
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
                                val retroCoords = mapTouchToRetro(change.position, viewW, viewH, layout, is3ds)
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
    is3ds: Boolean = false,
): Pair<Short, Short>? {
    val relX: Float
    val relY: Float
    if (is3ds) {
        when (layout) {
            NdsScreenLayout.TopBottom -> {
                // In stacked 3DS: Top screen is 400x240, Bottom is 320x240. Total canvas aspect 400x480.
                val topScreenBottom = viewH * 0.5f
                if (offset.y < topScreenBottom) return null
                // Bottom screen (320w) is centered inside 400w:
                // (400 - 320) / 2 = 40 pixels on each side (10% of width)
                val marginX = viewW * 0.10f
                if (offset.x < marginX || offset.x > (viewW - marginX)) return null
                relX = ((offset.x - marginX) / (viewW - 2f * marginX)).coerceIn(0f, 1f)
                relY = ((offset.y - topScreenBottom) / (viewH - topScreenBottom)).coerceIn(0f, 1f)
            }
            NdsScreenLayout.LeftRight -> {
                // In side-by-side 3DS: Top is 400x240, Bottom is 320x240. Total width 720.
                val leftScreenRight = viewW * (400f / 720f)
                if (offset.x < leftScreenRight) return null
                relX = ((offset.x - leftScreenRight) / (viewW - leftScreenRight)).coerceIn(0f, 1f)
                relY = (offset.y / viewH).coerceIn(0f, 1f)
            }
            NdsScreenLayout.BottomOnly -> {
                val normX = (offset.x / viewW).coerceIn(0f, 1f)
                relX = (400f + normX * 320f) / 720f
                relY = (offset.y / viewH).coerceIn(0f, 1f)
            }
            NdsScreenLayout.TopOnly -> {
                return null
            }
        }
    } else {
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
    }
    val retroX = ((relX * 2f - 1f) * 0x7fff).toInt().toShort()
    val retroY = ((relY * 2f - 1f) * 0x7fff).toInt().toShort()
    return Pair(retroX, retroY)
}
