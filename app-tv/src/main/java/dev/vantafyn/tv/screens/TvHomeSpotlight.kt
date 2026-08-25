package dev.vantafyn.tv.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.tv.media.TvArtworkResolver
import java.util.UUID

@Stable
data class TvHomeSpotlightItem(
    val id: UUID,
    val title: String,
    val subtitle: String? = null,
    val overview: String? = null,
    val year: Int? = null,
    val runtimeMinutes: Int? = null,
    val officialRating: String? = null,
    val communityRating: Float? = null,
    val genres: List<String> = emptyList(),
    val backdropUrl: String? = null,
    val logoUrl: String? = null,
    val posterUrl: String? = null,
)

@Stable
class TvHomeSpotlightState(
    initialItem: TvHomeSpotlightItem? = null,
) {
    var currentItem by mutableStateOf(initialItem)
        private set

    fun update(item: TvHomeSpotlightItem?) {
        if (item == null || currentItem?.id == item.id) return
        currentItem = item
    }

    fun updateIfNull(item: TvHomeSpotlightItem?) {
        if (currentItem == null && item != null) {
            currentItem = item
        }
    }
}

@Composable
fun rememberTvHomeSpotlightState(
    initialItem: TvHomeSpotlightItem? = null,
): TvHomeSpotlightState = remember {
    TvHomeSpotlightState(initialItem)
}

fun JellyfinHeroMediaItem.toSpotlight(serverUrl: String? = null): TvHomeSpotlightItem =
    TvArtworkResolver.resolveHeroSpotlightItem(this, serverUrl)

fun JellyfinMediaCard.toSpotlight(
    serverUrl: String? = null,
    heroMatch: JellyfinHeroMediaItem? = null,
): TvHomeSpotlightItem =
    TvArtworkResolver.resolveSpotlightItem(this, serverUrl, heroMatch)

fun JellyfinLibrary.toSpotlight(serverUrl: String? = null): TvHomeSpotlightItem =
    TvArtworkResolver.resolveLibrarySpotlightItem(this, serverUrl)
