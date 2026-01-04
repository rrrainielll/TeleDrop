package com.rrrainielll.teledrop.ui.folder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rrrainielll.teledrop.ui.theme.Motion
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderSelectionScreen(
    viewModel: FolderViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onPickFolder: () -> Unit
) {
    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Staggered animation states
    var showContent by remember { mutableStateOf(false) }
    var showFab by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(200)
        showFab = true
    }

    Scaffold(
        topBar = {
            val topBarAlpha by animateFloatAsState(
                targetValue = if (showContent) 1f else 0f,
                animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                label = "TopBarAlpha"
            )
            
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Select Folders",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.graphicsLayer { alpha = topBarAlpha }
                    ) 
                }
            )
        },
        floatingActionButton = {
            if (hasPermissions && !isLoading) {
                // Animated FAB entrance
                AnimatedVisibility(
                    visible = showFab,
                    enter = scaleIn(
                        initialScale = 0.6f,
                        animationSpec = Motion.BouncySpring
                    ) + fadeIn(animationSpec = tween(Motion.Duration.Medium2)),
                    exit = scaleOut(targetScale = 0.6f) + fadeOut()
                ) {
                    var isFabPressed by remember { mutableStateOf(false) }
                    val fabScale by animateFloatAsState(
                        targetValue = if (isFabPressed) 0.92f else 1f,
                        animationSpec = Motion.SnappySpring,
                        label = "FabScale"
                    )
                    
                    LargeFloatingActionButton(
                        onClick = onPickFolder,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .scale(fabScale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isFabPressed = true
                                        tryAwaitRelease()
                                        isFabPressed = false
                                    }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Folder",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            !hasPermissions -> {
                // Animated permission prompt
                val contentAlpha by animateFloatAsState(
                    targetValue = if (showContent) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "ContentAlpha"
                )
                val contentTranslationY by animateFloatAsState(
                    targetValue = if (showContent) 0f else 32f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "ContentTranslationY"
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentTranslationY
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val iconScale by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0.8f,
                        animationSpec = Motion.BouncySpring,
                        label = "PermIconScale"
                    )
                    
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp)
                            .scale(iconScale),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Media Access Required",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "TeleDrop needs access to your photos and videos to sync them to Telegram.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    var isButtonPressed by remember { mutableStateOf(false) }
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isButtonPressed) 0.96f else 1f,
                        animationSpec = Motion.SnappySpring,
                        label = "PermButtonScale"
                    )
                    
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .scale(buttonScale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isButtonPressed = true
                                        tryAwaitRelease()
                                        isButtonPressed = false
                                    }
                                )
                            }
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
            isLoading -> {
                // Animated loading state
                val loadingAlpha by animateFloatAsState(
                    targetValue = if (showContent) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium2),
                    label = "LoadingAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .graphicsLayer { alpha = loadingAlpha },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            label = "LoadingPulse"
                        )
                        
                        CircularProgressIndicator(
                            modifier = Modifier.scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Scanning folders...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            folders.isEmpty() -> {
                // Animated empty state
                val emptyAlpha by animateFloatAsState(
                    targetValue = if (showContent) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium3),
                    label = "EmptyAlpha"
                )
                val emptyTranslationY by animateFloatAsState(
                    targetValue = if (showContent) 0f else 32f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "EmptyTranslationY"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (showContent) 1f else 0.8f,
                    animationSpec = Motion.BouncySpring,
                    label = "EmptyIconScale"
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                        .graphicsLayer {
                            alpha = emptyAlpha
                            translationY = emptyTranslationY
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .scale(iconScale),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Media Folders Found",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No photos or videos were found on your device.\nTap the + button to add a folder manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Animated folder list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(items = folders, key = { _, folder -> folder.path }) { index, folder ->
                        // Staggered item entrance animation
                        var showItem by remember { mutableStateOf(false) }
                        LaunchedEffect(showContent) {
                            if (showContent) {
                                delay(Motion.staggerDelayWithCap(index, 40).toLong())
                                showItem = true
                            }
                        }
                        
                        val itemAlpha by animateFloatAsState(
                            targetValue = if (showItem) 1f else 0f,
                            animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                            label = "ItemAlpha$index"
                        )
                        val itemTranslationY by animateFloatAsState(
                            targetValue = if (showItem) 0f else 24f,
                            animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                            label = "ItemTranslationY$index"
                        )
                        val itemScale by animateFloatAsState(
                            targetValue = if (showItem) 1f else 0.95f,
                            animationSpec = Motion.EmphasizedSpring,
                            label = "ItemScale$index"
                        )
                        
                        // Card press animation
                        var isPressed by remember { mutableStateOf(false) }
                        val cardScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.98f else 1f,
                            animationSpec = Motion.SnappySpring,
                            label = "CardScale$index"
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = itemAlpha
                                    translationY = itemTranslationY
                                    scaleX = itemScale * cardScale
                                    scaleY = itemScale * cardScale
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            tryAwaitRelease()
                                            isPressed = false
                                        }
                                    )
                                }
                                .animateItem(
                                    placementSpec = Motion.LayoutSpringOffset
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Folder icon based on folder name
                                val folderIcon = when {
                                    folder.name.contains("Camera", ignoreCase = true) -> Icons.Default.Settings
                                    folder.name.contains("Download", ignoreCase = true) -> Icons.Default.Home
                                    folder.name.contains("Screenshot", ignoreCase = true) -> Icons.Default.Settings
                                    else -> Icons.Default.Home
                                }
                                
                                // Animated icon color based on sync status
                                val iconAlpha by animateFloatAsState(
                                    targetValue = if (folder.isAutoSync) 1f else 0.6f,
                                    animationSpec = tween(Motion.Duration.Short4),
                                    label = "IconAlpha$index"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = iconAlpha)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = folderIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(18.dp))
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    // Animated auto-sync indicator
                                    AnimatedVisibility(
                                        visible = folder.isAutoSync,
                                        enter = fadeIn(tween(Motion.Duration.Short4)) + 
                                            slideInVertically(
                                                initialOffsetY = { -it / 2 },
                                                animationSpec = tween(Motion.Duration.Medium2)
                                            ),
                                        exit = fadeOut(tween(Motion.Duration.Short3))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Auto-sync enabled",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                                
                                Switch(
                                    checked = folder.isAutoSync,
                                    onCheckedChange = { isChecked ->
                                        viewModel.toggleFolderSync(folder, isChecked)
                                    }
                                )
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
