package com.rrrainielll.teledrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.rrrainielll.teledrop.ui.home.HomeScreen
import com.rrrainielll.teledrop.ui.setup.SetupScreen
import com.rrrainielll.teledrop.ui.setup.SetupViewModel
import com.rrrainielll.teledrop.ui.setup.SetupViewModelFactory
import com.rrrainielll.teledrop.ui.theme.TeleDropTheme
import com.rrrainielll.teledrop.utils.PermissionHelper

class MainActivity : ComponentActivity() {
    
    private var permissionsGranted by mutableStateOf(false)
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }
    
    private var onFolderSelected: ((String) -> Unit)? = null
    
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { selectedUri ->
            // Extract folder path from URI
            val path = selectedUri.path?.substringAfter("primary:")
            if (!path.isNullOrBlank()) {
                onFolderSelected?.invoke(path)
            }
        }
    }
    
    fun launchFolderPicker(onSelected: (String) -> Unit) {
        onFolderSelected = onSelected
        folderPickerLauncher.launch(null)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check initial permission state
        permissionsGranted = PermissionHelper.hasMediaPermissions(this)
        
        val app = application as TeleDropApp
        val factory = SetupViewModelFactory(app.settingsManager, app.apiService)
        val setupViewModel = ViewModelProvider(this, factory)[SetupViewModel::class.java]

        setContent {
            TeleDropTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isConfigured by app.settingsManager.isConfigured.collectAsState(initial = null)
                    
                    var currentScreen by remember { mutableStateOf(Screen.Setup) }
                    
                    // Respond to config state
                    LaunchedEffect(isConfigured) {
                        if (isConfigured == true) {
                            currentScreen = Screen.Home
                        }
                    }

                    when (currentScreen) {
                        Screen.Setup -> {
                            if (isConfigured == true) {
                                // Already configured, waiting for Effect to switch. 
                                // Or we could just show Home, but logic above handles it.
                            } else {
                                SetupScreen(
                                    viewModel = setupViewModel,
                                    onSetupComplete = { currentScreen = Screen.Home }
                                )
                            }
                        }
                        Screen.Home -> {
                            val homeViewModel = ViewModelProvider(this)[com.rrrainielll.teledrop.ui.home.HomeViewModel::class.java]
                            HomeScreen(
                                viewModel = homeViewModel,
                                onManageFolders = { currentScreen = Screen.Folders },
                                onSettings = { currentScreen = Screen.Settings }
                            )
                        }
                        Screen.Folders -> {
                            // Request permissions if not granted
                            LaunchedEffect(Unit) {
                                if (!permissionsGranted) {
                                    permissionLauncher.launch(PermissionHelper.getRequiredMediaPermissions())
                                }
                            }
                            
                            val folderFactory = com.rrrainielll.teledrop.ui.folder.FolderViewModelFactory(app.database, app.mediaRepository)
                            val folderViewModel = ViewModelProvider(this, folderFactory)[com.rrrainielll.teledrop.ui.folder.FolderViewModel::class.java]
                            
                            androidx.activity.compose.BackHandler {
                                currentScreen = Screen.Home
                            }
                            
                            com.rrrainielll.teledrop.ui.folder.FolderSelectionScreen(
                                viewModel = folderViewModel,
                                hasPermissions = permissionsGranted,
                                onRequestPermissions = {
                                    permissionLauncher.launch(PermissionHelper.getRequiredMediaPermissions())
                                },
                                onPickFolder = {
                                    launchFolderPicker { path ->
                                        folderViewModel.addCustomFolder(path)
                                    }
                                }
                            )
                        }
                        Screen.Settings -> {
                            val settingsViewModel = ViewModelProvider(this)[com.rrrainielll.teledrop.ui.settings.SettingsViewModel::class.java]
                            
                            androidx.activity.compose.BackHandler {
                                currentScreen = Screen.Home
                            }
                            
                            com.rrrainielll.teledrop.ui.settings.SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Screen {
    Setup, Home, Folders, Settings
}