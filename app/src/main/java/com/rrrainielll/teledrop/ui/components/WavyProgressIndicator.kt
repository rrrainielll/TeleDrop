package com.rrrainielll.teledrop.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 3.dp,
    amplitude: Dp = 3.dp,
    waveLength: Dp = 15.dp
) {
    // Infinite animation for wave motion
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "WaveAnimation")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * PI.toFloat()), // Negative to move wave right
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "PhaseShift"
    )

    // Total height needs to accommodate amplitude * 2 plus some padding/stroke
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val progressWidth = width * progress.coerceIn(0f, 1f)

        // 1. Draw Track (Remaining) - Straight Line
        if (progress < 1f) {
            drawLine(
                color = trackColor,
                start = Offset(progressWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. Draw Progress (Wavy)
        if (progress > 0f) {
            val path = Path()
            val step = 1f // Pixel step for smoothness
            
            val ampPx = amplitude.toPx()
            val waveLenPx = waveLength.toPx()
            val angularFrequency = (2 * PI) / waveLenPx

            // Start slightly off-screen or at 0?
            path.moveTo(0f, centerY + ampPx * sin(phaseShift.toDouble()).toFloat())
            
            var x = 0f
            while (x <= progressWidth) {
                // Determine Y based on sine wave
                // x * angularFrequency gives us the phase
                // Subtract phaseShift to move wave right
                val y = centerY + ampPx * sin((x * angularFrequency - phaseShift).toDouble()).toFloat()
                path.lineTo(x, y)
                x += step
            }
            // Ensure we connect to the precise end point
            val finalY = centerY + ampPx * sin((progressWidth * angularFrequency - phaseShift).toDouble()).toFloat()
            path.lineTo(progressWidth, finalY)
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            
            // 3. Draw Separator/Tip (Vertical Line)
            // Height of separator: somewhat taller than the wave
            val separatorHeight = ampPx * 2.5f
            drawLine(
                color = color,
                start = Offset(progressWidth, centerY - separatorHeight / 2),
                end = Offset(progressWidth, centerY + separatorHeight / 2),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
