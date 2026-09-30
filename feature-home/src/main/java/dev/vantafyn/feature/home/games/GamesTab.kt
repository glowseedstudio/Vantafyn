package dev.vantafyn.feature.home.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class GamesTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Library("Search", Icons.Rounded.Search),
    Vault("PkVault", Icons.Rounded.CatchingPokemon),
    Saves("Saves", Icons.Rounded.CloudDone),
    Settings("Settings", Icons.Rounded.Settings),
}
