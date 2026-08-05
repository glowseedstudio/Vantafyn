package dev.vantafyn.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.jellyfin.JellyfinHomeSection
import dev.vantafyn.core.jellyfin.JellyfinMediaDetail
import dev.vantafyn.core.jellyfin.JellyfinMediaCard
import dev.vantafyn.core.jellyfin.JellyfinMediaCardShape
import dev.vantafyn.core.jellyfin.JellyfinMediaItem
import dev.vantafyn.core.jellyfin.JellyfinPublicUser
import dev.vantafyn.core.jellyfin.JellyfinSearchResult
import dev.vantafyn.core.jellyfin.JellyfinHeroMediaItem
import dev.vantafyn.core.jellyfin.SavedProfile
import dev.vantafyn.core.ui.MobilePosterSpec
import dev.vantafyn.core.ui.PosterCard
import dev.vantafyn.core.ui.TvPosterSpec
import dev.vantafyn.core.ui.VantafynButton
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynErrorCard
import dev.vantafyn.core.ui.VantafynLoadingIndicator
import dev.vantafyn.core.ui.VantafynLogoHeader
import dev.vantafyn.core.ui.VantafynOnboardingBackground
import dev.vantafyn.core.ui.VantafynProfileCard
import dev.vantafyn.core.ui.VantafynScreenScaffold
import dev.vantafyn.core.ui.VantafynServerCard
import dev.vantafyn.core.ui.VantafynSetupHeader
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.R as CoreUiR
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.HomeSectionType
import dev.vantafyn.feature.home.auth.MobileDestination
import dev.vantafyn.feature.home.auth.HomeSectionPreference
import dev.vantafyn.feature.home.auth.VantafynAppBackground
import dev.vantafyn.feature.home.auth.VantafynArtworkType
import dev.vantafyn.feature.home.auth.VantafynCardShape
import dev.vantafyn.feature.home.auth.VantafynCardSize
import dev.vantafyn.feature.home.auth.VantafynCardSpacing
import dev.vantafyn.feature.home.auth.VantafynSetupStep
import dev.vantafyn.feature.home.auth.supportedSmartRows
import kotlinx.coroutines.delay

@Composable
fun VantafynAppContent(
    tv: Boolean,
    modifier: Modifier = Modifier,
    viewModel: VantafynHomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val backgroundResId = state.selectedBackground.drawableResId()
    when (state.step) {
        VantafynSetupStep.Splash -> SplashScreen(tv = tv, backgroundResId = backgroundResId, modifier = modifier)
        VantafynSetupStep.Welcome -> WelcomeScreen(state, tv, viewModel::continueFromWelcome, backgroundResId, modifier)
        VantafynSetupStep.ConnectServer -> ConnectServerScreen(
            state = state,
            tv = tv,
            onServerUrlChanged = viewModel::onServerUrlChanged,
            onConnect = viewModel::connectToServer,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
        VantafynSetupStep.ServerConfirm -> ServerConfirmScreen(
            state = state,
            tv = tv,
            onContinue = viewModel::continueToLogin,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
        VantafynSetupStep.Login -> LoginScreen(
            state = state,
            tv = tv,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onLogin = viewModel::login,
            onQuickConnect = viewModel::startQuickConnect,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
        VantafynSetupStep.QuickConnect -> QuickConnectScreen(
            state = state,
            tv = tv,
            onBack = viewModel::cancelQuickConnect,
            onRetry = viewModel::retryQuickConnect,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
        VantafynSetupStep.ProfilePicker -> ProfilePickerScreen(
            state = state,
            tv = tv,
            onSelect = viewModel::selectProfile,
            onSelectPublicUser = viewModel::selectPublicUser,
            onAddProfile = viewModel::addProfile,
            onToggleManage = viewModel::toggleManageProfiles,
            onRequestRemove = viewModel::requestRemoveProfile,
            onCancelRemove = viewModel::cancelRemoveProfile,
            onConfirmRemove = viewModel::confirmRemoveProfile,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
        VantafynSetupStep.Home -> HomeScreen(
            state = state,
            tv = tv,
            onRetry = viewModel::retryLibraries,
            onSwitchUser = viewModel::showProfilePicker,
            onAddProfile = viewModel::addProfile,
            onQuickConnect = viewModel::startQuickConnect,
            onConfirmLogout = viewModel::confirmCurrentProfileLogout,
            onCancelLogout = viewModel::cancelCurrentProfileLogout,
            onLogoutCurrentProfile = viewModel::logoutCurrentProfile,
            onNavigateMobile = viewModel::navigateMobile,
            onOpenLibrary = viewModel::openLibrary,
            onRetryLibrary = viewModel::retryLibraryItems,
            onOpenMedia = viewModel::openMedia,
            onRetryMedia = viewModel::retryMediaDetail,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onLoadFavorites = viewModel::loadFavorites,
            onPlaybackComingSoon = viewModel::showPlaybackComingSoon,
            onClearMessage = viewModel::clearMobileMessage,
            onToggleHomeSection = viewModel::toggleHomeSection,
            onMoveHomeSection = viewModel::moveHomeSection,
            onResetHomeLayout = viewModel::resetHomeLayout,
            onAddSmartRow = viewModel::addSmartRow,
            onRemoveSmartRow = viewModel::removeSmartRow,
            onCycleArtwork = viewModel::cycleSectionArtwork,
            onCycleShape = viewModel::cycleSectionShape,
            onCycleSize = viewModel::cycleSectionSize,
            onCycleSpacing = viewModel::cycleSectionSpacing,
            onToggleThemeMusic = viewModel::toggleThemeMusic,
            onSelectBackground = viewModel::selectBackground,
            onToggleMediaFavorite = viewModel::toggleMediaFavorite,
            onToggleMediaPlayed = viewModel::toggleMediaPlayed,
            onEditPlaybackPreferences = viewModel::editPlaybackPreferences,
            onSavePlaybackPreferences = viewModel::savePlaybackPreferences,
            onChangePassword = viewModel::changeCurrentUserPassword,
            onOpenAdminUser = viewModel::openAdminUser,
            onCloseAdminUser = viewModel::closeAdminUser,
            onUpdateAdminUser = viewModel::updateSelectedAdminUser,
            onResetAdminPassword = viewModel::resetSelectedAdminPassword,
            modifier = modifier,
        )
    }
}

@Composable
fun SplashScreen(tv: Boolean, modifier: Modifier = Modifier, backgroundResId: Int = CoreUiR.drawable.vantafyn_onboarding_background, message: String = "Preparing Vantafyn") {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Vantafyn", color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(VantafynSpacing.sm))
            Text(message, color = VantafynColors.Muted, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun WelcomeScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onContinue: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        CenterPane {
            VantafynLogoHeader(
                title = "Vantafyn",
                tagline = "Your media, beautifully streamed.",
                tv = tv,
            )
            Spacer(Modifier.height(if (tv) 44.dp else 36.dp))
            VantafynButton("Continue", onClick = onContinue, modifier = Modifier.fillMaxWidth(if (tv) 0.42f else 0.68f))
            if (state.savedProfiles.isNotEmpty()) {
                Spacer(Modifier.height(VantafynSpacing.lg))
                Text("Add another Jellyfin profile", color = VantafynColors.Muted)
            }
        }
    }
}

@Composable
private fun ConnectServerScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onServerUrlChanged: (String) -> Unit,
    onConnect: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        CenterPane {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
            ) {
                VantafynSetupHeader(
                    title = "Connect your server",
                    subtitle = "Use a local address or domain for your Jellyfin server.",
                    tv = tv,
                )
                Spacer(Modifier.height(if (tv) VantafynSpacing.md else VantafynSpacing.xs))
                VantafynTextField(
                    value = state.serverUrl,
                    onValueChange = onServerUrlChanged,
                    label = "Server address",
                    placeholder = "http://192.168.1.29:8096",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    tvKeyboardRequiresClick = tv,
                )
                Text("Example: https://media.example.com", color = VantafynColors.Muted.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyLarge)
                StatusBlock(state)
                Spacer(Modifier.height(VantafynSpacing.sm))
                VantafynButton(
                    "Connect",
                    onClick = onConnect,
                    enabled = !state.isLoading && state.serverUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(if (tv) 0.42f else 0.68f),
                )
            }
        }
    }
}

@Composable
private fun ServerConfirmScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onContinue: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        CenterPane {
            VantafynSetupHeader(
                title = "Server found",
                subtitle = "Ready to connect to your media library.",
                tv = tv,
            )
            Spacer(Modifier.height(if (tv) VantafynSpacing.xxl else VantafynSpacing.xl))
            val server = state.server
            VantafynServerCard(
                name = server?.name ?: "Jellyfin Server",
                url = server?.url ?: state.serverUrl,
                version = server?.version,
            ) {
                val avatar = state.publicUsers.firstOrNull { it.isAdministrator && it.imageUrl != null }
                    ?: state.publicUsers.firstOrNull { it.displayName == state.session?.user?.name && it.imageUrl != null }
                    ?: state.publicUsers.firstOrNull { user -> state.savedProfiles.any { it.displayName == user.displayName } && user.imageUrl != null }
                ProfileAvatar(
                    name = avatar?.displayName ?: state.session?.user?.name ?: server?.name ?: "Jellyfin Server",
                    imageUrl = avatar?.imageUrl,
                )
            }
            StatusBlock(state)
            Spacer(Modifier.height(if (tv) VantafynSpacing.xxl else VantafynSpacing.xl))
            VantafynButton(
                "Continue to Sign In",
                onClick = onContinue,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(if (tv) 0.48f else 0.78f),
            )
        }
    }
}

@Composable
private fun LoginScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onQuickConnect: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        CenterPane {
            VantafynSetupHeader(
                title = state.server?.name ?: "Sign in",
                subtitle = state.server?.url ?: state.serverUrl,
                tv = tv,
            )
            Spacer(Modifier.height(VantafynSpacing.lg))
            VantafynTextField(
                value = state.username,
                onValueChange = onUsernameChanged,
                label = "Username",
                enabled = !state.isLoading,
                tvKeyboardRequiresClick = tv,
            )
            Spacer(Modifier.height(VantafynSpacing.sm))
            VantafynTextField(
                value = state.password,
                onValueChange = onPasswordChanged,
                label = "Password",
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                tvKeyboardRequiresClick = tv,
            )
            StatusBlock(state)
            Spacer(Modifier.height(VantafynSpacing.lg))
            VantafynButton(
                "Sign In",
                onClick = onLogin,
                enabled = !state.isLoading && state.username.isNotBlank(),
                modifier = Modifier.fillMaxWidth(if (tv) 0.42f else 0.68f),
            )
            Spacer(Modifier.height(VantafynSpacing.sm))
            TextButton(onClick = onQuickConnect, enabled = state.server != null && !state.isLoading) {
                Text("Use Quick Connect", color = VantafynColors.Muted)
            }
        }
    }
}

