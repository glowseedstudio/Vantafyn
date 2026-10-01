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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.jellyfin.GameSummary
import dev.vantafyn.core.jellyfin.JellyfinPokemonRepository
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.jellyfin.PokemonBackupDto
import dev.vantafyn.core.jellyfin.PokemonDiagnosticsDto
import dev.vantafyn.core.jellyfin.RestoreBackupRequest
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonBackupRestoreModal(
    session: JellyfinSession?,
    pokemonRepository: JellyfinPokemonRepository,
    selectedGame: GameSummary?,
    onDismiss: () -> Unit,
    onSaveRestored: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var backups by remember { mutableStateOf<List<PokemonBackupDto>>(emptyList()) }
    var diagnostics by remember { mutableStateOf<PokemonDiagnosticsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRestoring by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(true) }
    var backupToRestore by remember { mutableStateOf<PokemonBackupDto?>(null) }

    fun loadData() {
        if (session == null) return
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            pokemonRepository.getDiagnostics(session).onSuccess {
                diagnostics = it
            }
            if (selectedGame != null) {
                pokemonRepository.getGameBackups(session, "default", selectedGame.id).onSuccess {
                    backups = it
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedGame) {
        loadData()
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        PokemonModalContainer(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
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
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Save Backups & Diagnostics",
                            color = VantafynColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = selectedGame?.pokemon?.canonicalTitle ?: selectedGame?.cleanTitle ?: "Vantafyn Subsystem",
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

            // Status message banner
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

            // System Diagnostics Card
            diagnostics?.let { diag ->
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
                            text = "SYSTEM HEALTH",
                            color = VantafynColors.Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (diag.providerHealthy) Color(0xFF10B981).copy(alpha = 0.2f)
                                    else Color(0xFFEF4444).copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (diag.providerHealthy) "${diag.providerType.uppercase()} READY" else "PROVIDER OFFLINE",
                                color = if (diag.providerHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        DiagnosticMetric(label = "Vault Stored", value = "${diag.totalStoredPokemon}")
                        DiagnosticMetric(label = "Shinies", value = "${diag.totalShinyPokemon}")
                        DiagnosticMetric(label = "Snapshots", value = "${diag.totalBackups}")
                        DiagnosticMetric(label = "Active Leases", value = "${diag.activeSessions}")
                    }
                }
            }

            // Backups Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Historical Save Snapshots",
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { loadData() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = VantafynColors.Muted, modifier = Modifier.size(16.dp))
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF00E5FF))
                    }
                } else if (selectedGame == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1E2C))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Select a Game Cartridge in the storage view to inspect its save snapshots.",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                } else if (backups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1E2C))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No snapshots recorded yet. Backups are automatically generated prior to every transfer.",
                            color = VantafynColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                } else {
                    backups.forEach { b ->
                        BackupCard(
                            backup = b,
                            isRestoring = isRestoring,
                            onRestoreClick = { backupToRestore = b },
                        )
                    }
                }
            }
        }
    }
}

    // Confirmation Alert
    if (backupToRestore != null) {
        val b = backupToRestore!!
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            modifier = Modifier.border(1.5.dp, VantafynGradients.accentHorizontal(), RoundedCornerShape(28.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text(text = "Restore Save Snapshot?", color = VantafynColors.Ink, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will overwrite the active battery save for ${selectedGame?.pokemon?.canonicalTitle ?: selectedGame?.cleanTitle} with the snapshot from ${b.createdAtUtc}.",
                        color = VantafynColors.Ink,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "SHA256 integrity has been verified. The game must NOT be actively running in an emulator session.",
                        color = VantafynColors.Muted,
                        fontSize = 11.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = backupToRestore ?: return@TextButton
                        backupToRestore = null
                        if (session != null && selectedGame != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                isRestoring = true
                                statusMessage = null
                                pokemonRepository.restoreBackup(
                                    session,
                                    RestoreBackupRequest(backupId = target.backupId, gameId = selectedGame.id)
                                ).fold(
                                    onSuccess = { res ->
                                        isSuccessMessage = res.isSuccess
                                        statusMessage = res.message
                                        if (res.isSuccess) {
                                            loadData()
                                            onSaveRestored()
                                        }
                                    },
                                    onFailure = {
                                        isSuccessMessage = false
                                        statusMessage = it.message ?: "Failed to restore backup."
                                    }
                                )
                                isRestoring = false
                            }
                        }
                    }
                ) {
                    Text("Restore Now", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("Cancel", color = VantafynColors.Muted)
                }
            },
            containerColor = Color(0xFF1B1E2C),
        )
    }
}

@Composable
private fun DiagnosticMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = VantafynColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = VantafynColors.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun BackupCard(
    backup: PokemonBackupDto,
    isRestoring: Boolean,
    onRestoreClick: () -> Unit,
) {
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
            Column {
                Text(
                    text = backup.reason.ifBlank { "Save Backup" },
                    color = VantafynColors.Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${backup.sizeBytes / 1024} KB • ${backup.createdAtUtc}",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                )
            }

            if (backup.isRestored) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "RESTORED",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2B324D))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isRestoring) { onRestoreClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Restore",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
