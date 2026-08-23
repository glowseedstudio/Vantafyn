package dev.vantafyn.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.R
import dev.vantafyn.core.ui.VantafynGlassCard
import dev.vantafyn.core.ui.VantafynTextField
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.core.ui.VantafynSpacing

@Composable
fun DiscoverVantafynScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    onDeepLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionPreference()
    var revealProgress by remember { mutableFloatStateOf(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            kotlinx.coroutines.delay(60L)
            val anim = androidx.compose.animation.core.Animatable(0f)
            anim.animateTo(1f, animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)) {
                revealProgress = value
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val allFeatures = remember { discoverFeatureGuide() }
    val visibleFeatures = remember(isAdmin, allFeatures) {
        allFeatures.filter { !it.adminOnly || isAdmin }
    }
    val filteredFeatures = remember(searchQuery, visibleFeatures) {
        if (searchQuery.isBlank()) visibleFeatures
        else {
            val q = searchQuery.lowercase()
            visibleFeatures.filter { f ->
                f.title.lowercase().contains(q) ||
                    f.shortDescription.lowercase().contains(q) ||
                    f.category.label.lowercase().contains(q)
            }
        }
    }
    val categories = remember(filteredFeatures) {
        filteredFeatures.map { it.category }.distinct().sortedBy { it.order }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = revealProgress
                    translationY = (1f - revealProgress) * size.height / 12f
                },
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                DiscoverHeader(onBack = onBack)
            }
            item {
                DiscoverSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" },
                )
            }
            itemsIndexed(categories) { _, category ->
                val catFeatures = filteredFeatures.filter { it.category == category }
                DiscoverCategorySection(
                    category = category,
                    features = catFeatures,
                    expandedId = expandedId,
                    onExpandToggle = { id -> expandedId = if (expandedId == id) null else id },
                    onDeepLink = onDeepLink,
                )
            }
        }
    }
}

@Composable
private fun DiscoverHeader(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        CompactBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp, top = 16.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vantafyn_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Discover Vantafyn",
                color = VantafynColors.Ink,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Explore features, shortcuts and ways to make Vantafyn yours.",
                color = VantafynColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DiscoverSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        VantafynTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Search features",
            placeholder = "Feature, shortcut\u2026",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        if (query.isNotBlank()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .padding(end = 4.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = VantafynColors.Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DiscoverCategorySection(
    category: DiscoverCategory,
    features: List<DiscoverFeature>,
    expandedId: String?,
    onExpandToggle: (String) -> Unit,
    onDeepLink: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            category.label,
            color = VantafynColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        features.forEach { feature ->
            DiscoverFeatureCard(
                feature = feature,
                isExpanded = expandedId == feature.id,
                onExpandToggle = { onExpandToggle(feature.id) },
                onDeepLink = onDeepLink,
            )
        }
    }
}

@Composable
private fun DiscoverFeatureCard(
    feature: DiscoverFeature,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDeepLink: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
        VantafynGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() },
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column(modifier = Modifier.padding(VantafynSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VantafynSpacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            feature.icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = feature.title,
                                color = VantafynColors.Ink,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (feature.isNew) {
                                SoftBadge("NEW")
                            }
                            if (feature.adminOnly) {
                                SoftBadge("Admin", color = Color(0xFFFFB5BE), background = Color(0xFFFFB5BE).copy(alpha = 0.12f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            feature.shortDescription,
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + expandVertically(tween(350, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
                ) {
                    Column(modifier = Modifier.padding(top = VantafynSpacing.md)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.08f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                        Spacer(Modifier.height(VantafynSpacing.md))
                        Text(
                            feature.detailedDescription,
                            color = VantafynColors.Muted,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                        )
                        if (feature.steps.isNotEmpty()) {
                            Spacer(Modifier.height(VantafynSpacing.sm))
                            feature.steps.forEachIndexed { stepIdx, step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(VantafynGradients.accentHorizontal()),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${stepIdx + 1}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Text(step, color = VantafynColors.Ink, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (feature.deepLinkAction != null) {
                            Spacer(Modifier.height(VantafynSpacing.sm))
                            TextButton(
                                onClick = { onDeepLink(feature.deepLinkAction) },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(
                                    when (feature.deepLinkAction) {
                                        "open_settings" -> "Open Settings"
                                        "open_playback_preferences" -> "Open Playback Preferences"
                                        "open_home_layout" -> "Open Home Layout"
                                        "open_watch_party" -> "Open Watch Party"
                                        "open_social" -> "Open Social Hub"
                                        "open_achievements" -> "Open Achievements"
                                        "open_admin" -> "Open Admin"
                                        else -> "Open"
                                    },
                                    color = VantafynColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftBadge(
    text: String,
    color: Color = Color(0xFFC8D2FF),
    background: Color = Color(0xFF7B8DFF).copy(alpha = 0.16f),
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}
