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
                                val progressValue = if (total > 0) current.toFloat() / total.toFloat() else 0f

                                if (uriString != null) {
                                    AsyncImage(
                                        model = uriString,
                                        contentDescription = "Uploading Image",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(16.dp))
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
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(
                                            if (lastWork != null && lastWork.state == WorkInfo.State.SUCCEEDED)
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (lastWork != null && lastWork.state == WorkInfo.State.SUCCEEDED) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_launcher_background),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
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
                    }
                }
            }
        }
    }
}
