package com.ian.forcemultiplier.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.presentation.dashboard.DashboardScreen
import com.ian.forcemultiplier.presentation.prediction.PredictionScreen
import com.ian.forcemultiplier.presentation.leaderboard.LeaderboardScreen
import com.ian.forcemultiplier.presentation.wallet.WalletScreen
import com.ian.forcemultiplier.presentation.vault.VaultScreen
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.presentation.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showQuickActions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showQuickActions) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
        ) {
            QuickActionMenu(
                onNavigate = { route ->
                    showQuickActions = false
                    navController.navigate(route)
                },
                onDismiss = { showQuickActions = false }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { navController.navigate(it) }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen(navController = navController) }
                composable("prediction") { PredictionScreen(navController = navController) }
                composable("leaderboard") { LeaderboardScreen(navController = navController) }
                composable("wallet") { WalletScreen(navController = navController) }
                composable("vault") { VaultScreen(navController = navController) }
                composable("profile") { ProfileScreen(navController = navController, onThemeChange = onThemeChange) }
            }
        }

        // Floating Action Button - Positioned absolute to hover correctly
        FloatingActionButton(
            onClick = { showQuickActions = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp) // Adjusted to hover above the bar
                .size(64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Quick Actions",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(72.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavigationItem("dashboard", "Home", R.drawable.ic_home),
                NavigationItem("prediction", "Predict", R.drawable.ic_points),
                null, // Space for FAB
                NavigationItem("leaderboard", "Ranks", R.drawable.ic_rank),
                NavigationItem("profile", "Profile", R.drawable.ic_person)
            )

            items.forEach { item ->
                if (item == null) {
                    Spacer(modifier = Modifier.width(64.dp))
                } else {
                    NavButton(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun NavButton(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = item.label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionMenu(
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp, start = 24.dp, end = 24.dp, top = 8.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        QuickActionItem(
            title = "Create Prediction", 
            description = "Post a new challenge for the team",
            icon = R.drawable.ic_points
        ) {
            onNavigate("prediction")
        }
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionItem(
            title = "New Knowledge Note", 
            description = "Share what you've learned today",
            icon = R.drawable.ic_visibility_off
        ) {
            onNavigate("vault")
        }
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionItem(
            title = "Grant Recognition", 
            description = "Award a badge to a colleague",
            icon = R.drawable.ic_rank
        ) {
            onDismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionItem(
    title: String,
    description: String,
    icon: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: Int)
