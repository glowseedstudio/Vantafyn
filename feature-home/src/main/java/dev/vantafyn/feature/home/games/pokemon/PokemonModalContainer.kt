package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vantafyn.core.ui.VantafynGradients

/**
 * Standard modal container for Pokémon Vault and Pokémon Home dialogs.
 * Features the custom pkvaultmodal background with a rich dark scrim and
 * the signature static Vantafyn horizontal gradient border.
 */
@Composable
fun PokemonModalContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 1.5.dp,
    scrimAlphaTop: Float = 0.85f,
    scrimAlphaBottom: Float = 0.90f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, VantafynGradients.accentHorizontal(), shape),
    ) {
        // High-res thematic artwork background
        Image(
            painter = painterResource(id = dev.vantafyn.core.ui.R.drawable.pkvaultmodal),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        // Dark gradient scrim for high readability of UI components
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C0E17).copy(alpha = scrimAlphaTop),
                            Color(0xFF0E111C).copy(alpha = scrimAlphaBottom),
                        )
                    )
                )
        )

        // Modal Content
        content()
    }
}
