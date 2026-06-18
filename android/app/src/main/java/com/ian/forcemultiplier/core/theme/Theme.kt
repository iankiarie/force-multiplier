package com.ian.forcemultiplier.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ian.forcemultiplier.R

// ── Manrope Font Family ──────────────────────────────────────────────────────
val ManropeFontFamily = FontFamily(
    Font(R.font.manrope_regular,    FontWeight.Normal),
    Font(R.font.manrope_medium,     FontWeight.Medium),
    Font(R.font.manrope_semibold,   FontWeight.SemiBold),
    Font(R.font.manrope_bold,       FontWeight.Bold),
    Font(R.font.manrope_extrabold,  FontWeight.ExtraBold),
)

// ── Typography ───────────────────────────────────────────────────────────────
internal val FMTypography = Typography(
    displayLarge  = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 48.sp,
        lineHeight   = 56.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 40.sp,
        lineHeight   = 48.sp,
        letterSpacing = (-1.0).sp
    ),
    headlineLarge = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 26.sp,
        lineHeight   = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 22.sp,
        lineHeight   = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 20.sp,
        lineHeight   = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = ManropeFontFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.5.sp
    ),
)

// ── Dark Color Scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = FMColors.Primary,
    onPrimary          = FMColors.DarkBg,
    primaryContainer   = FMColors.PrimaryDim,
    onPrimaryContainer = FMColors.DarkOnSurface,
    secondary          = FMColors.Accent,
    onSecondary        = FMColors.DarkBg,
    tertiary           = FMColors.Info,
    onTertiary         = FMColors.DarkBg,
    background         = FMColors.DarkBg,
    onBackground       = FMColors.DarkOnSurface,
    surface            = FMColors.DarkSurface,
    onSurface          = FMColors.DarkOnSurface,
    surfaceVariant     = FMColors.DarkSurface2,
    onSurfaceVariant   = FMColors.DarkMuted,
    outline            = FMColors.DarkOutline,
    error              = FMColors.Error,
    onError            = FMColors.DarkOnSurface,
)

// ── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = FMColors.Primary,
    onPrimary          = FMColors.LightBg,
    primaryContainer   = FMColors.SuccessDim,
    onPrimaryContainer = FMColors.LightOnSurface,
    secondary          = FMColors.Accent,
    onSecondary        = FMColors.LightOnSurface,
    tertiary           = FMColors.Info,
    onTertiary         = FMColors.LightBg,
    background         = FMColors.LightBg,
    onBackground       = FMColors.LightOnSurface,
    surface            = FMColors.LightSurface,
    onSurface          = FMColors.LightOnSurface,
    surfaceVariant     = FMColors.LightSurface2,
    onSurfaceVariant   = FMColors.LightMuted,
    outline            = FMColors.LightOutline,
    error              = FMColors.Error,
    onError            = FMColors.LightBg,
)

// ── Theme Composable ─────────────────────────────────────────────────────────
@Composable
fun ForceMultiplierTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> systemDark
    }

    MaterialTheme(
        colorScheme = if (useDark) DarkColorScheme else LightColorScheme,
        typography  = FMTypography,
        content     = content
    )
}
