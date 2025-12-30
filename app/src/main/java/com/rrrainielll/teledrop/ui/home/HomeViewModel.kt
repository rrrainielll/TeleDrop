package com.rrrainielll.teledrop.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import android.provider.MediaStore
import com.rrrainielll.teledrop.worker.UploadWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)

    private val settingsManager = com.rrrainielll.teledrop.data.prefs.SettingsManager(application)
    val botTokenFlow = settingsManager.botToken
    val botUsernameFlow = settingsManager.botUsername

    // Observe work status
    val workStatus = workManager.getWorkInfosByTagFlow("sync_work")
    
    init {
        viewModelScope.launch {
            settingsManager.botToken.collect { token ->
                if (!token.isNullOrBlank()) {
                    val currentUsername = settingsManager.botUsername.first()
                    if (currentUsername.isNullOrBlank()) {
                        fetchBotUsername(token)
                    }
                }
            }
        }
    }

    private suspend fun fetchBotUsername(token: String) {
        try {
            val app = getApplication<com.rrrainielll.teledrop.TeleDropApp>()
            val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "getMe")
            val response = app.apiService.getMe(url)
            if (response.isSuccessful && response.body()?.ok == true) {
                val user = response.body()?.result
                if (user?.username != null) {
                    settingsManager.saveBotUsername(user.username)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_IS_AUTO_SYNC to true))
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("sync_work")
            .build()

        workManager.enqueue(syncRequest)
    }

    fun uploadSelectedMedia(uris: List<android.net.Uri>) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val uploadRequestBuilder = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("manual_upload_work")
            .addTag("sync_work")

        // Data limit is 10KB. If we have many URIs, pass via file.
        // Assuming ~100 chars per URI, 50 URIs = 5KB. Safety limit: 30.
        if (uris.size > 30) {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                val file = java.io.File.createTempFile("selected_media", ".txt", cacheDir)
                file.printWriter().use { out ->
                    uris.forEach { out.println(it.toString()) }
                }
                
                uploadRequestBuilder.setInputData(workDataOf(
                    UploadWorker.KEY_URI_LIST_FILE to file.absolutePath,
                    UploadWorker.KEY_IS_AUTO_SYNC to false
                ))
            } catch (e: Exception) {
                
                // Fallback to array if file creation fails (risky but better than nothing)
                val uriStrings = uris.map { it.toString() }.toTypedArray()
                uploadRequestBuilder.setInputData(workDataOf(
                    UploadWorker.KEY_URIS to uriStrings,
                    UploadWorker.KEY_IS_AUTO_SYNC to false
                ))
            }
        } else {
            val uriStrings = uris.map { it.toString() }.toTypedArray()
            uploadRequestBuilder.setInputData(workDataOf(
                UploadWorker.KEY_URIS to uriStrings,
                UploadWorker.KEY_IS_AUTO_SYNC to false
            ))
        }

        workManager.enqueue(uploadRequestBuilder.build())
    }

    fun enableAutoSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .build()
            
        val autoSyncRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_IS_AUTO_SYNC to true))
            .setConstraints(constraints)
            .addTag("auto_sync_observer")
            .build()
            
        // Use KEEP to avoid replacing if already running/scheduled
        workManager.enqueueUniqueWork(
            "auto_sync_observer",
            androidx.work.ExistingWorkPolicy.KEEP,
            autoSyncRequest
        )
    }
}
