package dev.vantafyn.feature.requests

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import dev.vantafyn.core.ombi.OmbiUserMatchState
import dev.vantafyn.core.ombi.OmbiUserMapping
import dev.vantafyn.core.ombi.RequestMediaDetail
import dev.vantafyn.core.ombi.RequestMediaSummary
import dev.vantafyn.core.ombi.RequestMediaType
import dev.vantafyn.core.ombi.RequestState
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynGlassChip
import dev.vantafyn.core.ui.VantafynGlassPanel
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import java.net.URI
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val RequestsSetupCinematicEasing = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

@Composable
fun RequestsScreen(
    session: JellyfinSession?,
    onOpenMedia: (UUID) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RequestsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
    if (!state.isConfigured) {
        UnconfiguredRequestsScreen(
            admin = state.isJellyfinAdmin,
            onSetup = viewModel::startSetup,
            modifier = modifier,
        )
        return
    }
    var revealActive by remember(state.currentUserId) { mutableStateOf(true) }
    LaunchedEffect(state.currentUserId) {
        revealActive = true
        delay(1_100L)
        revealActive = false
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 164.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!state.config.isEnabledForAdmins || !state.canUseRequests) {
            item {
                RequestContentReveal(index = 0, animate = revealActive) {
                    RequestInset {
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
                }
            }
            return@LazyColumn
        }
        if (state.connectionStatus == "Connection failed") {
            item {
                RequestContentReveal(index = 0, animate = revealActive) {
                    RequestInset {
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
            }
        }
        if (state.config.identityMode == OmbiIdentityMode.PerUserAccount && !state.canSearchAndRequest) {
            item { RequestContentReveal(index = 0, animate = revealActive) { RequestInset { OmbiAccessNeededState(state, viewModel) } } }
            return@LazyColumn
        }
        item { RequestContentReveal(index = 0, animate = revealActive) { RequestsHero(state, viewModel, onOpenMedia) } }
        item { RequestContentReveal(index = 1, animate = revealActive) { RequestInset { RequestsToolbar(state, viewModel) } } }
        state.message?.let { item { RequestContentReveal(index = 2, animate = revealActive) { RequestInset { MessageCard(it) } } } }
        when (state.activeRequestsView) {
            RequestsView.Discover -> {
                if (state.isLoadingDiscovery) item { RequestContentReveal(index = 3, animate = revealActive) { RequestInset { DiscoverySkeleton() } } }
                state.discoveryError?.let {
                    item { RequestContentReveal(index = 3, animate = revealActive) { RequestInset { InlineRetryCard(it, "Retry discovery", viewModel::loadDiscovery) } } }
                }
                if (!state.isLoadingDiscovery && state.discoveryRails.isEmpty() && state.discoveryError == null) {
                    item { RequestContentReveal(index = 3, animate = revealActive) { RequestInset { MessageCard("Search for a movie or series to start building your Requests queue.") } } }
                }
                itemsIndexed(state.discoveryRails, key = { _, rail -> rail.kind.name }) { index, rail ->
                    RequestContentReveal(index = index + 3, animate = revealActive) {
                        RequestInset {
                            DiscoveryRail(rail = rail, state = state, onOpen = viewModel::openRequestItem, onOpenAvailable = { viewModel.openAvailableItem(it, onOpenMedia) })
                        }
                    }
                }
            }
            RequestsView.Search -> {
                if (state.isSearching) item { RequestContentReveal(index = 3, animate = revealActive) { RequestInset { VantafynLoadingIndicator("Searching Ombi") } } }
                if (!state.isSearching && state.searchResults.isEmpty() && state.query.length > 1) {
                    item { RequestContentReveal(index = 3, animate = revealActive) { RequestInset { MessageCard("No matching request results yet.") } } }
                }
                itemsIndexed(
                    state.searchResults,
                    key = { index, item -> "search-${item.mediaType}-${item.externalId}-${item.movieDbId.orEmpty()}-${item.tvDbId.orEmpty()}-$index" },
                ) { index, item ->
                    RequestContentReveal(index = index + 3, animate = revealActive) {
                        RequestInset {
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
                }
            }
            RequestsView.MyRequests -> {
                item {
                    RequestContentReveal(index = 3, animate = revealActive) {
                        RequestInset {
                            RequestHistory(state.requests, state.isJellyfinAdmin, state.isLoadingRequests, onRefresh = viewModel::loadRequests)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestContentReveal(
    index: Int,
    animate: Boolean,
    content: @Composable () -> Unit,
) {
    if (!animate) {
        content()
        return
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index.coerceAtMost(8) * 78L).coerceAtMost(620L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 430, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 470, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 7 },
            ),
    ) {
        content()
    }
}

@Composable
private fun RequestInset(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
        content()
    }
}

@Composable
private fun RequestsHero(state: RequestsUiState, viewModel: RequestsViewModel, onOpenMedia: (UUID) -> Unit) {
    val heroItems = remember(state.discoveryRails) {
        state.discoveryRails
            .flatMap { it.items }
            .distinctBy { "${it.mediaType}-${it.externalId}-${it.movieDbId.orEmpty()}-${it.tvDbId.orEmpty()}-${it.title.lowercase()}" }
            .take(8)
    }
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(heroItems.map { it.externalId }, lifecycleOwner) {
        if (heroItems.size > 1) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(5_800)
                    val next = (listState.firstVisibleItemIndex + 1) % heroItems.size
                    listState.animateScrollToItem(next)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(264.dp)
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.52f)),
    ) {
        if (heroItems.isEmpty()) {
            RequestsHeroSlide(
                hero = null,
                state = state,
                viewModel = viewModel,
                onOpenMedia = onOpenMedia,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                items(heroItems, key = { "${it.mediaType}-${it.externalId}-${it.movieDbId.orEmpty()}-${it.tvDbId.orEmpty()}" }) { hero ->
                    RequestsHeroSlide(
                        hero = hero,
                        state = state,
                        viewModel = viewModel,
                        onOpenMedia = onOpenMedia,
                        modifier = Modifier.fillParentMaxWidth().height(264.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                heroItems.take(6).forEachIndexed { index, _ ->
                    val selected = index == listState.firstVisibleItemIndex.coerceAtMost(5)
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = if (selected) 0.74f else 0.32f)),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(74.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            VantafynColors.Graphite.copy(alpha = 0.72f),
                            VantafynColors.Graphite,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 16.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeChip("Requests", selected = true, onClick = {})
            if (state.isJellyfinAdmin && state.isConfigured) {
                TypeChip("Manage Ombi", selected = false, onClick = viewModel::manageOmbi)
            }
        }
    }
}

@Composable
private fun RequestsHeroSlide(
    hero: RequestMediaSummary?,
    state: RequestsUiState,
    viewModel: RequestsViewModel,
    onOpenMedia: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroAvailable = hero?.let { state.availabilityMatches[it.availabilityKey()] }
    Box(modifier = modifier.background(VantafynColors.SurfaceHigh.copy(alpha = 0.52f))) {
        val artwork = hero?.backdropUrl ?: hero?.posterUrl
        if (hero != null && artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = hero.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.80f), Color.Black.copy(alpha = 0.48f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.76f)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OmbiSetupWizard(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 42.dp,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            RequestsSetupHeader(
                title = "Connect Ombi",
                onBack = viewModel::previousSetupStep,
            )
        }
        item {
            VantafynGlassPanel(cornerRadius = 24.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepBadge(state.setupStep.stepTitle)
                    AnimatedContent(
                        targetState = state.setupStep,
                        transitionSpec = {
                            (
                                fadeIn(animationSpec = tween(durationMillis = 760, delayMillis = 90, easing = RequestsSetupCinematicEasing)) +
                                    slideInVertically(
                                        animationSpec = tween(durationMillis = 820, delayMillis = 90, easing = RequestsSetupCinematicEasing),
                                        initialOffsetY = { it / 16 },
                                    )
                                ).togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 360, easing = RequestsSetupCinematicEasing)),
                            ).using(SizeTransform(clip = false))
                        },
                        label = "ombiSetupStep",
                    ) { step ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            when (step) {
                                OmbiSetupStep.Connect -> ConnectStep(state, viewModel)
                                OmbiSetupStep.Authentication -> RequestModeStep(state, viewModel)
                                OmbiSetupStep.Test -> UserAccessStep(state, viewModel)
                                OmbiSetupStep.Access -> UserAccessStep(state, viewModel)
                                OmbiSetupStep.Finish -> FinishStep(state, viewModel)
                            }
                        }
                    }
                }
            }
        }
        state.message?.let { item { MessageCard(it) } }
    }
}

@Composable
private fun RequestsSetupHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(19.dp)) {
                val stroke = 2.35.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(11.5.dp.toPx(), 3.dp.toPx()),
                    end = Offset(5.dp.toPx(), 9.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(5.dp.toPx(), 9.dp.toPx()),
                    end = Offset(11.5.dp.toPx(), 15.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(5.5.dp.toPx(), 9.dp.toPx()),
                    end = Offset(16.dp.toPx(), 9.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
        Text(
            title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConnectStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Connect Ombi", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Vantafyn uses Ombi to request movies and shows from your server.", color = VantafynColors.Muted)
    VantafynTextField(
        value = state.baseUrl,
        onValueChange = viewModel::onBaseUrlChanged,
        label = "Ombi URL",
        placeholder = "https://requests.example.com",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        enabled = !state.isTesting,
    )
    VantafynTextField(
        value = state.apiKey,
        onValueChange = viewModel::onApiKeyChanged,
        label = if (state.hasApiKey) "Admin API key saved" else "Admin API key",
        enabled = !state.isTesting,
    )
    AddressWarning(state.baseUrl)
    VantafynButton(
        if (state.isTesting) "Testing" else "Test connection",
        onClick = viewModel::saveDraftAndTest,
        enabled = !state.isTesting && state.baseUrl.isNotBlank() && (state.apiKey.isNotBlank() || state.hasApiKey),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RequestModeStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Choose request mode", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text("Pick how requests are attributed in Ombi.", color = VantafynColors.Muted)
    IdentityModeOption(
        title = "Server request account",
        subtitle = "Everyone requests through the server’s configured Ombi connection. Simple setup.",
        mode = OmbiIdentityMode.SharedApiKey,
        selected = state.identityMode,
        onSelect = viewModel::onIdentityModeChanged,
    )
    IdentityModeOption(
        title = "Per-user Ombi login",
        subtitle = "Each person signs in with their own Ombi account. Best for families and shared servers.",
        mode = OmbiIdentityMode.PerUserAccount,
        selected = state.identityMode,
        onSelect = viewModel::onIdentityModeChanged,
    )
    WizardButtons(onBack = viewModel::previousSetupStep, onNext = viewModel::nextSetupStep)
}

@Composable
private fun UserAccessStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("User access", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    if (state.identityMode == OmbiIdentityMode.PerUserAccount) {
        Text("Users will sign in with the Ombi account you created for them.", color = VantafynColors.Muted)
        if (state.pendingAccessRequestCount > 0) {
            MessageCard("${state.pendingAccessRequestCount} access request${if (state.pendingAccessRequestCount == 1) "" else "s"} waiting.")
        }
    } else {
        Text("Requests will use the configured Ombi connection.", color = VantafynColors.Muted)
        Text("Users will not need their own Ombi login.", color = VantafynColors.Muted)
    }
    WizardButtons(onBack = viewModel::previousSetupStep, onNext = viewModel::nextSetupStep)
}

@Composable
private fun FinishStep(state: RequestsUiState, viewModel: RequestsViewModel) {
    Text("Requests are ready", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    SetupSummaryTile("Ombi connected", state.baseUrl.ifBlank { state.config.baseUrl })
    SetupSummaryTile("Request mode", state.identityMode.label)
    SetupSummaryTile("User access", if (state.identityMode == OmbiIdentityMode.PerUserAccount) "Users link their Ombi account" else "No user login needed")
    VantafynButton("Open Requests", onClick = viewModel::finishSetup, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = viewModel::previousSetupStep) { Text("Back") }
}

@Composable
private fun OmbiManageScreen(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 164.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            OmbiManageHero(state = state, viewModel = viewModel)
        }
        item {
            OmbiManageStatusSection(state = state, viewModel = viewModel)
        }
        item {
            OmbiManageRequestModeSection(state = state, viewModel = viewModel)
        }
        if (state.config.identityMode == OmbiIdentityMode.PerUserAccount) {
            item {
                OmbiManageUsersSummary(state)
            }
            item { AccessRequestsSection(state.accessRequests, viewModel) }
            item { UserMappingsSection(state.userMappings, viewModel) }
        }
        item {
            OmbiManageAdvancedSection(state = state, viewModel = viewModel)
        }
        state.message?.let { item { MessageCard(it) } }
    }
}

@Composable
private fun OmbiManageHero(state: RequestsUiState, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 26.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Manage Requests", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(state.config.baseUrl.ifBlank { "No Ombi server configured" }, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TypeChip("Requests", selected = true, onClick = viewModel::showRequests)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.055f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                ManageHeroInfoRow("Status", if (state.connectionStatus == "Connected") state.config.version?.let { "Connected · Ombi $it" } ?: "Connected" else state.connectionStatus)
                ManageHeroInfoRow("Mode", state.config.identityMode.label)
                ManageHeroInfoRow("Access", state.config.accessMode.label)
            }
        }
    }
}

@Composable
private fun ManageHeroInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = VantafynColors.Muted,
            modifier = Modifier.width(68.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value.ifBlank { "Not set" },
            color = VantafynColors.Ink,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OmbiManageStatusSection(state: RequestsUiState, viewModel: RequestsViewModel) {
    ManageSection(title = "Connection") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(state.config.baseUrl.ifBlank { "No Ombi server configured" }, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.config.version?.let { "Ombi $it" } ?: state.connectionStatus, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            VantafynToggleSwitch(
                checked = state.config.accessMode != OmbiAccessMode.Disabled,
                onCheckedChange = viewModel::setEnabled,
            )
        }
        state.config.lastSuccessfulConnectionAt?.let {
            ManageInfoRow("Last checked", DateFormat.getDateTimeInstance().format(Date(it)))
        }
        ManageInfoRow("Request mode", state.config.identityMode.label)
    }
}

@Composable
private fun VantafynToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (checked) {
                    Modifier.background(VantafynGradients.accentHorizontal())
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.12f))
                },
            )
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.94f)),
        )
    }
}

