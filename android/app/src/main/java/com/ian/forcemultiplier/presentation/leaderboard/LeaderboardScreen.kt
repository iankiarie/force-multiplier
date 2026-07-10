package com.ian.forcemultiplier.presentation.leaderboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import com.ian.forcemultiplier.core.designsystem.BoringAvatar
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.presentation.leaderboard.viewmodel.LeaderboardViewModel
import com.ian.forcemultiplier.util.Resource

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = FMColors.DarkBg
private val SurfaceDark  = FMColors.DarkSurface
private val Surface2Dark = FMColors.DarkSurface2
private val BorderDark   = FMColors.DarkOutline
private val MutedText    = FMColors.DarkMuted
private val GreenPrimary = FMColors.Primary
private val OnSurface    = FMColors.DarkOnSurface
private val Gold         = FMColors.Gold
private val Silver       = FMColors.Silver
private val Bronze       = FMColors.Bronze

@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val leaderboardState by viewModel.leaderboardState.collectAsState()
    var selectedPeriod by remember { mutableStateOf("All time") }
    val periods = listOf("All time", "This month", "This week")

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark)
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "Leaderboard",
                color = OnSurface, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Period tabs
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                periods.forEach { period ->
                    val sel = period == selectedPeriod
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) GreenPrimary.copy(0.15f) else Color.Transparent)
                            .border(if (sel) 1.dp else 0.dp, if (sel) GreenPrimary.copy(0.3f) else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(period, color = if (sel) GreenPrimary else MutedText, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when (val state = leaderboardState) {
            is Resource.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            is Resource.Error -> Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(state.message ?: "Could not load leaderboard", color = Color(0xFFFF4757), fontSize = 14.sp)
            }
            is Resource.Success -> {
                val users = state.data ?: emptyList()
                val top3  = users.take(3)
                val rest  = if (users.size > 3) users.drop(3) else emptyList()

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Podium ──────────────────────────────────────────
                    item {
                        PodiumSection(top3)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (rest.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                Text("RANKINGS", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // ── Ranked list ──────────────────────────────────────
                    items(rest.take(50)) { user ->
                        val rank = users.indexOf(user) + 1
                        RankRow(user = user, rank = rank, isCurrentUser = rank == 1)
                    }

                    if (users.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                                Text("No rankings yet", color = MutedText, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Podium ───────────────────────────────────────────────────────────────────
@Composable
private fun PodiumSection(top3: List<UserDto>) {
    // Order: silver(left=index 1), gold(center=index 0), bronze(right=index 2)
    val podiumOrder = buildList {
        if (top3.size > 1) add(Triple(top3[1], 2, Silver))
        if (top3.isNotEmpty()) add(Triple(top3[0], 1, Gold))
        if (top3.size > 2) add(Triple(top3[2], 3, Bronze))
    }
    val podiumHeights = mapOf(1 to 110.dp, 2 to 80.dp, 3 to 60.dp)
    val podiumAvatarSizes = mapOf(1 to 72.dp, 2 to 60.dp, 3 to 52.dp)

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark.copy(alpha = 0.8f), BgDark),
                    startY = 0f, endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(top = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            podiumOrder.forEach { (user, rank, color) ->
                val avatarSize = podiumAvatarSizes[rank] ?: 52.dp
                val barHeight  = podiumHeights[rank] ?: 60.dp
                val displayName = user.fullName?.split(" ")?.firstOrNull() ?: user.username

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Crown/medal
                    if (rank == 1) {
                        Text("CHAMPION", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Ring + avatar
                    Box(
                        modifier = Modifier.size(avatarSize + 6.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f))
                            .border(2.dp, color.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BoringAvatar(
                            name = user.fullName ?: user.username,
                            size = avatarSize - 4.dp,
                            fontSize = if (rank == 1) 22.sp else 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(displayName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = if (rank == 1) 14.sp else 12.sp, maxLines = 1)
                    Text("${user.points}", color = color, fontWeight = FontWeight.ExtraBold, fontSize = if (rank == 1) 13.sp else 11.sp)
                    Text("pts", color = MutedText, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Podium bar
                    Box(
                        modifier = Modifier.fillMaxWidth(if (rank == 1) 0.7f else 0.6f).height(barHeight)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.3f)))),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(rank.toString(), color = Color.White.copy(0.6f), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

// ─── Rank row ─────────────────────────────────────────────────────────────────
@Composable
private fun RankRow(user: UserDto, rank: Int, isCurrentUser: Boolean = false) {
    val bg = if (isCurrentUser) GreenPrimary.copy(alpha = 0.06f) else Color.Transparent
    Row(
        modifier = Modifier.fillMaxWidth().background(bg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank number
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Text(
                rank.toString(),
                color = if (rank <= 10) OnSurface else MutedText,
                fontWeight = if (rank <= 3) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Avatar
        BoringAvatar(
            name = user.fullName ?: user.username,
            size = 42.dp,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(14.dp))

        // Name + role
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName ?: user.username, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Text(user.role?.replaceFirstChar { it.uppercase() } ?: "Member", color = MutedText, fontSize = 11.sp)
        }

        // Score
        Column(horizontalAlignment = Alignment.End) {
            Text("${user.points}", color = GreenPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("pts", color = MutedText, fontSize = 10.sp)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderDark.copy(alpha = 0.5f))
}
