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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.presentation.bets.BetsScreen
import com.ian.forcemultiplier.presentation.dashboard.DashboardScreen
import com.ian.forcemultiplier.presentation.leaderboard.LeaderboardScreen
import com.ian.forcemultiplier.presentation.profile.ProfileScreen
import com.ian.forcemultiplier.presentation.vault.NoteDetailScreen
import com.ian.forcemultiplier.presentation.vault.VaultScreen
import com.ian.forcemultiplier.presentation.vault.viewmodel.VaultViewModel

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgBlack      = FMColors.DarkBg
private val SurfaceDark  = FMColors.DarkSurface
private val MutedText    = FMColors.DarkMuted
private val GreenPrimary = FMColors.Primary

data class NavigationItem(val route: String, val label: String, val icon: Int)

private val navItems = listOf(
    NavigationItem("dashboard",   "Home",    R.drawable.ic_home),
    NavigationItem("bets",        "Bets",    R.drawable.ic_bet),
    NavigationItem("vault_graph", "Notes",   R.drawable.ic_note),
    NavigationItem("leaderboard", "Ranks",   R.drawable.ic_trophy),
    NavigationItem("profile",     "Profile", R.drawable.ic_person)
)

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showNav = currentRoute != null && !currentRoute.startsWith("note_detail")

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = modifier.fillMaxSize()
        ) {
            composable("dashboard")   { DashboardScreen(navController = navController) }
            composable("bets")        { BetsScreen(navController = navController) }
            composable("leaderboard") { LeaderboardScreen(navController = navController) }

            // ── Notes graph: VaultScreen + NoteDetailScreen share ONE VaultViewModel ──
            // By scoping to "vault_graph", both composables get the same instance.
            // This means when NoteDetailScreen saves a note and pops back, VaultScreen
            // already has the updated notes list — no re-fetch timing race.
            navigation(route = "vault_graph", startDestination = "vault") {
                composable("vault") { back ->
                    val graphEntry = remember(back) {
                        navController.getBackStackEntry("vault_graph")
                    }
                    val sharedViewModel: VaultViewModel = hiltViewModel(graphEntry)
                    VaultScreen(navController = navController, viewModel = sharedViewModel)
                }
                composable(
                    route = "note_detail/{noteId}?parentId={parentId}&template={template}&folderId={folderId}",
                    arguments = listOf(
                        navArgument("noteId")   { type = NavType.StringType },
                        navArgument("parentId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("template") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("folderId") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { back ->
                    val graphEntry = remember(back) {
                        navController.getBackStackEntry("vault_graph")
                    }
                    val sharedViewModel: VaultViewModel = hiltViewModel(graphEntry)
                    NoteDetailScreen(
                        navController = navController,
                        viewModel = sharedViewModel,
                        noteId = back.arguments?.getString("noteId"),
                        parentId = back.arguments?.getString("parentId"),
                        template = back.arguments?.getString("template"),
                        initialFolderId = back.arguments?.getString("folderId")
                    )
                }
            }

            composable("profile") {
                ProfileScreen(navController = navController, onThemeChange = onThemeChange)
            }
        }

        AnimatedVisibility(
            visible = showNav,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FMBottomNav(currentRoute = currentRoute, onNavigate = { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
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
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(0.6f), spotColor = Color.Black.copy(0.6f))
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment    = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = when {
                    item.route == "vault_graph" -> currentRoute == "vault" || currentRoute?.startsWith("note_detail") == true
                    else                        -> currentRoute == item.route
                }
                NavPill(item = item, isSelected = isSelected, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun NavPill(item: NavigationItem, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor    by animateColorAsState(if (isSelected) GreenPrimary.copy(0.15f) else Color.Transparent, tween(200), label = "")
    val tintColor  by animateColorAsState(if (isSelected) GreenPrimary else MutedText, tween(200), label = "")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState  = isSelected,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = ""
        ) { sel ->
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