@Composable
private fun OmbiManageRequestModeSection(state: RequestsUiState, viewModel: RequestsViewModel) {
    ManageSection(title = "Request Mode") {
        ManageSelectableRow(
            title = "Server request account",
            subtitle = "Everyone requests through the server connection.",
            selected = state.config.identityMode == OmbiIdentityMode.SharedApiKey,
            onClick = { viewModel.onIdentityModeChanged(OmbiIdentityMode.SharedApiKey) },
        )
        ManageSelectableRow(
            title = "Per-user Ombi login",
            subtitle = "Each person signs in with their own Ombi account.",
            selected = state.config.identityMode == OmbiIdentityMode.PerUserAccount,
            onClick = { viewModel.onIdentityModeChanged(OmbiIdentityMode.PerUserAccount) },
        )
    }
}

@Composable
private fun OmbiManageUsersSummary(state: RequestsUiState) {
    ManageSection(title = "Users") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OmbiManageMetric("Linked", state.userMappings.count { it.state == OmbiLinkedAccountState.Linked }.toString(), Modifier.weight(1f))
            OmbiManageMetric("Pending", state.pendingAccessRequestCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun OmbiManageAdvancedSection(state: RequestsUiState, viewModel: RequestsViewModel) {
    ManageSection(title = "Advanced") {
        VantafynButton(if (state.isTesting) "Testing" else "Test connection", onClick = viewModel::testConnection, enabled = !state.isTesting && state.isConfigured, modifier = Modifier.fillMaxWidth())
        VantafynButton(
            if (state.isRefreshingAvailability) "Refreshing availability" else "Refresh availability index",
            onClick = { viewModel.refreshAvailabilityIndex() },
            enabled = !state.isRefreshingAvailability,
            modifier = Modifier.fillMaxWidth(),
        )
        state.availabilityIndex?.let {
            ManageInfoRow("Availability", "Indexed ${it.moviesCount} movies and ${it.seriesCount} series from ${it.sourceServerName ?: "Jellyfin"}.")
            ManageInfoRow("Last refreshed", DateFormat.getDateTimeInstance().format(Date(it.lastBuiltAt)))
        }
        state.availabilityMessage?.let { Text(it, color = VantafynColors.Muted) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            VantafynButton("Update key", onClick = viewModel::startSetup, modifier = Modifier.weight(1f))
            VantafynButton("Health", onClick = viewModel::showSetupHealth, modifier = Modifier.weight(1f))
        }
        VantafynButton("Remove integration", onClick = viewModel::resetIntegration, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OmbiSetupHealthScreen(state: RequestsUiState, viewModel: RequestsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
            bottom = 108.dp,
        ),
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
            title = if (state.hasRequestedAccess) "Access requested" else "Requests aren't ready yet",
            body = if (state.hasRequestedAccess) {
                "Your admin still needs to create or confirm your Ombi account."
            } else {
                "Your server admin has not finished setting up Requests for this profile."
            },
            action = if (state.hasRequestedAccess) "Check again" else null,
            onAction = viewModel::showRequests,
        )
    }
}

@Composable
private fun OmbiLoginState(state: RequestsUiState, viewModel: RequestsViewModel) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val matchFound = state.ombiUserMatch.state == OmbiUserMatchState.MatchFound
            TypeChip(if (matchFound) "Account found" else "Ombi login", selected = true, onClick = {})
            Text("Link Ombi", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                if (matchFound) "We found an Ombi account for you. Sign in to link it."
                else if (state.ombiUserMatch.state == OmbiUserMatchState.NoMatchFound) "No Ombi account was found for this profile. You can still sign in manually."
                else "Use the Ombi account your server admin created for you.",
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
            if (state.ombiUserMatch.state == OmbiUserMatchState.NoMatchFound) {
                VantafynButton("Request access", onClick = viewModel::requestOmbiAccess, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = viewModel::checkOmbiUserMatch) { Text("I already have an account") }
            } else {
                TextButton(onClick = viewModel::requestOmbiAccess) { Text(if (matchFound) "Not my account / Request access" else "Request access") }
            }
            if (state.ombiUserSession != null) {
                TextButton(onClick = viewModel::unlinkOmbiAccount) { Text("Forget Ombi session") }
            }
        }
    }
}

