package dev.vantafyn.feature.home.games.pokemon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.jellyfin.AcceptTradeRequest
import dev.vantafyn.core.jellyfin.CancelTradeRequest
import dev.vantafyn.core.jellyfin.CreateTradeRequest
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.JoinLinkTradeRequest
import dev.vantafyn.core.jellyfin.PokemonSummaryDto
import dev.vantafyn.core.jellyfin.PokemonTradeOffer
import dev.vantafyn.core.jellyfin.PokemonTradeSession
import dev.vantafyn.core.jellyfin.PokemonTradeStatus
import dev.vantafyn.core.jellyfin.PokemonTradeType
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class TradeTab(val title: String) {
    LinkCode("Link PIN"),
    Direct("Direct"),
    Inbox("Pending"),
    History("History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonTradeModal(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    selectedPokemon: PokemonSummaryDto?,
    onDismiss: () -> Unit,
    onTradeCompleted: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(TradeTab.LinkCode) }

    // Link Trade state
    var linkCodeInput by remember {
        mutableStateOf(Random.nextInt(100000, 999999).toString())
    }

    // Direct Trade state
    var targetTrainerInput by remember { mutableStateOf("") }

    // Pending Trades & History state
    var pendingTrades by remember { mutableStateOf<List<PokemonTradeSession>>(emptyList()) }
    var tradeHistory by remember { mutableStateOf<List<PokemonTradeSession>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }
    var isExecutingAction by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(true) }

    fun refreshLists() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            isLoadingList = true
            pokemonRepository.getPendingTrades(session).onSuccess {
                pendingTrades = it
            }
            pokemonRepository.getTradeHistory(session).onSuccess {
                tradeHistory = it
            }
            isLoadingList = false
        }
    }

    LaunchedEffect(selectedTab) {
        refreshLists()
    }

    fun buildOffer(): PokemonTradeOffer? {
        val p = selectedPokemon ?: return null
        return PokemonTradeOffer(
            pokemonId = p.id,
            species = p.species,
            speciesId = p.speciesId,
            nickname = p.nickname,
            level = p.level,
            isShiny = p.isShiny,
            generation = 0,
            isVault = true,
            boxIndex = p.boxIndex,
            slotIndex = p.slotIndex,
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151722))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Trade Center",
                            color = VantafynColors.Ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Personal Vault Exchange",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = VantafynColors.Muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C2030))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TradeTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) Color(0xFF2B324D) else Color.Transparent
                            )
                            .clickable {
                                selectedTab = tab
                                statusMessage = null
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) VantafynColors.Ink else VantafynColors.Muted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                            if (tab == TradeTab.Inbox && pendingTrades.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${pendingTrades.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status feedback message
            if (statusMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSuccessMessage) Color(0xFF10B981).copy(alpha = 0.15f)
                            else Color(0xFFEF4444).copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isSuccessMessage) Color(0xFF10B981).copy(alpha = 0.4f)
                            else Color(0xFFEF4444).copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                ) {
                    Text(
                        text = statusMessage!!,
                        color = if (isSuccessMessage) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Selected Pokémon Offer Card (shown for Link and Direct tabs)
            if (selectedTab == TradeTab.LinkCode || selectedTab == TradeTab.Direct) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B1E2C))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "YOUR OFFERED POKÉMON",
                        color = VantafynColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )

                    if (selectedPokemon != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF23283B)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = getPokemonSpriteUrl(selectedPokemon.speciesId, selectedPokemon.isShiny),
                                    contentDescription = selectedPokemon.species,
                                    modifier = Modifier.size(46.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = selectedPokemon.nickname.ifBlank { selectedPokemon.species },
                                        color = VantafynColors.Ink,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (selectedPokemon.isShiny) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Shiny",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = "Lv. ${selectedPokemon.level} • Box ${selectedPokemon.boxIndex ?: 1}",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No Pokémon selected. Select a Pokémon from your Personal Vault first to offer.",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                TradeTab.LinkCode -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Link Code (6-Digit PIN)",
                            color = VantafynColors.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = linkCodeInput,
                                onValueChange = { if (it.length <= 6) linkCodeInput = it.filter { char -> char.isDigit() } },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. 123456", color = VantafynColors.Muted) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = VantafynColors.Ink,
                                    unfocusedTextColor = VantafynColors.Ink,
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                ),
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF23283B))
                                    .clickable {
                                        linkCodeInput = Random.nextInt(100000, 999999).toString()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "New PIN",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        // Host vs Join buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Host
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedPokemon != null && linkCodeInput.length == 6 && !isExecutingAction)
                                            Color(0xFF2B324D)
                                        else
                                            Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = selectedPokemon != null && linkCodeInput.length == 6 && !isExecutingAction && session != null) {
                                        val offer = buildOffer() ?: return@clickable
                                        coroutineScope.launch(Dispatchers.IO) {
                                            isExecutingAction = true
                                            statusMessage = null
                                            pokemonRepository.createTrade(
                                                session!!,
                                                CreateTradeRequest(linkCode = linkCodeInput, offer = offer),
                                            ).fold(
                                                onSuccess = { res ->
                                                    isSuccessMessage = res.isSuccess
                                                    statusMessage = res.message
                                                    if (res.isSuccess) refreshLists()
                                                },
                                                onFailure = {
                                                    isSuccessMessage = false
                                                    statusMessage = it.message ?: "Failed to host Link Trade."
                                                }
                                            )
                                            isExecutingAction = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Host Room",
                                    color = VantafynColors.Ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            // Join
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (selectedPokemon != null && linkCodeInput.length == 6 && !isExecutingAction)
                                            Modifier.background(VantafynGradients.accentHorizontal())
                                        else
                                            Modifier.background(Color.White.copy(alpha = 0.05f))
                                    )
                                    .clickable(enabled = selectedPokemon != null && linkCodeInput.length == 6 && !isExecutingAction && session != null) {
                                        val offer = buildOffer() ?: return@clickable
                                        coroutineScope.launch(Dispatchers.IO) {
                                            isExecutingAction = true
                                            statusMessage = null
                                            pokemonRepository.joinLinkTrade(
                                                session!!,
                                                JoinLinkTradeRequest(linkCode = linkCodeInput, offer = offer),
                                            ).fold(
                                                onSuccess = { res ->
                                                    isSuccessMessage = res.isSuccess
                                                    statusMessage = res.message
                                                    if (res.isSuccess) {
                                                        refreshLists()
                                                        onTradeCompleted()
                                                    }
                                                },
                                                onFailure = {
                                                    isSuccessMessage = false
                                                    statusMessage = it.message ?: "Failed to join Link Trade."
                                                }
                                            )
                                            isExecutingAction = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isExecutingAction) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = "Join & Trade",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                TradeTab.Direct -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Target Trainer Name / User ID",
                            color = VantafynColors.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )

                        OutlinedTextField(
                            value = targetTrainerInput,
                            onValueChange = { targetTrainerInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Gary, Misty, or User ID", color = VantafynColors.Muted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VantafynColors.Ink,
                                unfocusedTextColor = VantafynColors.Ink,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            ),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (selectedPokemon != null && targetTrainerInput.isNotBlank() && !isExecutingAction)
                                        Modifier.background(VantafynGradients.accentHorizontal())
                                    else
                                        Modifier.background(Color.White.copy(alpha = 0.05f))
                                )
                                .clickable(enabled = selectedPokemon != null && targetTrainerInput.isNotBlank() && !isExecutingAction && session != null) {
                                    val offer = buildOffer() ?: return@clickable
                                    coroutineScope.launch(Dispatchers.IO) {
                                        isExecutingAction = true
                                        statusMessage = null
                                        pokemonRepository.createTrade(
                                            session!!,
                                            CreateTradeRequest(
                                                targetUserName = targetTrainerInput.trim(),
                                                offer = offer,
                                            ),
                                        ).fold(
                                            onSuccess = { res ->
                                                isSuccessMessage = res.isSuccess
                                                statusMessage = res.message
                                                if (res.isSuccess) {
                                                    targetTrainerInput = ""
                                                    refreshLists()
                                                }
                                            },
                                            onFailure = {
                                                isSuccessMessage = false
                                                statusMessage = it.message ?: "Failed to send trade offer."
                                            }
                                        )
                                        isExecutingAction = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isExecutingAction) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Send Direct Offer",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                TradeTab.Inbox -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Pending Invitations",
                                color = VantafynColors.Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = { refreshLists() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = VantafynColors.Muted, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (isLoadingList) {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF00E5FF))
                            }
                        } else if (pendingTrades.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B1E2C))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No pending trade invitations right now.",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        } else {
                            pendingTrades.forEach { trade ->
                                TradePendingCard(
                                    trade = trade,
                                    canCounterOffer = selectedPokemon != null,
                                    isExecutingAction = isExecutingAction,
                                    onAccept = {
                                        val offer = buildOffer() ?: return@TradePendingCard
                                        coroutineScope.launch(Dispatchers.IO) {
                                            isExecutingAction = true
                                            pokemonRepository.acceptTrade(
                                                session!!,
                                                AcceptTradeRequest(tradeId = trade.id, counterOffer = offer)
                                            ).fold(
                                                onSuccess = { res ->
                                                    isSuccessMessage = res.isSuccess
                                                    statusMessage = res.message
                                                    if (res.isSuccess) {
                                                        refreshLists()
                                                        onTradeCompleted()
                                                    }
                                                },
                                                onFailure = {
                                                    isSuccessMessage = false
                                                    statusMessage = it.message ?: "Failed to accept trade."
                                                }
                                            )
                                            isExecutingAction = false
                                        }
                                    },
                                    onCancel = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            isExecutingAction = true
                                            pokemonRepository.cancelTrade(
                                                session!!,
                                                CancelTradeRequest(tradeId = trade.id, reason = "Cancelled by user")
                                            ).fold(
                                                onSuccess = { res ->
                                                    isSuccessMessage = res.isSuccess
                                                    statusMessage = res.message
                                                    refreshLists()
                                                },
                                                onFailure = {
                                                    isSuccessMessage = false
                                                    statusMessage = it.message ?: "Failed to cancel trade."
                                                }
                                            )
                                            isExecutingAction = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                TradeTab.History -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Recent Transactions",
                                color = VantafynColors.Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = { refreshLists() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = VantafynColors.Muted, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (isLoadingList) {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF00E5FF))
                            }
                        } else if (tradeHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B1E2C))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No trade history logged yet.",
                                    color = VantafynColors.Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        } else {
                            tradeHistory.forEach { trade ->
                                TradeHistoryCard(trade = trade)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TradePendingCard(
    trade: PokemonTradeSession,
    canCounterOffer: Boolean,
    isExecutingAction: Boolean,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    val offer = trade.initiatorOffer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1E2C))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Offer from ${trade.initiatorUserName}",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (trade.linkCode != null) {
                Text(
                    text = "PIN: ${trade.linkCode}",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF23283B)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = getPokemonSpriteUrl(offer.speciesId, offer.isShiny),
                    contentDescription = offer.species,
                    modifier = Modifier.size(38.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = offer.nickname.ifBlank { offer.species },
                        color = VantafynColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (offer.isShiny) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Shiny",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Text(
                    text = "Lv. ${offer.level} • Gen ${offer.generation}",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isExecutingAction) { onCancel() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Reject", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (canCounterOffer && !isExecutingAction) Color(0xFF10B981)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .clickable(enabled = canCounterOffer && !isExecutingAction) { onAccept() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (canCounterOffer) "Accept & Trade" else "Select Offer First",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TradeHistoryCard(trade: PokemonTradeSession) {
    val isCompleted = trade.status == PokemonTradeStatus.Completed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1E2C))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${trade.initiatorUserName} ⇄ ${trade.targetUserName ?: "Trainer"}",
                color = VantafynColors.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isCompleted) Color(0xFF10B981).copy(alpha = 0.2f)
                        else Color(0xFFEF4444).copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = trade.status.name,
                    color = if (isCompleted) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${trade.initiatorOffer.species} (Lv. ${trade.initiatorOffer.level})",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
            )
            val targetOffer = trade.targetOffer
            if (targetOffer != null) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = VantafynColors.Muted, modifier = Modifier.size(12.dp))
                Text(
                    text = "${targetOffer.species} (Lv. ${targetOffer.level})",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                )
            }
        }

        if (trade.transactionId != null) {
            Text(
                text = "Tx: ${trade.transactionId}",
                color = VantafynColors.Muted,
                fontSize = 9.sp,
            )
        }
    }
}
