package com.ian.forcemultiplier.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.designsystem.FMCard
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.core.theme.FMColors

@Composable
fun ProfileScreen(
    navController: NavController,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── Profile Header ────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ian Kiarie",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ian@forcemultiplier.co  ·  Senior Engineer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Force Multiplier Candidate  •  #3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Stats Row ─────────────────────────────────────────
        FMCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("3,240", "Points")
                StatDivider()
                StatItem("78%", "Accuracy")
                StatDivider()
                StatItem("12", "Streak")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Theme Selector ────────────────────────────────────
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FMCard {
            Column(modifier = Modifier.padding(4.dp)) {
                AppTheme.entries.forEach { theme ->
                    ThemeOptionRow(
                        theme = theme,
                        onSelect = { onThemeChange(theme) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Activity Heatmap ─────────────────────────────────
        Text(
            text = "Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FMCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(26) { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(7) { day ->
                                val level = ((week * 7 + day) % 5)
                                val alpha = when (level) {
                                    0 -> 0.08f
                                    1 -> 0.2f
                                    2 -> 0.45f
                                    3 -> 0.7f
                                    else -> 1.0f
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(FMColors.Primary.copy(alpha = alpha))
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last 26 weeks of activity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Achievements Grid ─────────────────────────────────
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val achievements = listOf(
            Achievement("Top 3 Finisher",    "Bronze", FMColors.Bronze),
            Achievement("12 Day Streak",     "Silver", FMColors.Silver),
            Achievement("Knowledge Guru",    "Gold",   FMColors.Gold),
            Achievement("Prediction Master", "Epic",   FMColors.BadgeEpic),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            achievements.take(2).forEach { AchievementCard(it, Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            achievements.drop(2).forEach { AchievementCard(it, Modifier.weight(1f)) }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    )
}

@Composable
private fun ThemeOptionRow(theme: AppTheme, onSelect: () -> Unit) {
    val label = when (theme) {
        AppTheme.SYSTEM -> "System default"
        AppTheme.DARK   -> "Dark"
        AppTheme.LIGHT  -> "Light"
    }
    val sublabel = when (theme) {
        AppTheme.SYSTEM -> "Follow device setting"
        AppTheme.DARK   -> "Dark background"
        AppTheme.LIGHT  -> "Light background"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    FMCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement.level,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = achievement.color
            )
        }
    }
}

data class Achievement(
    val title: String,
    val level: String,
    val color: Color
)
