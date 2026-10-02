package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vantafyn.core.ui.VantafynGradients

/**
 * CompositionLocal providing the optional custom Pokémon modal and storage box background image URL.
 * When null or blank, containers gracefully fall back to the default dark theme.
 */
val LocalPokemonModalBackground = compositionLocalOf<String?> { null }

/**
 * Standard modal and container for Pokémon Vault, Pokémon Home storage boxes, and dialogs.
 * If a custom background image is configured in the companion plugin, it is rendered with
 * a dark scrim overlay. Otherwise, it gracefully renders the clean dark gradient background.
 * In all cases, it features the signature static Vantafyn horizontal gradient border.
 */
@Composable
fun PokemonModalContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 1.5.dp,
    scrimAlphaTop: Float = 0.85f,
    scrimAlphaBottom: Float = 0.90f,
    customBackgroundUrl: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val effectiveBackgroundUrl = customBackgroundUrl ?: LocalPokemonModalBackground.current

    val borderModifier = if (borderWidth > 0.dp) {
        Modifier.border(borderWidth, VantafynGradients.accentHorizontal(), shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(borderModifier)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131623),
                        Color(0xFF0E111C),
                    )
                )
            ),
    ) {
        if (!effectiveBackgroundUrl.isNullOrBlank()) {
            // User-configured custom background image from companion plugin or remote URL
            AsyncImage(
                model = effectiveBackgroundUrl,
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
        } else {
            // Default dark gradient background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF131623),
                                Color(0xFF0E111C),
                            )
                        )
                    )
            )
        }

        // Modal Content
        content()
    }
}
