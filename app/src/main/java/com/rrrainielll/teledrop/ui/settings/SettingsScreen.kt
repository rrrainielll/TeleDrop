package com.rrrainielll.teledrop.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrrainielll.teledrop.R
import com.rrrainielll.teledrop.data.prefs.BackupInterval
import com.rrrainielll.teledrop.ui.theme.Motion
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val backupInterval by viewModel.backupInterval.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val scrollState = rememberScrollState()
    
    // Staggered animation states
    var showBackButton by remember { mutableStateOf(false) }
    var showBackupCard by remember { mutableStateOf(false) }
    var showNetworkCard by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(50)
        showBackButton = true
        delay(100)
        showBackupCard = true
        delay(100)
        showNetworkCard = true
    }
    
    // Calculate scroll-based elevation
    val topBarElevation by animateDpAsState(
        targetValue = if (scrollState.value > 0) 4.dp else 0.dp,
        animationSpec = tween(Motion.Duration.Short4),
        label = "TopBarElevation"
    )
    
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
            containerColor = Color.Transparent,
            topBar = {
                // Animated back button
                val backButtonAlpha by animateFloatAsState(
                    targetValue = if (showBackButton) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium2, easing = Motion.EmphasizedDecelerate),
                    label = "BackButtonAlpha"
                )
                val backButtonScale by animateFloatAsState(
                    targetValue = if (showBackButton) 1f else 0.8f,
                    animationSpec = Motion.EmphasizedSpring,
                    label = "BackButtonScale"
                )
                
                TopAppBar(
                    title = { 
                        Text(
                            "Settings",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.graphicsLayer { alpha = backButtonAlpha }
                        ) 
                    },
                    navigationIcon = {
                        var isPressed by remember { mutableStateOf(false) }
                        val iconScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.85f else 1f,
                            animationSpec = Motion.SnappySpring,
                            label = "BackIconScale"
                        )
                        
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = backButtonAlpha
                                    scaleX = backButtonScale * iconScale
                                    scaleY = backButtonScale * iconScale
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
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (topBarElevation > 0.dp) 
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        else 
                            Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Animated Backup Schedule Section
                val backupCardAlpha by animateFloatAsState(
                    targetValue = if (showBackupCard) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "BackupCardAlpha"
                )
                val backupCardTranslationY by animateFloatAsState(
                    targetValue = if (showBackupCard) 0f else 32f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "BackupCardTranslationY"
                )
                val backupCardScale by animateFloatAsState(
                    targetValue = if (showBackupCard) 1f else 0.95f,
                    animationSpec = Motion.EmphasizedSpring,
                    label = "BackupCardScale"
                )
                
                AnimatedSettingsSection(
                    title = "Backup Schedule",
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.graphicsLayer {
                        alpha = backupCardAlpha
                        translationY = backupCardTranslationY
                        scaleX = backupCardScale
                        scaleY = backupCardScale
                    }
                ) {
                    Column(
                        modifier = Modifier.selectableGroup()
                    ) {
                        BackupInterval.entries.forEachIndexed { index, interval ->
                            // Stagger animation for each radio option
                            var showOption by remember { mutableStateOf(false) }
                            LaunchedEffect(showBackupCard) {
                                if (showBackupCard) {
                                    delay((index * 30).toLong())
                                    showOption = true
                                }
                            }
                            
                            val optionAlpha by animateFloatAsState(
                                targetValue = if (showOption) 1f else 0f,
                                animationSpec = tween(Motion.Duration.Short4, easing = Motion.EmphasizedDecelerate),
                                label = "OptionAlpha$index"
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { 
                                        alpha = optionAlpha
                                    }
                                    .selectable(
                                        selected = backupInterval == interval,
                                        onClick = { 
                                            viewModel.setBackupInterval(interval)
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = backupInterval == interval,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = interval.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                
                // Animated Network Preference Section
                val networkCardAlpha by animateFloatAsState(
                    targetValue = if (showNetworkCard) 1f else 0f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "NetworkCardAlpha"
                )
                val networkCardTranslationY by animateFloatAsState(
                    targetValue = if (showNetworkCard) 0f else 32f,
                    animationSpec = tween(Motion.Duration.Medium3, easing = Motion.EmphasizedDecelerate),
                    label = "NetworkCardTranslationY"
                )
                val networkCardScale by animateFloatAsState(
                    targetValue = if (showNetworkCard) 1f else 0.95f,
                    animationSpec = Motion.EmphasizedSpring,
                    label = "NetworkCardScale"
                )
                
                AnimatedSettingsSection(
                    title = "Network Preference",
                    icon = Icons.Default.Info,
                    modifier = Modifier.graphicsLayer {
                        alpha = networkCardAlpha
                        translationY = networkCardTranslationY
                        scaleX = networkCardScale
                        scaleY = networkCardScale
                    }
                ) {
                    var isPressed by remember { mutableStateOf(false) }
                    val rowScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.98f else 1f,
                        animationSpec = Motion.SnappySpring,
                        label = "NetworkRowScale"
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(rowScale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WiFi Only",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (wifiOnly) 
                                    "Backups only when connected to WiFi" 
                                else 
                                    "Backups allowed on mobile data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = { viewModel.setWifiOnly(it) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AnimatedSettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

