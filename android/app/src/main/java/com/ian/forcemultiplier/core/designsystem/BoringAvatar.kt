package com.ian.forcemultiplier.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Avatar gradient palette ──────────────────────────────────────────────────
private val avatarPalette = listOf(
    listOf(Color(0xFF2ED573), Color(0xFF17C3B2)),  // green-teal
    listOf(Color(0xFF3DABF5), Color(0xFF5F27CD)),  // blue-purple
    listOf(Color(0xFFFF9F43), Color(0xFFEE5A24)),  // orange-red
    listOf(Color(0xFFFF6B6B), Color(0xFF9C1239)),  // coral-crimson
    listOf(Color(0xFFFECA57), Color(0xFFFF9F43)),  // gold-orange
    listOf(Color(0xFF5F27CD), Color(0xFF341F97)),  // deep purple
    listOf(Color(0xFF00D2D3), Color(0xFF54A0FF)),  // cyan-blue
    listOf(Color(0xFFFF9FF3), Color(0xFFEE5A24)),  // pink-tangerine
    listOf(Color(0xFF1DD1A1), Color(0xFF10AC84)),  // emerald
    listOf(Color(0xFFc0392b), Color(0xFF8e44ad)),  // red-purple
)

private fun gradientsForName(name: String): List<Color> {
    val hash = name.trim().lowercase().hashCode().and(0x7FFFFFFF)
    return avatarPalette[hash % avatarPalette.size]
}

fun initialsFor(name: String): String = name
    .trim()
    .split(" ", "-", "_")
    .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
    .take(2)
    .joinToString("")
    .ifBlank { "?" }

/**
 * Boring-avatar style circle avatar.
 * Generates a consistent gradient + initials from [name].
 */
@Composable
fun BoringAvatar(
    name: String,
    size: Dp = 40.dp,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    val colors = gradientsForName(name)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsFor(name),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize
        )
    }
}

/**
 * Rounded-square variant (Slack/Notion style).
 */
@Composable
fun SquareAvatar(
    name: String,
    size: Dp = 40.dp,
    cornerRadius: Dp = 10.dp,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    val colors = gradientsForName(name)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsFor(name),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize
        )
    }
}
