package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonBoxDto
import dev.vantafyn.core.jellyfin.PokemonGameSaveDto
import dev.vantafyn.core.jellyfin.PokemonPokedexDto
import dev.vantafyn.core.jellyfin.PokemonPokedexEntryDto
import dev.vantafyn.core.jellyfin.PokemonPokedexGenerationProgressDto
import dev.vantafyn.core.jellyfin.PokemonSpeciesCatalog
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonPokedexModal(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    localSaves: List<PokemonGameSaveDto> = emptyList(),
    vaultBoxes: List<PokemonBoxDto> = emptyList(),
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var pokedex by remember { mutableStateOf<PokemonPokedexDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedGen by remember { mutableIntStateOf(0) } // 0 = All
    var searchQuery by remember { mutableStateOf("") }
    var filterCaughtOnly by remember { mutableStateOf(false) }
    var filterShinyOnly by remember { mutableStateOf(false) }
    var inspectingEntry by remember { mutableStateOf<PokemonPokedexEntryDto?>(null) }

    // 1. Gather all local Pokémon from active game saves & personal vault
    val localPokemon = remember(localSaves, vaultBoxes) {
        val list = mutableListOf<Pair<PokemonSummaryDto, String>>()
        for (save in localSaves) {
            val title = save.title.ifBlank { "Cartridge Save" }
            for (p in save.party) {
                if (p.speciesId > 0) list.add(p to title)
            }
            for (b in save.boxes) {
                for (p in b.entries) {
                    if (p.speciesId > 0) list.add(p to title)
                }
            }
        }
        for (b in vaultBoxes) {
            for (p in b.entries) {
                if (p.speciesId > 0) list.add(p to (p.originGame ?: "Personal Vault"))
            }
        }
        list
    }

    // 2. Aggregate caught/seen bitfields and party/box species
    val localCaughtIds = remember(localSaves, localPokemon) {
        val set = mutableSetOf<Int>()
        for (save in localSaves) {
            set.addAll(save.caughtSpeciesIds)
        }
        for ((p, _) in localPokemon) {
            if (p.speciesId in 1..1025) set.add(p.speciesId)
        }
        set
    }

    val localSeenIds = remember(localSaves, localCaughtIds) {
        val set = mutableSetOf<Int>()
        for (save in localSaves) {
            set.addAll(save.seenSpeciesIds)
        }
        set.addAll(localCaughtIds)
        set
    }

    val localShinyIds = remember(localPokemon) {
        localPokemon.filter { it.first.isShiny }.map { it.first.speciesId }.toSet()
    }

    val localFirstGameMap = remember(localPokemon) {
        val map = mutableMapOf<Int, String>()
        for ((p, game) in localPokemon) {
            if (!map.containsKey(p.speciesId) && game.isNotBlank()) {
                map[p.speciesId] = game
            }
        }
        map
    }

    fun loadPokedex() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            // If local data contains caught/seen species, sync them to companion plugin
            if (localCaughtIds.isNotEmpty() || localSeenIds.isNotEmpty()) {
                val primaryGame = localSaves.firstOrNull()?.title
                pokemonRepository.syncPokedex(
                    session = session,
                    caughtSpeciesIds = localCaughtIds.toList(),
                    seenSpeciesIds = localSeenIds.toList(),
                    originGame = primaryGame,
                ).fold(
                    onSuccess = { pokedex = it },
                    onFailure = {
                        pokemonRepository.getPokedex(session).onSuccess { pokedex = it }
                    }
                )
            } else {
                pokemonRepository.getPokedex(session).onSuccess {
                    pokedex = it
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPokedex()
    }

    // 3. National Pokédex: synthesize full 1025 catalog merging server + local saves + vault
    val allEntries = remember(pokedex, localCaughtIds, localSeenIds, localShinyIds, localFirstGameMap) {
        val serverMap = pokedex?.entries?.associateBy { it.speciesId } ?: emptyMap()
        (1..1025).map { speciesId ->
            val serverEntry = serverMap[speciesId]
            val isCaught = (serverEntry?.isCaught == true) || (speciesId in localCaughtIds)
            val isSeen = isCaught || (serverEntry?.isSeen == true) || (speciesId in localSeenIds)
            val hasShiny = (serverEntry?.hasShiny == true) || (speciesId in localShinyIds)
            val firstGame = serverEntry?.firstEncounteredGame ?: localFirstGameMap[speciesId]
            val name = serverEntry?.speciesName?.ifBlank { null } ?: PokemonSpeciesCatalog.resolveSpeciesName(speciesId)
            val gen = PokemonSpeciesCatalog.getGeneration(speciesId)

            PokemonPokedexEntryDto(
                speciesId = speciesId,
                speciesName = name,
                generation = gen,
                isCaught = isCaught,
                isSeen = isSeen,
                hasShiny = hasShiny,
                firstEncounteredGame = firstGame,
                firstEncounteredTimestamp = serverEntry?.firstEncounteredTimestamp,
                encounterCount = (serverEntry?.encounterCount ?: 0).coerceAtLeast(if (isSeen) 1 else 0),
            )
        }
    }

    val totalCaught = remember(allEntries) { allEntries.count { it.isCaught } }
    val totalSeen = remember(allEntries) { allEntries.count { it.isSeen } }
    val totalShinies = remember(allEntries) { allEntries.count { it.hasShiny } }

    val genBounds = remember {
        listOf(
            1 to (1..151),
            2 to (152..251),
            3 to (252..386),
            4 to (387..493),
            5 to (494..649),
            6 to (650..721),
            7 to (722..809),
            8 to (810..905),
            9 to (906..1025),
        )
    }

    val genProgressList = remember(allEntries, genBounds) {
        val genNames = listOf(
            "Gen I (Kanto)", "Gen II (Johto)", "Gen III (Hoenn)",
            "Gen IV (Sinnoh)", "Gen V (Unova)", "Gen VI (Kalos)",
            "Gen VII (Alola)", "Gen VIII (Galar)", "Gen IX (Paldea)"
        )
        genBounds.map { (gen, range) ->
            val genEntries = allEntries.filter { it.speciesId in range }
            val caught = genEntries.count { it.isCaught }
            val seen = genEntries.count { it.isSeen }
            val shiny = genEntries.count { it.hasShiny }
            val total = range.count()
            val pct = if (total > 0) ((caught.toDouble() / total) * 100.0) else 0.0
            PokemonPokedexGenerationProgressDto(
                generation = gen,
                generationName = genNames.getOrElse(gen - 1) { "Gen $gen" },
                minDexNumber = range.first,
                maxDexNumber = range.last,
                totalSpecies = total,
                caughtCount = caught,
                seenCount = seen,
                shinyCount = shiny,
                caughtPercentage = Math.round(pct * 10.0) / 10.0,
            )
        }
    }

    val filteredEntries = remember(allEntries, selectedGen, searchQuery, filterCaughtOnly, filterShinyOnly) {
        allEntries.filter { entry ->
            val matchesGen = selectedGen == 0 || entry.generation == selectedGen
            val matchesSearch = searchQuery.isBlank() ||
                entry.speciesName.contains(searchQuery.trim(), ignoreCase = true) ||
                entry.speciesId.toString().contains(searchQuery.trim())
            val matchesCaught = !filterCaughtOnly || entry.isCaught
            val matchesShiny = !filterShinyOnly || entry.hasShiny
            matchesGen && matchesSearch && matchesCaught && matchesShiny
        }
    }

    val activeProgress = remember(genProgressList, selectedGen) {
        if (selectedGen == 0) null
        else genProgressList.firstOrNull { it.generation == selectedGen }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        PokemonModalContainer(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(680.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VantafynGradients.accentHorizontal()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "National Pokédex",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Aggregated Cross-Game Archive (1025 Species)",
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { loadPokedex() },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh Pokédex",
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = VantafynColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Statistics Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E2B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "$totalCaught Caught",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "$totalSeen Seen",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "$totalShinies Shinies",
                            color = Color(0xFFFFD700),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Generation Selector Scroll Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GenPill(
                        label = "All Gens",
                        isSelected = selectedGen == 0,
                        onClick = { selectedGen = 0 },
                    )
                    val genNames = listOf(
                        "Gen I (Kanto)",
                        "Gen II (Johto)",
                        "Gen III (Hoenn)",
                        "Gen IV (Sinnoh)",
                        "Gen V (Unova)",
                        "Gen VI (Kalos)",
                        "Gen VII (Alola)",
                        "Gen VIII (Galar)",
                        "Gen IX (Paldea)",
                    )
                    for (gen in 1..9) {
                        GenPill(
                            label = genNames.getOrElse(gen - 1) { "Gen $gen" },
                            isSelected = selectedGen == gen,
                            onClick = { selectedGen = gen },
                        )
                    }
                }

                // Generation Progress Bar (if specific gen selected)
                if (activeProgress != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B1E2B).copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${activeProgress.generationName} Progress",
                                color = VantafynColors.Muted,
                                fontSize = 11.sp,
                            )
                            Text(
                                text = "${activeProgress.caughtCount} / ${activeProgress.totalSpecies} (${activeProgress.caughtPercentage}%)",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (activeProgress.caughtCount.toFloat() / activeProgress.totalSpecies.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF262C40),
                        )
                    }
                }

                // Search and Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var isSearchFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B1E2B))
                            .border(
                                width = if (isSearchFocused) 1.5.dp else 1.dp,
                                brush = if (isSearchFocused) VantafynGradients.accentHorizontal() else Brush.linearGradient(listOf(Color(0xFF3B425A), Color(0xFF3B425A))),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = if (isSearchFocused) Color(0xFF00E5FF) else VantafynColors.Muted,
                            modifier = Modifier.size(17.dp),
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            cursorBrush = VantafynGradients.accentHorizontal(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search Pokémon or #...",
                                            color = VantafynColors.Muted,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = VantafynColors.Muted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" },
                            )
                        }
                    }

                    // Caught filter chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (filterCaughtOnly) Modifier.background(Color(0xFF10B981).copy(alpha = 0.25f))
                                else Modifier.background(Color(0xFF1B1E2B))
                            )
                            .border(
                                1.dp,
                                if (filterCaughtOnly) Color(0xFF10B981) else Color(0xFF3B425A),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { filterCaughtOnly = !filterCaughtOnly }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "Caught",
                            color = if (filterCaughtOnly) Color(0xFF10B981) else VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Shiny filter chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (filterShinyOnly) Modifier.background(Color(0xFFFFD700).copy(alpha = 0.25f))
                                else Modifier.background(Color(0xFF1B1E2B))
                            )
                            .border(
                                1.dp,
                                if (filterShinyOnly) Color(0xFFFFD700) else Color(0xFF3B425A),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { filterShinyOnly = !filterShinyOnly }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "✨ Shiny",
                            color = if (filterShinyOnly) Color(0xFFFFD700) else VantafynColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Entries Grid
                if (isLoading && allEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                } else if (filteredEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = VantafynColors.Muted.copy(alpha = 0.6f),
                                modifier = Modifier.size(38.dp),
                            )
                            Text(
                                text = if (filterCaughtOnly && totalCaught == 0) "No Pokémon caught yet."
                                else if (filterShinyOnly && totalShinies == 0) "No Shiny Pokémon registered yet."
                                else if (searchQuery.isNotBlank()) "No Pokémon matching \"$searchQuery\"."
                                else "No Pokémon found for the current selection.",
                                color = VantafynColors.Muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredEntries, key = { it.speciesId }) { entry ->
                            PokedexCard(
                                entry = entry,
                                onClick = {
                                    if (entry.isCaught || entry.isSeen) {
                                        inspectingEntry = entry
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail modal for inspecting entry and lineage journey
    if (inspectingEntry != null) {
        val entry = inspectingEntry!!
        val localMatch = localPokemon.firstOrNull { it.first.speciesId == entry.speciesId }?.first
        val summary = localMatch ?: PokemonSummaryDto(
            id = "dex-${entry.speciesId}",
            species = entry.speciesName,
            speciesId = entry.speciesId,
            level = 50,
            isShiny = entry.hasShiny,
            originGame = entry.firstEncounteredGame,
        )
        PokemonDetailModal(
            pokemon = summary,
            session = session,
            pokemonRepository = pokemonRepository,
            onDismiss = { inspectingEntry = null },
        )
    }
}

@Composable
private fun GenPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isSelected) Modifier.background(VantafynGradients.accentHorizontal())
                else Modifier.background(Color(0xFF1B1E2B))
            )
            .border(
                1.dp,
                if (isSelected) Color.Transparent else Color(0xFF3B425A),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else VantafynColors.Muted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PokedexCard(
    entry: PokemonPokedexEntryDto,
    onClick: () -> Unit,
) {
    val spriteUrl = getPokemonSpriteUrl(entry.speciesId, entry.hasShiny)
    val isKnown = entry.isCaught || entry.isSeen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isKnown) Color(0xFF1B1E2B) else Color(0xFF131520))
            .border(
                1.dp,
                if (entry.hasShiny) Color(0xFFFFD700).copy(alpha = 0.5f)
                else if (entry.isCaught) Color(0xFF10B981).copy(alpha = 0.4f)
                else if (entry.isSeen) Color(0xFF00E5FF).copy(alpha = 0.3f)
                else Color(0xFF262A3B),
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = isKnown, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#%03d".format(entry.speciesId),
                color = if (isKnown) VantafynColors.Muted else Color(0xFF4A5568),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (entry.hasShiny) {
                    Text(text = "✨", fontSize = 9.sp)
                }
                if (entry.isCaught) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                    )
                } else if (entry.isSeen) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isKnown) Color(0xFF12141D) else Color(0xFF0E1017)),
            contentAlignment = Alignment.Center,
        ) {
            if (isKnown) {
                AsyncImage(
                    model = spriteUrl,
                    contentDescription = entry.speciesName,
                    modifier = Modifier
                        .size(46.dp)
                        .then(if (!entry.isCaught && entry.isSeen) Modifier.alpha(0.7f) else Modifier),
                    contentScale = ContentScale.Fit,
                )
            } else {
                AsyncImage(
                    model = spriteUrl,
                    contentDescription = "Unknown",
                    modifier = Modifier
                        .size(44.dp)
                        .alpha(0.12f),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Text(
            text = if (isKnown) entry.speciesName else "???",
            color = if (entry.isCaught) Color.White else if (entry.isSeen) Color(0xFFCBD5E1) else Color(0xFF4A5568),
            fontSize = 11.sp,
            fontWeight = if (entry.isCaught) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val firstGame = entry.firstEncounteredGame
        if (!firstGame.isNullOrEmpty() && entry.isCaught) {
            Text(
                text = firstGame,
                color = VantafynColors.Muted,
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (entry.isSeen && !entry.isCaught) {
            Text(
                text = "Seen",
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                fontSize = 8.5.sp,
                maxLines = 1,
            )
        }
    }
}
