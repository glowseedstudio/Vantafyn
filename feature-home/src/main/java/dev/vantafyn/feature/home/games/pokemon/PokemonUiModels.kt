package dev.vantafyn.feature.home.games.pokemon

import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.PokemonBoxDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.jellyfin.PokemonVaultBox
import dev.vantafyn.core.jellyfin.PokemonVaultEntry
import dev.vantafyn.core.jellyfin.SaveLockStateDto

enum class TransferSide {
    Source,
    Destination,
}

sealed interface StorageContainerType {
    object PersonalVault : StorageContainerType
    data class GameCartridge(val game: GameSummary) : StorageContainerType
}

data class SelectedPokemonItem(
    val side: TransferSide,
    val isVault: Boolean,
    val gameId: String?,
    val entryId: String?,
    val pokemonId: String,
    val summary: PokemonSummaryDto,
    val boxIndex: Int?,
    val slotIndex: Int,
    val isInParty: Boolean,
)

data class ContainerUiState(
    val side: TransferSide,
    val containerType: StorageContainerType = StorageContainerType.PersonalVault,
    val selectedBoxIndex: Int = 1,
    val isPartyMode: Boolean = false,
    val vaultBox: PokemonVaultBox? = null,
    val gameSave: PokemonGameSaveDto? = null,
    val lockState: SaveLockStateDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLocked: Boolean
        get() = lockState?.isLocked == true

    val totalBoxes: Int
        get() = when (containerType) {
            is StorageContainerType.PersonalVault -> 30
            is StorageContainerType.GameCartridge -> gameSave?.boxes?.size ?: 1
        }

    val currentBoxName: String
        get() = if (isPartyMode) {
            "Party (Team)"
        } else when (containerType) {
            is StorageContainerType.PersonalVault -> vaultBox?.name ?: "Vault Box $selectedBoxIndex"
            is StorageContainerType.GameCartridge -> {
                val box = gameSave?.boxes?.firstOrNull { it.boxIndex == selectedBoxIndex }
                box?.name?.ifBlank { "Box $selectedBoxIndex" } ?: "Box $selectedBoxIndex"
            }
        }

    val currentOccupiedCount: Int
        get() = if (isPartyMode) {
            gameSave?.party?.size ?: 0
        } else when (containerType) {
            is StorageContainerType.PersonalVault -> vaultBox?.entries?.size ?: 0
            is StorageContainerType.GameCartridge -> {
                val box = gameSave?.boxes?.firstOrNull { it.boxIndex == selectedBoxIndex }
                box?.entries?.size ?: 0
            }
        }

    val currentCapacity: Int
        get() = if (isPartyMode) 6 else 30
}

fun getPokemonSpriteUrl(speciesId: Int, isShiny: Boolean = false): String {
    if (speciesId <= 0) return ""
    return if (isShiny) {
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$speciesId.png"
    } else {
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$speciesId.png"
    }
}
