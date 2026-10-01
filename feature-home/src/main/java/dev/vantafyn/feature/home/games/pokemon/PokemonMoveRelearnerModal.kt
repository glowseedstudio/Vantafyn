package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonDetailsDto
import dev.vantafyn.core.jellyfin.PokemonLearnableMoveDto
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonMoveRelearnerModal(
    pokemon: PokemonSummaryDto,
    details: PokemonDetailsDto?,
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    isVault: Boolean,
    onDismiss: () -> Unit,
    onMovesUpdated: (List<String>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var currentMoves by remember { mutableStateOf(details?.moves ?: emptyList()) }
    var selectedSlotToReplace by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Fallback learnsets if details?.learnableMoves is empty
    val availableMoves = remember(details?.learnableMoves, pokemon) {
        val serverMoves = details?.learnableMoves ?: emptyList()
        if (serverMoves.isNotEmpty()) {
            serverMoves
        } else {
            generateClientFallbackMovepool(pokemon.species, pokemon.level)
        }
    }

    val filteredMoves = remember(availableMoves, searchQuery, selectedFilterCategory) {
        availableMoves.filter { move ->
            val matchesSearch = searchQuery.isBlank() ||
                move.name.contains(searchQuery, ignoreCase = true) ||
                move.type.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedFilterCategory) {
                "All" -> true
                "Level Up" -> move.learnMethod.contains("Level", ignoreCase = true)
                "TM / Tutor" -> !move.learnMethod.contains("Level", ignoreCase = true)
                "Physical" -> move.category.equals("Physical", ignoreCase = true)
                "Special" -> move.category.equals("Special", ignoreCase = true)
                "Status" -> move.category.equals("Status", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    fun handleLearnMove(move: PokemonLearnableMoveDto) {
        if (isSaving) return

        val newMovesList = currentMoves.toMutableList()
        if (selectedSlotToReplace in newMovesList.indices) {
            newMovesList[selectedSlotToReplace] = move.name
        } else if (newMovesList.size < 4) {
            newMovesList.add(move.name)
        } else {
            newMovesList[0] = move.name
        }

        currentMoves = newMovesList

        if (isVault && session != null) {
            isSaving = true
            coroutineScope.launch(Dispatchers.IO) {
                val res = pokemonRepository.updateVaultEntryMoves(session, pokemon.id, newMovesList)
                withContext(Dispatchers.Main) {
                    isSaving = false
                    res.fold(
                        onSuccess = {
                            statusMessage = "Learned ${move.name}!"
                            onMovesUpdated(newMovesList)
                        },
                        onFailure = { err ->
                            statusMessage = "Save failed: ${err.message}"
                        }
                    )
                }
            }
        } else {
            statusMessage = "Learned ${move.name}!"
            onMovesUpdated(newMovesList)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        PokemonModalContainer(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.3f), Color(0xFF00E5FF).copy(alpha = 0.3f))
                                )
                            )
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Move Relearner",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Move Relearner & Inspector",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${pokemon.species} • Lv. ${pokemon.level}",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Status message banner
            AnimatedVisibility(visible = statusMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = Color(0xFF10B981),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { statusMessage = null },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Active Moves Slots (Tap to select which slot to replace)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF191C2B))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ACTIVE MOVESET",
                        color = VantafynColors.Muted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tap a slot to select target for replacement",
                        color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        fontSize = 9.sp,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (slotIdx in 0 until 4) {
                        val moveName = currentMoves.getOrNull(slotIdx)
                        val isSelected = selectedSlotToReplace == slotIdx
                        ActiveMoveSlotChip(
                            slotNumber = slotIdx + 1,
                            moveName = moveName,
                            isSelected = isSelected,
                            onClick = { selectedSlotToReplace = slotIdx },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Search Bar & Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search moves by name or element...", fontSize = 11.5.sp, color = VantafynColors.Muted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = VantafynColors.Muted, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color(0xFF191C2B),
                    unfocusedContainerColor = Color(0xFF191C2B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
                shape = RoundedCornerShape(10.dp),
            )

            // Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val filters = listOf("All", "Level Up", "TM / Tutor", "Physical", "Special", "Status")
                filters.forEach { filter ->
                    val isSelected = selectedFilterCategory == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2235))
                            .border(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .clickable { selectedFilterCategory = filter }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color(0xFF0A0C14) else Color.White.copy(alpha = 0.85f),
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            // Learnable Moves List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (filteredMoves.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No learnable moves found matching '$searchQuery'",
                                color = VantafynColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                } else {
                    items(filteredMoves, key = { it.name }) { move ->
                        val isAlreadyLearned = currentMoves.any { it.equals(move.name, ignoreCase = true) }
                        LearnableMoveRow(
                            move = move,
                            isAlreadyLearned = isAlreadyLearned,
                            onLearn = { handleLearnMove(move) },
                            isSaving = isSaving,
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ActiveMoveSlotChip(
    slotNumber: Int,
    moveName: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.16f) else Color(0xFF131520))
            .border(
                1.5.dp,
                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Slot $slotNumber",
                    color = if (isSelected) Color(0xFF00E5FF) else VantafynColors.Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                    )
                }
            }

            Text(
                text = moveName ?: "—",
                color = if (moveName != null) Color.White else VantafynColors.Muted.copy(alpha = 0.5f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LearnableMoveRow(
    move: PokemonLearnableMoveDto,
    isAlreadyLearned: Boolean,
    onLearn: () -> Unit,
    isSaving: Boolean,
) {
    val typeColor = resolvePokemonTypeColor(move.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF181B28))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Type Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor.copy(alpha = 0.20f))
                        .border(0.8.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = move.type.uppercase(),
                        color = typeColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                // Category Badge (Physical, Special, Status)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = move.category,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = move.name,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Stats row (Power, Accuracy, PP, Learn condition)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val powerText = move.power?.let { "Pwr: $it" } ?: "Pwr: —"
                val accText = move.accuracy?.let { "Acc: $it%" } ?: "Acc: —"
                val ppText = "PP: ${move.pp}"

                Text(
                    text = "$powerText • $accText • $ppText",
                    color = VantafynColors.Muted,
                    fontSize = 10.sp,
                )

                val reqBadge = when {
                    move.levelLearned != null -> "Lv. ${move.levelLearned}"
                    else -> move.learnMethod
                }
                Text(
                    text = "• $reqBadge",
                    color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            val desc = move.description
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    color = VantafynColors.Muted.copy(alpha = 0.8f),
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Button
        if (isAlreadyLearned) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Active",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "ACTIVE",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            val buttonModifier = if (isSaving) {
                Modifier.background(Color.Gray.copy(alpha = 0.2f))
            } else {
                Modifier.background(VantafynGradients.accentHorizontal())
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(buttonModifier)
                    .clickable(enabled = !isSaving, onClick = onLearn)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "TEACH",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun resolvePokemonTypeColor(type: String): Color = when (type.lowercase()) {
    "fire" -> Color(0xFFEF4444)
    "water" -> Color(0xFF38BDF8)
    "grass" -> Color(0xFF22C55E)
    "electric" -> Color(0xFFFACC15)
    "ice" -> Color(0xFF06B6D4)
    "fighting" -> Color(0xFFDC2626)
    "poison" -> Color(0xFFA855F7)
    "ground" -> Color(0xFFD97706)
    "flying" -> Color(0xFF818CF8)
    "psychic" -> Color(0xFFEC4899)
    "bug" -> Color(0xFF84CC16)
    "rock" -> Color(0xFFB45309)
    "ghost" -> Color(0xFF7C3AED)
    "dragon" -> Color(0xFF6366F1)
    "steel" -> Color(0xFF94A3B8)
    "dark" -> Color(0xFF475569)
    "fairy" -> Color(0xFFF472B6)
    else -> Color(0xFF9CA3AF)
}

private fun generateClientFallbackMovepool(species: String, level: Int): List<PokemonLearnableMoveDto> {
    val clean = species.lowercase()
    val list = mutableListOf<PokemonLearnableMoveDto>()

    list.add(PokemonLearnableMoveDto("Tackle", "Normal", "Physical", 40, 100, 35, "Level Up", 1, "Charges target with a full-body tackle."))
    list.add(PokemonLearnableMoveDto("Quick Attack", "Normal", "Physical", 40, 100, 30, "Level Up", 8, "Priority attack at blinding speed."))
    list.add(PokemonLearnableMoveDto("Protect", "Normal", "Status", null, null, 10, "TM / TR", null, "Evades all incoming attacks this turn."))
    list.add(PokemonLearnableMoveDto("Substitute", "Normal", "Status", null, null, 10, "TM / TR", null, "Creates a decoy substitute using HP."))
    list.add(PokemonLearnableMoveDto("Rest", "Psychic", "Status", null, null, 10, "TM / TR", null, "Sleeps 2 turns to fully restore health."))

    if (clean.contains("char") || clean.contains("fire") || clean.contains("flare")) {
        list.add(PokemonLearnableMoveDto("Flamethrower", "Fire", "Special", 90, 100, 15, "Level Up", 30, "Shoots intense fire with a 10% burn chance."))
        list.add(PokemonLearnableMoveDto("Fire Blast", "Fire", "Special", 110, 85, 5, "TM / TR", null, "High-power flame attack with 10% burn chance."))
        list.add(PokemonLearnableMoveDto("Air Slash", "Flying", "Special", 75, 95, 15, "Level Up", 36, "Slices with blade of wind; 30% flinch chance."))
        list.add(PokemonLearnableMoveDto("Dragon Claw", "Dragon", "Physical", 80, 100, 15, "TM / TR", null, "Slashes the target with hard, sharp claws."))
    } else if (clean.contains("pika") || clean.contains("raichu") || clean.contains("electr")) {
        list.add(PokemonLearnableMoveDto("Thunderbolt", "Electric", "Special", 90, 100, 15, "Level Up", 32, "Strong electric blast with a 10% paralyze chance."))
        list.add(PokemonLearnableMoveDto("Thunder Wave", "Electric", "Status", null, 90, 20, "Level Up", 12, "Paralyzes the target."))
        list.add(PokemonLearnableMoveDto("Thunder", "Electric", "Special", 110, 70, 10, "Level Up", 45, "A wicked thunderbolt strikes the target."))
        list.add(PokemonLearnableMoveDto("Volt Switch", "Electric", "Special", 70, 100, 20, "TM / TR", null, "Deals damage and switches user out."))
    } else if (clean.contains("gengar") || clean.contains("ghost") || clean.contains("ghast")) {
        list.add(PokemonLearnableMoveDto("Shadow Ball", "Ghost", "Special", 80, 100, 15, "Level Up", 28, "Hurls a shadowy blob that may lower Sp. Def."))
        list.add(PokemonLearnableMoveDto("Sludge Bomb", "Poison", "Special", 90, 100, 10, "TM / TR", null, "Unleashes sludge with a 30% poison chance."))
        list.add(PokemonLearnableMoveDto("Destiny Bond", "Ghost", "Status", null, null, 5, "Level Up", 44, "When user faints, the attacker faints too."))
    } else {
        list.add(PokemonLearnableMoveDto("Hyper Beam", "Normal", "Special", 150, 90, 5, "TM / TR", null, "Powerful beam requiring a recharge turn."))
        list.add(PokemonLearnableMoveDto("Body Slam", "Normal", "Physical", 85, 100, 15, "Level Up", 25, "Drops full weight on target; 30% paralysis."))
    }

    return list
}
