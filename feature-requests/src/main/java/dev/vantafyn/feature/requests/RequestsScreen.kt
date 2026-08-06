package dev.vantafyn.feature.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.vantafyn.core.integrations.MediaRequestItem
import dev.vantafyn.core.integrations.MediaRequestSearchResult
import dev.vantafyn.core.integrations.MediaRequestStatus
import dev.vantafyn.core.integrations.MediaRequestType
import dev.vantafyn.core.jellyfin.JellyfinSession
import dev.vantafyn.core.ombi.OmbiAccessMode
import dev.vantafyn.core.ombi.OmbiAccessRequest
import dev.vantafyn.core.ombi.OmbiAccessRequestStatus
import dev.vantafyn.core.ombi.OmbiCapabilities
import dev.vantafyn.core.ombi.OmbiIdentityMode
import dev.vantafyn.core.ombi.OmbiLinkedAccountState
import dev.vantafyn.core.ombi.OmbiUserMapping
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import java.net.URI
import java.text.DateFormat
import java.util.Date

@Composable
fun RequestsScreen(
    session: JellyfinSession?,
    modifier: Modifier = Modifier,
    viewModel: RequestsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(session?.profileId) {
        viewModel.bindSession(session)
    }
    when (state.mode) {
        RequestsScreenMode.Setup -> OmbiSetupWizard(state, viewModel, modifier)
        RequestsScreenMode.Manage -> OmbiManageScreen(state, viewModel, modifier)
        RequestsScreenMode.Requests -> RequestsContent(state, viewModel, modifier)
    }
}

@Composable
private fun RequestsContent(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Header(
                title = "Requests",
                subtitle = null,
                action = if (state.isJellyfinAdmin && state.isConfigured) "Manage Ombi" else null,
                onAction = viewModel::manageOmbi,
            )
        }
        if (!state.isConfigured) {
            item {
                EmptySetupState(state.isJellyfinAdmin, onSetup = viewModel::startSetup)
            }
            return@LazyColumn
        }
        if (!state.config.isEnabledForAdmins) {
            item {
                UnavailableState(
                    title = "Requests are disabled",
                    body = "Ombi is configured, but request access is currently disabled.",
                    admin = state.isJellyfinAdmin,
                    onRetry = viewModel::testConnection,
                    onManage = viewModel::manageOmbi,
                )
            }
            return@LazyColumn
        }
        if (state.connectionStatus == "Connection failed") {
            item {
                UnavailableState(
                    title = "Requests are temporarily unavailable",
                    body = state.message ?: state.config.lastFailureMessage ?: "Vantafyn could not reach Ombi.",
                    admin = state.isJellyfinAdmin,
                    onRetry = viewModel::testConnection,
                    onManage = viewModel::manageOmbi,
                )
            }
        }
        if (state.config.identityMode == OmbiIdentityMode.PerUserAccount && !state.canSearchAndRequest) {
            item { OmbiAccessNeededState(state, viewModel) }
            return@LazyColumn
        }
        item { SearchCard(state, viewModel) }
        state.message?.let { item { MessageCard(it) } }
        if (state.isSearching) item { VantafynLoadingIndicator("Searching Ombi") }
        if (!state.isSearching && state.results.isEmpty() && state.query.length > 1) {
            item { MessageCard("No matching request results yet.") }
        }
        items(state.results, key = { "${it.type}-${it.providerId}" }) { item ->
            RequestSearchResultCard(item, state.activeRequestId == item.providerId) { viewModel.request(item) }
        }
        item {
            RequestHistory(state.requests, state.isJellyfinAdmin, state.isLoadingRequests, onRefresh = viewModel::loadRequests)
        }
    }
}

@Composable
private fun OmbiSetupWizard(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Header("Ombi setup", "Connect Ombi so users can request movies and TV shows from inside Vantafyn.")
        }
        item {
            VantafynGlassPanel(cornerRadius = 24.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepBadge(state.setupStep.stepTitle)
                    when (state.setupStep) {
                        OmbiSetupStep.Connect -> ConnectStep(state, viewModel)
                        OmbiSetupStep.Authentication -> AuthStep(state, viewModel)
                        OmbiSetupStep.Test -> TestStep(state, viewModel)
                        OmbiSetupStep.Access -> AccessStep(state, viewModel)
                        OmbiSetupStep.Finish -> FinishStep(state, viewModel)
                    }
                }
            }
        }
        state.message?.let { item { MessageCard(it) } }
    }
}

