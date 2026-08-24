package dev.vantafyn.tv.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors

@Composable
fun VantafynTvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = title,
            color = VantafynColors.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 13.sp,
            )
        }
    }
}
