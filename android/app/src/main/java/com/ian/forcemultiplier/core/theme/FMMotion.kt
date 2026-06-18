package com.ian.forcemultiplier.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * ForceMultiplier motion design tokens.
 * Consistent animation specs across the app — never hardcode durations inline.
 */
object FMMotion {

    // ── Durations (ms) ───────────────────────────────────────
    const val Fast   = 150
    const val Medium = 300
    const val Slow   = 500
    const val Counter = 800  // Number counter animations

    // ── Easing ───────────────────────────────────────────────
    val EaseOutQuart  = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)
    val EaseInOut     = CubicBezierEasing(0.45f, 0.0f, 0.55f, 1.0f)
    val EaseOut       = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // ── Spring Specs ─────────────────────────────────────────

    /** Bouncy: leaderboard entries, badge unlocks, podium drop */
    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    /** Smooth: card transitions, sheet expand */
    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    /** Tight: immediate UI feedback, pressed states */
    val SpringTight  = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessHigh
    )

    // ── Tween Specs ──────────────────────────────────────────
    fun fastTween()   = tween<Float>(durationMillis = Fast,   easing = EaseOut)
    fun mediumTween() = tween<Float>(durationMillis = Medium, easing = EaseOutQuart)
    fun slowTween()   = tween<Float>(durationMillis = Slow,   easing = EaseOutQuart)

    // ── Stagger delay per item in a list ─────────────────────
    fun staggerDelay(index: Int, baseDelay: Int = 50) = index * baseDelay
}
