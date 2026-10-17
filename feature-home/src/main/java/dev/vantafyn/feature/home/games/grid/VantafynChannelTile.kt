package dev.vantafyn.feature.home.games.grid

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CatchingPokemon
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import dev.vantafyn.core.jellyfin.GamePlayTracker
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vantafyn.core.ui.VantafynColors

/**
 * Visual channel tile component rendering different content types (Games, Widgets, Systems, Empty Slots)
 * with tactile 3D perspective tilt, specular reflections, and Wii-channel inspired framing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VantafynChannelTile(
    item: ChannelTileItem,
    isFocused: Boolean = false,
    isWiggling: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cornerShape = RoundedCornerShape(16.dp)

    // Pulsing animation for active hero widgets
    val infiniteTransition = rememberInfiniteTransition(label = "tilePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    // Wiggle / wobble animation for edit mode (Wii / iOS style)
    val wiggleAngle by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 110 + (Math.abs(item.id.hashCode()) % 35),
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wiggleAngle",
    )
    val wiggleY by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 125 + ((Math.abs(item.id.hashCode()) * 3) % 30),
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wiggleY",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (isWiggling && item.type != GridTileType.EMPTY_CHANNEL) {
                    rotationZ = wiggleAngle
                    translationY = wiggleY * density
                }
            }
            .tactile3DTilt(isFocused = isFocused)
            .clip(cornerShape)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF9D00FF),
                            Color(0xFFFF2A85),
                        )
                    )
                } else if (item.type == GridTileType.EMPTY_CHANNEL) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f),
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.06f),
                        )
                    )
                },
                shape = cornerShape,
            )
            .then(
                if (item.type == GridTileType.EMPTY_CHANNEL) {
                    Modifier.background(
                        Brush.verticalGradient(
                            if (isFocused) {
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.25f),
                                    Color(0xFF9D00FF).copy(alpha = 0.18f),
                                )
                            } else {
                                listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.12f),
                                    Color(0xFFE2EBF4).copy(alpha = 0.05f),
                                )
                            }
                        )
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            if (isFocused) {
                                listOf(
                                    Color(0xFF2B2144).copy(alpha = 0.98f),
                                    Color(0xFF151324).copy(alpha = 0.98f),
                                )
                            } else {
                                listOf(
                                    Color(0xFF1E1E28).copy(alpha = 0.95f),
                                    Color(0xFF12121C).copy(alpha = 0.95f),
                                )
                            }
                        )
                    )
                }
            ),
    ) {
        when (item.type) {
            GridTileType.EMPTY_CHANNEL -> {
                EmptyChannelContent(isFocused = isFocused)
            }
            GridTileType.CONTINUE_PLAYING -> {
                ContinuePlayingChannelContent(item = item, pulseAlpha = pulseAlpha, isFocused = isFocused)
            }
            GridTileType.POKEMON_VAULT -> {
                PokemonVaultChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.GAME -> {
                GameChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.SYSTEM -> {
                SystemChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.CLOUD_SAVES -> {
                CloudSavesChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.PROFILE_STATS -> {
                ProfileStatsChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.SETTINGS -> {
                SettingsChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.LIBRARY -> {
                LibraryChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.MAIN_HOME -> {
                MainHomeChannelContent(item = item, isFocused = isFocused)
            }
            GridTileType.SEARCH -> {
                SearchChannelContent(item = item, isFocused = isFocused)
            }
        }
    }
}

@Composable
private fun FloatingWatermarkIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "floatingWatermark_$seed")
    val durX = remember(seed) { 6800 + (Math.abs(seed) % 2400) }
    val durY = remember(seed) { 8200 + ((Math.abs(seed) * 3) % 2600) }
    val durRot = remember(seed) { 9600 + ((Math.abs(seed) * 7) % 3200) }

    val transX by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durX, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wmX",
    )
    val transY by transition.animateFloat(
        initialValue = -5.5f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durY, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wmY",
    )
    val rotZ by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durRot, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wmRot",
    )

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationX = transX * density
                translationY = transY * density
                rotationZ = rotZ
            },
    )
}

@Composable
private fun EmptyChannelContent(isFocused: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Debossed recessed socket inner shadow rim - soft, translucent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.06f),
                    )
                ),
                shape = RoundedCornerShape(16.dp),
            )
        )
    }
}

@Composable
private fun ContinuePlayingChannelContent(
    item: ChannelTileItem,
    pulseAlpha: Float,
    isFocused: Boolean,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveBoxart = remember(item.id, item.imageUrl, item.targetKey) {
        val targetId = item.targetKey ?: item.id.removePrefix("game_")
        val local = context.getSharedPreferences("vantafyn_retro_settings", android.content.Context.MODE_PRIVATE)
            .getString("boxart_$targetId", null)
        dev.vantafyn.core.jellyfin.GameBoxartScraper.convertToCdnUrl(local ?: item.imageUrl)
    }

    val isLargeSpan = (item.span.columns >= 2 && item.span.rows >= 2) || item.span == GridTileSpan.SPAN_4X2
    val kenTransition = rememberInfiniteTransition(label = "kenCP_${item.id}")
    val kenScale by if (isLargeSpan) {
        kenTransition.animateFloat(
            initialValue = 1.02f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenScale",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val kenTranslateX by if (isLargeSpan) {
        kenTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenTranslateX",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val kenTranslateY by if (isLargeSpan) {
        kenTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(11000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenTranslateY",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!effectiveBoxart.isNullOrBlank()) {
            AsyncImage(
                model = effectiveBoxart,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenScale
                        scaleY = kenScale
                        translationX = kenTranslateX * density
                        translationY = kenTranslateY * density
                    },
            )
            // Premium dark horizontal & vertical gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0F0F17).copy(alpha = 0.94f),
                                Color(0xFF0F0F17).copy(alpha = 0.75f),
                                Color(0xFF0F0F17).copy(alpha = 0.25f),
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.18f),
                                Color(0xFF9D00FF).copy(alpha = 0.18f),
                                Color(0xFF13131D),
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Badge: "CONTINUE PLAYING"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00E5FF).copy(alpha = pulseAlpha), CircleShape)
                )
                Text(
                    text = "CONTINUE PLAYING",
                    color = Color(0xFF8FE7FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }

            // Title & Quick Action
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.subtitle ?: item.title,
                    color = Color.White,
                    fontSize = if (item.span.columns >= 4) 18.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "RESUME",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonVaultChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isOneRow = item.span.rows <= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFFFF2A85).copy(alpha = 0.50f),
                            Color(0xFF9D00FF).copy(alpha = 0.42f),
                            Color(0xFF261434),
                        )
                    } else {
                        listOf(
                            Color(0xFFFF2A85).copy(alpha = 0.28f),
                            Color(0xFF9D00FF).copy(alpha = 0.22f),
                            Color(0xFF13131D),
                        )
                    }
                )
            ),
    ) {
        // Floating organic watermark icon
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.CatchingPokemon,
            tint = Color(0xFFFF2A85).copy(alpha = if (item.span.columns >= 2) 0.22f else 0.28f),
            size = if (isLarge) 82.dp else if (isOneRow) 38.dp else 52.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isOneRow) 6.dp else 10.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = "Pokémon Vault",
                color = Color.White,
                fontSize = if (isLarge) 15.sp else if (item.span.columns >= 2) 11.5.sp else 10.sp,
                lineHeight = if (isOneRow) 13.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.span.columns >= 2 || !isOneRow) {
                Text(
                    text = "Cloud Saves & Boxes",
                    color = VantafynColors.Muted,
                    fontSize = if (isOneRow) 9.sp else 11.sp,
                    lineHeight = if (isOneRow) 11.sp else 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GameChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveBoxart = remember(item.id, item.imageUrl, item.targetKey) {
        val targetId = item.targetKey ?: item.id.removePrefix("game_")
        val local = context.getSharedPreferences("vantafyn_retro_settings", android.content.Context.MODE_PRIVATE)
            .getString("boxart_$targetId", null)
        dev.vantafyn.core.jellyfin.GameBoxartScraper.convertToCdnUrl(local ?: item.imageUrl)
    }

    val isLargeSpan = (item.span.columns >= 2 && item.span.rows >= 2) || item.span == GridTileSpan.SPAN_4X2
    val kenTransition = rememberInfiniteTransition(label = "kenGame_${item.id}")
    val kenScale by if (isLargeSpan) {
        kenTransition.animateFloat(
            initialValue = 1.02f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenScale",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val kenTranslateX by if (isLargeSpan) {
        kenTransition.animateFloat(
            initialValue = -12f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenTranslateX",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val kenTranslateY by if (isLargeSpan) {
        kenTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(11000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "kenTranslateY",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!effectiveBoxart.isNullOrBlank()) {
            AsyncImage(
                model = effectiveBoxart,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenScale
                        scaleY = kenScale
                        translationX = kenTranslateX * density
                        translationY = kenTranslateY * density
                    },
            )
            // Bottom gradient overlay for title legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF0F0F17).copy(alpha = 0.55f),
                                Color(0xFF0F0F17).copy(alpha = 0.95f),
                            ),
                            startY = 60f,
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E1E28),
                                Color(0xFF14141E),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = VantafynColors.Muted,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // Title and system info
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Optional system tag in top right
            if (!item.subtitle.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.subtitle.uppercase(),
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            Text(
                text = item.title,
                color = Color.White,
                fontSize = if (item.span.columns >= 2) 13.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SystemChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val is3ds = item.id.contains("3ds", ignoreCase = true) ||
        (item.targetKey?.contains("3ds", ignoreCase = true) == true) ||
        item.title.contains("3ds", ignoreCase = true)

    val resolvedLogoUrl = remember(item.id, item.targetKey, item.title, item.imageUrl) {
        val sysId = item.targetKey ?: item.id.removePrefix("system_")
        dev.vantafyn.core.jellyfin.GameSystem(
            id = sysId,
            name = item.title,
            core = "",
            logoUrl = item.imageUrl,
        ).officialLogoUrl
    }

    val isOneRow = item.span.rows <= 1

    val seed = item.id.hashCode()
    val transition = rememberInfiniteTransition(label = "sysFloat_$seed")
    val durX = remember(seed) { 6800 + (Math.abs(seed) % 2400) }
    val durY = remember(seed) { 8200 + ((Math.abs(seed) * 3) % 2600) }
    val durRot = remember(seed) { 9600 + ((Math.abs(seed) * 7) % 3200) }

    val transX by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durX, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sysX",
    )
    val transY by transition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durY, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sysY",
    )
    val rotZ by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durRot, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sysRot",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF9D00FF).copy(alpha = 0.52f),
                            Color(0xFF28133C),
                        )
                    } else {
                        listOf(
                            Color(0xFF9D00FF).copy(alpha = 0.25f),
                            Color(0xFF12121B),
                        )
                    }
                )
            )
            .padding(horizontal = 6.dp, vertical = if (isOneRow) 4.dp else 6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Centered prominent logo with smooth organic float
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = transX * density
                        translationY = transY * density
                        rotationZ = rotZ
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (is3ds) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(dev.vantafyn.feature.home.R.drawable.system_logo_3ds),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight(if (isOneRow) 0.85f else 0.72f)
                            .fillMaxWidth(0.9f),
                    )
                } else if (resolvedLogoUrl.isNotBlank()) {
                    AsyncImage(
                        model = resolvedLogoUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight(if (isOneRow) 0.82f else 0.70f)
                            .fillMaxWidth(0.85f),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Dns,
                        contentDescription = item.title,
                        tint = Color(0xFFD180FF),
                        modifier = Modifier.size(if (isOneRow) 20.dp else 28.dp),
                    )
                }
            }

            // Game count at the bottom only (no console title in middle)
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    color = VantafynColors.Muted,
                    fontSize = if (isOneRow) 9.5.sp else 11.sp,
                    lineHeight = if (isOneRow) 11.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CloudSavesChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isOneRow = item.span.rows <= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF00E676).copy(alpha = 0.40f),
                            Color(0xFF0B291A),
                        )
                    } else {
                        listOf(
                            Color(0xFF00E676).copy(alpha = 0.18f),
                            Color(0xFF12121B),
                        )
                    }
                )
            ),
    ) {
        // Floating organic watermark icon
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.CloudDone,
            tint = Color(0xFF00E676).copy(alpha = if (isFocused) 0.35f else 0.24f),
            size = if (isLarge) 80.dp else if (isOneRow) 38.dp else 50.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isOneRow) 5.dp else 10.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = "Cloud Saves",
                color = Color.White,
                fontSize = if (isLarge) 14.sp else if (item.span.columns >= 2) 11.5.sp else 10.sp,
                lineHeight = if (isOneRow) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.span.columns >= 2 || !isOneRow) {
                Text(
                    text = "Active & Synced",
                    color = Color(0xFF81C784),
                    fontSize = if (isOneRow) 8.5.sp else 10.sp,
                    lineHeight = if (isOneRow) 10.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val context = LocalContext.current
    val tracker = remember(context) { GamePlayTracker(context) }
    val totalPlayTimeMs = remember(tracker) { tracker.getTotalPlayTimeMs() }
    val formattedPlaytime = remember(totalPlayTimeMs) { GamePlayTracker.formatPlayTime(totalPlayTimeMs) }
    val recentGames = remember(tracker) { tracker.getRecentGames(30) }
    val gamesPlayedCount = recentGames.size
    val topGame = remember(recentGames) { recentGames.maxByOrNull { it.playTimeMs } ?: recentGames.firstOrNull() }

    val retroPrefs = remember(context) {
        context.getSharedPreferences("vantafyn_retro_settings", android.content.Context.MODE_PRIVATE)
    }
    val userName = remember(retroPrefs) {
        retroPrefs.getString("user_profile_name", null)?.ifBlank { null }
            ?: retroPrefs.getString("offline_user_name", null)?.ifBlank { null }
            ?: "Player"
    }
    val userAvatarUrl = remember(retroPrefs) {
        retroPrefs.getString("user_profile_avatar", null)?.ifBlank { null }
            ?: retroPrefs.getString("offline_user_avatar", null)?.ifBlank { null }
    }

    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isWide = item.span.columns >= 2 && item.span.rows == 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF0F2B48),
                            Color(0xFF2C164A),
                            Color(0xFF141224),
                        )
                    } else {
                        listOf(
                            Color(0xFF0A1828),
                            Color(0xFF190D2A),
                            Color(0xFF0D0C16),
                        )
                    }
                )
            ),
    ) {
        // Floating ambient controller watermark in background
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.SportsEsports,
            tint = Color(0xFF00E5FF).copy(alpha = if (isFocused) 0.20f else 0.12f),
            size = if (isLarge) 90.dp else if (isWide) 56.dp else 42.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        if (isLarge) {
            // 2x2 or 4x2 Rich Console Dashboard Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Header: User Profile Avatar + Name + Level Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A2C))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFBD00FF))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!userAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val level = maxOf(1, (totalPlayTimeMs / (1000 * 3600 * 2)).toInt())
                        Text(
                            text = "LVL $level • RETRO GAMER",
                            color = Color(0xFF00E5FF),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                // Center Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Playtime Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "PLAYTIME",
                                    color = Color(0xFF8FE7FF),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedPlaytime,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }

                    // Titles Played Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color(0xFFFF2A85).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    tint = Color(0xFFFF2A85),
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "TITLES",
                                    color = Color(0xFFFF85B8),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$gamesPlayedCount",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }

                // Bottom Most Played Title Banner
                if (topGame != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "TOP:",
                            color = Color(0xFF00E5FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = topGame.title,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = GamePlayTracker.formatPlayTime(topGame.playTimeMs),
                            color = Color(0xFF8FE7FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text(
                        text = "Play games to log stats",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else if (isWide) {
            // 2x1 Console Horizontal Strip
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Avatar + Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A2C))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFBD00FF))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!userAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Column {
                        Text(
                            text = userName,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "$gamesPlayedCount Titles Played",
                            color = Color(0xFF00E5FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Right: Playtime highlight
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = formattedPlaytime,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "TOTAL PLAYTIME",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        } else {
            // 1x1 Compact Console Tile
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                // Top: Avatar + Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A2C))
                            .border(
                                1.dp,
                                Color(0xFF00E5FF).copy(alpha = 0.8f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!userAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                }

                // Center & Bottom: Playtime + Label
                Column {
                    Text(
                        text = formattedPlaytime,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Text(
                        text = "PLAYTIME",
                        color = Color(0xFF00E5FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isOneRow = item.span.rows <= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF382A63),
                            Color(0xFF1B1632),
                        )
                    } else {
                        listOf(
                            Color(0xFF1F1B38),
                            Color(0xFF12101F),
                        )
                    }
                )
            ),
    ) {
        // Floating organic watermark icon
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.Settings,
            tint = Color(0xFFA78BFA).copy(alpha = if (isFocused) 0.38f else 0.26f),
            size = if (isLarge) 82.dp else if (isOneRow) 38.dp else 52.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isOneRow) 5.dp else 10.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = item.title.ifEmpty { "Settings" },
                color = Color.White,
                fontSize = if (isLarge) 14.sp else if (item.span.columns >= 2) 11.5.sp else 10.sp,
                lineHeight = if (isOneRow) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.span.columns >= 2 || !isOneRow) {
                Text(
                    text = item.subtitle ?: "Preferences & Audio",
                    color = VantafynColors.Muted,
                    fontSize = if (isOneRow) 8.5.sp else 10.sp,
                    lineHeight = if (isOneRow) 10.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LibraryChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isOneRow = item.span.rows <= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF143B66),
                            Color(0xFF0C1D38),
                        )
                    } else {
                        listOf(
                            Color(0xFF0F1E36),
                            Color(0xFF0A1324),
                        )
                    }
                )
            ),
    ) {
        // Floating organic watermark icon
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.SportsEsports,
            tint = Color(0xFF00E5FF).copy(alpha = if (isFocused) 0.38f else 0.24f),
            size = if (isLarge) 80.dp else if (isOneRow) 38.dp else 50.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isOneRow) 5.dp else 10.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = item.title.ifEmpty { "Games Library" },
                color = Color.White,
                fontSize = if (isLarge) 14.sp else if (item.span.columns >= 2) 11.5.sp else 10.sp,
                lineHeight = if (isOneRow) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.span.columns >= 2 || !isOneRow) {
                Text(
                    text = item.subtitle ?: "Consoles & ROMs",
                    color = VantafynColors.Muted,
                    fontSize = if (isOneRow) 8.5.sp else 10.sp,
                    lineHeight = if (isOneRow) 10.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MainHomeChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isOneRow = item.span.rows <= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF45194D),
                            Color(0xFF220F2B),
                        )
                    } else {
                        listOf(
                            Color(0xFF28112B),
                            Color(0xFF140B1A),
                        )
                    }
                )
            ),
    ) {
        // Floating organic watermark icon
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.Home,
            tint = Color(0xFFFF2A85).copy(alpha = if (isFocused) 0.38f else 0.24f),
            size = if (isLarge) 80.dp else if (isOneRow) 38.dp else 50.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isOneRow) 5.dp else 10.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = item.title.ifEmpty { "Main Home" },
                color = Color.White,
                fontSize = if (isLarge) 14.sp else if (item.span.columns >= 2) 11.5.sp else 10.sp,
                lineHeight = if (isOneRow) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.span.columns >= 2 || !isOneRow) {
                Text(
                    text = item.subtitle ?: "Movies & Shows",
                    color = VantafynColors.Muted,
                    fontSize = if (isOneRow) 8.5.sp else 10.sp,
                    lineHeight = if (isOneRow) 10.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchChannelContent(
    item: ChannelTileItem,
    isFocused: Boolean,
) {
    val isLarge = item.span.columns >= 2 && item.span.rows >= 2
    val isOneRow = item.span.rows <= 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    if (isFocused) {
                        listOf(
                            Color(0xFF163E56),
                            Color(0xFF0F2236),
                        )
                    } else {
                        listOf(
                            Color(0xFF0F2636),
                            Color(0xFF0C1624),
                        )
                    }
                )
            ),
    ) {
        // Floating organic watermark icon
        FloatingWatermarkIcon(
            imageVector = Icons.Rounded.Search,
            tint = Color(0xFF00E5FF).copy(alpha = 0.24f),
            size = if (isLarge) 80.dp else if (isOneRow) 38.dp else 50.dp,
            seed = item.id.hashCode(),
            modifier = Modifier.align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (isOneRow) 5.dp else 10.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = item.title.ifEmpty { "Search" },
                color = Color.White,
                fontSize = if (isLarge) 14.sp else if (item.span.columns >= 2) 11.5.sp else 10.sp,
                lineHeight = if (isOneRow) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.span.columns >= 2 || !isOneRow) {
                Text(
                    text = item.subtitle ?: "Games & Consoles",
                    color = VantafynColors.Muted,
                    fontSize = if (isOneRow) 8.5.sp else 10.sp,
                    lineHeight = if (isOneRow) 10.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
