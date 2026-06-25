package com.ian.forcemultiplier.presentation.profile

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.presentation.login.ui.LoginActivity
import com.ian.forcemultiplier.presentation.profile.viewmodel.ProfileViewModel
import com.ian.forcemultiplier.util.Resource

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF0B0F14)
private val SurfaceDark  = Color(0xFF151B23)
private val Surface2Dark = Color(0xFF1C2128)
private val BorderDark   = Color(0xFF30363D)
private val MutedText    = Color(0xFF8B949E)
private val GreenPrimary = Color(0xFF2ED573)
private val OnSurface    = Color(0xFFE6EDF3)
private val ErrorRed     = Color(0xFFFF4757)

@Composable
fun ProfileScreen(
    navController: NavController,
    onThemeChange: (AppTheme) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    var selectedTheme by remember { mutableStateOf(AppTheme.DARK) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            // ── Section header ──────────────────────────────────────────
            Text(
                text = "Profile",
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── User card ───────────────────────────────────────────────
            when (val state = userState) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp)
                    }
                }
                is Resource.Error -> {
                    // Offline placeholder
                    ProfileHeader(
                        name = "Your Profile",
                        email = "Offline",
                        initials = "?"
                    )
                }
                is Resource.Success -> {
                    val user = state.data
                    if (user != null) {
                        ProfileHeader(
                            name = user.fullName ?: user.username,
                            email = user.email,
                            initials = (user.fullName ?: user.username)
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString("")
                                .uppercase()
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Stats grid ──────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                value = user.points.toString(),
                                label = "Points",
                                modifier = Modifier.weight(1f),
                                accent = GreenPrimary
                            )
                            StatCard(
                                value = "${(user.accuracy * 100).toInt()}%",
                                label = "Accuracy",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = "${user.streak}d",
                                label = "Streak",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = "#${user.rank ?: "-"}",
                                label = "Rank",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Activity heatmap ────────────────────────────────────────
            SectionLabel("Contribution Activity")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(20) { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(7) { day ->
                                    val level = (week * 7 + day) % 6
                                    val alpha = when (level) {
                                        0 -> 0.04f; 1 -> 0.18f; 2 -> 0.36f
                                        3 -> 0.55f; 4 -> 0.76f; else -> 1.0f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(GreenPrimary.copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "84 contributions in the last month",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Appearance ──────────────────────────────────────────────
            SectionLabel("Appearance")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    AppTheme.entries.forEachIndexed { index, theme ->
                        val label = when (theme) {
                            AppTheme.SYSTEM -> "System default"
                            AppTheme.DARK   -> "Dark"
                            AppTheme.LIGHT  -> "Light"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = theme
                                    onThemeChange(theme)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (selectedTheme == theme) OnSurface else MutedText,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTheme == theme) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = selectedTheme == theme,
                                onClick = { selectedTheme = theme; onThemeChange(theme) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GreenPrimary,
                                    unselectedColor = MutedText
                                )
                            )
                        }
                        if (index < AppTheme.entries.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = BorderDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Account section ─────────────────────────────────────────
            SectionLabel("Account")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    AccountRow("Notifications", showChevron = true) {}
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderDark)
                    AccountRow("Privacy Policy", showChevron = true) {}
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderDark)
                    AccountRow("Terms of Service", showChevron = true) {}
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderDark)
                    AccountRow("App version  1.0.0", valueText = "1.0.0", showChevron = false) {}
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Logout ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ErrorRed.copy(alpha = 0.08f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .clickable {
                        viewModel.logout {
                            val ctx = navController.context
                            ctx.startActivity(
                                Intent(ctx, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }
                    }
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sign out",
                    color = ErrorRed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(name: String, email: String, initials: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Avatar with gradient initials
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2ED573), Color(0xFF17C3B2))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifBlank { "?" },
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = email,
                color = MutedText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Edit profile", color = MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = OnSurface
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Text(
                text = label,
                color = MutedText,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MutedText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun AccountRow(
    label: String,
    valueText: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = OnSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (valueText != null) {
            Text(text = valueText, color = MutedText, fontSize = 13.sp)
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(">", color = MutedText, fontSize = 12.sp)
        }
    }
}

// Keep the data class accessible from ProfileScreen (no-op now, moved inline)
data class Achievement(
    val title: String,
    val level: String,
    val color: Color
)
