package com.rrrainielll.teledrop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A progress indicator that follows a squircle (rounded rectangle) shape.
 * Used to show upload progress around squircle-shaped thumbnails.
 */
@Composable
fun SquircleProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    strokeWidth: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Canvas(
        modifier = modifier
            .progressSemantics(progress)
            .size(140.dp)
    ) {
        val cornerRadiusPx = cornerRadius.toPx()
        val strokeWidthPx = strokeWidth.toPx()
        val inset = strokeWidthPx / 2
        
        // Create the squircle path (rounded rectangle)
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset,
                    top = inset,
                    right = size.width - inset,
                    bottom = size.height - inset,
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            )
        }
        
        // Draw the track (background outline)
        drawPath(
            path = path,
            color = trackColor,
            style = Stroke(width = strokeWidthPx)
        )
        
        // Draw the progress
        if (progress > 0f) {
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(path, false)
            val pathLength = pathMeasure.length
            val progressLength = pathLength * progress.coerceIn(0f, 1f)
            
            val progressPath = Path()
            pathMeasure.getSegment(0f, progressLength, progressPath, true)
            
            drawPath(
                path = progressPath,
                color = color,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
