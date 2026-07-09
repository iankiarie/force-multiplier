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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF080808)
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
    val contributionState by viewModel.contributionState.collectAsState()
    var selectedTheme by remember { mutableStateOf(AppTheme.DARK) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.5).sp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_note), null, tint = MutedText, modifier = Modifier.size(18.dp))
            }
        }

        // ── Avatar section ──────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with gradient ring
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                // Gradient ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(GreenPrimary, Color(0xFF17C3B2), Color(0xFF2ED573).copy(alpha = 0.4f))
                            )
                        )
                )
                // Inner avatar
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2ED573), Color(0xFF17C3B2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = when (val s = userState) {
                        is Resource.Success -> s.data?.let { u ->
                            (u.fullName ?: u.username)
                                .split(" ").mapNotNull { it.firstOrNull()?.toString() }
                                .take(2).joinToString("").uppercase()
                        } ?: "?"
                        else -> "?"
                    }
                    Text(initials, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                }
                // Online badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(BgDark)
                        .border(2.dp, BgDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(14.dp).clip(CircleShape)
                            .background(GreenPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (val s = userState) {
                is Resource.Success -> {
                    val user = s.data
                    if (user != null) {
                        Text(
                            user.fullName ?: user.username,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            user.email,
                            color = MutedText,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Role badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(GreenPrimary.copy(alpha = 0.12f))
                                .border(1.dp, GreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                (user.role ?: "member").replaceFirstChar { it.uppercase() },
                                color = GreenPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is Resource.Loading -> {
                    Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Stats row ────────────────────────────────────────────────────────
        if (userState is Resource.Success) {
            val user = (userState as Resource.Success).data
            if (user != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStat("Points", user.points.toString(), GreenPrimary, Modifier.weight(1f))
                    ProfileStat("Accuracy", "${(user.accuracy * 100).toInt()}%", Color(0xFF6E98FF), Modifier.weight(1f))
                    ProfileStat("Streak", "${user.streak}d", Color(0xFFFFC93C), Modifier.weight(1f))
                    ProfileStat("Rank", "#${user.rank ?: "-"}", Color(0xFFFF6B9D), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Force Multiplier Progress ────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Force Multiplier Level", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${(user.accuracy * 100).toInt()}%", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // Progress track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Surface2Dark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(user.accuracy.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(listOf(GreenPrimary, Color(0xFF17C3B2)))
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${user.points} pts total  ·  ${user.streak} day streak", color = MutedText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Contribution Activity heatmap ────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionLabel("Contribution Activity")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    val contributions = (contributionState as? Resource.Success)?.data ?: emptyMap()
                    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(20) { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(7) { day ->
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, -((19 - week) * 7 + (6 - day)))
                                    val dateKey = dateFmt.format(cal.time)
                                    val count = contributions[dateKey] ?: 0
                                    val alpha = when {
                                        count == 0 -> 0.06f
                                        count == 1 -> 0.28f
                                        count <= 3 -> 0.54f
                                        count <= 5 -> 0.78f
                                        else       -> 1.00f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(GreenPrimary.copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val monthTotal = run {
                        val cutoff = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -30) }
                        val cutoffKey = dateFmt.format(cutoff.time)
                        contributions.entries.filter { it.key >= cutoffKey }.sumOf { it.value }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            monthTotal.toString() + " contribution" + (if (monthTotal != 1) "s" else "") + " this month",
                            color = MutedText, fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Less", color = MutedText, fontSize = 10.sp)
                            listOf(0.06f, 0.28f, 0.54f, 0.78f, 1.0f).forEach { a ->
                                Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(GreenPrimary.copy(a)))
                            }
                            Text("More", color = MutedText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Appearance ──────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionLabel("Appearance")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    AppTheme.entries.forEachIndexed { i, theme ->
                        val label = when (theme) {
                            AppTheme.SYSTEM -> "System default"
                            AppTheme.DARK   -> "Dark"
                            AppTheme.LIGHT  -> "Light"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTheme = theme; onThemeChange(theme) }
                                .padding(horizontal = 18.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = if (selectedTheme == theme) OnSurface else MutedText,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTheme == theme) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = selectedTheme == theme,
                                onClick = { selectedTheme = theme; onThemeChange(theme) },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary, unselectedColor = MutedText)
                            )
                        }
                        if (i < AppTheme.entries.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = BorderDark)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Account ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionLabel("Account")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    AccountRow(icon = R.drawable.ic_person, label = "Personal information") {}
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = BorderDark)
                    AccountRow(icon = R.drawable.ic_note, label = "Notifications") {}
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = BorderDark)
                    AccountRow(icon = R.drawable.ic_lock, label = "Privacy Policy") {}
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = BorderDark)
                    AccountRowValue(label = "App version", value = "1.0.0")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Sign out ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed.copy(alpha = 0.07f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
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
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign out", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

// ─── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun ProfileStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = MutedText, fontSize = 10.sp, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = MutedText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun AccountRow(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Surface2Dark),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), null, tint = MutedText, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("›", color = MutedText, fontSize = 18.sp)
    }
}

@Composable
private fun AccountRowValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = MutedText, fontSize = 13.sp)
    }
}

data class Achievement(val title: String, val level: String, val color: Color)
