package com.rrrainielll.teledrop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rrrainielll.teledrop.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * Material 3 Animated Button with press scale effect
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = Motion.SnappySpring,
        label = "ButtonScale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        enabled = enabled,
        shape = shape,
        colors = colors
    ) {
        content()
    }
}

/**
 * Animated Card with entrance animation and hover effect
 */
@Composable
fun AnimatedCard(
    modifier: Modifier = Modifier,
    animationDelay: Int = 0,
    elevation: Dp = 2.dp,
    shape: Shape = MaterialTheme.shapes.large,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.Duration.Medium2,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "CardAlpha"
    )
    
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 24f,
        animationSpec = tween(
            durationMillis = Motion.Duration.Medium3,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "CardTranslationY"
    )
    
    Card(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = shape,
        colors = colors
    ) {
        content()
    }
}

/**
 * Pulsing indicator for sync/loading states
 */
@Composable
fun PulsingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 12.dp,
    isActive: Boolean = true
) {
    var pulse by remember { mutableStateOf(false) }
    
    LaunchedEffect(isActive) {
        while (isActive) {
            pulse = !pulse
            delay(800)
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = if (pulse && isActive) 1.3f else 1f,
        animationSpec = Motion.GentleSpring,
        label = "PulseScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (pulse && isActive) 0.7f else 1f,
        animationSpec = Motion.GentleSpring,
        label = "PulseAlpha"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Animated icon with rotation and scale transitions
 */
@Composable
fun AnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    isAnimating: Boolean = false,
    rotationDegrees: Float = 0f
) {
    val rotation by animateFloatAsState(
        targetValue = if (isAnimating) rotationDegrees else 0f,
        animationSpec = tween(
            durationMillis = Motion.Duration.Medium4,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "IconRotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.1f else 1f,
        animationSpec = Motion.EmphasizedSpring,
        label = "IconScale"
    )
    
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier
            .graphicsLayer {
                this.rotationZ = rotation
                this.scaleX = scale
                this.scaleY = scale
            },
        tint = tint
    )
}

/**
 * Shimmer loading effect for placeholder content
 */
@Composable
fun ShimmerLoadingBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium
) {
    var shimmerPosition by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            shimmerPosition = if (shimmerPosition >= 1f) 0f else shimmerPosition + 0.02f
            delay(16) // ~60fps
        }
    }
    
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColor = MaterialTheme.colorScheme.surface
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        baseColor,
                        shimmerColor,
                        baseColor
                    ),
                    startX = shimmerPosition * 1000f - 500f,
                    endX = shimmerPosition * 1000f + 500f
                )
            )
    )
}

/**
 * Staggered animation wrapper for list items
 */
@Composable
fun StaggeredAnimationItem(
    index: Int,
    baseDelay: Int = 50,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(Motion.staggerDelayWithCap(index, baseDelay).toLong())
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.Duration.Medium2,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "StaggerAlpha"
    )
    
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 32f,
        animationSpec = tween(
            durationMillis = Motion.Duration.Medium3,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "StaggerTranslationY"
    )
    
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        },
        content = content
    )
}
