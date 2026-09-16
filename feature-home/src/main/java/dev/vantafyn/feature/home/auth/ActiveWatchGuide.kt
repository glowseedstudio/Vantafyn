package dev.vantafyn.feature.home.auth

sealed interface ActiveWatchGuide {
    data class Mcu(val state: VantafynMcuWatchGuideDialogState) : ActiveWatchGuide
    data class Saw(val state: VantafynSawWatchGuideDialogState) : ActiveWatchGuide
    data class ResidentEvil(val state: VantafynResidentEvilWatchGuideDialogState) : ActiveWatchGuide
    data class HarryPotter(val state: VantafynHarryPotterWatchGuideDialogState) : ActiveWatchGuide
    data class HungerGames(val state: VantafynHungerGamesWatchGuideDialogState) : ActiveWatchGuide
    data class Scream(val state: VantafynScreamWatchGuideDialogState) : ActiveWatchGuide
    data class Matrix(val state: VantafynMatrixWatchGuideDialogState) : ActiveWatchGuide
    data class Jumanji(val state: VantafynJumanjiWatchGuideDialogState) : ActiveWatchGuide
    data class Jurassic(val state: VantafynJurassicWatchGuideDialogState) : ActiveWatchGuide
    data class Pirates(val state: VantafynPiratesWatchGuideDialogState) : ActiveWatchGuide
    data class Pokemon(val state: VantafynPokemonWatchGuideDialogState) : ActiveWatchGuide
    data class ScaryMovie(val state: VantafynScaryMovieWatchGuideDialogState) : ActiveWatchGuide
    data class Twilight(val state: VantafynTwilightWatchGuideDialogState) : ActiveWatchGuide
    data class Underworld(val state: VantafynUnderworldWatchGuideDialogState) : ActiveWatchGuide
    data class XMen(val state: VantafynXMenWatchGuideDialogState) : ActiveWatchGuide
    data class MiddleEarth(val state: VantafynMiddleEarthWatchGuideDialogState) : ActiveWatchGuide
}