@Composable
private fun ConnectStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Connect Ombi", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Use the address your users can reach, such as your Ombi domain or local address.", color = VantafynColors.Muted)
    VantafynTextField(
        value = state.baseUrl,
        onValueChange = viewModel::onBaseUrlChanged,
        label = "Ombi server URL",
        placeholder = "https://requests.example.com",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        enabled = !state.isTesting,
    )
    AddressWarning(state.baseUrl)
    VantafynButton("Continue", onClick = viewModel::nextSetupStep, enabled = state.baseUrl.isNotBlank(), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun AuthStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Authentication", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Find this in Ombi settings. Vantafyn stores it securely on this device.", color = VantafynColors.Muted)
    VantafynTextField(
        value = state.apiKey,
        onValueChange = viewModel::onApiKeyChanged,
        label = if (state.hasApiKey) "API key saved - enter a new key to replace" else "Ombi API key",
        enabled = !state.isTesting,
    )
    SecurityNotes()
    Text("Per-user Ombi login is planned for later. For now, requests are submitted using the configured Ombi API key identity.", color = VantafynColors.Muted)
    WizardButtons(onBack = viewModel::previousSetupStep, onNext = viewModel::nextSetupStep, nextEnabled = state.apiKey.isNotBlank() || state.hasApiKey)
}

@Composable
private fun TestStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Test connection", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Vantafyn checks that Ombi is reachable and that the API key can access request endpoints.", color = VantafynColors.Muted)
    ConnectionSummary(state)
    VantafynButton(
        if (state.isTesting) "Testing" else "Test connection",
        onClick = viewModel::saveDraftAndTest,
        enabled = !state.isTesting && state.baseUrl.isNotBlank() && (state.apiKey.isNotBlank() || state.hasApiKey),
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = viewModel::previousSetupStep) { Text("Back") }
}

@Composable
private fun AccessStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Who can use Requests?", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    AccessOption("Enabled for all users", OmbiAccessMode.AllUsers, state.accessMode, viewModel::onAccessModeChanged)
    AccessOption("Enabled for admins only", OmbiAccessMode.AdminsOnly, state.accessMode, viewModel::onAccessModeChanged)
    AccessOption("Disabled", OmbiAccessMode.Disabled, state.accessMode, viewModel::onAccessModeChanged)
    WizardButtons(onBack = viewModel::previousSetupStep, onNext = viewModel::nextSetupStep)
}

@Composable
private fun FinishStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Finish", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Ombi URL: ${state.baseUrl}", color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text("Access: ${state.accessMode.label}", color = VantafynColors.Muted)
    CapabilityList(state.capabilities)
    SecurityNotes()
    VantafynButton("Enable Requests", onClick = viewModel::finishSetup, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = viewModel::previousSetupStep) { Text("Back") }
}

