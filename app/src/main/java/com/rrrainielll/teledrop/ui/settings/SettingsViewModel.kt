package com.rrrainielll.teledrop.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rrrainielll.teledrop.data.prefs.BackupInterval
import com.rrrainielll.teledrop.data.prefs.SettingsManager
import com.rrrainielll.teledrop.worker.UploadWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager(application)
    private val workManager = WorkManager.getInstance(application)
    
    val backupInterval = settingsManager.backupInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupInterval.MANUAL)
    
    val wifiOnly = settingsManager.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    fun setBackupInterval(interval: BackupInterval) {
        viewModelScope.launch {
            settingsManager.saveBackupInterval(interval)
            schedulePeriodicBackup(interval)
        }
    }
    
    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveWifiOnly(enabled)
            // Re-schedule backup with new network constraint
            val currentInterval = backupInterval.value
            if (currentInterval != BackupInterval.MANUAL) {
                schedulePeriodicBackup(currentInterval)
            }
        }
    }
    
    private fun schedulePeriodicBackup(interval: BackupInterval) {
        // Cancel any existing scheduled/periodic work first
        workManager.cancelUniqueWork("scheduled_backup")
        workManager.cancelUniqueWork("content_observer_backup")
        
        if (interval == BackupInterval.MANUAL) {
            // Nothing more to do - work is already cancelled
            return
        }
        
        val networkType = if (wifiOnly.value) NetworkType.UNMETERED else NetworkType.CONNECTED
        
        if (interval == BackupInterval.WHEN_ADDED) {
            // Use content observer trigger for immediate backup when new media is added
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .addContentUriTrigger(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                .addContentUriTrigger(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
                .build()
            
            val contentObserverRequest = androidx.work.OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(workDataOf(UploadWorker.KEY_IS_AUTO_SYNC to true))
                .setConstraints(constraints)
                .addTag("content_observer_backup")
                .build()
            
            workManager.enqueueUniqueWork(
                "content_observer_backup",
                androidx.work.ExistingWorkPolicy.KEEP,
                contentObserverRequest
            )
        } else {
            // Schedule periodic backup at the specified interval
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()
            
            val periodicRequest = PeriodicWorkRequestBuilder<UploadWorker>(
                interval.minutes, TimeUnit.MINUTES
            )
                .setInputData(workDataOf(UploadWorker.KEY_IS_AUTO_SYNC to true))
                .setConstraints(constraints)
                .addTag("scheduled_backup")
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                "scheduled_backup",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }
    }
}

