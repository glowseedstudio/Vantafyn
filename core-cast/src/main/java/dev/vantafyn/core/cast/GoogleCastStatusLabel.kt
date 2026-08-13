package dev.vantafyn.core.cast

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassPill

@Composable
fun GoogleCastStatusLabel(modifier: Modifier = Modifier) {
    if (!VantafynCastFeatureFlags.googleCastEnabled) return
    val context = LocalContext.current
    val outputState by PlaybackOutputCoordinator.get(context).state.collectAsStateWithLifecycle()
    val receiverName = outputState.castState.receiverName?.takeIf(String::isNotBlank) ?: return
    if (outputState.castState.connectionState != RemoteConnectionState.Connected) return

    VantafynGlassPill(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = "Playing on $receiverName",
            color = VantafynColors.Ink.copy(alpha = 0.88f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