@Composable
private fun QuickConnectScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = tv, modifier = modifier, backgroundResId = backgroundResId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(VantafynSpacing.lg),
        ) {
            CompactBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            )
            CenterPane {
                VantafynSetupHeader(
                    title = "Quick Connect",
                    subtitle = "Approve Vantafyn from your Jellyfin dashboard.",
                    tv = tv,
                )
                Spacer(Modifier.height(VantafynSpacing.xl))
                Box(
                    modifier = Modifier
                        .width(if (tv) 420.dp else 312.dp)
                        .height(if (tv) 238.dp else 210.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(VantafynColors.Surface.copy(alpha = 0.68f))
                        .padding(VantafynSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        Text("Code", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            state.quickConnectSession?.code ?: "----",
                            color = VantafynColors.Ink,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (tv) 76.dp else 68.dp),
                        )
                        Text(
                            state.quickConnectMessage ?: if (state.isLoading) "Preparing code..." else "Waiting for approval...",
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                StatusBlock(state)
                Spacer(Modifier.height(VantafynSpacing.lg))
                VantafynButton("Refresh Code", onClick = onRetry, enabled = !state.isLoading)
            }
        }
    }
}

@Composable
private fun ProfilePickerScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onSelect: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onToggleManage: () -> Unit,
    onRequestRemove: (SavedProfile) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    if (tv) {
        TvProfilePickerScreen(
            state = state,
            onSelect = onSelect,
            onSelectPublicUser = onSelectPublicUser,
            onAddProfile = onAddProfile,
            onToggleManage = onToggleManage,
            onRequestRemove = onRequestRemove,
            onCancelRemove = onCancelRemove,
            onConfirmRemove = onConfirmRemove,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
    } else {
        MobileProfilePickerScreen(
            state = state,
            onSelect = onSelect,
            onSelectPublicUser = onSelectPublicUser,
            onAddProfile = onAddProfile,
            onToggleManage = onToggleManage,
            onRequestRemove = onRequestRemove,
            onCancelRemove = onCancelRemove,
            onConfirmRemove = onConfirmRemove,
            backgroundResId = backgroundResId,
            modifier = modifier,
        )
    }
}

@Composable
private fun MobileProfilePickerScreen(
    state: VantafynHomeUiState,
    onSelect: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onToggleManage: () -> Unit,
    onRequestRemove: (SavedProfile) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = false, modifier = modifier, backgroundResId = backgroundResId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            ManageProfilesButton(
                state = state,
                onToggleManage = onToggleManage,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(52.dp))
                Text(
                    "Who's watching?",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(VantafynSpacing.xl))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    contentPadding = PaddingValues(bottom = VantafynSpacing.xl),
                ) {
                    gridItems(state.savedProfiles) { profile ->
                        SavedProfileTile(
                            profile = profile,
                            showServer = state.savedProfiles.map { it.serverRef }.distinct().size > 1,
                            manageMode = state.manageProfiles,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                            onSelect = { onSelect(profile) },
                            onRemove = { onRequestRemove(profile) },
                        )
                    }
                    gridItems(state.publicUsers, key = { it.id }) { user ->
                        PublicUserTile(
                            user = user,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                            onSelect = { onSelectPublicUser(user) },
                        )
                    }
                    item {
                        AddProfileTile(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(198.dp),
                            onClick = onAddProfile,
                        )
                    }
                }
                ProfilePickerFeedback(
                    state = state,
                    onCancelRemove = onCancelRemove,
                    onConfirmRemove = onConfirmRemove,
                )
            }
        }
    }
}

@Composable
private fun TvProfilePickerScreen(
    state: VantafynHomeUiState,
    onSelect: (SavedProfile) -> Unit,
    onSelectPublicUser: (JellyfinPublicUser) -> Unit,
    onAddProfile: () -> Unit,
    onToggleManage: () -> Unit,
    onRequestRemove: (SavedProfile) -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    backgroundResId: Int,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = true, modifier = modifier, backgroundResId = backgroundResId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = VantafynSpacing.tvGutter, vertical = 44.dp),
        ) {
            ManageProfilesButton(
                state = state,
                onToggleManage = onToggleManage,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Who's watching?",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(46.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
                    contentPadding = PaddingValues(horizontal = VantafynSpacing.xl),
                ) {
                    items(state.savedProfiles) { profile ->
                        SavedProfileTile(
                            profile = profile,
                            showServer = state.savedProfiles.map { it.serverRef }.distinct().size > 1,
                            manageMode = state.manageProfiles,
                            modifier = Modifier
                                .width(150.dp)
                                .height(202.dp),
                            onSelect = { onSelect(profile) },
                            onRemove = { onRequestRemove(profile) },
                        )
                    }
                    items(state.publicUsers, key = { it.id }) { user ->
                        PublicUserTile(
                            user = user,
                            modifier = Modifier
                                .width(150.dp)
                                .height(202.dp),
                            onSelect = { onSelectPublicUser(user) },
                        )
                    }
                    item {
                        AddProfileTile(
                            modifier = Modifier
                                .width(150.dp)
                                .height(202.dp),
                            onClick = onAddProfile,
                        )
                    }
                }
                ProfilePickerFeedback(
                    state = state,
                    onCancelRemove = onCancelRemove,
                    onConfirmRemove = onConfirmRemove,
                )
            }
        }
    }
}