@Composable
private fun OmbiManageScreen(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Header("Ombi", "Let users request movies and TV shows through Ombi.", action = "Requests", onAction = viewModel::showRequests)
        }
        item {
            VantafynGlassPanel(cornerRadius = 24.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.connectionStatus, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(state.config.baseUrl.ifBlank { "No Ombi server configured" }, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = state.config.accessMode != OmbiAccessMode.Disabled, onCheckedChange = viewModel::setEnabled)
                    }
                    Text("Current access: ${state.config.accessMode.label}", color = VantafynColors.Muted)
                    Text("User access mode: ${state.config.identityMode.label}", color = VantafynColors.Muted)
                    state.config.version?.let { Text("Ombi $it", color = VantafynColors.Muted) }
                    state.config.lastSuccessfulConnectionAt?.let {
                        Text("Last successful test: ${DateFormat.getDateTimeInstance().format(Date(it))}", color = VantafynColors.Muted)
                    }
                    CapabilityList(state.config.capabilities)
                    if (!state.config.capabilities.adminModeration) {
                        MessageCard("Request moderation is not available for this Ombi version yet. Approve and deny controls are hidden until the endpoint is confirmed.")
                    }
                    Text("User access mode", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IdentityModeOption(
                        title = "Shared request account",
                        subtitle = "Users can request media through the Ombi connection configured by the admin. Easiest for home and family setups.",
                        mode = OmbiIdentityMode.SharedApiKey,
                        selected = state.config.identityMode,
                        onSelect = viewModel::onIdentityModeChanged,
                    )
                    IdentityModeOption(
                        title = "Each user links Ombi account",
                        subtitle = "Tracks who needs an Ombi account. Credential linking stays unavailable until Ombi token support is confirmed.",
                        mode = OmbiIdentityMode.PerUserAccount,
                        selected = state.config.identityMode,
                        onSelect = viewModel::onIdentityModeChanged,
                    )
                    if (state.pendingAccessRequestCount > 0) {
                        MessageCard("${state.pendingAccessRequestCount} Ombi access request${if (state.pendingAccessRequestCount == 1) "" else "s"} pending.")
                    }
                    VantafynButton(if (state.isTesting) "Testing" else "Test connection", onClick = viewModel::testConnection, enabled = !state.isTesting && state.isConfigured, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VantafynButton("Edit credentials", onClick = viewModel::startSetup, modifier = Modifier.weight(1f))
                        VantafynButton("Remove", onClick = viewModel::resetIntegration, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item { AccessRequestsSection(state.accessRequests, viewModel) }
        item { UserMappingsSection(state.userMappings, viewModel) }
        state.message?.let { item { MessageCard(it) } }
    }
}

@Composable
private fun OmbiAccessNeededState(state: RequestsUiState, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.hasRequestedAccess) {
                Text("Access requested", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Your server admin has been notified inside Vantafyn on this device/server setup. Once your Ombi account is ready, you can link it here.", color = VantafynColors.Muted)
                MessageCard("Ombi account login is not enabled yet because a safe per-user token endpoint has not been confirmed.")
            } else {
                Text("Requests need access", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("To request movies and shows, link your Ombi account. If you do not have one, ask your server admin for access.", color = VantafynColors.Muted)
                VantafynButton("Link Ombi account", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
                VantafynButton("Request access", onClick = viewModel::requestOmbiAccess, modifier = Modifier.fillMaxWidth())
                MessageCard("Linking is coming later. Access requests are stored locally for admins on this device/server setup.")
            }
            Text("What is Ombi? Ombi lets you ask your server admin to add movies or shows.", color = VantafynColors.Muted)
        }
    }
}

@Composable
private fun EmptySetupState(admin: Boolean, onSetup: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(284.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF172235),
                        Color(0xFF25234A),
                        Color(0xFF0A0F1C),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            VantafynColors.Primary.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        center = Offset(760f, 80f),
                        radius = 620f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.075f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.20f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Requests are ready when you are",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (admin) "Connect Ombi to let this home request movies and series from Vantafyn."
                    else "Your server admin can enable movie and series requests from here.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Movies", "Series", "Family queue")) { label ->
                    TypeChip(label, selected = true, onClick = {})
                }
            }
            if (admin) {
                VantafynButton("Set up Ombi", onClick = onSetup, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun UnavailableState(title: String, body: String, admin: Boolean, onRetry: () -> Unit, onManage: () -> Unit) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(body, color = VantafynColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VantafynButton("Retry", onClick = onRetry, modifier = Modifier.weight(1f))
                if (admin) VantafynButton("Manage Ombi", onClick = onManage, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchCard(state: RequestsUiState, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Find something to request", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            VantafynTextField(value = state.query, onValueChange = viewModel::onQueryChanged, label = "Movie or series title")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(RequestSearchFilter.entries.toList()) { filter ->
                    TypeChip(filter.label, state.searchFilter == filter) { viewModel.onSearchFilterChanged(filter) }
                }
            }
            VantafynButton("Search", onClick = viewModel::search, enabled = state.query.length > 1 && !state.isSearching, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RequestSearchResultCard(item: MediaRequestSearchResult, loading: Boolean, onRequest: () -> Unit) {
    VantafynGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(item.posterUrl, item.title)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(item.type.label, item.year?.toString()).joinToString(" · "), color = VantafynColors.Muted)
                StatusChip(item.status)
                item.overview?.let { Text(it, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            VantafynButton(if (loading) "Sending" else item.status.actionLabel, onClick = onRequest, enabled = !loading && item.status == MediaRequestStatus.NotRequested)
        }
    }
}

@Composable
private fun RequestHistory(items: List<MediaRequestItem>, admin: Boolean, loading: Boolean, onRefresh: () -> Unit) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (admin) "Request queue" else "Your requests", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
            if (loading) {
                VantafynLoadingIndicator("Loading requests")
            } else if (items.isEmpty()) {
                Text("No requests returned by Ombi yet.", color = VantafynColors.Muted)
            } else {
                items.take(16).forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Poster(item.posterUrl, item.title, small = true)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(item.type.label, item.status.label, item.requestedBy).joinToString(" · "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        StatusChip(item.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsTrustNote(identityMode: OmbiIdentityMode) {
    MessageCard(
        when (identityMode) {
            OmbiIdentityMode.SharedApiKey -> "What is Requests? Requests lets you ask your server admin to add movies or shows. Requests are handled through the server’s configured Ombi account."
            OmbiIdentityMode.PerUserAccount -> "What is Requests? Requests lets you ask your server admin to add movies or shows. Your requests are linked to your Ombi account when linking is available."
        },
    )
}

@Composable
private fun Header(title: String, subtitle: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = VantafynColors.Muted)
            }
        }
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun CapabilityList(capabilities: OmbiCapabilities) {
    val labels = capabilities.labels.ifEmpty { listOf("No capabilities detected yet") }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels) { label -> TypeChip(label, selected = label != "No capabilities detected yet", onClick = {}) }
    }
}

@Composable
private fun ConnectionSummary(state: RequestsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Server: ${state.baseUrl.ifBlank { "Not entered" }}", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("API key: ${if (state.apiKey.isNotBlank() || state.hasApiKey) "Ready" else "Missing"}", color = VantafynColors.Muted)
        state.report?.version?.let { Text("Ombi $it", color = VantafynColors.Muted) }
        CapabilityList(state.capabilities)
    }
}

@Composable
private fun AddressWarning(value: String) {
    if (value.isBlank()) return
    val uri = runCatching { URI(if (value.contains("://")) value else "https://$value") }.getOrNull() ?: return
    val host = uri.host.orEmpty()
    val scheme = uri.scheme.orEmpty().lowercase()
    val warning = when {
        scheme == "http" && host.isPrivateHost() -> "This looks like a local address. It may only work on your home network."
        scheme == "http" -> "For remote access, HTTPS is recommended."
        else -> null
    }
    warning?.let { MessageCard(it) }
}

@Composable
private fun SecurityNotes() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Vantafyn stores your Ombi key securely on this device using Android encrypted storage.", color = VantafynColors.Muted)
        Text("Your Ombi key is only used to connect to your Ombi server. Vantafyn does not log Ombi credentials.", color = VantafynColors.Muted)
    }
}

