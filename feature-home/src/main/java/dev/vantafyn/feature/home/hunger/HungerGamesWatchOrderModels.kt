package dev.vantafyn.feature.home.hunger

import java.util.UUID

data class HungerGamesMovie(
    val id: String,
    val title: String,
    val releaseYear: Int,
    val releaseOrder: Int,
    val timelineOrder: Int,
    val timelineSetting: String,
    val era: String,
    val tmdbId: Int?,
    val imdbId: String?,
    val fallbackPosterPath: String,
    val overview: String? = null,
) {
    val fallbackPosterUrl: String
        get() = if (fallbackPosterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$fallbackPosterPath" else ""
}

enum class HungerGamesSortMode(val label: String) {
    Release("Release Order"),
    Timeline("Timeline Order"),
}

enum class HungerGamesFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class HungerGamesWatchItemUi(
    val movie: HungerGamesMovie,
    val isOnServer: Boolean = false,
    val isPlayed: Boolean = false,
    val mediaItemId: UUID? = null,
    val serverImageUrl: String? = null,
    val serverBackdropUrl: String? = null,
    val playbackPositionPercentage: Float? = null,
) {
    val displayPosterUrl: String
        get() = serverImageUrl?.takeIf { it.isNotBlank() } ?: movie.fallbackPosterUrl
}

object HungerGamesMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<HungerGamesMovie> = listOf(
        HungerGamesMovie(
            id = "the_hunger_games_the_ballad_of_songbirds_and_snakes_2023",
            title = "The Hunger Games: The Ballad of Songbirds & Snakes",
            releaseYear = 2023,
            releaseOrder = 5,
            timelineOrder = 1,
            timelineSetting = "10th Annual Hunger Games (64 Years Earlier)",
            era = "The Dark Days",
            tmdbId = 695721,
            imdbId = "tt10545296",
            fallbackPosterPath = "/mBaXZ95R2OxueZhvQbcEWy2DqyO.jpg",
            overview = "Years before he becomes the tyrannical President of Panem, 18-year-old Coriolanus Snow mentors Lucy Gray Baird in the 10th Hunger Games.",
        ),
        HungerGamesMovie(
            id = "the_hunger_games_2012",
            title = "The Hunger Games",
            releaseYear = 2012,
            releaseOrder = 1,
            timelineOrder = 2,
            timelineSetting = "74th Annual Hunger Games",
            era = "District 12",
            tmdbId = 70160,
            imdbId = "tt1392170",
            fallbackPosterPath = "/apa5G43Hha7kH7wJG0gkkHT7FA9.jpg",
            overview = "Katniss Everdeen volunteers to take her younger sister's place in the 74th Hunger Games, a televised fight to the death among teens from Panem's districts.",
        ),
        HungerGamesMovie(
            id = "the_hunger_games_catching_fire_2013",
            title = "The Hunger Games: Catching Fire",
            releaseYear = 2013,
            releaseOrder = 2,
            timelineOrder = 3,
            timelineSetting = "75th Hunger Games (Quarter Quell)",
            era = "The Spark",
            tmdbId = 101299,
            imdbId = "tt1951264",
            fallbackPosterPath = "/vrQHDXjVmbYzadOXQ0UaObunoy2.jpg",
            overview = "Katniss and Peeta embark on a Victory Tour while revolution brews across Panem, leading to the Third Quarter Quell.",
        ),
        HungerGamesMovie(
            id = "the_hunger_games_mockingjay_part_1_2014",
            title = "The Hunger Games: Mockingjay – Part 1",
            releaseYear = 2014,
            releaseOrder = 3,
            timelineOrder = 4,
            timelineSetting = "District 13 Rebellion",
            era = "The Mockingjay",
            tmdbId = 131631,
            imdbId = "tt1951265",
            fallbackPosterPath = "/4FAA18ZIja70d1Tu5hr5cj2q1sB.jpg",
            overview = "Katniss Everdeen finds herself in District 13 after she shatters the games forever. Under President Coin, she becomes the symbol of hope.",
        ),
        HungerGamesMovie(
            id = "the_hunger_games_mockingjay_part_2_2015",
            title = "The Hunger Games: Mockingjay – Part 2",
            releaseYear = 2015,
            releaseOrder = 4,
            timelineOrder = 5,
            timelineSetting = "Assault on the Capitol",
            era = "The Revolution",
            tmdbId = 131634,
            imdbId = "tt1951266",
            fallbackPosterPath = "/lImKHDfExAulp16grYm8zD5eONE.jpg",
            overview = "Katniss and her unit confront President Snow in an epic final battle for the liberation of Panem.",
        ),
    )
}
