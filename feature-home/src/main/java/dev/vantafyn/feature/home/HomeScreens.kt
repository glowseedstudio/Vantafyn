package dev.vantafyn.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.vantafyn.core.jellyfin.JellyfinLibrary
import dev.vantafyn.core.ui.MobilePosterSpec
import dev.vantafyn.core.ui.PosterCard
import dev.vantafyn.core.ui.TvPosterSpec
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynSpacing
import dev.vantafyn.feature.home.auth.VantafynHomeUiState
import dev.vantafyn.feature.home.auth.VantafynHomeViewModel
import dev.vantafyn.feature.home.auth.VantafynSetupStep

@Composable
fun VantafynAppContent(
    tv: Boolean,
    modifier: Modifier = Modifier,
    viewModel: VantafynHomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (state.step) {
        VantafynSetupStep.Splash -> SplashScreen()
        VantafynSetupStep.Server -> ServerAddressScreen(
            state = state,
            onServerUrlChanged = viewModel::onServerUrlChanged,
            onConnect = viewModel::connectToServer,
            modifier = modifier,
        )
        VantafynSetupStep.Login -> LoginScreen(
            state = state,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onLogin = viewModel::login,
            modifier = modifier,
        )
        VantafynSetupStep.Home -> HomeScreen(
            state = state,
            tv = tv,
            onRetry = viewModel::retryLibraries,
            onLogout = viewModel::logout,
            modifier = modifier,
        )
    }
}

@Composable
fun SplashScreen(message: String = "Connecting to your library") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VantafynSpacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Vantafyn",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.sm))
        Text(
            text = message,
            color = VantafynColors.Muted,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ServerAddressScreen(
    state: VantafynHomeUiState,
    onServerUrlChanged: (String) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SetupPane(modifier = modifier) {
        Text(
            text = "Connect to Jellyfin",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server address") },
            singleLine = true,
            placeholder = { Text("http://192.168.1.29:8096") },
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        StatusText(state)
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        Button(
            onClick = onConnect,
            enabled = !state.isLoading && state.serverUrl.isNotBlank(),
        ) {
            Text(if (state.isLoading) "Connecting" else "Continue")
        }
    }
}

@Composable
private fun LoginScreen(
    state: VantafynHomeUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SetupPane(modifier = modifier) {
        Text(
            text = state.server?.name ?: "Sign in",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.xs))
        Text(
            text = state.server?.url ?: state.serverUrl,
            color = VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.sm))
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        StatusText(state)
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        Button(
            onClick = onLogin,
            enabled = !state.isLoading && state.username.isNotBlank(),
        ) {
            Text(if (state.isLoading) "Signing in" else "Sign in")
        }
    }
}

@Composable
private fun SetupPane(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(VantafynSpacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun StatusText(state: VantafynHomeUiState) {
    if (state.isLoading) {
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
            CircularProgressIndicator()
            Text(
                text = "Please wait",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    state.errorMessage?.let { message ->
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        Text(
            text = message,
            color = VantafynColors.Gold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HomeScreen(
    state: VantafynHomeUiState,
    tv: Boolean,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = VantafynSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(if (tv) VantafynSpacing.xl else VantafynSpacing.lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                Text(
                    text = state.server?.name ?: "Vantafyn",
                    color = VantafynColors.Ink,
                    style = if (tv) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Signed in as ${state.session?.user?.name.orEmpty()}",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        if (state.isLibrariesLoading) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading libraries",
                        color = VantafynColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        state.errorMessage?.let { message ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                    Text(
                        text = message,
                        color = VantafynColors.Gold,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
        if (!state.isLibrariesLoading && state.libraries.isEmpty() && state.errorMessage == null) {
            item {
                Text(
                    text = "No libraries were returned for this user.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (state.libraries.isNotEmpty()) {
            item {
                LibraryRow(
                    title = "Libraries",
                    libraries = state.libraries,
                    tv = tv,
                )
            }
        }
        items(state.libraries) { library ->
            LibraryRow(
                title = library.name,
                libraries = listOf(library),
                tv = tv,
            )
        }
        item {
            OutlinedButton(onClick = onLogout) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun LibraryRow(
    title: String,
    libraries: List<JellyfinLibrary>,
    tv: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Text(
            text = title,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp)) {
            items(libraries) { library ->
                PosterCard(
                    title = library.name,
                    spec = if (tv) TvPosterSpec else MobilePosterSpec,
                )
            }
        }
    }
}
