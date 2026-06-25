package com.ian.forcemultiplier.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.presentation.dashboard.DashboardScreen
import com.ian.forcemultiplier.presentation.leaderboard.LeaderboardScreen
import com.ian.forcemultiplier.presentation.profile.ProfileScreen
import com.ian.forcemultiplier.presentation.vault.NoteDetailScreen
import com.ian.forcemultiplier.presentation.vault.VaultScreen
import com.ian.forcemultiplier.presentation.wallet.WalletScreen
import com.ian.forcemultiplier.presentation.prediction.PredictionScreen

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF0B0F14)
private val SurfaceDark  = Color(0xFF151B23)
private val Surface2Dark = Color(0xFF1C2128)
private val BorderDark   = Color(0xFF30363D)
private val MutedText    = Color(0xFF8B949E)
private val GreenPrimary = Color(0xFF2ED573)

data class NavigationItem(val route: String, val label: String, val icon: Int)

private val navItems = listOf(
    NavigationItem("dashboard", "Home",    R.drawable.ic_home),
    NavigationItem("vault",     "Notes",   R.drawable.ic_note),
    NavigationItem("leaderboard","Ranks",  R.drawable.ic_rank),
    NavigationItem("profile",   "Profile", R.drawable.ic_person)
)

// Routes where the bottom bar should be hidden (full-screen note editor)
private val hideNavRoutes = setOf("note_detail/{noteId}")

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide nav bar when inside note editor
    val showNav = currentRoute != null && !currentRoute.startsWith("note_detail")

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        // ── Content ──────────────────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = modifier.fillMaxSize()
        ) {
            composable("dashboard")  { DashboardScreen(navController = navController) }
            composable("prediction") { PredictionScreen(navController = navController) }
            composable("leaderboard"){ LeaderboardScreen(navController = navController) }
            composable("wallet")     { WalletScreen(navController = navController) }
            composable("vault")      { VaultScreen(navController = navController) }
            composable(
                route = "note_detail/{noteId}?parentId={parentId}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType },
                    navArgument("parentId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                NoteDetailScreen(
                    navController = navController,
                    noteId = backStackEntry.arguments?.getString("noteId"),
                    parentId = backStackEntry.arguments?.getString("parentId")
                )
            }
            composable("profile") {
                ProfileScreen(navController = navController, onThemeChange = onThemeChange)
            }
        }

        // ── Floating bottom nav ───────────────────────────────────────
        AnimatedVisibility(
            visible = showNav,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FMBottomNav(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FMBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route ||
                        (item.route == "vault" && currentRoute?.startsWith("note_detail") == true)

                NavPill(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) GreenPrimary.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = "nav_pill_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) GreenPrimary else MutedText,
        animationSpec = tween(200),
        label = "nav_icon_tint"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) GreenPrimary else MutedText,
        animationSpec = tween(200),
        label = "nav_text_color"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "nav_content"
        ) { selected ->
            if (selected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = item.label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