@Composable
private fun ManageProfilesButton(
    state: VantafynHomeUiState,
    onToggleManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.savedProfiles.isNotEmpty()) {
        TextButton(
            onClick = onToggleManage,
            modifier = modifier.widthIn(min = 96.dp),
        ) {
            Text(
                if (state.manageProfiles) "Done" else "Manage",
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfilePickerFeedback(
    state: VantafynHomeUiState,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
) {
    if (state.isLoading) {
        Spacer(Modifier.height(VantafynSpacing.xl))
        VantafynLoadingIndicator("Restoring profile")
    }
    state.pendingRemoval?.let { profile ->
        Spacer(Modifier.height(VantafynSpacing.lg))
        VantafynErrorCard("Remove ${profile.displayName} from this device?") {
            VantafynButton("Remove", onClick = onConfirmRemove)
            Spacer(Modifier.width(VantafynSpacing.sm))
            OutlinedButton(onClick = onCancelRemove) {
                Text("Cancel")
            }
        }
    }
    state.errorMessage?.let {
        Spacer(Modifier.height(VantafynSpacing.lg))
        VantafynErrorCard(it)
    }
}

@Composable
private fun SavedProfileTile(
    profile: SavedProfile,
    showServer: Boolean,
    manageMode: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    VantafynProfileCard(
        label = profile.displayName,
        subtitle = if (showServer) profile.serverName ?: profile.serverUrl else null,
        modifier = modifier,
        manageMode = manageMode,
        onClick = onSelect,
        onRemove = onRemove,
    ) {
        ProfileAvatar(
            name = profile.displayName,
            imageUrl = profile.imageUrl,
        )
    }
}

@Composable
private fun PublicUserTile(
    user: JellyfinPublicUser,
    modifier: Modifier,
    onSelect: () -> Unit,
) {
    VantafynProfileCard(
        label = user.displayName,
        modifier = modifier,
        onClick = onSelect,
    ) {
        ProfileAvatar(
            name = user.displayName,
            imageUrl = user.imageUrl,
        )
    }
}

@Composable
private fun AddProfileTile(modifier: Modifier, onClick: () -> Unit) {
    VantafynProfileCard(
        label = "Add Profile",
        modifier = modifier,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VantafynColors.SurfaceHigh.copy(alpha = 0.56f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, imageUrl: String?, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3D5266), Color(0xFF756A8A), Color(0xFF20252D)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(initials(name), color = VantafynColors.Ink, style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun HomeScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onRetry: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onLogoutCurrentProfile: () -> Unit,
    onNavigateMobile: (MobileDestination) -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onRetryLibrary: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onRetryMedia: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadFavorites: () -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onClearMessage: () -> Unit,
    onToggleHomeSection: (HomeSectionType) -> Unit,
    onMoveHomeSection: (HomeSectionType, Int) -> Unit,
    onResetHomeLayout: () -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onToggleMediaFavorite: () -> Unit,
    onToggleMediaPlayed: () -> Unit,
    onEditPlaybackPreferences: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onOpenAdminUser: (java.util.UUID) -> Unit,
    onCloseAdminUser: () -> Unit,
    onUpdateAdminUser: (Boolean?, Boolean?, Boolean?, Boolean?, List<java.util.UUID>?) -> Unit,
    onResetAdminPassword: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!tv) {
        MobileShellScreen(
            state = state,
            onRetryHome = onRetry,
            onSwitchUser = onSwitchUser,
            onAddProfile = onAddProfile,
            onQuickConnect = onQuickConnect,
            onConfirmLogout = onConfirmLogout,
            onCancelLogout = onCancelLogout,
            onLogoutCurrentProfile = onLogoutCurrentProfile,
            onNavigate = onNavigateMobile,
            onOpenLibrary = onOpenLibrary,
            onRetryLibrary = onRetryLibrary,
            onOpenMedia = onOpenMedia,
            onRetryMedia = onRetryMedia,
            onSearchQueryChanged = onSearchQueryChanged,
            onLoadFavorites = onLoadFavorites,
            onPlaybackComingSoon = onPlaybackComingSoon,
            onClearMessage = onClearMessage,
            onToggleHomeSection = onToggleHomeSection,
            onMoveHomeSection = onMoveHomeSection,
            onResetHomeLayout = onResetHomeLayout,
            onAddSmartRow = onAddSmartRow,
            onRemoveSmartRow = onRemoveSmartRow,
            onCycleArtwork = onCycleArtwork,
            onCycleShape = onCycleShape,
            onCycleSize = onCycleSize,
            onCycleSpacing = onCycleSpacing,
            onToggleThemeMusic = onToggleThemeMusic,
            onSelectBackground = onSelectBackground,
            onToggleMediaFavorite = onToggleMediaFavorite,
            onToggleMediaPlayed = onToggleMediaPlayed,
            onEditPlaybackPreferences = onEditPlaybackPreferences,
            onSavePlaybackPreferences = onSavePlaybackPreferences,
            onChangePassword = onChangePassword,
            onOpenAdminUser = onOpenAdminUser,
            onCloseAdminUser = onCloseAdminUser,
            onUpdateAdminUser = onUpdateAdminUser,
            onResetAdminPassword = onResetAdminPassword,
            modifier = modifier,
        )
        return
    }
    VantafynScreenScaffold(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = VantafynSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(if (tv) VantafynSpacing.xl else VantafynSpacing.lg),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                    Text(state.server?.name ?: "Vantafyn", color = VantafynColors.Ink, style = if (tv) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium)
                    Text("Signed in as ${state.session?.user?.name.orEmpty()}", color = VantafynColors.Muted, style = MaterialTheme.typography.titleLarge)
                }
            }
            if (state.isLibrariesLoading) {
                item { VantafynLoadingIndicator("Loading libraries") }
            }
            state.errorMessage?.let { message ->
                item {
                    VantafynErrorCard(message) {
                        VantafynButton("Retry", onClick = onRetry)
                    }
                }
            }
            if (!state.isLibrariesLoading && state.libraries.isEmpty() && state.errorMessage == null) {
                item { VantafynErrorCard("No libraries were returned for this user.") }
            }
            if (state.libraries.isNotEmpty()) {
                item { LibraryRow("Libraries", state.libraries, tv) }
            }
            items(state.libraries) { library ->
                LibraryRow(library.name, listOf(library), tv)
            }
            item {
                OutlinedButton(onClick = onSwitchUser) {
                    Text("Add or switch profile")
                }
            }
        }
    }
}

@Composable
private fun MobileShellScreen(
    state: VantafynHomeUiState,
    onRetryHome: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onConfirmLogout: () -> Unit,
    onCancelLogout: () -> Unit,
    onLogoutCurrentProfile: () -> Unit,
    onNavigate: (MobileDestination) -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onRetryLibrary: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onRetryMedia: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onLoadFavorites: () -> Unit,
    onPlaybackComingSoon: () -> Unit,
    onClearMessage: () -> Unit,
    onToggleHomeSection: (HomeSectionType) -> Unit,
    onMoveHomeSection: (HomeSectionType, Int) -> Unit,
    onResetHomeLayout: () -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onToggleMediaFavorite: () -> Unit,
    onToggleMediaPlayed: () -> Unit,
    onEditPlaybackPreferences: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSavePlaybackPreferences: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onOpenAdminUser: (java.util.UUID) -> Unit,
    onCloseAdminUser: () -> Unit,
    onUpdateAdminUser: (Boolean?, Boolean?, Boolean?, Boolean?, List<java.util.UUID>?) -> Unit,
    onResetAdminPassword: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    VantafynOnboardingBackground(tv = false, modifier = modifier, backgroundResId = state.selectedBackground.drawableResId()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            when (state.mobileDestination) {
                MobileDestination.Home -> MobileHomeContent(
                    state = state,
                    onRetry = onRetryHome,
                    onSearch = { onNavigate(MobileDestination.Search) },
                    onProfile = { onNavigate(MobileDestination.Profile) },
                    onOpenLibrary = onOpenLibrary,
                    onOpenMedia = onOpenMedia,
                    onPlaybackComingSoon = onPlaybackComingSoon,
                )
                MobileDestination.MediaDetail -> MediaDetailScreen(
                    state = state,
                    onBack = { onNavigate(MobileDestination.Home) },
                    onRetry = onRetryMedia,
                    onOpenMedia = onOpenMedia,
                    onPlaybackComingSoon = onPlaybackComingSoon,
                    themeMusicEnabled = state.themeMusicEnabled,
                    onToggleFavorite = onToggleMediaFavorite,
                    onTogglePlayed = onToggleMediaPlayed,
                )
                else -> Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    when (state.mobileDestination) {
                        MobileDestination.Libraries -> LibrariesScreen(state, onOpenLibrary)
                        MobileDestination.Search -> SearchScreen(state, onSearchQueryChanged, onOpenMedia)
                        MobileDestination.Favorites -> FavoritesScreen(state, onLoadFavorites, onOpenMedia)
                        MobileDestination.Admin -> AdminScreen(state, onOpenUser = onOpenAdminUser)
                        MobileDestination.Profile -> ProfileSettingsScreen(
                            state = state,
                            onHomeLayout = { onNavigate(MobileDestination.HomeLayout) },
                            onPlaybackPreferences = { onNavigate(MobileDestination.PlaybackPreferences) },
                            onToggleThemeMusic = onToggleThemeMusic,
                            onSwitchUser = onSwitchUser,
                            onAddProfile = onAddProfile,
                            onQuickConnect = onQuickConnect,
                            onLogout = onConfirmLogout,
                            onSelectBackground = onSelectBackground,
                            onChangePassword = onChangePassword,
                        )
                        MobileDestination.PlaybackPreferences -> PlaybackPreferencesScreen(
                            state = state,
                            onBack = { onNavigate(MobileDestination.Profile) },
                            onEdit = onEditPlaybackPreferences,
                            onSave = onSavePlaybackPreferences,
                        )
                        MobileDestination.HomeLayout -> HomeLayoutScreen(
                            state = state,
                            onBack = { onNavigate(MobileDestination.Profile) },
                            onToggle = onToggleHomeSection,
                            onMove = onMoveHomeSection,
                            onReset = onResetHomeLayout,
                            onAddSmartRow = onAddSmartRow,
                            onRemoveSmartRow = onRemoveSmartRow,
                            onCycleArtwork = onCycleArtwork,
                            onCycleShape = onCycleShape,
                            onCycleSize = onCycleSize,
                            onCycleSpacing = onCycleSpacing,
                        )
                        MobileDestination.LibraryDetail -> LibraryDetailScreen(
                            state = state,
                            onBack = { onNavigate(MobileDestination.Libraries) },
                            onRetry = onRetryLibrary,
                            onOpenMedia = onOpenMedia,
                            onPlaybackComingSoon = onPlaybackComingSoon,
                        )
                        else -> Unit
                    }
                }
            }
            MobileBottomNav(
                selected = state.mobileDestination,
                onSelected = onNavigate,
                isAdmin = state.session?.user?.isAdministrator == true,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        state.adminUserDetail?.let { detail ->
            AdminUserDetailDialog(
                state = state,
                detail = detail,
                onDismiss = onCloseAdminUser,
                onUpdate = onUpdateAdminUser,
                onResetPassword = onResetAdminPassword,
            )
        }
    }
    state.mobileMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onClearMessage,
            confirmButton = {
                TextButton(onClick = onClearMessage) { Text("OK") }
            },
            title = { Text(message) },
            text = { Text("Vantafyn will add playback after navigation and details are polished.") },
        )
    }
    if (state.confirmLogout) {
        AlertDialog(
            onDismissRequest = onCancelLogout,
            confirmButton = {
                TextButton(onClick = onLogoutCurrentProfile) { Text("Log Out") }
            },
            dismissButton = {
                TextButton(onClick = onCancelLogout) { Text("Cancel") }
            },
            title = { Text("Log out of this profile?") },
            text = { Text("This removes the saved session for ${state.session?.user?.name.orEmpty()} from this device.") },
        )
    }
}

@Composable
private fun MobileHomeContent(
    state: VantafynHomeUiState,
    onRetry: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
        ) {
            state.homeLayout.sortedBy { it.order }.filter { it.visible }.forEach { preference ->
                when (preference.type) {
                    HomeSectionType.MediaBar -> item {
                        val hero = state.home?.heroItems.orEmpty()
                        if (hero.isNotEmpty()) {
                            HeroCarousel(items = hero, onOpen = { onOpenMedia(it.id) })
                        } else {
                            HomeFallbackHero(state)
                        }
                    }
                    HomeSectionType.MyMedia -> item {
                        HomeRowInset { LibraryShowcaseRow("My Media", mainLibraries(state.libraries), onOpenLibrary) }
                    }
                    HomeSectionType.ContinueWatching -> homeSection(state, "Continue")?.let { section ->
                        item {
                            HomeRowInset {
                                HomeMediaSection(
                                    section = section,
                                    preference = preference,
                                    onOpenMedia = onOpenMedia,
                                    onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                    onPlaybackComingSoon = onPlaybackComingSoon,
                                )
                            }
                        }
                    }
                    HomeSectionType.RecentlyAddedMovies -> homeSection(state, "Movies")?.let { section ->
                        item {
                            HomeRowInset {
                                HomeMediaSection(
                                    section = section,
                                    preference = preference,
                                    onOpenMedia = onOpenMedia,
                                    onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                    onPlaybackComingSoon = onPlaybackComingSoon,
                                )
                            }
                        }
                    }
                    HomeSectionType.RecentlyAddedTv -> homeSection(state, "TV")?.let { section ->
                        item {
                            HomeRowInset {
                                HomeMediaSection(
                                    section = section,
                                    preference = preference,
                                    onOpenMedia = onOpenMedia,
                                    onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                    onPlaybackComingSoon = onPlaybackComingSoon,
                                )
                            }
                        }
                    }
                    HomeSectionType.LiveTvChannels -> homeSection(state, "Live TV")?.let { section ->
                        item {
                            HomeRowInset {
                                HomeMediaSection(
                                    section = section,
                                    preference = preference,
                                    onOpenMedia = { onPlaybackComingSoon() },
                                    onOpenLibrary = { libraryId -> state.libraries.firstOrNull { it.id == libraryId }?.let(onOpenLibrary) },
                                    onPlaybackComingSoon = onPlaybackComingSoon,
                                )
                            }
                        }
                    }
                    HomeSectionType.SmartRows -> item {
                        HomeRowInset {
                            SmartRowsSection(
                                state = state,
                                preference = preference,
                                onOpenMedia = onOpenMedia,
                                onPlaybackComingSoon = onPlaybackComingSoon,
                            )
                        }
                    }
                    HomeSectionType.OtherLibraries -> {
                        val other = otherLibraries(state.libraries)
                        if (other.isNotEmpty()) item {
                            HomeRowInset { LibraryShowcaseRow("More Libraries", other, onOpenLibrary) }
                        }
                    }
                }
            }
            if (state.favorites.isNotEmpty()) {
                item {
                    HomeRowInset {
                        MyListHomeRow(items = state.favorites.take(16), onOpenMedia = onOpenMedia)
                    }
                }
            }
            if (state.isHomeLoading) {
                item { HomeRowInset { HomeLoadingShelf() } }
            }
            state.homeErrorMessage?.let { message ->
                item {
                    HomeRowInset {
                        VantafynErrorCard(message) {
                            VantafynButton("Retry", onClick = onRetry)
                        }
                    }
                }
            }
        }
        MobileHomeProfileAvatar(
            state = state,
            onProfile = onProfile,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 2.dp, end = VantafynSpacing.xl),
        )
    }
}

