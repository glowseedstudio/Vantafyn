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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import dev.vantafyn.core.ombi.OmbiDiscoverRail
import dev.vantafyn.core.ombi.OmbiIdentityMode
import dev.vantafyn.core.ombi.OmbiLinkedAccountState
import dev.vantafyn.core.ombi.OmbiTvRequestSelection
import dev.vantafyn.core.ombi.OmbiUserMapping
import dev.vantafyn.core.ombi.RequestMediaDetail
import dev.vantafyn.core.ombi.RequestMediaSummary
import dev.vantafyn.core.ombi.RequestMediaType
import dev.vantafyn.core.ombi.RequestState
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
import java.util.UUID

@Composable
fun RequestsScreen(
    session: JellyfinSession?,
    onOpenMedia: (UUID) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RequestsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(session?.profileId) {
        viewModel.bindSession(session)
    }
    when (state.mode) {
        RequestsScreenMode.Setup -> if (state.isJellyfinAdmin) OmbiSetupWizard(state, viewModel, modifier) else RequestsContent(state, viewModel, onOpenMedia, modifier)
        RequestsScreenMode.Manage -> if (state.isJellyfinAdmin) OmbiManageScreen(state, viewModel, modifier) else RequestsContent(state, viewModel, onOpenMedia, modifier)
        RequestsScreenMode.Health -> if (state.isJellyfinAdmin) OmbiSetupHealthScreen(state, viewModel, modifier) else RequestsContent(state, viewModel, onOpenMedia, modifier)
        RequestsScreenMode.Requests -> RequestsContent(state, viewModel, onOpenMedia, modifier)
    }
}

@Composable
private fun RequestsContent(state: RequestsUiState, viewModel: RequestsViewModel, onOpenMedia: (UUID) -> Unit, modifier: Modifier) {
    state.selectedRequestItem?.let {
        RequestDetailScreen(item = it, state = state, viewModel = viewModel, onOpenMedia = onOpenMedia, modifier = modifier)
        return
    }
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
        if (!state.config.isEnabledForAdmins || !state.canUseRequests) {
            item {
                if (state.isJellyfinAdmin) {
                    UnavailableState(
                        title = "Requests are disabled",
                        body = "Ombi is configured, but request access is currently disabled.",
                        admin = true,
                        onRetry = viewModel::testConnection,
                        onManage = viewModel::manageOmbi,
                    )
                } else {
                    UserUnavailableState(
                        title = "Requests aren't ready yet",
                        body = "Your server admin has not enabled Requests for this profile.",
                        action = "Check again",
                        onAction = viewModel::showRequests,
                    )
                }
            }
            return@LazyColumn
        }
        if (state.connectionStatus == "Connection failed") {
            item {
                if (state.isJellyfinAdmin) {
                    UnavailableState(
                        title = "Requests are temporarily unavailable",
                        body = state.message ?: state.config.lastFailureMessage ?: "Vantafyn could not reach Ombi.",
                        admin = true,
                        onRetry = viewModel::testConnection,
                        onManage = viewModel::manageOmbi,
                    )
                } else {
                    UserUnavailableState(
                        title = "Requests are temporarily unavailable",
                        body = "Couldn't reach Requests. Try again in a moment.",
                        action = "Check again",
                        onAction = viewModel::showRequests,
                    )
                }
            }
        }
        if (state.config.identityMode == OmbiIdentityMode.PerUserAccount && !state.canSearchAndRequest) {
            item { OmbiAccessNeededState(state, viewModel) }
            return@LazyColumn
        }
        item { RequestsHero(state, viewModel, onOpenMedia) }
        item { RequestsTabs(state.activeRequestsView, viewModel::selectRequestsView) }
        item { SearchCard(state, viewModel) }
        state.message?.let { item { MessageCard(it) } }
        when (state.activeRequestsView) {
            RequestsView.Discover -> {
                if (state.isLoadingDiscovery) item { DiscoverySkeleton() }
                state.discoveryError?.let {
                    item { InlineRetryCard(it, "Retry discovery", viewModel::loadDiscovery) }
                }
                if (!state.isLoadingDiscovery && state.discoveryRails.isEmpty() && state.discoveryError == null) {
                    item { MessageCard("Search for a movie or series to start building your Requests queue.") }
                }
                items(state.discoveryRails, key = { it.kind.name }) { rail ->
                    DiscoveryRail(rail = rail, state = state, onOpen = viewModel::openRequestItem, onOpenAvailable = { viewModel.openAvailableItem(it, onOpenMedia) })
                }
            }
            RequestsView.Search -> {
                if (state.isSearching) item { VantafynLoadingIndicator("Searching Ombi") }
                if (!state.isSearching && state.searchResults.isEmpty() && state.query.length > 1) {
                    item { MessageCard("No matching request results yet.") }
                }
                items(state.searchResults, key = { "${it.mediaType}-${it.externalId}" }) { item ->
                    RequestPosterListCard(
                        item = item,
                        loading = state.activeRequestId == item.externalId,
                        availableMatch = state.availabilityMatches[item.availabilityKey()],
                        onOpen = { viewModel.openRequestItem(item) },
                        onOpenAvailable = { viewModel.openAvailableItem(item, onOpenMedia) },
                        onRequest = { viewModel.request(item) },
                    )
                }
            }
            RequestsView.MyRequests -> {
                item {
                    RequestHistory(state.requests, state.isJellyfinAdmin, state.isLoadingRequests, onRefresh = viewModel::loadRequests)
                }
            }
        }
    }
}