@Composable
private fun WizardButtons(onBack: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VantafynButton("Back", onClick = onBack, modifier = Modifier.weight(1f))
        VantafynButton("Continue", onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AccessOption(label: String, mode: OmbiAccessMode, selected: OmbiAccessMode, onSelect: (OmbiAccessMode) -> Unit) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) },
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeChip(if (selected == mode) "Selected" else "Choose", selected == mode) { onSelect(mode) }
            Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IdentityModeOption(
    title: String,
    subtitle: String,
    mode: OmbiIdentityMode,
    selected: OmbiIdentityMode,
    onSelect: (OmbiIdentityMode) -> Unit,
) {
    VantafynGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) },
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeChip(if (selected == mode) "Selected" else "Choose", selected == mode) { onSelect(mode) }
                Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            }
            Text(subtitle, color = VantafynColors.Muted)
        }
    }
}

@Composable
private fun AccessRequestsSection(requests: List<OmbiAccessRequest>, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Access Requests", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            val visible = requests.filter { it.status != OmbiAccessRequestStatus.Dismissed }
            if (visible.isEmpty()) {
                Text("No Ombi access requests on this device/server setup.", color = VantafynColors.Muted)
            } else {
                visible.forEach { request ->
                    VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(request.jellyfinUserName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                            Text("Suggested Ombi username: ${request.suggestedOmbiUserName}", color = VantafynColors.Muted)
                            Text("${request.status.label} · ${DateFormat.getDateTimeInstance().format(Date(request.requestedAt))}", color = VantafynColors.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VantafynButton("Account created", onClick = { viewModel.markAccessRequestAccountCreated(request.jellyfinUserId) }, modifier = Modifier.weight(1f))
                                VantafynButton("Linked", onClick = { viewModel.markAccessRequestLinked(request.jellyfinUserId) }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VantafynButton("Dismiss", onClick = { viewModel.dismissAccessRequest(request.jellyfinUserId) }, modifier = Modifier.weight(1f))
                                VantafynButton("Copy name", onClick = {}, enabled = false, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMappingsSection(mappings: List<OmbiUserMapping>, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("User Mappings", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (mappings.isEmpty()) {
                Text("Mappings appear after a user requests access or an admin marks an account state.", color = VantafynColors.Muted)
            } else {
                mappings.forEach { mapping ->
                    VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(mapping.jellyfinUserName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                                Text(mapping.ombiUserName ?: "No Ombi username", color = VantafynColors.Muted)
                                Text(mapping.state.label, color = VantafynColors.Muted)
                            }
                            TextButton(onClick = { viewModel.clearMapping(mapping.jellyfinUserId) }) { Text("Clear") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) VantafynColors.Primary.copy(alpha = 0.34f) else VantafynColors.SurfaceHigh.copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusChip(status: MediaRequestStatus) {
    TypeChip(status.label, selected = status != MediaRequestStatus.NotRequested, onClick = {})
}

@Composable
private fun StepBadge(text: String) {
    Text(text, color = VantafynColors.Primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Poster(url: String?, title: String, small: Boolean = false) {
    Box(
        modifier = Modifier
            .size(width = if (small) 42.dp else 64.dp, height = if (small) 58.dp else 88.dp)
            .clip(RoundedCornerShape(if (small) 10.dp else 14.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(title.take(1), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    VantafynGlassCard(cornerRadius = 20.dp) {
        Text(message, color = VantafynColors.Ink)
    }
}

private val OmbiSetupStep.stepTitle: String
    get() = when (this) {
        OmbiSetupStep.Connect -> "Step 1 of 5"
        OmbiSetupStep.Authentication -> "Step 2 of 5"
        OmbiSetupStep.Test -> "Step 3 of 5"
        OmbiSetupStep.Access -> "Step 4 of 5"
        OmbiSetupStep.Finish -> "Step 5 of 5"
    }

private val OmbiAccessMode.label: String
    get() = when (this) {
        OmbiAccessMode.Disabled -> "Disabled"
        OmbiAccessMode.AdminsOnly -> "Admins only"
        OmbiAccessMode.AllUsers -> "All users"
    }

private val OmbiIdentityMode.label: String
    get() = when (this) {
        OmbiIdentityMode.SharedApiKey -> "Shared request account"
        OmbiIdentityMode.PerUserAccount -> "Each user links Ombi account"
    }

private val OmbiAccessRequestStatus.label: String
    get() = when (this) {
        OmbiAccessRequestStatus.Pending -> "Pending"
        OmbiAccessRequestStatus.Seen -> "Seen"
        OmbiAccessRequestStatus.AccountCreated -> "Account created"
        OmbiAccessRequestStatus.Linked -> "Linked"
        OmbiAccessRequestStatus.Dismissed -> "Dismissed"
    }

private val OmbiLinkedAccountState.label: String
    get() = when (this) {
        OmbiLinkedAccountState.NotLinked -> "Not linked"
        OmbiLinkedAccountState.AccessRequested -> "Access requested"
        OmbiLinkedAccountState.AccountCreated -> "Account created"
        OmbiLinkedAccountState.Linked -> "Linked"
        OmbiLinkedAccountState.Disabled -> "Disabled"
    }

private val RequestSearchFilter.label: String
    get() = when (this) {
        RequestSearchFilter.All -> "All"
        RequestSearchFilter.Movies -> "Movies"
        RequestSearchFilter.TvShows -> "TV Shows"
    }

private val MediaRequestType.label: String get() = if (this == MediaRequestType.Movie) "Movie" else "TV"
private val MediaRequestStatus.label: String get() = if (this == MediaRequestStatus.Pending) "Requested" else name.replaceFirstChar(Char::titlecase)
private val MediaRequestStatus.actionLabel: String
    get() = when (this) {
        MediaRequestStatus.NotRequested -> "Request"
        MediaRequestStatus.Available -> "Available"
        MediaRequestStatus.Pending -> "Requested"
        MediaRequestStatus.Approved -> "Approved"
        MediaRequestStatus.Denied -> "Denied"
        MediaRequestStatus.Processing -> "Processing"
        MediaRequestStatus.Unknown -> "Status"
    }

private fun String.isPrivateHost(): Boolean {
    if (endsWith(".local")) return true
    val parts = split('.').mapNotNull { it.toIntOrNull() }
    return parts.size == 4 && (
        parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31)
        )
}
