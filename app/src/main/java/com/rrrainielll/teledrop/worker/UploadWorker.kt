package com.rrrainielll.teledrop.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rrrainielll.teledrop.TeleDropApp
import com.rrrainielll.teledrop.data.db.UploadedMediaEntity
import com.rrrainielll.teledrop.data.model.MediaType
import kotlinx.coroutines.flow.first
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class UploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID = 1001
        
        // Input Data Keys
        const val KEY_URIS = "key_uris" // Array of String (URIs)
        const val KEY_IS_AUTO_SYNC = "key_is_auto_sync" // Boolean
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as TeleDropApp
        val settings = app.settingsManager
        val database = app.database
        val api = app.apiService

        // 1. Get Config
        val token = settings.botToken.first()
        val chatId = settings.chatId.first()

        if (token.isNullOrBlank() || chatId.isNullOrBlank()) {
            return Result.failure()
        }

        // 2. Identify files to upload
        val isAutoSync = inputData.getBoolean(KEY_IS_AUTO_SYNC, false)
        val inputUris = inputData.getStringArray(KEY_URIS)

        val filesToUpload = mutableListOf<PendingFile>()

        if (!inputUris.isNullOrEmpty()) {
            // Manual Upload
            inputUris.forEach { uriString ->
                val uri = Uri.parse(uriString)
                filesToUpload.add(PendingFile(uri))
            }
        } else if (isAutoSync) {
            // Auto Sync Scan
            val folders = database.syncFolderDao().getAutoSyncFolders()
            folders.forEach { folder ->
                val mediaList = app.mediaRepository.getMediaInFolder(folder.path)
                mediaList.forEach { media ->
                    // Check if uploaded (by ID or URI)
                    val isUploadedById = if (media.id != 0L) {
                        database.uploadedMediaDao().isUploaded(media.id)
                    } else false
                    
                    val isUploadedByUri = database.uploadedMediaDao().isUploadedByUri(media.uri.toString())
                    
                    if (!isUploadedById && !isUploadedByUri) {
                        filesToUpload.add(PendingFile(media.uri, media.id, media.name, media.size, media.type))
                    }
                }
            }
        } else {
            return Result.success() // Nothing to do
        }

        if (filesToUpload.isEmpty()) {
            if (isAutoSync) scheduleNextAutoSync()
            return Result.success()
        }

        // 3. Set as expedited work (Android 12+ compatible)
        try {
            setForegroundAsync(createForegroundInfo(0, filesToUpload.size, null))
        } catch (e: Exception) {
            // If foreground fails, continue anyway with regular notifications
            Log.w("UploadWorker", "Could not set foreground: ${e.message}")
        }

        // 4. Start Upload Loop
        val total = filesToUpload.size
        
        filesToUpload.forEachIndexed { index, file ->
            // Update Progress
            setProgressAsync(workDataOf(
                "current" to index + 1,
                "total" to total,
                "filename" to (file.name ?: "File"),
                "uri" to file.uri.toString()
            ))
            
            // Update Notification
            try {
                setForegroundAsync(createForegroundInfo(index + 1, total, file.name))
            } catch (e: Exception) {
                // Ignore if can't update foreground
            }

            try {
                // Copy stream to temp file because Retrofit needs a File or RequestBody from stream
                val tempFile = createTempFileFromUri(file.uri) ?: return@forEachIndexed

                // Extract metadata for caption
                val caption = extractMetadata(file.uri, file.name)
                val captionPart = caption?.toRequestBody(MultipartBody.FORM)

                val requestFile = tempFile.asRequestBody(null)
                val body = MultipartBody.Part.createFormData(
                    if (file.type == MediaType.VIDEO) "video" else "photo",
                    file.name ?: "file",
                    requestFile
                )
                val chatIdPart = chatId.toRequestBody(MultipartBody.FORM)

                val response = if (file.type == MediaType.VIDEO) {
                    val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "sendVideo")
                    api.sendVideo(url, chatIdPart, body, captionPart)
                } else {
                    val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "sendPhoto")
                    api.sendPhoto(url, chatIdPart, body, captionPart)
                }

                if (response.isSuccessful) {
                    // Mark as Uploaded - use URI as fallback if mediaId is 0
                    database.uploadedMediaDao().insertUploaded(
                        UploadedMediaEntity(
                            mediaId = if (file.mediaId != 0L) file.mediaId else System.currentTimeMillis(), // Use timestamp as unique ID if no mediaId
                            contentUri = file.uri.toString(),
                            size = file.size
                        )
                    )
                } else {
                    Log.e("UploadWorker", "Failed: ${response.errorBody()?.string()}")
                }

                // Cleanup
                tempFile.delete()

            } catch (e: Exception) {
                Log.e("UploadWorker", "Error uploading ${file.name}", e)
                // Continue to next file
            }
        }

        if (isAutoSync) scheduleNextAutoSync()
        return Result.success()
    }

    private fun createForegroundInfo(current: Int, total: Int, fileName: String?): ForegroundInfo {
        val title = "TeleDrop Syncing"
        val text = if (current > 0) {
            "Uploading $current of $total${if (fileName != null) ": $fileName" else ""}"
        } else {
            "Starting upload..."
        }
        val progress = if (total > 0) ((current.toFloat() / total.toFloat()) * 100).toInt() else 0
        
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, TeleDropApp.UPLOAD_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setProgress(100, progress, false)
                .build()
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun extractMetadata(uri: Uri, fileName: String?): String? {
        return try {
            val cursor = applicationContext.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.MediaStore.MediaColumns.DATE_ADDED,
                    android.provider.MediaStore.MediaColumns.DATE_MODIFIED,
                    android.provider.MediaStore.MediaColumns.SIZE,
                    android.provider.MediaStore.MediaColumns.WIDTH,
                    android.provider.MediaStore.MediaColumns.HEIGHT,
                    android.provider.MediaStore.MediaColumns.MIME_TYPE,
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val dateAdded = it.getLongOrNull(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_ADDED))
                    val size = it.getLongOrNull(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.SIZE))
                    val width = it.getIntOrNull(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.WIDTH))
                    val height = it.getIntOrNull(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.HEIGHT))
                    val mimeType = it.getStringOrNull(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.MIME_TYPE))
                    val displayName = it.getStringOrNull(it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME))
                    
                    // Build caption with metadata
                    buildString {
                        if (displayName != null) {
                            append("📁 $displayName\n")
                        }
                        if (dateAdded != null) {
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(dateAdded * 1000))
                            append("📅 $date\n")
                        }
                        if (width != null && height != null) {
                            append("📐 ${width}x${height}\n")
                        }
                        if (size != null) {
                            val sizeInMB = size / (1024.0 * 1024.0)
                            append("💾 %.2f MB".format(sizeInMB))
                        }
                    }.takeIf { it.isNotBlank() }
                } else null
            }
        } catch (e: Exception) {
            Log.e("UploadWorker", "Error extracting metadata", e)
            null
        }
    }

    // Helper extensions for nullable cursor columns
    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else null
    }

    private fun android.database.Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getInt(columnIndex) else null
    }

    private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex) else null
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = applicationContext.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload", "tmp", applicationContext.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class PendingFile(
        val uri: Uri,
        val mediaId: Long = 0,
        val name: String? = null,
        val size: Long = 0,
        val type: MediaType = MediaType.PHOTO
    )

    private fun scheduleNextAutoSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .addContentUriTrigger(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(KEY_IS_AUTO_SYNC to true))
            .setConstraints(constraints)
            .addTag("auto_sync_observer")
            .build()

        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "auto_sync_observer",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
