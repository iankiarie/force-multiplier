package com.ian.forcemultiplier.core.theme

import androidx.compose.ui.graphics.Color

/**
 * ForceMultiplier semantic color tokens.
 * Use these instead of hardcoding hex values in composables.
 */
object FMColors {

    // ── Brand ────────────────────────────────────────────────
    val Primary        = Color(0xFF2ED573)   // FM Green
    val PrimaryDim     = Color(0xFF1AAD57)   // Pressed / darker green
    val Accent         = Color(0xFFF6C445)   // Gold / secondary
    val AccentDim      = Color(0xFFD4A017)

    // ── Backgrounds (Dark) ───────────────────────────────────
    val DarkBg         = Color(0xFF080808)
    val DarkSurface    = Color(0xFF111111)
    val DarkSurface2   = Color(0xFF171717)
    val DarkOutline    = Color(0xFF262626)
    val DarkOnSurface  = Color(0xFFFFFFFF)
    val DarkMuted      = Color(0xFF6B6B6B)

    // ── Backgrounds (Light) ──────────────────────────────────
    val LightBg        = Color(0xFFF6F8FA)
    val LightSurface   = Color(0xFFFFFFFF)
    val LightSurface2  = Color(0xFFF0F2F5)
    val LightOutline   = Color(0xFFD0D7DE)
    val LightOnSurface = Color(0xFF0B0F14)
    val LightMuted     = Color(0xFF57606A)

    // ── Status ───────────────────────────────────────────────
    val Success        = Color(0xFF2ED573)
    val SuccessDim     = Color(0xFF1E9650)
    val Warning        = Color(0xFFF6C445)
    val Error          = Color(0xFFFF4757)
    val ErrorDim       = Color(0xFFCC2233)
    val Info           = Color(0xFF3DABF5)

    // ── Rank Metals ──────────────────────────────────────────
    val Gold           = Color(0xFFFFD700)
    val GoldDim        = Color(0xFFB8960C)
    val Silver         = Color(0xFFBFC9D1)
    val SilverDim      = Color(0xFF8996A0)
    val Bronze         = Color(0xFFCD7F32)
    val BronzeDim      = Color(0xFF9A5C22)

    // ── Prediction Outcomes ──────────────────────────────────
    val TeamA          = Color(0xFF3DABF5)   // Blue
    val TeamB          = Color(0xFFFF6B6B)   // Coral
    val Draw           = Color(0xFFFFD93D)   // Yellow

    // ── Badge Tiers ──────────────────────────────────────────
    val BadgeCommon    = Color(0xFF8B949E)
    val BadgeRare      = Color(0xFF3DABF5)
    val BadgeEpic      = Color(0xFF9C59FF)
    val BadgeLegendary = Color(0xFFFFD700)

    // ── FM Gradient (use as Brush.linearGradient) ────────────
    val FMGradStart    = Color(0xFF2ED573)
    val FMGradMid      = Color(0xFF17C3B2)
    val FMGradEnd      = Color(0xFF17A2B8)

    // ── Transaction ──────────────────────────────────────────
    val Credit         = Color(0xFF2ED573)
    val Debit          = Color(0xFFFF4757)
    val Bonus          = Color(0xFFF6C445)
    val Award          = Color(0xFF9C59FF)
}
