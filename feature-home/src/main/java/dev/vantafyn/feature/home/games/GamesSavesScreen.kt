package dev.vantafyn.feature.home.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vantafyn.core.ui.VantafynColors
import dev.vantafyn.core.ui.VantafynGradients
import dev.vantafyn.feature.home.CompactBackButton

@Composable
fun GamesSavesScreen(
    serverName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompactBackButton(onClick = onBack)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Saves",
                        color = VantafynColors.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Cross-device sync via Jellyfin",
                        color = VantafynColors.Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 2. Cloud Sync Active Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0F2B26).copy(alpha = 0.85f),
                                Color(0xFF101935).copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00E676).copy(alpha = 0.6f),
                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                            ),
                        ),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Sync Active",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Connected to $serverName. Battery SRAM & state snapshots sync seamlessly.",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        // 3. How It Works Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "SYNC FEATURES",
                    color = VantafynColors.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )

                SaveFeatureCard(
                    title = "Battery SRAM In-Game Saves",
                    subtitle = "Traditional cartridge saves (Pokemon badges, RPG progress) sync to your Jellyfin user directory automatically.",
                    icon = Icons.Rounded.Save,
                    accentColor = Color(0xFF00E5FF),
                )

                SaveFeatureCard(
                    title = "Libretro State Snapshots",
                    subtitle = "Quick Save and Load slots capture the exact CPU memory state for instantaneous continuation anywhere.",
                    icon = Icons.Rounded.History,
                    accentColor = Color(0xFF9D00FF),
                )

                SaveFeatureCard(
                    title = "Cross-Play Between TV & Mobile",
                    subtitle = "Play in bed on your phone, then pick up right where you left off on the living room Android TV.",
                    icon = Icons.Rounded.Devices,
                    accentColor = Color(0xFFFF2A85),
                )

                SaveFeatureCard(
                    title = "Tamper-Proof Cloud Storage",
                    subtitle = "Saves are stored per-user on your Jellyfin server, safe from device resets or cache clearing.",
                    icon = Icons.Rounded.Security,
                    accentColor = Color(0xFF00E676),
                )
            }
        }
    }
}

@Composable
private fun SaveFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VantafynColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = VantafynColors.Muted,
                fontSize = 12.sp,
            )
        }
    }
}