@Composable
private fun MyListHomeRow(items: List<JellyfinMediaItem>, onOpenMedia: (java.util.UUID) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("My List", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(items, key = { it.id }) { item ->
                MediaItemCard(item = item, onClick = { onOpenMedia(item.id) })
            }
        }
    }
}

@Composable
private fun HomeRowInset(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 8.dp)) {
        content()
    }
}

@Composable
private fun MobileHomeProfileAvatar(state: VantafynHomeUiState, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.76f))
            .clickable(onClick = onProfile),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = state.session?.user?.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(initials(state.session?.user?.name.orEmpty()), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GlassAction(text: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.sm),
    ) {
        Text(text, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HeroCarousel(
    items: List<JellyfinHeroMediaItem>,
    onOpen: (JellyfinHeroMediaItem) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(5_500)
                val next = (listState.firstVisibleItemIndex + 1) % items.size
                listState.animateScrollToItem(next)
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            items(items, key = { it.id }) { item ->
                CinematicHero(
                    item = item,
                    onOpen = { onOpen(item) },
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = VantafynSpacing.xl, bottom = VantafynSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.take(6).forEachIndexed { index, _ ->
                val selected = index == listState.firstVisibleItemIndex.coerceAtMost(5)
                Box(
                    modifier = Modifier
                        .width(if (selected) 18.dp else 7.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = if (selected) 0.78f else 0.34f)),
                )
            }
        }
    }
}

@Composable
private fun CinematicHero(
    item: JellyfinHeroMediaItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(292.dp)
            .background(VantafynColors.Graphite)
            .clickable(onClick = onOpen),
    ) {
        AsyncImage(
            model = item.backdropUrl ?: item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xC0070A12),
                            0.18f to Color(0x33070A12),
                            0.56f to Color.Transparent,
                            0.82f to Color(0xD4070A12),
                            1.00f to Color(0xFF070A12),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xD8070A12), Color(0x66070A12), Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = VantafynSpacing.xl, end = 96.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (item.logoUrl != null) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(76.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    item.title,
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                listOfNotNull(
                    item.year?.toString(),
                    item.runtimeMinutes?.let { "${it}m" },
                    item.officialRating,
                    item.communityRating?.let { "★ ${"%.1f".format(it)}" },
                ).joinToString(" · "),
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.genres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item.genres.take(2).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(genre, color = VantafynColors.Ink.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFallbackHero(state: VantafynHomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF172037), Color(0xFF302A52), VantafynColors.Graphite)))
            .padding(VantafynSpacing.lg),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text("Welcome back, ${state.session?.user?.name.orEmpty()}", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium)
            Text("Your Jellyfin library is ready.", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HomeLoadingShelf() {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Loading your home", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(4) {
                Box(
                    modifier = Modifier
                        .width(146.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.72f)),
                )
            }
        }
    }
}

@Composable
private fun LibraryShowcaseRow(
    title: String,
    libraries: List<JellyfinLibrary>,
    onOpenLibrary: (JellyfinLibrary) -> Unit,
) {
    if (libraries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(libraries, key = { it.id }) { library ->
                Column(
                    modifier = Modifier.width(210.dp),
                    verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
                ) {
                    ArtworkBox(
                        imageUrl = library.imageUrl,
                        title = library.name,
                        wide = true,
                        progress = null,
                        onClick = { onOpenLibrary(library) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp),
                    )
                    Text(library.name, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(library.collectionType?.replaceFirstChar(Char::titlecase) ?: "Library", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HomeMediaSection(
    section: JellyfinHomeSection,
    preference: HomeSectionPreference,
    onOpenMedia: (java.util.UUID) -> Unit,
    onOpenLibrary: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    val spacing = preference.spacing.toDp()
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(section.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            items(section.items, key = { it.id }) { item ->
                MediaArtworkCard(
                    item = item,
                    preference = preference,
                    onClick = {
                        when {
                            item.shape == JellyfinMediaCardShape.Library -> onOpenLibrary(item.id)
                            item.itemType?.startsWith("LiveTv") == true -> onPlaybackComingSoon()
                            else -> onOpenMedia(item.id)
                        }
                    },
                )
            }
        }
    }
}

private fun homeSection(state: VantafynHomeUiState, token: String): JellyfinHomeSection? =
    state.home?.sections.orEmpty().firstOrNull { it.title.contains(token, ignoreCase = true) }

private fun mainLibraries(libraries: List<JellyfinLibrary>): List<JellyfinLibrary> = libraries

private fun otherLibraries(libraries: List<JellyfinLibrary>): List<JellyfinLibrary> {
    val mainIds = mainLibraries(libraries).map { it.id }.toSet()
    return libraries.filterNot { it.id in mainIds }
}

private fun previewImagesFor(state: VantafynHomeUiState, type: HomeSectionType): List<String> =
    when (type) {
        HomeSectionType.MediaBar -> state.home?.heroItems.orEmpty().mapNotNull { it.backdropUrl ?: it.posterUrl }
        HomeSectionType.MyMedia -> mainLibraries(state.libraries).mapNotNull { it.imageUrl }
        HomeSectionType.ContinueWatching -> homeSection(state, "Continue")?.items.orEmpty().mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.RecentlyAddedMovies -> homeSection(state, "Movies")?.items.orEmpty().mapNotNull { it.imageUrl ?: it.backdropUrl }
        HomeSectionType.RecentlyAddedTv -> homeSection(state, "TV")?.items.orEmpty().mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.LiveTvChannels -> homeSection(state, "Live TV")?.items.orEmpty().mapNotNull { it.imageUrl ?: it.backdropUrl }
        HomeSectionType.SmartRows -> state.home?.sections.orEmpty()
            .filter { it.title in state.configuredSmartRows }
            .flatMap { it.items }
            .mapNotNull { it.backdropUrl ?: it.imageUrl }
        HomeSectionType.OtherLibraries -> otherLibraries(state.libraries).mapNotNull { it.imageUrl }
    }

@Composable
private fun SmartRowsPlaceholder() {
    GlassPanel {
        Text("Smart Rows", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Add real metadata rows such as New in Crime, Highly Rated, and Recently Released from Home Sections. Rows stay hidden until backed by Jellyfin results.",
            color = VantafynColors.Muted,
        )
    }
}

@Composable
private fun SmartRowsSection(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    onOpenMedia: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    val smartSections = state.home?.sections.orEmpty().filter {
        it.title in state.configuredSmartRows
    }
    if (smartSections.isEmpty()) {
        SmartRowsPlaceholder()
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg)) {
            smartSections.forEach { section ->
                HomeMediaSection(
                    section = section,
                    preference = preference,
                    onOpenMedia = onOpenMedia,
                    onOpenLibrary = {},
                    onPlaybackComingSoon = onPlaybackComingSoon,
                )
            }
        }
    }
}

private fun HomeSectionPreference.cardWidth(wide: Boolean) =
    when (cardSize) {
        VantafynCardSize.Small -> if (wide) 194.dp else 124.dp
        VantafynCardSize.Medium -> if (wide) 226.dp else 142.dp
        VantafynCardSize.Large -> if (wide) 268.dp else 162.dp
    }

private fun HomeSectionPreference.cardHeight(wide: Boolean) =
    when (cardSize) {
        VantafynCardSize.Small -> if (wide) 110.dp else 186.dp
        VantafynCardSize.Medium -> if (wide) 128.dp else 214.dp
        VantafynCardSize.Large -> if (wide) 150.dp else 244.dp
    }

private fun HomeSectionPreference.cardCorner() =
    when (cardShape) {
        VantafynCardShape.Rounded -> 16.dp
        VantafynCardShape.Squircle -> 22.dp
        VantafynCardShape.Soft -> 12.dp
        VantafynCardShape.Sharpish -> 6.dp
    }

private fun VantafynCardSpacing.toDp() =
    when (this) {
        VantafynCardSpacing.Compact -> 8.dp
        VantafynCardSpacing.Comfortable -> VantafynSpacing.md
        VantafynCardSpacing.Spacious -> 20.dp
    }

private fun JellyfinMediaCard.resolveArtwork(preference: HomeSectionPreference?, wide: Boolean): String? =
    when (preference?.artworkType ?: VantafynArtworkType.Auto) {
        VantafynArtworkType.PrimaryPoster -> imageUrl ?: backdropUrl ?: thumbUrl
        VantafynArtworkType.Backdrop -> backdropUrl ?: thumbUrl ?: imageUrl
        VantafynArtworkType.Thumb -> thumbUrl ?: backdropUrl ?: imageUrl
        VantafynArtworkType.Logo -> logoUrl ?: backdropUrl ?: thumbUrl ?: imageUrl
        VantafynArtworkType.Auto -> if (wide) backdropUrl ?: thumbUrl ?: imageUrl else imageUrl ?: backdropUrl ?: thumbUrl
    }

private fun JellyfinMediaItem.resolveArtwork(wide: Boolean): String? =
    if (wide) backdropUrl ?: thumbUrl ?: imageUrl else imageUrl ?: backdropUrl ?: thumbUrl

@Composable
private fun MediaArtworkCard(item: JellyfinMediaCard, preference: HomeSectionPreference? = null, onClick: () -> Unit = {}) {
    val wide = preference?.type != HomeSectionType.RecentlyAddedMovies &&
        (item.shape == JellyfinMediaCardShape.Wide || item.shape == JellyfinMediaCardShape.Library || preference?.artworkType != VantafynArtworkType.PrimaryPoster)
    val progress = item.progress
    val width = preference?.cardWidth(wide) ?: if (wide) 226.dp else 142.dp
    val height = preference?.cardHeight(wide) ?: if (wide) 128.dp else 214.dp
    val corner = preference?.cardCorner() ?: 16.dp
    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(corner))
                .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh)))
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = item.resolveArtwork(preference, wide),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(VantafynColors.Primary),
                )
            }
        }
        Text(
            item.title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (wide) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(iterations = 1),
        )
        item.subtitle?.let {
            Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MediaItemCard(item: JellyfinMediaItem, onClick: () -> Unit) {
    val wide = item.shape == JellyfinMediaCardShape.Wide || item.shape == JellyfinMediaCardShape.Library
    Column(
        modifier = Modifier.width(if (wide) 226.dp else 142.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        ArtworkBox(
            imageUrl = item.resolveArtwork(wide),
            title = item.title,
            wide = wide,
            progress = item.progress,
            onClick = onClick,
        )
        Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = if (wide) 1 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee(iterations = 1))
        item.subtitle?.let { Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun SearchResultRow(item: JellyfinSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.58f))
            .clickable(onClick = onClick)
            .padding(VantafynSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(
            imageUrl = item.imageUrl ?: item.backdropUrl,
            title = item.title,
            wide = false,
            progress = null,
            onClick = onClick,
            modifier = Modifier.width(72.dp).height(104.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, maxLines = 2)
            Text(listOfNotNull(item.year?.toString(), item.itemType).joinToString("  "), color = VantafynColors.Muted)
            item.subtitle?.let { Text(it, color = VantafynColors.Muted.copy(alpha = 0.82f), maxLines = 1) }
        }
    }
}

@Composable
private fun ArtworkBox(
    imageUrl: String?,
    title: String,
    wide: Boolean,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(if (wide) 128.dp else 214.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF24304D), Color(0xFF393456), VantafynColors.SurfaceHigh)))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (progress != null && progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(VantafynColors.Primary),
            )
        }
    }
}

