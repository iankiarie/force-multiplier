package com.ian.forcemultiplier.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.designsystem.FMCard
import com.ian.forcemultiplier.core.designsystem.FMStatCard
import com.ian.forcemultiplier.presentation.dashboard.viewmodel.DashboardViewModel
import com.ian.forcemultiplier.util.Resource

@Composable
fun DashboardScreen(
    navController: NavController = rememberNavController(),
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        when (val state = userState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message ?: "Error loading user",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is Resource.Success -> {
                val user = state.data!!
                
                // Top Section: Greeting
                Text(
                    text = "Good morning ${user.fullName ?: user.username}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "#3 in Nairobi Office",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_win_rate), 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "2 positions this week",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Force Multiplier Card (Apple Fitness style)
                FMCard(
                    containerColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "FORCE MULTIPLIER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Current Leader",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sarah K.",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "${user.points / 10.0} Score",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "7 days remaining",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Ring Visualization Placeholder
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            CircularProgressIndicator(
                                progress = (user.points % 100) / 100f,
                                strokeWidth = 12.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxSize()
                            )
                            CircularProgressIndicator(
                                progress = 0.65f,
                                strokeWidth = 12.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Row
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FMStatCard(
                        title = "Points",
                        value = user.points.toString(),
                        subValue = "+140 today",
                        icon = painterResource(id = R.drawable.ic_points)
                    )
                    FMStatCard(
                        title = "Predictions",
                        value = "12",
                        subValue = "4 active",
                        icon = painterResource(id = R.drawable.ic_rank)
                    )
                    FMStatCard(
                        title = "Accuracy",
                        value = "78%",
                        subValue = "↑ 5%",
                        icon = painterResource(id = R.drawable.ic_win_rate)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Activity Feed
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ActivityItem("Sarah won 150 points", "2h ago", MaterialTheme.colorScheme.primary)
            ActivityItem("Mike created prediction", "4h ago", MaterialTheme.colorScheme.secondary)
            ActivityItem("You earned badge", "1d ago", MaterialTheme.colorScheme.tertiary)
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Nav bar space
    }
}

@Composable
fun ActivityItem(title: String, time: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
