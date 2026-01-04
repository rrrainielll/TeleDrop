package com.rrrainielll.teledrop.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * Material 3 Motion System for TeleDrop
 * 
 * Implements expressive motion tokens following Material Design 3 guidelines
 * for creating delightful, purposeful animations.
 */
object Motion {
    
    // ===== Duration Tokens (in milliseconds) =====
    object Duration {
        const val Short1 = 50
        const val Short2 = 100
        const val Short3 = 150
        const val Short4 = 200
        const val Medium1 = 250
        const val Medium2 = 300
        const val Medium3 = 350
        const val Medium4 = 400
        const val Long1 = 450
        const val Long2 = 500
        const val Long3 = 550
        const val Long4 = 600
        const val ExtraLong1 = 700
        const val ExtraLong2 = 800
        const val ExtraLong3 = 900
        const val ExtraLong4 = 1000
    }
    
    // ===== M3 Easing Curves =====
    
    /** For elements entering the screen - starts fast, ends slow */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    
    /** For elements exiting the screen - starts slow, ends fast */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    
    /** Combined emphasized easing for persistent elements */
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    /** Standard easing for common transitions */
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    /** Standard decelerate for elements appearing */
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    
    /** Standard accelerate for elements disappearing */
    val StandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)
    
    // ===== Spring Animations =====
    
    /** Emphasized spring with slight bounce - for important state changes */
    val EmphasizedSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 300f
    )
    
    /** Layout spring for structural changes */
    val LayoutSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 400f
    )
    
    /** Snappy spring for quick interactions */
    val SnappySpring = spring<Float>(
        dampingRatio = 1.0f,
        stiffness = 800f
    )
    
    /** Bouncy spring for playful animations */
    val BouncySpring = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = 300f
    )
    
    /** Gentle spring for subtle movements */
    val GentleSpring = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 200f
    )
    
    /** Spring for progress indicators */
    val ProgressSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 400f
    )
    
    // ===== IntOffset Springs for position animations =====
    
    val LayoutSpringOffset = spring<IntOffset>(
        dampingRatio = 0.85f,
        stiffness = 400f
    )
    
    val SnappySpringOffset = spring<IntOffset>(
        dampingRatio = 1.0f,
        stiffness = 800f
    )
    
    // ===== Stagger Delay Helper =====
    
    /** Calculate stagger delay for item at given index */
    fun staggerDelay(index: Int, baseDelay: Int = 50): Int = index * baseDelay
    
    /** Calculate stagger delay with max cap */
    fun staggerDelayWithCap(index: Int, baseDelay: Int = 50, maxDelay: Int = 300): Int = 
        minOf(index * baseDelay, maxDelay)
}