@Composable
private fun LibrariesScreen(state: VantafynHomeUiState, onOpenLibrary: (JellyfinLibrary) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item { ScreenTitle("Libraries", "Browse every Jellyfin view available to this profile.") }
        if (state.isLibrariesLoading) item { HomeLoadingShelf() }
        state.errorMessage?.let { item { VantafynErrorCard(it) } }
        if (!state.isLibrariesLoading && state.libraries.isEmpty()) {
            item { EmptyState("No libraries returned", "Check this user's Jellyfin permissions.") }
        }
        items(state.libraries, key = { it.id }) { library ->
            LibraryListCard(library = library, onClick = { onOpenLibrary(library) })
        }
    }
}

@Composable
private fun LibraryDetailScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
) {
    val library = state.selectedLibrary
    val liveTv = library?.collectionType.isLiveTvCollection()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                ScreenTitle(library?.name ?: "Library", library?.collectionType?.replaceFirstChar(Char::titlecase) ?: "Jellyfin library")
            }
        }
        if (!liveTv) item { FilterChips(listOf("Recently Added", "A-Z", "Favorites", "Unwatched")) }
        if (state.isLibraryItemsLoading) item { HomeLoadingShelf() }
        state.libraryItemsError?.let { message ->
            item { VantafynErrorCard(message) { VantafynButton("Retry", onClick = onRetry) } }
        }
        if (liveTv) {
            item {
                LiveTvGuideSection(
                    channels = state.home?.liveTvChannels.orEmpty(),
                    programs = state.home?.liveTvPrograms.orEmpty(),
                    onProgram = onPlaybackComingSoon,
                )
            }
        }
        if (!state.isLibraryItemsLoading && state.libraryItems.isEmpty() && state.libraryItemsError == null) {
            item {
                if (liveTv) {
                    EmptyState("No Live TV channels found", "Jellyfin did not return channels for this profile/server.")
                } else {
                    EmptyState("Nothing here yet", "This library returned no browsable items.")
                }
            }
        }
        if (state.libraryItems.isNotEmpty()) item {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(142.dp),
                modifier = Modifier.height(900.dp),
                horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
            ) {
                gridItems(state.libraryItems, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        onClick = {
                            if (liveTv || item.itemType?.startsWith("LiveTv") == true) onPlaybackComingSoon() else onOpenMedia(item.id)
                        },
                    )
                }
            }
        }
    }
}

private fun String?.isLiveTvCollection(): Boolean =
    this?.lowercase()?.replace(" ", "") in setOf("livetv", "livetvchannels")

