package dev.vantafyn.feature.home.potter

import java.util.UUID

data class HarryPotterMovie(
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
    val alternateTitle: String? = null,
    val overview: String? = null,
) {
    val fallbackPosterUrl: String
        get() = if (fallbackPosterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$fallbackPosterPath" else ""
}

enum class HarryPotterSortMode(val label: String) {
    Timeline("Timeline Order"),
    Release("Release Order"),
}

enum class HarryPotterFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class HarryPotterWatchItemUi(
    val movie: HarryPotterMovie,
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

object HarryPotterMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .replace("sorcerersstone", "philosophersstone")
    }

    val movies: List<HarryPotterMovie> = listOf(
        HarryPotterMovie(
            id = "fantastic_beasts_1_2016",
            title = "Fantastic Beasts and Where to Find Them",
            releaseYear = 2016,
            releaseOrder = 9,
            timelineOrder = 1,
            timelineSetting = "1926 (New York)",
            era = "Fantastic Beasts",
            tmdbId = 259316,
            imdbId = "tt3183660",
            fallbackPosterPath = "/h6NYfVUyM6CDURtZSnBpz647Ldd.jpg",
            overview = "In 1926, magizoologist Newt Scamander arrives in New York with a leather suitcase full of extraordinary magical creatures that escape into the city.",
        ),
        HarryPotterMovie(
            id = "fantastic_beasts_2_2018",
            title = "Fantastic Beasts: The Crimes of Grindelwald",
            releaseYear = 2018,
            releaseOrder = 10,
            timelineOrder = 2,
            timelineSetting = "1927 (Paris)",
            era = "Fantastic Beasts",
            tmdbId = 338952,
            imdbId = "tt4123430",
            fallbackPosterPath = "/fMMrl8fD9gRCFJvsx0SuFwkEOop.jpg",
            overview = "Gellert Grindelwald escapes custody and begins gathering followers to raise pure-blood wizards up to rule over all non-magical beings.",
        ),
        HarryPotterMovie(
            id = "fantastic_beasts_3_2022",
            title = "Fantastic Beasts: The Secrets of Dumbledore",
            releaseYear = 2022,
            releaseOrder = 11,
            timelineOrder = 3,
            timelineSetting = "1932 (Berlin / Bhutan)",
            era = "Fantastic Beasts",
            tmdbId = 338953,
            imdbId = "tt4123432",
            fallbackPosterPath = "/3c5GNLB4yRSLBby0trHoA1DSQxQ.jpg",
            overview = "Professor Albus Dumbledore entrusts Newt Scamander to lead an intrepid team of wizards and witches to stop Grindelwald's bid to seize control of the wizarding world.",
        ),
        HarryPotterMovie(
            id = "hp_1_2001",
            title = "Harry Potter and the Philosopher's Stone",
            releaseYear = 2001,
            releaseOrder = 1,
            timelineOrder = 4,
            timelineSetting = "1991 (Year 1)",
            era = "Hogwarts",
            tmdbId = 671,
            imdbId = "tt0241527",
            fallbackPosterPath = "/wuMc08IPKEatf9rnMNXvIDxqP4W.jpg",
            alternateTitle = "Harry Potter and the Sorcerer's Stone",
            overview = "On his eleventh birthday, orphan Harry Potter discovers he is a wizard and is invited to attend Hogwarts School of Witchcraft and Wizardry.",
        ),
        HarryPotterMovie(
            id = "hp_2_2002",
            title = "Harry Potter and the Chamber of Secrets",
            releaseYear = 2002,
            releaseOrder = 2,
            timelineOrder = 5,
            timelineSetting = "1992 (Year 2)",
            era = "Hogwarts",
            tmdbId = 672,
            imdbId = "tt0295297",
            fallbackPosterPath = "/sdEOH0992YZ0QSxgXNIGLq1ToUi.jpg",
            overview = "Harry ignores warnings not to return to Hogwarts, only to find the school plagued by mysterious attacks petrifying students.",
        ),
        HarryPotterMovie(
            id = "hp_3_2004",
            title = "Harry Potter and the Prisoner of Azkaban",
            releaseYear = 2004,
            releaseOrder = 3,
            timelineOrder = 6,
            timelineSetting = "1993 (Year 3)",
            era = "Hogwarts",
            tmdbId = 673,
            imdbId = "tt0304141",
            fallbackPosterPath = "/aWxwnYoe8p2d2fcxOqtvAtJ72Rw.jpg",
            overview = "Harry enters his third year at Hogwarts facing Dementors sent to guard the school from escaped convicted murderer Sirius Black.",
        ),
        HarryPotterMovie(
            id = "hp_4_2005",
            title = "Harry Potter and the Goblet of Fire",
            releaseYear = 2005,
            releaseOrder = 4,
            timelineOrder = 7,
            timelineSetting = "1994 (Year 4)",
            era = "Hogwarts",
            tmdbId = 674,
            imdbId = "tt0330373",
            fallbackPosterPath = "/fECBtHlr0RB3foNHDiCBXeg9Bv9.jpg",
            overview = "Harry is mysteriously entered into the perilous Triwizard Tournament, competing against older champions from three wizarding schools.",
        ),
        HarryPotterMovie(
            id = "hp_5_2007",
            title = "Harry Potter and the Order of the Phoenix",
            releaseYear = 2007,
            releaseOrder = 5,
            timelineOrder = 8,
            timelineSetting = "1995 (Year 5)",
            era = "Hogwarts",
            tmdbId = 675,
            imdbId = "tt0373889",
            fallbackPosterPath = "/5aOyriWkPec0zUDxmHFP9qMmBaj.jpg",
            overview = "As the Ministry of Magic denies Voldemort's return, Harry and Hermione secretly train students as Dumbledore's Army to prepare for the wizarding war.",
        ),
        HarryPotterMovie(
            id = "hp_6_2009",
            title = "Harry Potter and the Half-Blood Prince",
            releaseYear = 2009,
            releaseOrder = 6,
            timelineOrder = 9,
            timelineSetting = "1996 (Year 6)",
            era = "Hogwarts",
            tmdbId = 767,
            imdbId = "tt0417741",
            fallbackPosterPath = "/z7uo9zmQdQwU5ZJHFpv2Upl30i1.jpg",
            overview = "Harry and Dumbledore delve into Lord Voldemort's dark past to find his greatest weakness: Horcruxes containing fragments of his soul.",
        ),
        HarryPotterMovie(
            id = "hp_7_1_2010",
            title = "Harry Potter and the Deathly Hallows – Part 1",
            releaseYear = 2010,
            releaseOrder = 7,
            timelineOrder = 10,
            timelineSetting = "1997 (Year 7)",
            era = "Deathly Hallows",
            tmdbId = 12444,
            imdbId = "tt0926084",
            fallbackPosterPath = "/iGoXIpQb7Pot00EEdwpwPajheZ5.jpg",
            overview = "Without Dumbledore, Harry, Ron, and Hermione leave Hogwarts behind on an urgent mission across Britain to track down and destroy Voldemort's Horcruxes.",
        ),
        HarryPotterMovie(
            id = "hp_7_2_2011",
            title = "Harry Potter and the Deathly Hallows – Part 2",
            releaseYear = 2011,
            releaseOrder = 8,
            timelineOrder = 11,
            timelineSetting = "1998 (The Battle)",
            era = "Deathly Hallows",
            tmdbId = 12445,
            imdbId = "tt1201607",
            fallbackPosterPath = "/c54HpQmuwXjHq2C9wmoACjxoom3.jpg",
            overview = "The final battle between the forces of good and evil in the wizarding world escalates into an all-out war at Hogwarts castle.",
        ),
    )
}
