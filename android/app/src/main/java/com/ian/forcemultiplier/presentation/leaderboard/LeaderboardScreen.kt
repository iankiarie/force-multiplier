package com.ian.forcemultiplier.presentation.leaderboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.presentation.leaderboard.viewmodel.LeaderboardViewModel
import com.ian.forcemultiplier.util.Resource

@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val leaderboardState by viewModel.leaderboardState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Leaderboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-1).sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (val state = leaderboardState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            }
            is Resource.Success -> {
                val users = state.data ?: emptyList()
                val top3 = users.take(3)
                val remaining = if (users.size > 3) users.drop(3) else emptyList()

                // ── Podium Section (Duolingo Style) ──────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (top3.size > 1) {
                        PodiumColumn(
                            user = top3[1],
                            rank = 2,
                            height = 130.dp,
                            color = FMColors.Silver,
                            icon = "🥈"
                        )
                    }
                    if (top3.isNotEmpty()) {
                        PodiumColumn(
                            user = top3[0],
                            rank = 1,
                            height = 170.dp,
                            color = FMColors.Gold,
                            icon = "🥇",
                            isWinner = true
                        )
                    }
                    if (top3.size > 2) {
                        PodiumColumn(
                            user = top3[2],
                            rank = 3,
                            height = 100.dp,
                            color = FMColors.Bronze,
                            icon = "🥉"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // ── Remaining List ──────────────────────────────────────────
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(remaining) { user ->
                        LeaderboardListItem(user, users.indexOf(user) + 1)
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumColumn(
    user: UserDto,
    rank: Int,
    height: Dp,
    color: Color,
    icon: String,
    isWinner: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(text = icon, fontSize = if (isWinner) 32.sp else 24.sp)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .size(if (isWinner) 84.dp else 70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = user.fullName?.split(" ")?.firstOrNull() ?: user.username,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "${user.points} pts",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color, color.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun LeaderboardListItem(user: UserDto, rank: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp, 
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.role?.uppercase() ?: "USER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = user.points.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
