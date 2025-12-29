package com.rrrainielll.teledrop.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object Motion {
    // Easing emphasis (simulating Emphasized Decelerate)
    val EmphasizedSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Standard expressive spring for layout changes
    val LayoutSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )
    
    // Snappy spring for small interactions
    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
