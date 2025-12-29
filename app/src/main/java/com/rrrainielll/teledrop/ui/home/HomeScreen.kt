package com.rrrainielll.teledrop.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.work.WorkInfo
import coil.compose.AsyncImage
import com.rrrainielll.teledrop.R
import com.rrrainielll.teledrop.ui.components.SquircleProgressIndicator
import com.rrrainielll.teledrop.ui.folder.FolderSelectionScreen
import com.rrrainielll.teledrop.ui.folder.FolderViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onManageFolders: () -> Unit
) {
    // Observing Flow from ViewModel
    val workInfos by viewModel.workStatus.collectAsState(initial = emptyList())
    
    val isSyncing = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    val lastWork = workInfos.find { it.state == WorkInfo.State.RUNNING } ?: workInfos.firstOrNull()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.enableAutoSync()
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
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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

                    
                    // App Icon/Logo

                    
                    // Enhanced Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                        if (isSyncing) {
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
                                val progressValue = if (total > 0) current.toFloat() / total.toFloat() else 0f

                                // Thumbnail with squircle progress around it
                                val squircleCornerRadius = 32.dp // ~27% of 120dp for squircle effect
                                
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(140.dp)
                                ) {
                                    // Squircle progress indicator (outline around squircle)
                                    if (uploadPercent > 0) {
                                        SquircleProgressIndicator(
                                            progress = uploadPercent / 100f,
                                            modifier = Modifier.size(140.dp),
                                            cornerRadius = squircleCornerRadius + 10.dp, // Slightly larger for outline effect
                                            strokeWidth = 6.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                    
                                    // Thumbnail image in squircle shape or loading indicator
                                    if (uriString != null) {
                                        AsyncImage(
                                            model = uriString,
                                            contentDescription = "Uploading Image",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(squircleCornerRadius))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(56.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 5.dp
                                        )
                                    }
                                    
                                    // Percentage text overlay with matching squircle shape
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

                                if (total > 0) {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = progressValue,
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                } else {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (lastWork != null && lastWork.state == WorkInfo.State.SUCCEEDED)
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (lastWork != null && lastWork.state == WorkInfo.State.SUCCEEDED) {
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
                                
                                if (lastWork != null && lastWork.state == WorkInfo.State.SUCCEEDED) {
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
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Enhanced Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = viewModel::triggerSync,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(16.dp)
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
                        
                        OutlinedButton(
                            onClick = onManageFolders,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
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
                        
                        // New Photo Picker Integration
                        val maxItems = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                             android.provider.MediaStore.getPickImagesMaxLimit()
                        } else {
                             100 // Fallback for older versions
                        }
                        
                        val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(maxItems)
                        ) { uris ->
                            if (uris.isNotEmpty()) {
                                viewModel.uploadSelectedMedia(uris)
                            }
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Select from Photos",
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
