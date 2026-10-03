package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal fun pokemonCardAtmosphereBrush(
    accentColor: Color = Color(0xFF38BDF8),
    accentLift: Float = 0.045f,
): Brush =
    Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFF202537), accentColor, accentLift),
            Color(0xFF171B29),
            Color(0xFF10131D),
        ),
    )

internal fun pokemonCompactCardAtmosphereBrush(
    accentColor: Color = Color(0xFF38BDF8),
): Brush =
    Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFF202436), accentColor, 0.035f),
            Color(0xFF181C2A),
            Color(0xFF121520),
        ),
    )