@Composable
private fun LiveTvGuideSection(
    channels: List<dev.vantafyn.core.jellyfin.JellyfinLiveTvChannel>,
    programs: List<dev.vantafyn.core.jellyfin.JellyfinLiveTvProgram>,
    onProgram: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Program Guide", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            GlassAction("Guide", onClick = onProgram)
        }
        if (channels.isEmpty() && programs.isEmpty()) {
            EmptyState("Guide data unavailable", "Jellyfin did not return channel or program listings for this profile.")
        } else {
            val programsByChannel = programs.groupBy { it.channelId }
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                channels.take(12).forEach { channel ->
                    val rowPrograms = programsByChannel[channel.id].orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.055f))
                            .clickable(onClick = onProgram)
                            .padding(VantafynSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkBox(
                            imageUrl = channel.imageUrl,
                            title = channel.name,
                            wide = false,
                            progress = null,
                            onClick = onProgram,
                            modifier = Modifier.size(58.dp),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(listOfNotNull(channel.number, channel.name).joinToString(" · "), color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(channel.currentProgramName ?: rowPrograms.firstOrNull()?.title ?: "No current program returned", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            rowPrograms.firstOrNull()?.let {
                                Text(listOfNotNull(it.startDate?.take(16), it.endDate?.take(16)).joinToString(" - "), color = VantafynColors.Muted.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    state: VantafynHomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    val groupedResults = state.searchResults.groupBy { it.itemType?.ifBlank { "Other" } ?: "Other" }
    val typeFilters = groupedResults.keys.sorted()
    val visibleGroups = if (selectedType == null) groupedResults else groupedResults.filterKeys { it == selectedType }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Text("Search", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(VantafynSpacing.md))
            VantafynTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                label = "Search Jellyfin",
                placeholder = "Movie, show, episode...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
        }
        if (typeFilters.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                    item { SelectableChip("All", selectedType == null) { selectedType = null } }
                    items(typeFilters, key = { it }) { type ->
                        SelectableChip(type.searchGroupLabel(), selectedType == type) { selectedType = type }
                    }
                }
            }
        }
        if (state.searchQuery.trim().length < 2) {
            item { EmptyState("Start typing", "Search runs after a short pause.") }
        }
        if (state.isSearchLoading) item { VantafynLoadingIndicator("Searching") }
        state.searchError?.let { item { VantafynErrorCard(it) } }
        if (!state.isSearchLoading && state.searchQuery.trim().length >= 2 && state.searchResults.isEmpty() && state.searchError == null) {
            item { EmptyState("No results", "Try a different title or person.") }
        }
        visibleGroups.toSortedMap().forEach { (type, results) ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                    Text(type.searchGroupLabel(), color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                        items(results, key = { it.id }) { result ->
                            SearchResultCard(item = result, onClick = { onOpenMedia(result.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) VantafynColors.Ink else VantafynColors.Muted,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFF7B8DFF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
            .border(1.dp, if (selected) Color(0xFF9AA9FF).copy(alpha = 0.44f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

@Composable
private fun SearchResultCard(item: JellyfinSearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(if (item.shape == JellyfinMediaCardShape.Wide) 210.dp else 132.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
    ) {
        ArtworkBox(
            imageUrl = item.imageUrl ?: item.backdropUrl,
            title = item.title,
            wide = item.shape == JellyfinMediaCardShape.Wide,
            progress = null,
            onClick = onClick,
        )
        Text(item.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(item.year?.toString(), item.subtitle).joinToString(" · "), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FavoritesScreen(
    state: VantafynHomeUiState,
    onLoadFavorites: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
) {
    val grouped = state.favorites.groupBy { it.itemType?.ifBlank { "Other" } ?: "Other" }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My List", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                GlassAction("Refresh", onClick = onLoadFavorites)
            }
        }
        if (state.isFavoritesLoading) item { HomeLoadingShelf() }
        state.favoritesError?.let { item { VantafynErrorCard(it) } }
        if (!state.isFavoritesLoading && state.favorites.isEmpty() && state.favoritesError == null) {
            item { EmptyState("Your list is empty", "Add movies and shows from their detail pages.") }
        }
        grouped.toSortedMap().forEach { (type, itemsForType) ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                    Text(type.searchGroupLabel(), color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
                        items(itemsForType, key = { it.id }) { item ->
                            MediaItemCard(item = item, onClick = { onOpenMedia(item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(state: VantafynHomeUiState, onOpenUser: (java.util.UUID) -> Unit) {
    val overview = state.adminOverview
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item { ScreenTitle("Admin", overview?.serverName ?: state.server?.name ?: "Jellyfin Server") }
        if (state.isAdminLoading) item { VantafynLoadingIndicator("Loading admin dashboard") }
        state.adminError?.let { item { VantafynErrorCard(it) } }
        if (overview != null) {
            item {
                GlassPanel {
                    Text(overview.serverName ?: "Jellyfin Server", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(listOfNotNull(overview.serverVersion?.let { "Jellyfin $it" }, overview.operatingSystem).joinToString(" · "), color = VantafynColors.Muted)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), modifier = Modifier.fillMaxWidth()) {
                    AdminStatCard("Version", overview.serverVersion ?: "Unknown", Modifier.weight(1f))
                    AdminStatCard("Libraries", overview.libraryCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), modifier = Modifier.fillMaxWidth()) {
                    AdminStatCard("Sessions", overview.activeSessions.size.toString(), Modifier.weight(1f))
                    AdminStatCard("Users", overview.users.size.toString(), Modifier.weight(1f))
                }
            }
            item {
                GlassPanel {
                    Text("Statistics", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Total items: ${overview.totalItems?.toString() ?: "Unavailable"}", color = VantafynColors.Muted)
                    Text("Movies: ${overview.moviesCount?.toString() ?: "Unavailable"}", color = VantafynColors.Muted)
                    Text("Series: ${overview.seriesCount?.toString() ?: "Unavailable"}", color = VantafynColors.Muted)
                    Text("Episodes: ${overview.episodesCount?.toString() ?: "Unavailable"}", color = VantafynColors.Muted)
                    Text("Music: ${overview.musicCount?.toString() ?: "Unavailable"}", color = VantafynColors.Muted)
                    overview.unavailableStats.forEach { Text(it, color = VantafynColors.Muted.copy(alpha = 0.78f)) }
                }
            }
            item { AdminSessionsSection(overview.activeSessions) }
            item { AdminUsersSection(overview.users, onOpenUser) }
            item {
                GlassPanel {
                    Text("More / Server Tools", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    listOf("Activity Log", "Devices / Sessions", "Plugins", "Scheduled Tasks", "System Info", "Server Logs").forEach {
                        Text("$it · Coming soon", color = VantafynColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier) {
        Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(label, color = VantafynColors.Muted)
    }
}

@Composable
private fun AdminSessionsSection(sessions: List<dev.vantafyn.core.jellyfin.JellyfinAdminSession>) {
    GlassPanel {
        Text("Active Sessions", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (sessions.isEmpty()) {
            Text("No active sessions reported.", color = VantafynColors.Muted)
        }
        sessions.take(8).forEach { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .padding(VantafynSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(name = session.userName ?: "User", imageUrl = session.userImageUrl, modifier = Modifier.size(44.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.userName ?: "Unknown user", color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(session.client, session.deviceName, session.nowPlayingTitle?.let { "Playing $it" }, if (session.isTranscoding) "Transcoding" else "Direct/unknown").joinToString(" · "), color = VantafynColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AdminUsersSection(users: List<dev.vantafyn.core.jellyfin.JellyfinAdminUser>, onOpenUser: (java.util.UUID) -> Unit) {
    GlassPanel {
        Text("Users", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        users.take(10).forEach { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenUser(user.id) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(name = user.name, imageUrl = user.imageUrl, modifier = Modifier.size(42.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(if (user.isAdministrator) "Admin" else "User", if (user.isDisabled) "Disabled" else null, if (user.isHidden) "Hidden" else null, user.lastActivity?.let { "Active $it" }).joinToString(" · "),
                        color = VantafynColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsScreen(
    state: VantafynHomeUiState,
    onHomeLayout: () -> Unit,
    onPlaybackPreferences: () -> Unit,
    onToggleThemeMusic: () -> Unit,
    onSwitchUser: () -> Unit,
    onAddProfile: () -> Unit,
    onQuickConnect: () -> Unit,
    onLogout: () -> Unit,
    onSelectBackground: (VantafynAppBackground) -> Unit,
    onChangePassword: (String, String) -> Unit,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Text("Settings", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            ProfileDashboardCard(state)
        }
        item {
            GlassPanel {
                Text("Appearance", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Background", color = VantafynColors.Muted)
                BackgroundSelector(selected = state.selectedBackground, onSelect = onSelectBackground)
                PremiumToggleRow(
                    title = "Theme music on detail pages",
                    subtitle = "Plays Jellyfin theme songs when a detail page exposes one.",
                    checked = state.themeMusicEnabled,
                    onClick = onToggleThemeMusic,
                )
            }
        }
        item {
            GlassPanel {
                Text("Profile", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                SettingsRow("Switch User", "Choose another saved Jellyfin profile.", onSwitchUser, compact = true)
                SettingsRow("Add Profile", "Use ${state.server?.name ?: "this server"} with another user.", onAddProfile, compact = true)
                SettingsRow("Quick Connect", "Authorize Vantafyn from Jellyfin.", onQuickConnect, compact = true)
                SettingsRow("Log Out", "Remove this profile from this device.", onLogout, compact = true, destructive = true)
            }
        }
        item {
            GlassPanel {
                Text("Vantafyn", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                SettingsRow("Home Sections", "Preview and tune your home rows.", onHomeLayout, compact = true)
                SettingsRow("Playback Preferences", "Audio language, subtitle mode, and track memory.", onPlaybackPreferences, compact = true)
                SettingsRow("Change Password", "Update your current Jellyfin password.", { showPasswordDialog = true }, compact = true)
                SettingsRow("App version", "Vantafyn 0.1.0", {}, compact = true)
            }
        }
    }
    if (showPasswordDialog) {
        PasswordChangeDialog(
            title = "Change Password",
            requiresCurrent = true,
            onDismiss = { showPasswordDialog = false },
            onSubmit = { current, new ->
                showPasswordDialog = false
                onChangePassword(current, new)
            },
        )
    }
}

@Composable
private fun ProfileDashboardCard(state: VantafynHomeUiState) {
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = state.session?.user?.name ?: "Vantafyn User",
                imageUrl = state.savedProfiles.firstOrNull { it.jellyfinUserId == state.session?.user?.id }?.imageUrl,
                modifier = Modifier.size(78.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.session?.user?.name ?: "Vantafyn User", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.session?.user?.isAdministrator == true) {
                        SoftBadge("Admin")
                    }
                }
                Text(state.server?.name ?: "Jellyfin Server", color = VantafynColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.server?.url ?: "", color = VantafynColors.Muted.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            MiniStat("Libraries", state.libraries.size.toString(), Modifier.weight(1f))
            MiniStat("My List", state.favorites.size.takeIf { it > 0 }?.toString() ?: "Load", Modifier.weight(1f))
            MiniStat("Background", state.selectedBackground.label, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BackgroundSelector(selected: VantafynAppBackground, onSelect: (VantafynAppBackground) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(VantafynAppBackground.entries.toList(), key = { it.name }) { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) Color(0xFF7B8DFF).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF93A7FF) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(4.dp)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(option.drawableResId()),
                    contentDescription = option.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(999.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun PremiumToggleRow(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .clickable(onClick = onClick)
            .padding(VantafynSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) Color(0xFF7B8DFF).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.12f))
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
}

@Composable
private fun SoftBadge(text: String) {
    Text(
        text,
        color = Color(0xFFC8D2FF),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF7B8DFF).copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlaybackPreferencesScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onEdit: ((dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> dev.vantafyn.core.jellyfin.JellyfinUserPlaybackPreferences) -> Unit,
    onSave: () -> Unit,
) {
    val preferences = state.editablePlaybackPreferences
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                Text("Playback Preferences", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.isPlaybackPreferencesLoading) item { VantafynLoadingIndicator("Loading preferences") }
        state.playbackPreferencesError?.let { item { VantafynErrorCard(it) } }
        if (preferences != null) {
            item {
                GlassPanel {
                    Text("Languages", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    VantafynTextField(
                        value = preferences.audioLanguagePreference.orEmpty(),
                        onValueChange = { value -> onEdit { it.copy(audioLanguagePreference = value) } },
                        label = "Preferred audio language",
                        placeholder = "eng",
                    )
                    VantafynTextField(
                        value = preferences.subtitleLanguagePreference.orEmpty(),
                        onValueChange = { value -> onEdit { it.copy(subtitleLanguagePreference = value) } },
                        label = "Preferred subtitle language",
                        placeholder = "eng",
                    )
                }
            }
            item {
                GlassPanel {
                    Text("Subtitles", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                        items(listOf("Default", "Always", "OnlyForced", "None", "Smart")) { mode ->
                            SelectableChip(mode, preferences.subtitleMode.equals(mode, ignoreCase = true)) {
                                onEdit { it.copy(subtitleMode = mode) }
                            }
                        }
                    }
                    PremiumToggleRow("Remember subtitle selections", "Saved through Jellyfin user configuration.", preferences.rememberSubtitleSelections) {
                        onEdit { it.copy(rememberSubtitleSelections = !it.rememberSubtitleSelections) }
                    }
                }
            }
            item {
                GlassPanel {
                    Text("Audio", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    PremiumToggleRow("Play default audio track", "Let Jellyfin choose the default track first.", preferences.playDefaultAudioTrack) {
                        onEdit { it.copy(playDefaultAudioTrack = !it.playDefaultAudioTrack) }
                    }
                    PremiumToggleRow("Remember audio selections", "Saved through Jellyfin user configuration.", preferences.rememberAudioSelections) {
                        onEdit { it.copy(rememberAudioSelections = !it.rememberAudioSelections) }
                    }
                    PremiumToggleRow("Next episode autoplay", "Controls Jellyfin's user-level next episode setting.", preferences.enableNextEpisodeAutoPlay) {
                        onEdit { it.copy(enableNextEpisodeAutoPlay = !it.enableNextEpisodeAutoPlay) }
                    }
                }
            }
            item {
                VantafynButton(
                    if (state.isPlaybackPreferencesSaving) "Saving" else "Save Preferences",
                    onClick = onSave,
                    enabled = !state.isPlaybackPreferencesSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PasswordChangeDialog(
    title: String,
    requiresCurrent: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                if (requiresCurrent) {
                    VantafynTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = "Current password",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
                VantafynTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New password",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(current, newPassword) },
                enabled = newPassword.length >= 6 && (!requiresCurrent || current.isNotBlank()),
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AdminUserDetailDialog(
    state: VantafynHomeUiState,
    detail: dev.vantafyn.core.jellyfin.JellyfinAdminUserDetail,
    onDismiss: () -> Unit,
    onUpdate: (Boolean?, Boolean?, Boolean?, Boolean?, List<java.util.UUID>?) -> Unit,
    onResetPassword: (String) -> Unit,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail.user.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                state.adminUserError?.let { Text(it, color = Color(0xFFFFB5BE)) }
                PremiumToggleRow("Hidden from login", "Uses Jellyfin user policy.", detail.user.isHidden) {
                    onUpdate(!detail.user.isHidden, null, null, null, null)
                }
                PremiumToggleRow("Disabled", "Prevents this user from signing in.", detail.user.isDisabled) {
                    onUpdate(null, !detail.user.isDisabled, null, null, null)
                }
                PremiumToggleRow("Administrator", "Blocked for your current profile to prevent lockout.", detail.user.isAdministrator) {
                    onUpdate(null, null, !detail.user.isAdministrator, null, null)
                }
                PremiumToggleRow("Access all libraries", "Per-library selection can be expanded later from the returned folder ids.", detail.enableAllFolders) {
                    onUpdate(null, null, null, !detail.enableAllFolders, null)
                }
                Text("Library Access", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                state.libraries.forEach { library ->
                    val enabled = detail.enableAllFolders || library.id in detail.enabledFolderIds
                    PremiumToggleRow(
                        title = library.name,
                        subtitle = if (detail.enableAllFolders) "Included by All Libraries" else "Jellyfin folder access",
                        checked = enabled,
                    ) {
                        if (!detail.enableAllFolders) {
                            val updated = if (library.id in detail.enabledFolderIds) {
                                detail.enabledFolderIds - library.id
                            } else {
                                detail.enabledFolderIds + library.id
                            }
                            onUpdate(null, null, null, false, updated)
                        }
                    }
                }
                SettingsRow("Reset Password", "Set a new Jellyfin password for this user.", { showPasswordDialog = true }, compact = true)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
    if (showPasswordDialog) {
        PasswordChangeDialog(
            title = "Reset Password",
            requiresCurrent = false,
            onDismiss = { showPasswordDialog = false },
            onSubmit = { _, new ->
                showPasswordDialog = false
                onResetPassword(new)
            },
        )
    }
}

@Composable
private fun HomeLayoutScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onToggle: (HomeSectionType) -> Unit,
    onMove: (HomeSectionType, Int) -> Unit,
    onReset: () -> Unit,
    onAddSmartRow: (String) -> Unit,
    onRemoveSmartRow: (String) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                CompactBackButton(onClick = onBack)
                Text("Home Sections", color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        items(state.homeLayout.sortedBy { it.order }, key = { it.type.name }) { preference ->
            HomeSectionEditorCard(
                state = state,
                preference = preference,
                onToggle = onToggle,
                onMove = onMove,
                onCycleArtwork = onCycleArtwork,
                onCycleShape = onCycleShape,
                onCycleSize = onCycleSize,
                onCycleSpacing = onCycleSpacing,
            )
        }
        item {
            GlassPanel {
                Text("Add Smart Row", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Only rows backed by real Jellyfin metadata queries are available.", color = VantafynColors.Muted)
                supportedSmartRows.forEach { row ->
                    val selected = row in state.configuredSmartRows
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(row, color = VantafynColors.Ink, modifier = Modifier.weight(1f))
                        TinyAction(if (selected) "Remove" else "Add") {
                            if (selected) onRemoveSmartRow(row) else onAddSmartRow(row)
                        }
                    }
                }
            }
        }
        item {
            VantafynButton("Reset to default", onClick = onReset, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HomeSectionEditorCard(
    state: VantafynHomeUiState,
    preference: HomeSectionPreference,
    onToggle: (HomeSectionType) -> Unit,
    onMove: (HomeSectionType, Int) -> Unit,
    onCycleArtwork: (HomeSectionType) -> Unit,
    onCycleShape: (HomeSectionType) -> Unit,
    onCycleSize: (HomeSectionType) -> Unit,
    onCycleSpacing: (HomeSectionType) -> Unit,
) {
    GlassPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(preference.type.label, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(if (preference.visible) "Visible" else "Hidden", color = VantafynColors.Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                TinyAction(if (preference.visible) "Hide" else "Show") { onToggle(preference.type) }
                TinyAction("↑") { onMove(preference.type, -1) }
                TinyAction("↓") { onMove(preference.type, 1) }
            }
        }
        HomeSectionPreview(state, preference.type)
        if (preference.type != HomeSectionType.MediaBar) {
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs), modifier = Modifier.fillMaxWidth()) {
                TinyAction("Art ${preference.artworkType.shortLabel()}", Modifier.weight(1f)) { onCycleArtwork(preference.type) }
                TinyAction("Shape ${preference.cardShape.shortLabel()}", Modifier.weight(1f)) { onCycleShape(preference.type) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs), modifier = Modifier.fillMaxWidth()) {
                TinyAction("Size ${preference.cardSize.shortLabel()}", Modifier.weight(1f)) { onCycleSize(preference.type) }
                TinyAction("Space ${preference.spacing.shortLabel()}", Modifier.weight(1f)) { onCycleSpacing(preference.type) }
            }
        }
    }
}

@Composable
private fun HomeSectionPreview(state: VantafynHomeUiState, type: HomeSectionType) {
    val images = previewImagesFor(state, type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (images.isEmpty()) {
            Text("No preview data yet", color = VantafynColors.Muted, modifier = Modifier.padding(horizontal = 6.dp))
        } else {
            images.take(4).forEach { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VantafynColors.SurfaceHigh.copy(alpha = 0.46f)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun TinyAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        color = VantafynColors.Ink,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.075f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

private fun VantafynArtworkType.shortLabel(): String =
    when (this) {
        VantafynArtworkType.Auto -> "Auto"
        VantafynArtworkType.PrimaryPoster -> "Poster"
        VantafynArtworkType.Backdrop -> "Backdrop"
        VantafynArtworkType.Thumb -> "Thumb"
        VantafynArtworkType.Logo -> "Logo"
    }

private fun VantafynCardShape.shortLabel(): String =
    when (this) {
        VantafynCardShape.Rounded -> "Rounded"
        VantafynCardShape.Squircle -> "Squircle"
        VantafynCardShape.Soft -> "Soft"
        VantafynCardShape.Sharpish -> "Sharp"
    }

private fun VantafynCardSize.shortLabel(): String =
    when (this) {
        VantafynCardSize.Small -> "Small"
        VantafynCardSize.Medium -> "Medium"
        VantafynCardSize.Large -> "Large"
    }

private fun VantafynCardSpacing.shortLabel(): String =
    when (this) {
        VantafynCardSpacing.Compact -> "Compact"
        VantafynCardSpacing.Comfortable -> "Comfort"
        VantafynCardSpacing.Spacious -> "Spacious"
    }

@Composable
private fun MediaDetailScreen(
    state: VantafynHomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMedia: (java.util.UUID) -> Unit,
    onPlaybackComingSoon: () -> Unit,
    themeMusicEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
) {
    val detail = state.mediaDetail
    DetailThemeAudio(
        url = detail?.themeSongUrl,
        enabled = themeMusicEnabled,
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.lg),
    ) {
        if (state.isMediaDetailLoading) item {
            HomeRowInset {
                VantafynLoadingIndicator("Loading details")
            }
        }
        state.mediaDetailError?.let { item {
            HomeRowInset {
                VantafynErrorCard(it) { VantafynButton("Retry", onClick = onRetry) }
            }
        } }
        if (detail != null) {
            item {
                MediaDetailHero(
                    detail = detail,
                    onBack = onBack,
                    onPlaybackComingSoon = onPlaybackComingSoon,
                )
            }
            item {
                HomeRowInset {
                    DetailActionPanel(
                        detail = detail,
                        onPlaybackComingSoon = onPlaybackComingSoon,
                        onToggleFavorite = onToggleFavorite,
                        onTogglePlayed = onTogglePlayed,
                    )
                }
            }
            item {
                HomeRowInset {
                    DetailOverview(detail)
                }
            }
            if (detail.episodes.isNotEmpty()) {
                item {
                    HomeRowInset {
                        EpisodeSection(detail) { onPlaybackComingSoon() }
                    }
                }
            }
            if (detail.people.isNotEmpty()) {
                item {
                    HomeRowInset {
                        PeopleSection(detail, onPerson = onPlaybackComingSoon)
                    }
                }
            }
            if (detail.related.isNotEmpty()) {
                item {
                    HomeRowInset {
                        RelatedSection(detail, onOpenMedia)
                    }
                }
            }
            if (detail.externalLinks.isNotEmpty()) {
                item {
                    HomeRowInset {
                        ExternalLinksSection(detail, onOpen = onPlaybackComingSoon)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailThemeAudio(url: String?, enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(url, enabled) {
        if (!enabled || url.isNullOrBlank()) {
            onDispose { }
        } else {
            val player = ExoPlayer.Builder(context).build().apply {
                volume = 0.18f
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
            onDispose {
                player.volume = 0f
                player.stop()
                player.release()
            }
        }
    }
}

@Composable
private fun DetailActionPanel(
    detail: JellyfinMediaDetail,
    onPlaybackComingSoon: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        VantafynButton(detail.primaryActionLabel(), onClick = onPlaybackComingSoon, modifier = Modifier.fillMaxWidth())
        if (detail.streamInfo.isNotEmpty()) DetailChipRow(detail.streamInfo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            DetailAction(if (detail.isFavorite) "♥" else "♡", if (detail.isFavorite) "In My List" else "Add to My List", onToggleFavorite, Modifier.weight(1f))
            DetailAction("✓", if (detail.isPlayed) "Watched" else "Mark Watched", onTogglePlayed, Modifier.weight(1f))
            DetailAction("⋯", "More", onPlaybackComingSoon, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailAction(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.56f))
            .clickable(onClick = onClick)
            .padding(vertical = VantafynSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(label, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailOverview(detail: JellyfinMediaDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Overview", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            detail.overview?.takeIf { it.isNotBlank() } ?: "No overview provided by Jellyfin.",
            color = VantafynColors.Ink.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailChipRow(values: List<String>) {
    if (values.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        values.forEach { value ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(value, color = VantafynColors.Ink.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
        }
    }
}

@Composable
private fun EpisodeSection(detail: JellyfinMediaDetail, onEpisodeAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Episodes", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            detail.seasons.firstOrNull()?.let { Text(it.title, color = VantafynColors.Muted) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(detail.episodes, key = { it.id }) { episode ->
                Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                    ArtworkBox(
                        imageUrl = episode.imageUrl,
                        title = episode.title,
                        wide = true,
                        progress = episode.progress,
                        onClick = onEpisodeAction,
                        modifier = Modifier.fillMaxWidth().height(146.dp),
                    )
                    Text(episode.subtitle ?: "Episode", color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(episode.title, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                    episode.overview?.let { Text(it, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 3) }
                }
            }
        }
    }
}

@Composable
private fun PeopleSection(detail: JellyfinMediaDetail, onPerson: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Cast & Crew", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(detail.people, key = { it.id }) { person ->
                Column(modifier = Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF4F657D), Color(0xFF756A8A), Color(0xFF20252D))))
                            .clickable(onClick = onPerson),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (person.imageUrl != null) {
                            AsyncImage(model = person.imageUrl, contentDescription = person.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(initials(person.name), color = VantafynColors.Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(person.name, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyLarge, maxLines = 1, textAlign = TextAlign.Center)
                    Text(person.role ?: person.type.orEmpty(), color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RelatedSection(detail: JellyfinMediaDetail, onOpenMedia: (java.util.UUID) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("Related", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
            items(detail.related, key = { it.id }) { item ->
                MediaItemCard(item = item, onClick = { onOpenMedia(item.id) })
            }
        }
    }
}

@Composable
private fun ExternalLinksSection(detail: JellyfinMediaDetail, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text("External Links", color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
            items(detail.externalLinks, key = { it.url }) { link ->
                GlassAction(link.name, onClick = onOpen)
            }
        }
    }
}

@Composable
private fun MediaDetailHero(detail: JellyfinMediaDetail, onBack: () -> Unit, onPlaybackComingSoon: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(472.dp)
            .background(VantafynColors.SurfaceHigh),
    ) {
        AsyncImage(
            model = detail.backdropUrl ?: detail.imageUrl,
            contentDescription = detail.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xD8070A12),
                            0.22f to Color(0x44070A12),
                            0.58f to Color.Transparent,
                            0.82f to Color(0xDD070A12),
                            1.00f to Color(0xFF070A12),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xB8070A12), Color(0x44070A12), Color.Transparent))),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.xl, vertical = VantafynSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactBackButton(onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                FloatingCircleButton("♡", onPlaybackComingSoon)
                FloatingCircleButton("⋯", onPlaybackComingSoon)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = VantafynSpacing.xl, vertical = 34.dp),
            verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        ) {
            detail.logoUrl?.let {
                AsyncImage(model = it, contentDescription = detail.title, modifier = Modifier.fillMaxWidth(0.68f).height(92.dp), contentScale = ContentScale.Fit)
            } ?: Text(
                detail.title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailChipRow(
                listOfNotNull(
                    detail.year?.toString(),
                    detail.runtimeMinutes?.let { "${it}m" },
                    detail.officialRating,
                    detail.communityRating?.let { "★ ${"%.1f".format(it)}" },
                    detail.subtitle,
                ),
            )
            if (detail.genres.isNotEmpty()) {
                DetailChipRow(detail.genres.take(4))
            }
        }
    }
}

@Composable
private fun CompactBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(VantafynColors.Surface.copy(alpha = 0.66f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 2.35.dp.toPx()
            drawLine(
                color = VantafynColors.Ink,
                start = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 3.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = VantafynColors.Ink,
                start = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 9.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(11.5.dp.toPx(), 15.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = VantafynColors.Ink,
                start = androidx.compose.ui.geometry.Offset(5.5.dp.toPx(), 9.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 9.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun FloatingCircleButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(VantafynColors.Surface.copy(alpha = 0.62f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LibraryListCard(library: JellyfinLibrary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.58f))
            .clickable(onClick = onClick)
            .padding(VantafynSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(
            imageUrl = library.imageUrl,
            title = library.name,
            wide = true,
            progress = null,
            onClick = onClick,
            modifier = Modifier.width(112.dp).height(72.dp),
        )
        Column {
            Text(library.name, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(library.collectionType?.replaceFirstChar(Char::titlecase) ?: "Library", color = VantafynColors.Muted)
        }
    }
}

@Composable
private fun FilterChips(labels: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
        items(labels) { label -> GlassAction(label) }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    GlassPanel {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = VantafynColors.Muted)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    compact: Boolean = false,
    destructive: Boolean = false,
) {
    val textColor = if (destructive) Color(0xFFFFB5BE) else VantafynColors.Ink
    val modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(if (compact) 16.dp else 22.dp))
        .background(Color.White.copy(alpha = if (compact) 0.045f else 0.06f))
        .clickable(onClick = onClick)
        .padding(if (compact) VantafynSpacing.md else VantafynSpacing.lg)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = textColor, style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = VantafynColors.Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(VantafynColors.SurfaceHigh.copy(alpha = 0.58f))
            .padding(VantafynSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
        content = content,
    )
}

@Composable
private fun MobileBottomNav(
    selected: MobileDestination,
    onSelected: (MobileDestination) -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
) {
    val tabs = if (isAdmin) {
        listOf(
            MobileDestination.Home,
            MobileDestination.Libraries,
            MobileDestination.Search,
            MobileDestination.Favorites,
            MobileDestination.Admin,
            MobileDestination.Profile,
        )
    } else {
        listOf(
            MobileDestination.Home,
            MobileDestination.Libraries,
            MobileDestination.Search,
            MobileDestination.Favorites,
            MobileDestination.Profile,
        )
    }
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxWidth()
            .padding(horizontal = VantafynSpacing.md, vertical = VantafynSpacing.sm)
            .height(62.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(VantafynColors.Surface.copy(alpha = 0.82f))
            .padding(VantafynSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { destination ->
            val selectedTab = selected == destination || (selected == MobileDestination.HomeLayout && destination == MobileDestination.Profile)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedTab) VantafynColors.Primary.copy(alpha = 0.20f) else Color.Transparent)
                    .clickable { onSelected(destination) },
                contentAlignment = Alignment.Center,
            ) {
                MiniNavIcon(destination, selectedTab)
            }
        }
    }
}

@Composable
private fun MiniNavIcon(destination: MobileDestination, selected: Boolean) {
    val color = if (selected) VantafynColors.Ink else VantafynColors.Muted
    Canvas(modifier = Modifier.size(23.dp)) {
        val stroke = 2.35.dp.toPx()
        val outline = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (destination) {
            MobileDestination.Home -> {
                val path = Path().apply {
                    moveTo(3.5.dp.toPx(), 11.dp.toPx())
                    lineTo(11.5.dp.toPx(), 4.dp.toPx())
                    lineTo(19.5.dp.toPx(), 11.dp.toPx())
                    lineTo(19.5.dp.toPx(), 20.dp.toPx())
                    lineTo(14.3.dp.toPx(), 20.dp.toPx())
                    lineTo(14.3.dp.toPx(), 14.3.dp.toPx())
                    lineTo(8.7.dp.toPx(), 14.3.dp.toPx())
                    lineTo(8.7.dp.toPx(), 20.dp.toPx())
                    lineTo(3.5.dp.toPx(), 20.dp.toPx())
                    close()
                }
                drawPath(path, color, style = outline)
            }
            MobileDestination.Libraries -> {
                listOf(
                    androidx.compose.ui.geometry.Offset(3.dp.toPx(), 3.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(13.dp.toPx(), 3.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(3.dp.toPx(), 13.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(13.dp.toPx(), 13.dp.toPx()),
                ).forEach { offset ->
                    drawRoundRect(
                        color,
                        topLeft = offset,
                        size = androidx.compose.ui.geometry.Size(7.dp.toPx(), 7.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                        style = outline,
                    )
                }
            }
            MobileDestination.Search -> {
                drawCircle(color, radius = 6.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()), style = outline)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(14.dp.toPx(), 14.dp.toPx()), end = androidx.compose.ui.geometry.Offset(20.dp.toPx(), 20.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            MobileDestination.Favorites -> {
                val path = Path().apply {
                    moveTo(11.5.dp.toPx(), 20.dp.toPx())
                    cubicTo(4.dp.toPx(), 15.dp.toPx(), 3.dp.toPx(), 10.dp.toPx(), 5.7.dp.toPx(), 7.dp.toPx())
                    cubicTo(8.dp.toPx(), 4.5.dp.toPx(), 10.5.dp.toPx(), 6.dp.toPx(), 11.5.dp.toPx(), 8.dp.toPx())
                    cubicTo(12.5.dp.toPx(), 6.dp.toPx(), 15.dp.toPx(), 4.5.dp.toPx(), 17.3.dp.toPx(), 7.dp.toPx())
                    cubicTo(20.dp.toPx(), 10.dp.toPx(), 19.dp.toPx(), 15.dp.toPx(), 11.5.dp.toPx(), 20.dp.toPx())
                }
                drawPath(path, color, style = outline)
            }
            MobileDestination.Admin -> {
                val path = Path().apply {
                    moveTo(11.5.dp.toPx(), 3.dp.toPx())
                    lineTo(18.5.dp.toPx(), 6.dp.toPx())
                    lineTo(18.5.dp.toPx(), 11.4.dp.toPx())
                    cubicTo(18.5.dp.toPx(), 16.dp.toPx(), 15.4.dp.toPx(), 19.dp.toPx(), 11.5.dp.toPx(), 21.dp.toPx())
                    cubicTo(7.6.dp.toPx(), 19.dp.toPx(), 4.5.dp.toPx(), 16.dp.toPx(), 4.5.dp.toPx(), 11.4.dp.toPx())
                    lineTo(4.5.dp.toPx(), 6.dp.toPx())
                    close()
                }
                drawPath(path, color, style = outline)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(8.3.dp.toPx(), 11.5.dp.toPx()), end = androidx.compose.ui.geometry.Offset(10.6.dp.toPx(), 14.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(10.6.dp.toPx(), 14.dp.toPx()), end = androidx.compose.ui.geometry.Offset(15.dp.toPx(), 9.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            MobileDestination.Profile -> {
                drawCircle(color, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f), style = outline)
                repeat(8) { index ->
                    val angle = (index * 45.0) * kotlin.math.PI / 180.0
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val start = androidx.compose.ui.geometry.Offset(
                        x = cx + kotlin.math.cos(angle).toFloat() * 7.dp.toPx(),
                        y = cy + kotlin.math.sin(angle).toFloat() * 7.dp.toPx(),
                    )
                    val end = androidx.compose.ui.geometry.Offset(
                        x = cx + kotlin.math.cos(angle).toFloat() * 9.5.dp.toPx(),
                        y = cy + kotlin.math.sin(angle).toFloat() * 9.5.dp.toPx(),
                    )
                    drawLine(color, start = start, end = end, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                drawCircle(color, radius = 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f), style = outline)
            }
            else -> {
                drawCircle(color, radius = 4.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, 7.5.dp.toPx()), style = outline)
                drawRoundRect(
                    color,
                    topLeft = androidx.compose.ui.geometry.Offset(5.dp.toPx(), 14.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(13.dp.toPx(), 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                    style = outline,
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingsDialog(
    state: VantafynHomeUiState,
    onDismiss: () -> Unit,
    onSwitchUser: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onSwitchUser()
            }) {
                Text("Switch User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(state.session?.user?.name ?: "Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                Text(state.server?.name ?: "Jellyfin Server")
                Text(state.server?.url ?: "", color = VantafynColors.Muted)
                Text("Add Profile from the profile picker.")
                Text("App version 0.1.0")
            }
        },
    )
}

@Composable
private fun LibraryRow(
    title: String,
    libraries: List<JellyfinLibrary>,
    tv: Boolean,
    onOpenLibrary: (JellyfinLibrary) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(title, color = VantafynColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp)) {
            items(libraries) { library ->
                Box(modifier = Modifier.clickable { onOpenLibrary(library) }) {
                    PosterCard(title = library.name, spec = if (tv) TvPosterSpec else MobilePosterSpec)
                }
            }
        }
    }
}

@Composable
private fun CenterPane(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val widthFraction = if (maxWidth > 700.dp) 0.56f else 1f
        Column(
            modifier = Modifier.fillMaxWidth(widthFraction),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
private fun StatusBlock(state: VantafynHomeUiState) {
    if (state.isLoading) {
        Spacer(Modifier.height(VantafynSpacing.md))
        VantafynLoadingIndicator("Please wait")
    }
    state.errorMessage?.let {
        Spacer(Modifier.height(VantafynSpacing.md))
        VantafynErrorCard(it)
    }
}

private fun initials(name: String): String =
    name
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "V" }

private fun VantafynAppBackground.drawableResId(): Int =
    when (this) {
        VantafynAppBackground.Nebula -> CoreUiR.drawable.vantafyn_onboarding_background
        VantafynAppBackground.Background1 -> CoreUiR.drawable.vantafyn_background_1
        VantafynAppBackground.Background2 -> CoreUiR.drawable.vantafyn_background_2
        VantafynAppBackground.Background3 -> CoreUiR.drawable.vantafyn_background_3
        VantafynAppBackground.Background4 -> CoreUiR.drawable.vantafyn_background_4
    }

private fun String.searchGroupLabel(): String =
    when (lowercase()) {
        "movie" -> "Movies"
        "series" -> "TV Shows"
        "episode" -> "Episodes"
        "boxset" -> "Collections"
        "audio", "musicalbum", "musicartist" -> "Music"
        "book" -> "Books"
        "livetvchannel", "livetvprogram" -> "Live TV"
        else -> replaceFirstChar(Char::titlecase)
    }

private fun JellyfinMediaDetail.primaryActionLabel(): String {
    val watchedProgress = progress
    return when {
        watchedProgress != null && watchedProgress > 0.05f -> "Resume"
        itemType.equals("Book", ignoreCase = true) -> "Open"
        itemType.equals("Episode", ignoreCase = true) && subtitle != null -> "Play $subtitle"
        else -> "Play"
    }
}
