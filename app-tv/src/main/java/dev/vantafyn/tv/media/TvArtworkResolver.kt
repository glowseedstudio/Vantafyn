package dev.vantafyn.tv.media

import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.tv.screens.TvHomeSpotlightItem
import java.util.UUID

/**
 * Centralized Android TV image quality and URL resolution policy.
 * Ensures 1080p/4K TV displays receive crisp, uncompressed high-resolution backdrops,
 * logos, and card artwork without pixelation or stretching low-res thumbnails.
 */
object TvArtworkResolver {
    const val HERO_BACKDROP_WIDTH = 1920
    const val HERO_BACKDROP_QUALITY = 90
    const val HERO_LOGO_WIDTH = 800
    const val HERO_POSTER_WIDTH = 600

    const val CARD_POSTER_WIDTH = 360
    const val CARD_POSTER_QUALITY = 85
    const val CARD_WIDE_WIDTH = 540
    const val CARD_WIDE_QUALITY = 85

    fun buildBackdropUrl(
        serverUrl: String,
        itemId: UUID,
        width: Int = HERO_BACKDROP_WIDTH,
        quality: Int = HERO_BACKDROP_QUALITY,
    ): String =
        "${serverUrl.trimEnd('/')}/Items/$itemId/Images/Backdrop/0?maxWidth=$width&quality=$quality&format=WEBP"

    fun buildLogoUrl(
        serverUrl: String,
        itemId: UUID,
        width: Int = HERO_LOGO_WIDTH,
        quality: Int = 90,
    ): String =
        "${serverUrl.trimEnd('/')}/Items/$itemId/Images/Logo?maxWidth=$width&quality=$quality&format=WEBP"

    fun buildPrimaryUrl(
        serverUrl: String,
        itemId: UUID,
        width: Int = CARD_POSTER_WIDTH,
        quality: Int = CARD_POSTER_QUALITY,
    ): String =
        "${serverUrl.trimEnd('/')}/Items/$itemId/Images/Primary?maxWidth=$width&quality=$quality&format=WEBP"

    fun buildThumbUrl(
        serverUrl: String,
        itemId: UUID,
        width: Int = CARD_WIDE_WIDTH,
        quality: Int = CARD_WIDE_QUALITY,
    ): String =
        "${serverUrl.trimEnd('/')}/Items/$itemId/Images/Thumb?maxWidth=$width&quality=$quality&format=WEBP"

    fun resolveHeroSpotlightItem(
        hero: JellyfinHeroMediaItem,
        serverUrl: String?,
    ): TvHomeSpotlightItem {
        val highResBackdrop = hero.backdropUrl ?: if (serverUrl != null) buildBackdropUrl(serverUrl, hero.id) else null
        val highResLogo = hero.logoUrl ?: if (serverUrl != null) buildLogoUrl(serverUrl, hero.id) else null
        return TvHomeSpotlightItem(
            id = hero.id,
            title = hero.title,
            subtitle = hero.subtitle,
            overview = hero.overview,
            year = hero.year,
            runtimeMinutes = hero.runtimeMinutes,
            officialRating = hero.officialRating,
            communityRating = hero.communityRating,
            genres = hero.genres,
            backdropUrl = highResBackdrop,
            logoUrl = highResLogo,
            posterUrl = hero.posterUrl,
        )
    }

    fun resolveSpotlightItem(
        card: JellyfinMediaCard,
        serverUrl: String?,
        heroMatch: JellyfinHeroMediaItem? = null,
    ): TvHomeSpotlightItem {
        if (heroMatch != null) {
            return resolveHeroSpotlightItem(heroMatch, serverUrl)
        }

        val highResBackdrop = if (serverUrl != null) {
            buildBackdropUrl(serverUrl, card.id)
        } else {
            card.backdropUrl
        }

        val highResLogo = if (serverUrl != null) {
            buildLogoUrl(serverUrl, card.id)
        } else {
            card.logoUrl
        }

        val cardPoster = card.imageUrl ?: card.backdropUrl ?: if (serverUrl != null) buildPrimaryUrl(serverUrl, card.id) else null

        return TvHomeSpotlightItem(
            id = card.id,
            title = card.title,
            subtitle = card.subtitle ?: card.year?.toString(),
            overview = card.overview,
            year = card.year,
            runtimeMinutes = card.runtimeMinutes,
            officialRating = card.officialRating,
            communityRating = card.communityRating,
            genres = card.genres,
            backdropUrl = highResBackdrop,
            logoUrl = highResLogo,
            posterUrl = cardPoster,
        )
    }

    fun resolveLibrarySpotlightItem(
        library: JellyfinLibrary,
        serverUrl: String?,
    ): TvHomeSpotlightItem {
        val highResPrimary = if (serverUrl != null) buildPrimaryUrl(serverUrl, library.id, width = 600) else library.imageUrl
        val highResBackdrop = if (serverUrl != null) buildBackdropUrl(serverUrl, library.id) else highResPrimary

        return TvHomeSpotlightItem(
            id = library.id,
            title = library.name,
            subtitle = library.collectionType?.replaceFirstChar { it.uppercase() } ?: "Media Library",
            overview = "Browse all content inside your ${library.name} collection.",
            year = null,
            runtimeMinutes = null,
            officialRating = null,
            communityRating = null,
            genres = emptyList(),
            backdropUrl = highResBackdrop,
            logoUrl = null,
            posterUrl = highResPrimary,
        )
    }
}
