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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CatchingPokemon
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonPokedexDto
import dev.vantafyn.core.jellyfin.PokemonPokedexEntryDto
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
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var pokedex by remember { mutableStateOf<PokemonPokedexDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGen by remember { mutableIntStateOf(0) } // 0 = All
    var searchQuery by remember { mutableStateOf("") }
    var filterCaughtOnly by remember { mutableStateOf(false) }
    var filterShinyOnly by remember { mutableStateOf(false) }
    var inspectingEntry by remember { mutableStateOf<PokemonPokedexEntryDto?>(null) }

    fun loadPokedex() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            pokemonRepository.getPokedex(session).onSuccess {
                pokedex = it
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPokedex()
    }

    val allEntries = pokedex?.entries ?: emptyList()
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

    val activeProgress = remember(pokedex, selectedGen) {
        if (selectedGen == 0) null
        else pokedex?.generationProgress?.firstOrNull { it.generation == selectedGen }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .height(680.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151722))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                            text = "Aggregated Cross-Game Archive",
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
            if (pokedex != null) {
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
                            text = "${pokedex?.totalCaught ?: 0} Caught",
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
                            text = "${pokedex?.totalSeen ?: 0} Seen",
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
                            text = "${pokedex?.totalShinies ?: 0} Shinies",
                            color = Color(0xFFFFD700),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
                        label = genNames.getOrNull(gen - 1) ?: "Gen $gen",
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Pokémon or #...", fontSize = 12.sp, color = VantafynColors.Muted) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = VantafynColors.Muted,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF3B425A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00E5FF),
                    ),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )

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
            if (isLoading) {
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
                            text = if (allEntries.isEmpty()) "No Pokémon registered yet." else "No Pokémon match the filter criteria.",
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
                            onClick = { inspectingEntry = entry },
                        )
                    }
                }
            }
        }
    }

    // Detail modal for inspecting entry and lineage journey
    if (inspectingEntry != null) {
        val entry = inspectingEntry!!
        val summary = PokemonSummaryDto(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1E2B))
            .border(
                1.dp,
                if (entry.hasShiny) Color(0xFFFFD700).copy(alpha = 0.4f)
                else if (entry.isCaught) Color(0xFF10B981).copy(alpha = 0.3f)
                else Color(0xFF2E344A),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
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
                color = VantafynColors.Muted,
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
                .background(Color(0xFF12141D)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = spriteUrl,
                contentDescription = entry.speciesName,
                modifier = Modifier.size(46.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Text(
            text = entry.speciesName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )

        val firstGame = entry.firstEncounteredGame
        if (!firstGame.isNullOrEmpty()) {
            Text(
                text = firstGame,
                color = VantafynColors.Muted,
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}
