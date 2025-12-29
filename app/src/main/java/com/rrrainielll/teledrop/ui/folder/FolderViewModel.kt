package com.rrrainielll.teledrop.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrrainielll.teledrop.data.db.AppDatabase
import com.rrrainielll.teledrop.data.db.SyncFolderEntity
import com.rrrainielll.teledrop.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolderViewModel(
    private val database: AppDatabase,
    private val repository: MediaRepository
) : ViewModel() {

    private val _folders = MutableStateFlow<List<SyncFolderEntity>>(emptyList())
    val folders: StateFlow<List<SyncFolderEntity>> = _folders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get all folders from storage (device scan)
                val deviceFolders = repository.getMediaFolders()
                
                // Get saved config from DB
                val savedFolders = database.syncFolderDao().getAllFolders()
                
                // Merge: If device folder exists in DB, use DB version (to keep isAutoSync). 
                // If not, use device version.
                val mergedFolders = deviceFolders.map { deviceFolder ->
                    savedFolders.find { it.path == deviceFolder.path } ?: deviceFolder
                }

                _folders.value = mergedFolders
            } catch (e: Exception) {
                // Log error but don't crash - empty list is acceptable
                _folders.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshFolders() {
        loadFolders()
    }

    fun toggleFolderSync(folder: SyncFolderEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedFolder = folder.copy(isAutoSync = isEnabled)
            database.syncFolderDao().insertFolder(updatedFolder)
            
            // Update local state
            _folders.value = _folders.value.map { 
                if (it.path == folder.path) updatedFolder else it 
            }
        }
    }
    
    fun addCustomFolder(path: String) {
        viewModelScope.launch {
            val folderName = path.substringAfterLast("/").ifBlank { "Custom Folder" }
            val fullPath = if (path.startsWith("/")) path else "/storage/emulated/0/$path"
            
            val newFolder = SyncFolderEntity(
                path = fullPath,
                name = folderName,
                isAutoSync = true
            )
            
            database.syncFolderDao().insertFolder(newFolder)
            
            // Refresh folder list
            refreshFolders()
        }
    }
}

class FolderViewModelFactory(
    private val database: AppDatabase,
    private val repository: MediaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FolderViewModel(database, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
