package dev.vantafyn.feature.home.xmen

import java.util.UUID

data class XMenMovie(
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

enum class XMenSortMode(val label: String) {
    Timeline("Timeline Order"),
    Release("Release Order"),
}

enum class XMenFilterMode(val label: String) {
    All("All"),
    Watched("Watched"),
    Unwatched("Unwatched"),
    Missing("Not on server"),
}

data class XMenWatchItemUi(
    val movie: XMenMovie,
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

object XMenMoviesCatalog {

    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    val movies: List<XMenMovie> = listOf(
        XMenMovie(
            id = "x_men_first_class_2011",
            title = "X-Men: First Class",
            releaseYear = 2011,
            releaseOrder = 5,
            timelineOrder = 1,
            timelineSetting = "1962 (Cuban Missile Crisis)",
            era = "Origins",
            tmdbId = 49538,
            imdbId = "tt1270798",
            fallbackPosterPath = "/7UhKtkj0QY1rW3d4jG9z2c9C0k.jpg",
            overview = "In the 1960s during the Cold War, Charles Xavier and Erik Lehnsherr unite to form the first team of mutants.",
        ),
        XMenMovie(
            id = "x_men_days_of_future_past_2014",
            title = "X-Men: Days of Future Past",
            releaseYear = 2014,
            releaseOrder = 7,
            timelineOrder = 2,
            timelineSetting = "1973 (Paris Peace Accords)",
            era = "Time Travel",
            tmdbId = 127585,
            imdbId = "tt1877832",
            fallbackPosterPath = "/tK1cq674d8HqG0zX4bV0k0L9Q8.jpg",
            overview = "Wolverine is sent back to 1973 to prevent the assassination of Bolivar Trask and change history.",
        ),
        XMenMovie(
            id = "x_men_origins_wolverine_2009",
            title = "X-Men Origins: Wolverine",
            releaseYear = 2009,
            releaseOrder = 4,
            timelineOrder = 3,
            timelineSetting = "1845–1979 (Weapon X)",
            era = "Weapon X",
            tmdbId = 2080,
            imdbId = "tt0458525",
            fallbackPosterPath = "/7A0T22n6yW7d8j9m9n0k1L2b3.jpg",
            overview = "The early years of Logan and his brother Victor Creed, leading to the adamantium Weapon X procedure.",
        ),
        XMenMovie(
            id = "x_men_apocalypse_2016",
            title = "X-Men: Apocalypse",
            releaseYear = 2016,
            releaseOrder = 9,
            timelineOrder = 4,
            timelineSetting = "1983 (Revised Timeline)",
            era = "Ancient Mutant",
            tmdbId = 246655,
            imdbId = "tt3385516",
            fallbackPosterPath = "/2qgJg8X2L7n9k1m0p1q2r3s4t.jpg",
            overview = "The first and most powerful mutant, En Sabah Nur, awakens in 1983 and recruits four horsemen to cleanse mankind.",
        ),
        XMenMovie(
            id = "dark_phoenix_2019",
            title = "Dark Phoenix",
            releaseYear = 2019,
            releaseOrder = 12,
            timelineOrder = 5,
            timelineSetting = "1992 (Space Mission)",
            era = "Phoenix Force",
            tmdbId = 320288,
            imdbId = "tt6565702",
            fallbackPosterPath = "/cCTJPelKGLilqHQHIJJ6Rnkdy8.jpg",
            overview = "During a rescue mission in space, Jean Grey is hit by a cosmic force that transforms her into the Dark Phoenix.",
        ),
        XMenMovie(
            id = "x_men_2000",
            title = "X-Men",
            releaseYear = 2000,
            releaseOrder = 1,
            timelineOrder = 6,
            timelineSetting = "c. 2000 (Liberty Island)",
            era = "Original Trilogy",
            tmdbId = 36657,
            imdbId = "tt0120903",
            fallbackPosterPath = "/bRDAc4GogS9yiTQUJQDTfZ6b9IV.jpg",
            overview = "Two mutants come to a private academy for their kind whose resident superhero team must oppose a terrorist organization.",
        ),
        XMenMovie(
            id = "x2_2003",
            title = "X2",
            releaseYear = 2003,
            releaseOrder = 2,
            timelineOrder = 7,
            timelineSetting = "c. 2003 (Alkali Lake)",
            era = "Original Trilogy",
            tmdbId = 36658,
            imdbId = "tt0290334",
            fallbackPosterPath = "/2K4xR9d7g2e3p0w1v2u3t4s5r.jpg",
            overview = "The X-Men band together to find a mutant assassin who has made an attempt on the President's life, while the Academy is attacked.",
        ),
        XMenMovie(
            id = "x_men_the_last_stand_2006",
            title = "X-Men: The Last Stand",
            releaseYear = 2006,
            releaseOrder = 3,
            timelineOrder = 8,
            timelineSetting = "c. 2006 (Alcatraz Battle)",
            era = "Original Trilogy",
            tmdbId = 2108,
            imdbId = "tt0376994",
            fallbackPosterPath = "/4p8v25g1k4p6l0p1v0m0k2n.jpg",
            overview = "A cure for mutancy threatens to alter history. For the first time, mutants have a choice: retain their uniqueness or be cured.",
        ),
        XMenMovie(
            id = "the_wolverine_2013",
            title = "The Wolverine",
            releaseYear = 2013,
            releaseOrder = 6,
            timelineOrder = 9,
            timelineSetting = "c. 2013 (Tokyo, Japan)",
            era = "Wolverine Solo",
            tmdbId = 76170,
            imdbId = "tt1430132",
            fallbackPosterPath = "/9Xw0I5rv20m0g9k8j7h6f5d4.jpg",
            overview = "Wolverine faces his ultimate nemesis in an action-packed battle that takes him to modern-day Japan.",
        ),
        XMenMovie(
            id = "deadpool_2016",
            title = "Deadpool",
            releaseYear = 2016,
            releaseOrder = 8,
            timelineOrder = 10,
            timelineSetting = "c. 2016 (Modern Day)",
            era = "Deadpool",
            tmdbId = 293660,
            imdbId = "tt1431045",
            fallbackPosterPath = "/inVq3MeDAw0rILihZe5Ev83Nx0n.jpg",
            overview = "A wisecracking mercenary gets experimented on and becomes immortal but ugly, and sets out to track down the man who ruined his looks.",
        ),
        XMenMovie(
            id = "deadpool_2_2018",
            title = "Deadpool 2",
            releaseYear = 2018,
            releaseOrder = 11,
            timelineOrder = 11,
            timelineSetting = "c. 2018 (Cable Incursion)",
            era = "Deadpool",
            tmdbId = 383498,
            imdbId = "tt5463162",
            fallbackPosterPath = "/to0spRl1CMDvyUbvdEG3fRJYEvL.jpg",
            overview = "Foul-mouthed mutant mercenary Wade Wilson assembles a team of fellow mutant rogues to protect a young boy with supernatural abilities from Cable.",
        ),
        XMenMovie(
            id = "the_new_mutants_2020",
            title = "The New Mutants",
            releaseYear = 2020,
            releaseOrder = 13,
            timelineOrder = 12,
            timelineSetting = "c. 2020 (Milbury Hospital)",
            era = "Next Gen",
            tmdbId = 340102,
            imdbId = "tt4682266",
            fallbackPosterPath = "/xrI45i9a7w0e1r2t3y4u5i6o.jpg",
            overview = "Five young mutants, just discovering their abilities while held in a secret facility against their will, fight to escape their past sins and save themselves.",
        ),
        XMenMovie(
            id = "logan_2017",
            title = "Logan",
            releaseYear = 2017,
            releaseOrder = 10,
            timelineOrder = 13,
            timelineSetting = "2029 (Dystopian Border)",
            era = "Farewell",
            tmdbId = 263115,
            imdbId = "tt3315342",
            fallbackPosterPath = "/fnbjcRDYn6YviCcePDnGdyAkYsB.jpg",
            overview = "In the near future, a weary Logan cares for an ailing Professor X in a hide out on the Mexican border. But Logan's attempts to hide from the world are up-ended when a young mutant arrives.",
        ),
    )
}
