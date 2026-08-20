package dev.vantafyn.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPanel

@Composable
fun LibraryPlaceholder() {
    VantafynGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.02f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color(0xFF31D7FF).copy(alpha = 0.18f),
                            ),
                        ),
                        RoundedCornerShape(22.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.CollectionsBookmark,
                    contentDescription = null,
                    tint = VantafynColors.Ink.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                "Library",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Your media library will appear here.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
