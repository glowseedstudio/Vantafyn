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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.vantafyn.core.ui.MobilePosterSpec
import dev.vantafyn.core.ui.PosterCard
import dev.vantafyn.core.ui.TvPosterSpec
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynSpacing

@Composable
fun SplashScreen(message: String = "Preparing your library") {
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
fun ServerAddressScreen(onContinue: () -> Unit) {
    var server by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VantafynSpacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Connect to Jellyfin",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server address") },
            singleLine = true,
            placeholder = { Text("https://media.example.com") },
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        Button(onClick = onContinue) {
            Text("Continue")
        }
    }
}

@Composable
fun LoginPlaceholderScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VantafynSpacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Sign in",
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.sm))
        Text(
            text = "Authentication will use the shared Jellyfin session core.",
            color = VantafynColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(VantafynSpacing.md))
        Button(onClick = onContinue) {
            Text("Open preview home")
        }
    }
}

@Composable
fun TvHomePlaceholder(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = VantafynSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xl),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.sm)) {
                Text(
                    text = "Vantafyn",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = "Continue watching, recent additions, and curated library rows.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        items(HomeRows) { row ->
            MediaRow(title = row.title, items = row.items, tv = true)
        }
    }
}

@Composable
fun MobileHomePlaceholder(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(VantafynSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xl),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.xs)) {
                Text(
                    text = "Vantafyn",
                    color = VantafynColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Your Jellyfin library, shaped for phone browsing.",
                    color = VantafynColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(HomeRows) { row ->
            MediaRow(title = row.title, items = row.items, tv = false)
        }
    }
}

@Composable
private fun MediaRow(
    title: String,
    items: List<String>,
    tv: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VantafynSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp)) {
            items(items) { item ->
                PosterCard(
                    title = item,
                    spec = if (tv) TvPosterSpec else MobilePosterSpec,
                )
            }
        }
    }
}

private data class HomeRow(
    val title: String,
    val items: List<String>,
)

private val HomeRows = listOf(
    HomeRow("Continue Watching", listOf("Moon Garden", "Harbor Nine", "Northline", "Old River")),
    HomeRow("Recently Added Movies", listOf("Signal House", "Stillwater", "The Long Room", "Afterlight")),
    HomeRow("Next Up", listOf("The Archive", "Sunday Road", "Blue Hour", "Small Fires")),
)
