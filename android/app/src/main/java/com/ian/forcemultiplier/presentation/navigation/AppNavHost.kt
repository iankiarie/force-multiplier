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
import com.ian.forcemultiplier.presentation.bets.BetsScreen
import com.ian.forcemultiplier.presentation.dashboard.DashboardScreen
import com.ian.forcemultiplier.presentation.leaderboard.LeaderboardScreen
import com.ian.forcemultiplier.presentation.profile.ProfileScreen
import com.ian.forcemultiplier.presentation.vault.NoteDetailScreen
import com.ian.forcemultiplier.presentation.vault.VaultScreen

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF0B0F14)
private val SurfaceDark  = Color(0xFF151B23)
private val BorderDark   = Color(0xFF30363D)
private val MutedText    = Color(0xFF8B949E)
private val GreenPrimary = Color(0xFF2ED573)

data class NavigationItem(val route: String, val label: String, val icon: Int)

private val navItems = listOf(
    NavigationItem("dashboard",   "Home",    R.drawable.ic_home),
    NavigationItem("bets",        "Bets",    R.drawable.ic_bet),
    NavigationItem("vault",       "Notes",   R.drawable.ic_note),
    NavigationItem("leaderboard", "Ranks",   R.drawable.ic_trophy),
    NavigationItem("profile",     "Profile", R.drawable.ic_person)
)

// Routes that hide the bottom bar (immersive full-screen)
private val immersiveRoutes = setOf("note_detail/{noteId}")

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showNav = currentRoute != null && !currentRoute.startsWith("note_detail")

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = modifier.fillMaxSize()
        ) {
            composable("dashboard")   { DashboardScreen(navController = navController) }
            composable("bets")        { BetsScreen(navController = navController) }
            composable("leaderboard") { LeaderboardScreen(navController = navController) }
            composable("vault")       { VaultScreen(navController = navController) }
            composable(
                route = "note_detail/{noteId}?parentId={parentId}&template={template}",
                arguments = listOf(
                    navArgument("noteId")   { type = NavType.StringType },
                    navArgument("parentId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("template") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { back ->
                NoteDetailScreen(
                    navController = navController,
                    noteId = back.arguments?.getString("noteId"),
                    parentId = back.arguments?.getString("parentId"),
                    template = back.arguments?.getString("template")
                )
            }
            composable("profile") { ProfileScreen(navController = navController, onThemeChange = onThemeChange) }
        }

        AnimatedVisibility(
            visible = showNav,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FMBottomNav(currentRoute = currentRoute, onNavigate = { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            })
        }
    }
}

@Composable
private fun FMBottomNav(currentRoute: String?, onNavigate: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(0.6f), spotColor = Color.Black.copy(0.6f))
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route ||
                        (item.route == "vault" && currentRoute?.startsWith("note_detail") == true)
                NavPill(item = item, isSelected = isSelected, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun NavPill(item: NavigationItem, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isSelected) GreenPrimary.copy(0.15f) else Color.Transparent, tween(200), label = ""
    )
    val tintColor by animateColorAsState(
        if (isSelected) GreenPrimary else MutedText, tween(200), label = ""
    )

    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = isSelected, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "") { sel ->
            if (sel) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(painterResource(item.icon), item.label, tint = tintColor, modifier = Modifier.size(17.dp))
                    Text(item.label, color = tintColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Icon(painterResource(item.icon), item.label, tint = tintColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}
