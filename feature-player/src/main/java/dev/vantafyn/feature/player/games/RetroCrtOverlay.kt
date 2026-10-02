package dev.vantafyn.feature.player.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * High-fidelity, hardware-accelerated retro CRT scanline & shadow mask overlay.
 * Renders authentic horizontal scanlines, radial tube vignette, and subtle phosphor aperture grille.
 */
@Composable
fun RetroCrtOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Horizontal Scanlines
        // Alternating dark scanlines spaced every ~3dp
        val scanlineSpacing = (density * 3f).coerceAtLeast(3f)
        val scanlineThickness = (scanlineSpacing * 0.42f).coerceAtLeast(1.2f)
        var y = 0f
        val scanlineColor = Color(0x45000000) // ~27% opacity black

        while (y < height) {
            drawRect(
                color = scanlineColor,
                topLeft = Offset(0f, y),
                size = Size(width, scanlineThickness)
            )
            y += scanlineSpacing
        }

        // 2. Radial CRT Vignette (curved tube glass edge shadow)
        val radius = maxOf(width, height) * 0.72f
        val center = Offset(width / 2f, height / 2f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x05000000),
                    Color(0x25000000),
                    Color(0x65000000),
                    Color(0x95000000)
                ),
                center = center,
                radius = radius
            ),
            size = size
        )

        // 3. Subtle Aperture Grille (very faint vertical phosphor subpixel lines)
        val grilleSpacing = (density * 4f).coerceAtLeast(4f)
        var x = 0f
        val grilleColor = Color(0x0800E5FF) // subtle cool phosphor glow
        while (x < width) {
            drawLine(
                color = grilleColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += grilleSpacing
        }
    }
}
