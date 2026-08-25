package dev.vantafyn.tv.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvGlassButton
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvScreenScaffold
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import java.util.UUID

@Composable
fun TvLibraryScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    initialLibraryId: UUID? = null,
    title: String = "Library",
    onOpenMedia: (UUID) -> Unit = {},
) {
    var selectedLibraryId by remember(state.libraries, initialLibraryId) {
        mutableStateOf(initialLibraryId ?: state.libraries.firstOrNull()?.id)
    }

    VantafynTvScreenScaffold(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            VantafynTvSectionHeader(
                title = title,
                subtitle = "Browse your Jellyfin media libraries",
            )

            // Library Filter Tabs
            if (state.libraries.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(state.libraries, key = { it.id }) { lib ->
                        VantafynTvGlassButton(
                            text = lib.name,
                            icon = Icons.Rounded.Folder,
                            isPrimary = lib.id == selectedLibraryId,
                            onClick = { selectedLibraryId = lib.id },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of media items
            val displayCards = state.home?.sections?.flatMap { it.items }.orEmpty()
            if (displayCards.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(displayCards, key = { it.id }) { item ->
                        val imageUrl = item.imageUrl ?: item.backdropUrl ?: "${session?.server?.url?.trimEnd('/')}/Items/${item.id}/Images/Primary"
                        VantafynTvPosterCard(
                            title = item.title,
                            imageUrl = imageUrl,
                            subtitle = item.subtitle ?: item.year?.toString(),
                            onClick = { onOpenMedia(item.id) },
                        )
                    }
                }
            }
        }
    }
}
