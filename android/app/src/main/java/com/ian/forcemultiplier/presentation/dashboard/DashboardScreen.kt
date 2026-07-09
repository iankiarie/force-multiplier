package com.ian.forcemultiplier.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.designsystem.FMCard
import com.ian.forcemultiplier.core.designsystem.FMStatCard
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.presentation.dashboard.viewmodel.DashboardViewModel
import com.ian.forcemultiplier.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    val activityState by viewModel.activityState.collectAsState()
    val leaderState by viewModel.leaderState.collectAsState()
    
    Scaffold(
        containerColor = Color(0xFF080808),
        bottomBar = { /* Bottom nav would be here */ }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            when (val state = userState) {
                is Resource.Loading -> {
                    DashboardShimmer()
                }
                is Resource.Error -> {
                    ErrorContent(state.message)
                }
                is Resource.Success -> {
                    val user = state.data!!
                    
                    // ── Header Section ──────────────────────────────────────────
                    Column {
                        Text(
                            text = "Good morning ${user.fullName?.split(" ")?.firstOrNull() ?: user.username}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-1).sp
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (user.rank != null) "#${user.rank} globally" else "Unranked  •  ${user.points} pts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Keep it up!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Hero Section: Force Multiplier Ring ──────────────────────
                    ForceMultiplierHeroCard(
                        points = user.points,
                        leaderName = (leaderState as? com.ian.forcemultiplier.util.Resource.Success)?.data?.let { it.fullName ?: it.username },
                        leaderScore = (leaderState as? com.ian.forcemultiplier.util.Resource.Success)?.data?.accuracy
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Stats Section ───────────────────────────────────────────
                    SectionHeader("Performance Stats")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FMStatCard(
                            title = "Wallet",
                            value = "${user.points} pts",
                            subValue = "+${user.dailyPoints} today",
                            icon = painterResource(id = R.drawable.ic_points)
                        )
                        FMStatCard(
                            title = "Accuracy",
                            value = "${(user.accuracy * 100).toInt()}%",
                            subValue = "win rate",
                            icon = painterResource(id = R.drawable.ic_win_rate)
                        )
                        FMStatCard(
                            title = "Streak",
                            value = "${user.streak}",
                            subValue = "days",
                            icon = painterResource(id = R.drawable.ic_rank)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Activity Feed Section ────────────────────────────────────
                    SectionHeader("Team Activity")
                    
                    when (val aState = activityState) {
                        is Resource.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                        is Resource.Error -> {
                            Text(text = "Could not load activity", color = MaterialTheme.colorScheme.error)
                        }
                        is Resource.Success -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                aState.data?.forEach { activity ->
                                    ActivityTimelineItem(
                                        title = activity.title,
                                        subtitle = activity.description,
                                        time = activity.createdAt?.take(10) ?: "Just now",
                                        color = when(activity.type) {
                                            "prediction" -> FMColors.Primary
                                            "note" -> FMColors.BadgeEpic
                                            else -> FMColors.Info
                                        }
                                    )
                                }
                                if (aState.data.isNullOrEmpty()) {
                                    Text(
                                        text = "No recent activity",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun ForceMultiplierHeroCard(points: Int, leaderName: String? = null, leaderScore: Float? = null) {
    val progress by animateFloatAsState(targetValue = (points % 1000) / 1000f, label = "progress")
    
    FMCard(
        containerColor = Color(0xFF151B23),
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                Text(
                    text = "FORCE MULTIPLIER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Current Leader",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = leaderName ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = leaderScore?.let { "%.1f Score".format(it * 100) } ?: "-",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "7 days remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Ring Visualization (Apple Fitness Style)
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = 1f,
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )
                CircularProgressIndicator(
                    progress = progress,
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                )
                CircularProgressIndicator(
                    progress = 0.45f,
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.ic_points),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ActivityTimelineItem(title: String, subtitle: String, time: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DashboardShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(160.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)))
            Box(modifier = Modifier.size(160.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
fun ErrorContent(message: String?) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = message ?: "Unknown error", color = MaterialTheme.colorScheme.error)
    }
}