@Composable
private fun UnconfiguredRequestsScreen(
    admin: Boolean,
    onSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .padding(bottom = 164.dp),
        contentAlignment = Alignment.Center,
    ) {
        RequestContentReveal(index = 0, animate = true) {
            EmptySetupState(
                admin = admin,
                onSetup = onSetup,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptySetupState(
    admin: Boolean,
    onSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(if (admin) 356.dp else 304.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF111A2B),
                        Color(0xFF1B1B38),
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
                            Color(0xFFA956FF).copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        center = Offset(720f, 120f),
                        radius = 700f,
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
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TypeChip("Requests", selected = true, onClick = {})
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    if (admin) "Requests are ready when you are" else "Requests aren't ready yet",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (admin) "Connect Ombi to let this home request movies and series from Vantafyn."
                    else "Your server admin has not finished setting up Requests.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SetupFeaturePill("Movies", Modifier.weight(1f))
                SetupFeaturePill("Series", Modifier.weight(1f))
                SetupFeaturePill("Family", Modifier.weight(1f))
            }
            if (admin) {
                VantafynButton("Set up Ombi", onClick = onSetup, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SetupFeaturePill(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.075f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = VantafynColors.Ink.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
private fun RequestsToolbar(state: RequestsUiState, viewModel: RequestsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(RequestsView.entries.toList()) { tab ->
                TypeChip(tab.label, state.activeRequestsView == tab) { viewModel.selectRequestsView(tab) }
            }
        }
        VantafynTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            label = "Search movies and series",
            placeholder = "Search movies and series",
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(RequestSearchFilter.entries.toList()) { filter ->
                    TypeChip(filter.label, state.searchFilter == filter) { viewModel.onSearchFilterChanged(filter) }
                }
            }
            TextButton(
                onClick = viewModel::search,
                enabled = state.query.length > 1 && !state.isSearching,
            ) {
                Text(
                    if (state.isSearching) "Searching" else "Search",
                    color = if (state.query.length > 1 && !state.isSearching) VantafynColors.Ink else VantafynColors.Muted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
            itemsIndexed(
                rail.items,
                key = { index, item -> "${rail.kind}-${item.mediaType}-${item.externalId}-${item.movieDbId.orEmpty()}-${item.tvDbId.orEmpty()}-$index" },
            ) { _, item ->
                RequestPosterCard(
                    item = item,
                    availableMatch = state.availabilityMatches[item.availabilityKey()],
                    onClick = { if (state.availabilityMatches[item.availabilityKey()] != null) onOpenAvailable(item) else onOpen(item) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RequestPosterCard(item: RequestMediaSummary, availableMatch: Any?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 132.dp, height = 264.dp)
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
                Text(item.resolvedShortLabel(availableMatch != null), color = VantafynColors.Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Text(
            item.title,
            color = VantafynColors.Ink,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(),
        )
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
                Text(listOfNotNull(item.mediaType.label, item.year?.toString(), item.resolvedLabel(availableMatch != null)).joinToString(" · "), color = VantafynColors.Muted)
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
                    TypeChip(display.resolvedLabel(availableMatch != null), selected = true, onClick = {})
                    Text(display.title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    RequestRatingMetadata(
                        values = listOfNotNull(
                            display.mediaType.label,
                            display.year?.toString(),
                            detail?.runtimeMinutes?.let { "${it}m" },
                            detail?.certification,
                            detail?.network,
                            display.rating?.let { "★ %.1f".format(it) },
                        ),
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
                RequestStatusSummary(
                    display = display,
                    verifiedAvailable = availableMatch != null,
                    serverName = availableMatch?.serverName,
                    matchedTitle = availableMatch?.title,
                )
                detail?.let { RequestDetailMetadataChips(it) }
                RequestArtworkSection(display, detail)
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
    }
}

@Composable
private fun RequestStatusSummary(
    display: RequestMediaSummary,
    verifiedAvailable: Boolean,
    serverName: String?,
    matchedTitle: String?,
) {
    VantafynGlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeChip(display.resolvedLabel(verifiedAvailable), selected = true, onClick = {})
                if (verifiedAvailable) {
                    TypeChip("Verified", selected = true, onClick = {})
                } else if (display.state == RequestState.Available) {
                    TypeChip("Ombi available", selected = true, onClick = {})
                }
            }
            val statusLine = when {
                verifiedAvailable -> "Matched in ${serverName ?: "Jellyfin"}${matchedTitle?.takeIf { it.isNotBlank() }?.let { " as $it" }.orEmpty()}."
                display.state == RequestState.Available -> "Ombi has marked this title available. Vantafyn will only open a verified Jellyfin match."
                display.requestedBy != null -> "Requested by ${display.requestedBy}."
                else -> display.state.explainer
            }
            Text(statusLine, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RequestDetailMetadataChips(detail: RequestMediaDetail) {
    val values = listOfNotNull(
        detail.summary.mediaType.label,
        detail.summary.year?.toString(),
        detail.runtimeMinutes?.let { "${it} min" },
        detail.certification,
        detail.network,
    )
    if (values.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values) { value -> TypeChip(value, selected = true, onClick = {}) }
    }
}

@Composable
private fun RequestArtworkSection(display: RequestMediaSummary, detail: RequestMediaDetail?) {
    val artwork = listOfNotNull(
        display.posterUrl?.let { "Poster" to it },
        display.backdropUrl?.let { "Backdrop" to it },
        detail?.summary?.posterUrl?.let { "Poster" to it },
        detail?.summary?.backdropUrl?.let { "Backdrop" to it },
    ).distinctBy { it.second }
    if (artwork.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Artwork", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(artwork) { (label, url) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = if (label == "Backdrop") 210.dp else 120.dp, height = 160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.64f)),
                    ) {
                        AsyncImage(model = url, contentDescription = "$label artwork", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f)))))
                        Text(label, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesSeasonSection(detail: RequestMediaDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Seasons", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        detail.seasons.forEach { season ->
            VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    season.posterUrl?.let { Poster(it, "Season ${season.seasonNumber}", small = true) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
private fun SetupSummaryTile(title: String, value: String) {
    VantafynGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
            Text(value.ifBlank { "Not set" }, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
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
    ManageSection(title = "Access Requests") {
        val visible = requests.filter { it.status != OmbiAccessRequestStatus.Dismissed }
        if (visible.isEmpty()) {
            Text("No pending Ombi access requests.", color = VantafynColors.Muted)
        } else {
            visible.forEach { request ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(request.jellyfinUserName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                            Text(request.suggestedOmbiUserName, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TypeChip(request.status.label, selected = request.status == OmbiAccessRequestStatus.Linked, onClick = {})
                    }
                    Text(DateFormat.getDateTimeInstance().format(Date(request.requestedAt)), color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    VantafynButton(
                        "Mark account created",
                        onClick = { viewModel.markAccessRequestAccountCreated(request.jellyfinUserId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        VantafynButton("Mark linked", onClick = { viewModel.markAccessRequestLinked(request.jellyfinUserId) }, modifier = Modifier.weight(1f))
                        VantafynButton("Dismiss", onClick = { viewModel.dismissAccessRequest(request.jellyfinUserId) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMappingsSection(mappings: List<OmbiUserMapping>, viewModel: RequestsViewModel) {
    ManageSection(title = "User Mappings") {
        if (mappings.isEmpty()) {
            Text("Mappings appear after users link Ombi accounts.", color = VantafynColors.Muted)
        } else {
            mappings.forEach { mapping ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(mapping.jellyfinUserName, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text(mapping.ombiUserName ?: "No Ombi username", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TypeChip(mapping.state.label, selected = mapping.state == OmbiLinkedAccountState.Linked, onClick = {})
                    TextButton(onClick = { viewModel.clearMapping(mapping.jellyfinUserId) }) { Text("Clear") }
                }
            }
        }
    }
}

@Composable
private fun ManageSection(title: String, content: @Composable () -> Unit) {
    VantafynGlassPanel(cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun OmbiManageMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ManageInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = VantafynColors.Muted, modifier = Modifier.weight(0.42f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = VantafynColors.Ink, modifier = Modifier.weight(0.58f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ManageSelectableRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFF6174FF).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.055f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) Brush.horizontalGradient(listOf(Color(0xFF43D3FF), Color(0xFFA956FF))) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.12f)))),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    VantafynGlassChip(
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
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
        OmbiSetupStep.Connect -> "Step 1 of 4"
        OmbiSetupStep.Authentication -> "Step 2 of 4"
        OmbiSetupStep.Test, OmbiSetupStep.Access -> "Step 3 of 4"
        OmbiSetupStep.Finish -> "Step 4 of 4"
    }

private val OmbiAccessMode.label: String
    get() = when (this) {
        OmbiAccessMode.Disabled -> "Disabled"
        OmbiAccessMode.AdminsOnly -> "Admins only"
        OmbiAccessMode.AllUsers -> "All users"
    }

private val OmbiIdentityMode.label: String
    get() = when (this) {
        OmbiIdentityMode.SharedApiKey -> "Server request account"
        OmbiIdentityMode.PerUserAccount -> "Per-user Ombi login"
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
        RequestState.Available -> "Available according to Ombi"
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
        RequestState.Available -> "Ombi available"
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
        RequestState.Available -> "Ombi reports this title as available. Vantafyn verifies the Jellyfin item separately before opening it."
        RequestState.Declined -> "This request was declined."
        RequestState.Failed -> "Ombi reported a problem with this request."
        RequestState.Unknown -> "Ombi returned a status Vantafyn does not fully understand yet."
    }

private fun RequestMediaSummary.resolvedLabel(verifiedJellyfinMatch: Boolean): String =
    if (verifiedJellyfinMatch) "Available in Jellyfin" else state.label

private fun RequestMediaSummary.resolvedShortLabel(verifiedJellyfinMatch: Boolean): String =
    if (verifiedJellyfinMatch) "Available" else state.shortLabel

private fun RequestMediaSummary.primaryActionLabel(): String =
    when (state) {
        RequestState.NotRequested -> if (mediaType == RequestMediaType.Series) "Request series" else "Request movie"
        RequestState.PendingApproval -> "Awaiting approval"
        RequestState.Approved -> "Approved"
        RequestState.Processing -> "Being added"
        RequestState.PartiallyAvailable -> "View seasons"
        RequestState.Available -> "Status"
        RequestState.Declined -> "Request declined"
        RequestState.Failed -> "Needs attention"
        RequestState.Unknown -> "View status"
    }

private fun RequestMediaSummary.accessibilityLabel(): String =
    "${title}, ${mediaType.label}, ${state.label}"

@Composable
private fun RequestRatingMetadata(values: List<String>) {
    if (values.isEmpty()) return
    Text(
        text = buildAnnotatedString {
            values.forEachIndexed { index, value ->
                if (index > 0) append(" · ")
                if (value.trimStart().startsWith("★")) {
                    withStyle(SpanStyle(color = VantafynColors.Gold, fontWeight = FontWeight.SemiBold)) {
                        append(value)
                    }
                } else {
                    append(value)
                }
            }
        },
        color = VantafynColors.Muted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
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