@Composable
private fun RequestsHero(state: RequestsUiState, viewModel: RequestsViewModel, onOpenMedia: (UUID) -> Unit) {
    val hero = state.discoveryRails.firstOrNull { it.items.isNotEmpty() }?.items?.firstOrNull()
    val heroAvailable = hero?.let { state.availabilityMatches[it.availabilityKey()] }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.52f)),
    ) {
        if (hero?.backdropUrl != null || hero?.posterUrl != null) {
            AsyncImage(
                model = hero.backdropUrl ?: hero.posterUrl,
                contentDescription = hero.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.46f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TypeChip("Native Requests", selected = true, onClick = {})
            Text(
                hero?.title ?: "Request movies and series",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                hero?.overview ?: "Search Ombi, track your requests, and open available titles back in Vantafyn.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VantafynButton(
                    if (heroAvailable != null) "Watch now" else hero?.primaryActionLabel() ?: "Search",
                    onClick = {
                        when {
                            hero != null && heroAvailable != null -> viewModel.openAvailableItem(hero, onOpenMedia)
                            hero != null -> viewModel.openRequestItem(hero)
                            else -> viewModel.selectRequestsView(RequestsView.Search)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                VantafynButton("Refresh", onClick = viewModel::loadDiscovery, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RequestsTabs(selected: RequestsView, onSelect: (RequestsView) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(RequestsView.entries.toList()) { tab ->
            TypeChip(tab.label, selected == tab) { onSelect(tab) }
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
        item { ReadyForRequestsChecklist(state, viewModel) }
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
                        subtitle = "Users sign in with their own Ombi account. Passwords are never stored; Vantafyn keeps a secure session token per Jellyfin profile.",
                        mode = OmbiIdentityMode.PerUserAccount,
                        selected = state.config.identityMode,
                        onSelect = viewModel::onIdentityModeChanged,
                    )
                    if (state.pendingAccessRequestCount > 0) {
                        MessageCard("${state.pendingAccessRequestCount} Ombi access request${if (state.pendingAccessRequestCount == 1) "" else "s"} pending.")
                    }
                    VantafynButton(if (state.isTesting) "Testing" else "Test connection", onClick = viewModel::testConnection, enabled = !state.isTesting && state.isConfigured, modifier = Modifier.fillMaxWidth())
                    VantafynButton(
                        if (state.isRefreshingAvailability) "Refreshing availability" else "Refresh availability index",
                        onClick = { viewModel.refreshAvailabilityIndex() },
                        enabled = !state.isRefreshingAvailability,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.availabilityIndex?.let {
                        Text("Indexed ${it.moviesCount} movies and ${it.seriesCount} series from ${it.sourceServerName ?: "Jellyfin"}.", color = VantafynColors.Muted)
                        Text("Last refreshed: ${DateFormat.getDateTimeInstance().format(Date(it.lastBuiltAt))}", color = VantafynColors.Muted)
                    }
                    state.availabilityMessage?.let { Text(it, color = VantafynColors.Muted) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VantafynButton("Edit credentials", onClick = viewModel::startSetup, modifier = Modifier.weight(1f))
                        VantafynButton("Setup Health", onClick = viewModel::showSetupHealth, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
private fun OmbiSetupHealthScreen(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item { Header("Setup Health", "Admin checks for Requests readiness.", action = "Ombi", onAction = viewModel::manageOmbi) }
        item {
            HealthCard(
                title = "Ombi server",
                status = state.connectionStatus,
                body = state.config.baseUrl.ifBlank { "No Ombi server configured" },
                fix = urlHealthHint(state.config.baseUrl),
            )
        }
        item {
            HealthCard(
                title = "Admin API key",
                status = if (state.hasApiKey && state.connectionStatus != "Connection failed") "Valid or ready" else if (state.hasApiKey) "Needs test" else "Missing",
                body = "Used for admin setup and shared request mode. The key is never shown.",
                fix = if (state.hasApiKey) "Run Test connection after changing Ombi settings." else "Paste an Ombi API key in setup.",
            )
        }
        item {
            HealthCard(
                title = "Ombi media server connection",
                status = "Not verified",
                body = "Ombi must be connected to Jellyfin for accurate availability checks.",
                fix = "Vantafyn verifies availability directly against the active Jellyfin server. Ombi-side media server settings are not exposed by the confirmed API yet.",
            )
        }
        item {
            HealthCard(
                title = "Request engines",
                status = "Not verified by API",
                body = "Movie and TV request capabilities are inferred from working Ombi search/request endpoints.",
                fix = "Check Radarr and Sonarr in Ombi if requests fail.",
            )
        }
        item {
            HealthCard(
                title = "User identity mode",
                status = state.config.identityMode.label,
                body = if (state.config.identityMode == OmbiIdentityMode.SharedApiKey) {
                    "Requests may appear under the configured Ombi account."
                } else {
                    "Per-user Ombi login is recommended if you want requests tied to each person."
                },
                fix = "Create Ombi accounts for users, then they can link them in Vantafyn.",
            )
        }
        item {
            HealthCard(
                title = "Jellyfin availability index",
                status = state.availabilityIndex?.let { "Ready" } ?: "Not built",
                body = state.availabilityIndex?.let { "Indexed ${it.moviesCount} movies and ${it.seriesCount} series." }
                    ?: "Vantafyn can match Ombi items to Jellyfin by provider IDs after the index is built.",
                fix = "Use Refresh availability index from Ombi management.",
            )
        }
    }
}

@Composable
private fun ReadyForRequestsChecklist(state: RequestsUiState, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ready for Requests", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            ChecklistRow("Ombi reachable", state.connectionStatus == "Connected", state.connectionStatus)
            ChecklistRow("API key valid", state.hasApiKey && state.connectionStatus != "Connection failed", if (state.hasApiKey) "Saved securely" else "Missing")
            ChecklistRow("Requests enabled", state.config.accessMode != OmbiAccessMode.Disabled, state.config.accessMode.label)
            ChecklistRow("Movie search works", state.config.capabilities.movieSearch, "Discovery depends on this")
            ChecklistRow("TV search works", state.config.capabilities.tvSearch, "Series discovery depends on this")
            ChecklistRow("Movie requests available", state.config.capabilities.movieRequest, "Endpoint support")
            ChecklistRow("TV requests available", state.config.capabilities.tvRequest, "Endpoint support")
            ChecklistRow("Recently requested/available", state.config.capabilities.userRequestListing, "Queue/status endpoints")
            ChecklistRow("Jellyfin availability", state.availabilityIndex != null, state.availabilityIndex?.let { "${it.moviesCount + it.seriesCount} indexed" } ?: "Not verified yet")
            ChecklistRow("User identity mode", true, state.config.identityMode.label)
            VantafynButton("Open Setup Health", onClick = viewModel::showSetupHealth, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ChecklistRow(label: String, ok: Boolean, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TypeChip(if (ok) "Ready" else "Check", selected = ok, onClick = {})
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            Text(detail, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HealthCard(title: String, status: String, body: String, fix: String) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeChip(status, selected = status.contains("Ready", true) || status.contains("Connected", true) || status.contains("Valid", true), onClick = {})
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(body, color = VantafynColors.Muted)
            Text("Fix this: $fix", color = VantafynColors.Muted)
        }
    }
}

@Composable
private fun OmbiAccessNeededState(state: RequestsUiState, viewModel: RequestsViewModel) {
    if (state.canShowOmbiLogin) {
        OmbiLoginState(state, viewModel)
    } else {
        UserUnavailableState(
            title = if (state.hasRequestedAccess) "Access requested" else "Link Ombi to request",
            body = if (state.hasRequestedAccess) {
                "Your server admin has been notified. Once your Ombi account is ready, you can link it here."
            } else {
                "Use the Ombi account your server admin created for you, or request access if you do not have one yet."
            },
            action = if (state.hasRequestedAccess) "Check again" else "Request access",
            onAction = if (state.hasRequestedAccess) viewModel::showRequests else viewModel::requestOmbiAccess,
        )
    }
}

@Composable
private fun OmbiLoginState(state: RequestsUiState, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeChip(
                    when {
                        state.isValidatingOmbiSession -> "Checking session"
                        state.currentUserMapping?.state == OmbiLinkedAccountState.TokenExpired -> "Sign in again"
                        else -> "Ombi profile ready"
                    },
                    selected = true,
                    onClick = {},
                )
                state.ombiUserSession?.let {
                    Text("Linked as ${it.bestName}", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("Sign in to Requests", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Sign in with the Ombi account your server admin created for you. Vantafyn remembers the secure session token on this device and never stores your password.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
            )
            VantafynTextField(
                value = state.ombiUsername.ifBlank { state.currentUserMapping?.ombiUserName.orEmpty() },
                onValueChange = viewModel::onOmbiUsernameChanged,
                label = "Ombi username or email",
                enabled = !state.isLinkingOmbi,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            VantafynTextField(
                value = state.ombiPassword,
                onValueChange = viewModel::onOmbiPasswordChanged,
                label = "Ombi password",
                enabled = !state.isLinkingOmbi,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
            VantafynButton(
                if (state.isLinkingOmbi) "Linking" else "Link account",
                onClick = viewModel::loginOmbiAccount,
                enabled = !state.isLinkingOmbi &&
                    (state.ombiUsername.isNotBlank() || !state.currentUserMapping?.ombiUserName.isNullOrBlank()) &&
                    state.ombiPassword.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!state.hasRequestedAccess) {
                TextButton(onClick = viewModel::requestOmbiAccess) { Text("Request access") }
            }
            if (state.ombiUserSession != null) {
                TextButton(onClick = viewModel::unlinkOmbiAccount) { Text("Forget Ombi session") }
            }
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
                    if (admin) "Requests are ready when you are" else "Requests aren't ready yet",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (admin) "Connect Ombi to let this home request movies and series from Vantafyn."
                    else "Your server admin has not finished setting up Requests.",
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
private fun UserUnavailableState(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TypeChip("Requests", selected = true, onClick = {})
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(body, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
            action?.let {
                VantafynButton(it, onClick = onAction, modifier = Modifier.fillMaxWidth())
            }
            Text("Requests are managed by your server admin.", color = VantafynColors.Muted)
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
private fun DiscoveryRail(
    rail: OmbiDiscoverRail,
    state: RequestsUiState,
    onOpen: (RequestMediaSummary) -> Unit,
    onOpenAvailable: (RequestMediaSummary) -> Unit,
) {
    if (rail.items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(rail.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rail.items, key = { "${rail.kind}-${it.mediaType}-${it.externalId}-${it.requestId.orEmpty()}" }) { item ->
                RequestPosterCard(
                    item = item,
                    availableMatch = state.availabilityMatches[item.availabilityKey()],
                    onClick = { if (state.availabilityMatches[item.availabilityKey()] != null) onOpenAvailable(item) else onOpen(item) },
                )
            }
        }
    }
}

@Composable
private fun RequestPosterCard(item: RequestMediaSummary, availableMatch: Any?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 132.dp, height = 238.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.64f)),
        ) {
            if (!item.posterUrl.isNullOrBlank()) {
                AsyncImage(model = item.posterUrl, contentDescription = item.accessibilityLabel(), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(item.title.take(1), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(if (availableMatch != null) "Available" else item.state.shortLabel, color = VantafynColors.Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Text(item.title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(item.mediaType.label, item.year?.toString()).joinToString(" · "), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RequestPosterListCard(
    item: RequestMediaSummary,
    loading: Boolean,
    availableMatch: Any?,
    onOpen: () -> Unit,
    onOpenAvailable: () -> Unit,
    onRequest: () -> Unit,
) {
    VantafynGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpen)) {
            Poster(item.posterUrl, item.title)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(item.mediaType.label, item.year?.toString(), item.state.label).joinToString(" · "), color = VantafynColors.Muted)
                item.overview?.let { Text(it, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            VantafynButton(
                if (availableMatch != null) "Open in Vantafyn" else if (loading) "Sending" else item.primaryActionLabel(),
                onClick = if (availableMatch != null) onOpenAvailable else onRequest,
                enabled = availableMatch != null || (!loading && item.state == RequestState.NotRequested),
            )
        }
    }
}

@Composable
private fun RequestDetailScreen(item: RequestMediaSummary, state: RequestsUiState, viewModel: RequestsViewModel, onOpenMedia: (UUID) -> Unit, modifier: Modifier) {
    val detail = state.selectedRequestDetail
    val display = detail?.summary ?: item
    val availableMatch = state.availabilityMatches[display.availabilityKey()]
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .background(VantafynColors.Surface.copy(alpha = 0.2f)),
            ) {
                if (!display.backdropUrl.isNullOrBlank() || !display.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = display.backdropUrl ?: display.posterUrl,
                        contentDescription = display.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.9f)))))
                TextButton(
                    onClick = viewModel::closeRequestItem,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                ) { Text("‹", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium) }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TypeChip(display.state.label, selected = true, onClick = {})
                    Text(display.title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(
                            display.mediaType.label,
                            display.year?.toString(),
                            detail?.runtimeMinutes?.let { "${it}m" },
                            detail?.certification,
                            detail?.network,
                            display.rating?.let { "★ %.1f".format(it) },
                        ).joinToString(" · "),
                        color = VantafynColors.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (display.mediaType == RequestMediaType.Series && display.state == RequestState.NotRequested && availableMatch == null) {
                        TvRequestOptions(state.tvRequestSelection, viewModel::onTvRequestSelectionChanged)
                    }
                    VantafynButton(
                        if (availableMatch != null) "Watch now" else display.primaryActionLabel(),
                        onClick = {
                            if (availableMatch != null) viewModel.openAvailableItem(display, onOpenMedia)
                            else if (display.state == RequestState.NotRequested) viewModel.request(display)
                        },
                        enabled = availableMatch != null || display.state == RequestState.NotRequested,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.isLoadingRequestDetail) {
                    VantafynLoadingIndicator("Loading details")
                }
                state.requestDetailError?.let {
                    InlineRetryCard(it, "Retry details") { viewModel.openRequestItem(display) }
                }
                detail?.tagline?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = VantafynColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (detail?.genres?.isNotEmpty() == true) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(detail.genres.take(6)) { genre -> TypeChip(genre, selected = true, onClick = {}) }
                    }
                }
                display.overview?.let {
                    Text("Overview", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                }
                VantafynGlassPanel(cornerRadius = 22.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Request status", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text(display.state.explainer, color = VantafynColors.Muted)
                        if (display.mediaType == RequestMediaType.Series) {
                            Text("Use the series request action for the whole show. Season-level controls are shown below when Ombi returns a season graph for this title.", color = VantafynColors.Muted)
                        }
                        if (availableMatch != null) {
                            Text("Available in ${availableMatch.serverName ?: "Jellyfin"} as ${availableMatch.title}.", color = VantafynColors.Muted)
                        } else if (display.state == RequestState.Available) {
                            Text("Available according to Ombi. Vantafyn could not verify a Jellyfin provider-ID match yet.", color = VantafynColors.Muted)
                        }
                    }
                }
                detail?.let { RequestDetailMetadata(it) }
                if (detail != null && detail.seasons.isNotEmpty()) {
                    SeriesSeasonSection(detail)
                }
                if (detail != null && detail.similar.isNotEmpty()) {
                    DiscoveryRail(
                        OmbiDiscoverRail(dev.vantafyn.core.ombi.OmbiDiscoverRailKind.SearchResults, "More like this", detail.similar),
                        state = state,
                        onOpen = viewModel::openRequestItem,
                        onOpenAvailable = { viewModel.openAvailableItem(it, onOpenMedia) },
                    )
                }
                state.message?.let { MessageCard(it) }
            }
        }
    }
}

@Composable
private fun TvRequestOptions(selected: OmbiTvRequestSelection, onSelect: (OmbiTvRequestSelection) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(OmbiTvRequestSelection.entries.toList()) { option ->
                TypeChip(option.label, selected == option) { onSelect(option) }
            }
        }
        Text(
            "Season and episode-specific requests are hidden until Ombi's episode request body is confirmed. These options use the confirmed TV request flags.",
            color = VantafynColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RequestDetailMetadata(detail: RequestMediaDetail) {
    VantafynGlassPanel(cornerRadius = 22.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Details", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(
                    detail.summary.mediaType.label,
                    detail.summary.year?.toString(),
                    detail.runtimeMinutes?.let { "${it} minutes" },
                    detail.certification,
                    detail.network,
                ).joinToString(" · ").ifBlank { "Details are limited from Ombi for this title." },
                color = VantafynColors.Muted,
            )
        }
    }
}

@Composable
private fun SeriesSeasonSection(detail: RequestMediaDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Seasons", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        detail.seasons.forEach { season ->
            VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Season ${season.seasonNumber}", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TypeChip(season.state.shortLabel, selected = season.state != RequestState.NotRequested, onClick = {})
                    }
                    season.overview?.let { Text(it, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    if (season.episodes.isNotEmpty()) {
                        Text(
                            season.episodes.take(4).joinToString(" · ") { episode ->
                                "E${episode.episodeNumber} ${episode.state.shortLabel}"
                            },
                            color = VantafynColors.Muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverySkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Loading discovery", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) {
                Box(
                    modifier = Modifier
                        .size(width = 132.dp, height = 190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.52f)),
                )
            }
        }
    }
}

@Composable
private fun InlineRetryCard(message: String, action: String, onRetry: () -> Unit) {
    VantafynGlassCard(cornerRadius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message, color = VantafynColors.Ink)
            VantafynButton(action, onClick = onRetry, modifier = Modifier.fillMaxWidth())
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
            OmbiIdentityMode.PerUserAccount -> "What is Requests? Requests lets you ask your server admin to add movies or shows. Your requests are linked to your Ombi account."
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
        host == "localhost" || host == "127.0.0.1" || host == "::1" -> "localhost only works on the device running Ombi. Phones and TVs need a server IP or domain."
        scheme == "http" && host.isPrivateHost() -> "This looks like a local address. It may only work on your home network."
        scheme == "http" -> "For remote access, HTTPS is recommended."
        else -> null
    }
    warning?.let { MessageCard(it) }
}

private fun urlHealthHint(value: String): String {
    if (value.isBlank()) return "Enter the Ombi URL users can reach from phones and TVs."
    val uri = runCatching { URI(if (value.contains("://")) value else "https://$value") }.getOrNull()
        ?: return "Enter a valid Ombi URL."
    val host = uri.host.orEmpty()
    val scheme = uri.scheme.orEmpty().lowercase()
    return when {
        host == "localhost" || host == "127.0.0.1" || host == "::1" -> "localhost only works on the device running Ombi. Phones and TVs need your server IP or domain."
        host.isPrivateHost() -> "This may only work at home unless you use VPN or remote access."
        scheme == "http" -> "Use HTTPS for remote access when possible."
        else -> "This URL looks suitable for phone and TV access if your network allows it."
    }
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

private val OmbiTvRequestSelection.label: String
    get() = when (this) {
        OmbiTvRequestSelection.AllSeasons -> "All seasons"
        OmbiTvRequestSelection.FirstSeason -> "First season"
        OmbiTvRequestSelection.LatestSeason -> "Latest season"
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
        OmbiLinkedAccountState.TokenExpired -> "Sign in again"
        OmbiLinkedAccountState.Disabled -> "Disabled"
    }

private val RequestSearchFilter.label: String
    get() = when (this) {
        RequestSearchFilter.All -> "All"
        RequestSearchFilter.Movies -> "Movies"
        RequestSearchFilter.TvShows -> "TV Shows"
    }

private val RequestsView.label: String
    get() = when (this) {
        RequestsView.Discover -> "Discover"
        RequestsView.Search -> "Search"
        RequestsView.MyRequests -> "My Requests"
    }

private val RequestMediaType.label: String
    get() = when (this) {
        RequestMediaType.Movie -> "Movie"
        RequestMediaType.Series -> "Series"
    }

private val RequestState.label: String
    get() = when (this) {
        RequestState.NotRequested -> "Not requested"
        RequestState.PendingApproval -> "Awaiting approval"
        RequestState.Approved -> "Approved and queued"
        RequestState.Processing -> "Being added"
        RequestState.PartiallyAvailable -> "Some seasons available"
        RequestState.Available -> "Available in Jellyfin"
        RequestState.Declined -> "Request declined"
        RequestState.Failed -> "Couldn’t complete request"
        RequestState.Unknown -> "Status unavailable"
    }

private val RequestState.shortLabel: String
    get() = when (this) {
        RequestState.NotRequested -> "Request"
        RequestState.PendingApproval -> "Awaiting"
        RequestState.Approved -> "Approved"
        RequestState.Processing -> "Adding"
        RequestState.PartiallyAvailable -> "Partial"
        RequestState.Available -> "Available"
        RequestState.Declined -> "Declined"
        RequestState.Failed -> "Failed"
        RequestState.Unknown -> "Status"
    }

private val RequestState.explainer: String
    get() = when (this) {
        RequestState.NotRequested -> "This title can be requested from Ombi."
        RequestState.PendingApproval -> "The request has been sent and is waiting for approval."
        RequestState.Approved -> "The request is approved and waiting to be added."
        RequestState.Processing -> "Ombi says this title is being added."
        RequestState.PartiallyAvailable -> "Some of this series is already available in Jellyfin."
        RequestState.Available -> "Ombi reports this title is available in Jellyfin."
        RequestState.Declined -> "This request was declined."
        RequestState.Failed -> "Ombi reported a problem with this request."
        RequestState.Unknown -> "Ombi returned a status Vantafyn does not fully understand yet."
    }

private fun RequestMediaSummary.primaryActionLabel(): String =
    when (state) {
        RequestState.NotRequested -> if (mediaType == RequestMediaType.Series) "Request series" else "Request movie"
        RequestState.PendingApproval -> "Awaiting approval"
        RequestState.Approved -> "Approved"
        RequestState.Processing -> "Being added"
        RequestState.PartiallyAvailable -> "View seasons"
        RequestState.Available -> "Open in Jellyfin"
        RequestState.Declined -> "Request declined"
        RequestState.Failed -> "Needs attention"
        RequestState.Unknown -> "View status"
    }

private fun RequestMediaSummary.accessibilityLabel(): String =
    "${title}, ${mediaType.label}, ${state.label}"

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
