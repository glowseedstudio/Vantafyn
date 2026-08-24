package dev.vantafyn.tv.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.tv.components.VantafynTvPosterCard
import dev.vantafyn.tv.components.VantafynTvScreenScaffold
import dev.vantafyn.tv.components.VantafynTvSectionHeader
import dev.vantafyn.tv.components.VantafynTvTextField
import java.util.UUID

@Composable
fun TvSearchScreen(
    state: VantafynHomeUiState,
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onOpenMedia: (UUID) -> Unit = {},
) {
    VantafynTvScreenScaffold(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            VantafynTvSectionHeader(
                title = "Search",
                subtitle = "Find movies, TV series, music, and artists",
            )

            // TV Search Input Box with 4-Color Gradient Focus
            VantafynTvTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                placeholder = "Type title, actor, director, or genre...",
                modifier = Modifier.fillMaxWidth(0.68f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                trailingContent = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = VantafynColors.Muted,
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Search Results Grid
            val results = state.searchResults
            if (results.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 40.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { item ->
                        val imageUrl = item.imageUrl ?: item.backdropUrl ?: "${session?.server?.url?.trimEnd('/')}/Items/${item.id}/Images/Primary"
                        VantafynTvPosterCard(
                            title = item.title,
                            imageUrl = imageUrl,
                            subtitle = item.subtitle ?: item.year?.toString(),
                            onClick = { onOpenMedia(item.id) },
                        )
                    }
                }
            } else if (state.searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found for \"${state.searchQuery}\"",
                        color = VantafynColors.Muted,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
