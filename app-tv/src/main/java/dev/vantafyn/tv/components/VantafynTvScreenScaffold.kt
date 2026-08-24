package dev.vantafyn.tv.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val TvSafeHorizontalPadding = 28.dp
val TvSafeTopPadding = 20.dp
val TvSafeBottomPadding = 32.dp

@Composable
fun VantafynTvScreenScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = TvSafeHorizontalPadding,
        top = TvSafeTopPadding,
        end = TvSafeHorizontalPadding,
        bottom = TvSafeBottomPadding,
    ),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        content()
    }
}
