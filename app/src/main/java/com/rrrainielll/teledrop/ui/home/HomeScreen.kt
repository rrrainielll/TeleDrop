package com.rrrainielll.teledrop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.rrrainielll.teledrop.ui.theme.Motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.work.WorkInfo
import coil.compose.AsyncImage
import com.rrrainielll.teledrop.R
import com.rrrainielll.teledrop.ui.components.SquircleProgressIndicator
import kotlinx.coroutines.delay


@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onManageFolders: () -> Unit,
    onSettings: () -> Unit
) {
    // Observing Flow from ViewModel
    val workInfos by viewModel.workStatus.collectAsState(initial = emptyList())
    val botUsername by viewModel.botUsernameFlow.collectAsState(initial = null)
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val botToken by viewModel.botTokenFlow.collectAsState(initial = null)
    
    val isSyncing = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    val lastWork = workInfos.find { it.state == WorkInfo.State.RUNNING } ?: workInfos.firstOrNull()

    // Entrance animation states
    var showContent by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.enableAutoSync()
        delay(100)
        showContent = true
        delay(150)
        showCard = true
        delay(100)
        showButtons = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Animated Settings icon button at top-right
                val settingsAlpha by animateFloatAsState(
                    targetValue = if (showContent) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                    label = "SettingsAlpha"
                )
                
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .graphicsLayer { alpha = settingsAlpha }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Main content centered
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Animated Status Card
                        val cardAlpha by animateFloatAsState(
                            targetValue = if (showCard) 1f else 0f,
                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                            label = "CardAlpha"
                        )
                        val cardScale by animateFloatAsState(
                            targetValue = if (showCard) 1f else 0.95f,
                            animationSpec = Motion.EmphasizedSpring,
                            label = "CardScale"
                        )
                        val cardTranslationY by animateFloatAsState(
                            targetValue = if (showCard) 0f else 24f,
                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                            label = "CardTranslationY"
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = cardAlpha
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    translationY = cardTranslationY
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Enhanced AnimatedContent with M3 transitions
                                AnimatedContent(
                                    targetState = isSyncing,
                                    transitionSpec = {
                                        (fadeIn(
                                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                                        ) + scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                                        ) + slideInVertically(
                                            initialOffsetY = { it / 4 },
                                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                                        )).togetherWith(
                                            fadeOut(
                                                animationSpec = tween(Motion.Duration.Short4, easing = Motion.EmphasizedAccelerate)
                                            ) + scaleOut(
                                                targetScale = 1.05f,
                                                animationSpec = tween(Motion.Duration.Short4)
                                            ) + slideOutVertically(
                                                targetOffsetY = { -it / 4 },
                                                animationSpec = tween(Motion.Duration.Short4)
                                            )
                                        )
                                    },
                                    label = "Status Animation"
                                ) { syncing ->
                                    if (syncing) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val progress = lastWork?.progress
                                            val current = progress?.getInt("current", 0) ?: 0
                                            val total = progress?.getInt("total", 0) ?: 0
                                            val filename = progress?.getString("filename")
                                            val uriString = progress?.getString("uri")
                                            val uploadPercent = progress?.getInt("uploadPercent", 0) ?: 0
                                            val uploadedBytes = progress?.getLong("uploadedBytes", 0L) ?: 0L
                                            val totalBytes = progress?.getLong("totalBytes", 0L) ?: 0L
                                            val uploadSpeedBps = progress?.getLong("uploadSpeedBps", 0L) ?: 0L
                                            val etaSeconds = progress?.getLong("etaSeconds", 0L) ?: 0L
                                            val progressValue = if (totalBytes > 0) {
                                                uploadedBytes.toFloat() / totalBytes.toFloat()
                                            } else if (total > 0) {
                                                current.toFloat() / total.toFloat()
                                            } else 0f

                                            // Thumbnail with squircle progress around it
                                            val squircleCornerRadius = 32.dp
                                            
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(140.dp)
                                            ) {
                                                // Animated progress with smooth spring
                                                val animatedProgress by animateFloatAsState(
                                                    targetValue = progressValue,
                                                    animationSpec = Motion.ProgressSpring,
                                                    label = "Progress Animation"
                                                )
                                                
                                                if (uploadPercent > 0) {
                                                    SquircleProgressIndicator(
                                                        progress = animatedProgress,
                                                        modifier = Modifier.size(140.dp),
                                                        cornerRadius = squircleCornerRadius + 10.dp,
                                                        strokeWidth = 6.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                }
                                                
                                                // Thumbnail image with animated appearance
                                                if (uriString != null) {
                                                    var imageLoaded by remember { mutableStateOf(false) }
                                                    val imageScale by animateFloatAsState(
                                                        targetValue = if (imageLoaded) 1f else 0.9f,
                                                        animationSpec = Motion.EmphasizedSpring,
                                                        label = "ImageScale"
                                                    )
                                                    val imageAlpha by animateFloatAsState(
                                                        targetValue = if (imageLoaded) 1f else 0f,
                                                        animationSpec = tween(Motion.Duration.Medium2),
                                                        label = "ImageAlpha"
                                                    )
                                                    
                                                    AsyncImage(
                                                        model = uriString,
                                                        contentDescription = "Uploading Image",
                                                        modifier = Modifier
                                                            .size(120.dp)
                                                            .clip(RoundedCornerShape(squircleCornerRadius))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                            .graphicsLayer {
                                                                scaleX = imageScale
                                                                scaleY = imageScale
                                                                alpha = imageAlpha
                                                            },
                                                        contentScale = ContentScale.Crop,
                                                        onSuccess = { imageLoaded = true }
                                                    )
                                                } else {
                                                    // Pulsing loading indicator
                                                    var pulse by remember { mutableStateOf(false) }
                                                    LaunchedEffect(Unit) {
                                                        while (true) {
                                                            pulse = !pulse
                                                            delay(600)
                                                        }
                                                    }
                                                    val pulseScale by animateFloatAsState(
                                                        targetValue = if (pulse) 1.1f else 1f,
                                                        animationSpec = Motion.GentleSpring,
                                                        label = "PulseScale"
                                                    )
                                                    
                                                    CircularProgressIndicator(
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .scale(pulseScale),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        strokeWidth = 5.dp
                                                    )
                                                }
                                                
                                                // Percentage text overlay with animated appearance
                                                if (uploadPercent > 0 && uriString != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(120.dp)
                                                            .clip(RoundedCornerShape(squircleCornerRadius))
                                                            .background(Color.Black.copy(alpha = 0.5f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$uploadPercent%",
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Text(
                                                text = if (total > 0) "Uploading $current of $total" else "Syncing in Progress",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            if (filename != null) {
                                                Text(
                                                    text = filename,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                // Show detailed progress if available
                                                if (uploadPercent > 0 || totalBytes > 0) {
                                                    // Uploaded bytes / Total bytes
                                                    if (totalBytes > 0) {
                                                        Text(
                                                            text = "${formatFileSize(uploadedBytes)} / ${formatFileSize(totalBytes)}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    }
                                                    
                                                    // Speed and ETA
                                                    if (uploadSpeedBps > 0 || etaSeconds > 0) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            if (uploadSpeedBps > 0) {
                                                                Text(
                                                                    text = formatSpeed(uploadSpeedBps),
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            if (uploadSpeedBps > 0 && etaSeconds > 0) {
                                                                Text(
                                                                    text = " • ",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            if (etaSeconds > 0) {
                                                                Text(
                                                                    text = "${formatDuration(etaSeconds)} left",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                            } else {
                                                Text(
                                                    text = "Uploading your media to Telegram",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                    } else {
                                        // Idle state with animated icon
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val isSuccess = lastWork != null && lastWork.state == WorkInfo.State.SUCCEEDED
                                            
                                            // Animated icon container
                                            var iconVisible by remember { mutableStateOf(false) }
                                            LaunchedEffect(Unit) {
                                                delay(100)
                                                iconVisible = true
                                            }
                                            
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (iconVisible) 1f else 0.8f,
                                                animationSpec = Motion.BouncySpring,
                                                label = "IconScale"
                                            )
                                            val iconAlpha by animateFloatAsState(
                                                targetValue = if (iconVisible) 1f else 0f,
                                                animationSpec = tween(Motion.Duration.Medium2),
                                                label = "IconAlpha"
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .graphicsLayer {
                                                        scaleX = iconScale
                                                        scaleY = iconScale
                                                        alpha = iconAlpha
                                                    }
                                                    .clip(RoundedCornerShape(24.dp))
                                                    .background(
                                                        if (isSuccess)
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.primaryContainer
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSuccess) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(40.dp),
                                                        tint = MaterialTheme.colorScheme.tertiary
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                                        contentDescription = "TeleDrop Logo",
                                                        modifier = Modifier.size(80.dp),
                                                        tint = Color.Unspecified
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Text(
                                                text = "Ready to Sync",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            // Animated success message
                                            AnimatedVisibility(
                                                visible = isSuccess,
                                                enter = fadeIn(tween(Motion.Duration.Medium2)) + 
                                                    slideInVertically(
                                                        initialOffsetY = { it / 2 },
                                                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                                                    ),
                                                exit = fadeOut(tween(Motion.Duration.Short4))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.tertiary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "Last sync completed successfully",
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        // Animated Buttons Section
                        val buttonsAlpha by animateFloatAsState(
                            targetValue = if (showButtons) 1f else 0f,
                            animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                            label = "ButtonsAlpha"
                        )
                        val buttonsTranslationY by animateFloatAsState(
                            targetValue = if (showButtons) 0f else 32f,
                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                            label = "ButtonsTranslationY"
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = buttonsAlpha
                                    translationY = buttonsTranslationY
                                },
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sync Now Button with press animation
                            var isSyncPressed by remember { mutableStateOf(false) }
                            val syncButtonScale by animateFloatAsState(
                                targetValue = if (isSyncPressed && !isSyncing) 0.96f else 1f,
                                animationSpec = Motion.SnappySpring,
                                label = "SyncButtonScale"
                            )
                            
                            Button(
                                onClick = viewModel::triggerSync,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(syncButtonScale)
                                    .pointerInput(!isSyncing) {
                                        detectTapGestures(
                                            onPress = {
                                                isSyncPressed = true
                                                tryAwaitRelease()
                                                isSyncPressed = false
                                            }
                                        )
                                    },
                                enabled = !isSyncing,
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Sync Now",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                )
                            }
                            
                            // Manage Folders Button with press animation
                            var isFoldersPressed by remember { mutableStateOf(false) }
                            val foldersButtonScale by animateFloatAsState(
                                targetValue = if (isFoldersPressed) 0.96f else 1f,
                                animationSpec = Motion.SnappySpring,
                                label = "FoldersButtonScale"
                            )
                            
                            OutlinedButton(
                                onClick = onManageFolders,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(foldersButtonScale)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isFoldersPressed = true
                                                tryAwaitRelease()
                                                isFoldersPressed = false
                                            }
                                        )
                                    },
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Manage Folders",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                                )
                            }
                            
                            // Animated Open Bot Button
                            AnimatedVisibility(
                                visible = !botUsername.isNullOrBlank(),
                                enter = fadeIn(tween(Motion.Duration.Medium2)) + 
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate)
                                    ),
                                exit = fadeOut(tween(Motion.Duration.Short4)) + 
                                    slideOutVertically(targetOffsetY = { it })
                            ) {
                                var isBotPressed by remember { mutableStateOf(false) }
                                val botButtonScale by animateFloatAsState(
                                    targetValue = if (isBotPressed) 0.96f else 1f,
                                    animationSpec = Motion.SnappySpring,
                                    label = "BotButtonScale"
                                )
                                
                                OutlinedButton(
                                    onClick = {
                                        uriHandler.openUri("https://t.me/$botUsername")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .scale(botButtonScale)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isBotPressed = true
                                                    tryAwaitRelease()
                                                    isBotPressed = false
                                                }
                                            )
                                        },
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Open @$botUsername",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to format file size
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

// Helper function to format upload speed
private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSecond / 1024.0)
        else -> "%.1f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
    }
}

// Helper function to format duration
private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val mins = seconds / 60
            val secs = seconds % 60
            if (secs == 0L) "${mins}m" else "${mins}m ${secs}s"
        }
        else -> {
            val hours = seconds / 3600
            val mins = (seconds % 3600) / 60
            if (mins == 0L) "${hours}h" else "${hours}h ${mins}m"
        }
    }
}

