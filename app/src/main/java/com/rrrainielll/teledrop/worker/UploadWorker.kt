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
import com.rrrainielll.teledrop.utils.FileHashUtil
import com.rrrainielll.teledrop.utils.ProgressRequestBody
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
        
        // Telegram API file size limits
        const val MAX_PHOTO_SIZE = 10L * 1024 * 1024  // 10MB
        const val MAX_VIDEO_SIZE = 50L * 1024 * 1024  // 50MB
        const val MAX_DOCUMENT_SIZE = 50L * 1024 * 1024  // 50MB
        
        // Input Data Keys
        const val KEY_URIS = "key_uris" // Array of String (URIs)
        const val KEY_URI_LIST_FILE = "key_uri_list_file" // String (Path to file containing URIs)
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
        val inputUriFile = inputData.getString(KEY_URI_LIST_FILE)

        val filesToUpload = mutableListOf<PendingFile>()

        if (!inputUris.isNullOrEmpty()) {
            // Manual Upload (Array)
            inputUris.forEach { uriString ->
                val uri = Uri.parse(uriString)
                filesToUpload.add(PendingFile(uri))
            }
        } else if (!inputUriFile.isNullOrBlank()) {
            // Manual Upload (File - for large selections)
            try {
                val file = File(inputUriFile)
                if (file.exists()) {
                    file.readLines().forEach { uriString ->
                        if (uriString.isNotBlank()) {
                            val uri = Uri.parse(uriString)
                            filesToUpload.add(PendingFile(uri))
                        }
                    }
                    // Cleanup list file
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("UploadWorker", "Error reading URI list file: ${e.message}")
            }
        } else if (isAutoSync) {
            // Auto Sync Scan
            val folders = database.syncFolderDao().getAutoSyncFolders()
            folders.forEach { folder ->
                val mediaList = app.mediaRepository.getMediaInFolder(folder.path)
                mediaList.forEach { media ->
                    // Calculate checksum for content-based duplicate detection
                    val checksum = FileHashUtil.calculateMD5(applicationContext, media.uri)
                    
                    // Check if uploaded using multiple methods
                    val isUploadedByChecksum = if (checksum != null) {
                        database.uploadedMediaDao().isUploadedByChecksum(checksum)
                    } else false
                    
                    val isUploadedById = if (media.id != 0L) {
                        database.uploadedMediaDao().isUploaded(media.id)
                    } else false
                    
                    val isUploadedByUri = database.uploadedMediaDao().isUploadedByUri(media.uri.toString())
                    
                    // Skip if already uploaded (prioritize checksum check)
                    if (!isUploadedByChecksum && !isUploadedById && !isUploadedByUri) {
                        filesToUpload.add(PendingFile(media.uri, media.id, media.name, media.size, media.type, checksum))
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
        
        var uploadedCount = 0
        var skippedCount = 0
        
        filesToUpload.forEachIndexed { index, file ->
            // Update Progress
            setProgressAsync(workDataOf(
                "current" to index + 1,
                "total" to total,
                "filename" to (file.name ?: "File"),
                "uri" to file.uri.toString()
            ))
            
            // Update Notification - Checking phase
            try {
                setForegroundAsync(createForegroundInfo(index + 1, total, file.name, "Checking"))
            } catch (e: Exception) {
                // Ignore if can't update foreground
            }


                // Calculate checksum if not already available
                val checksum = file.checksum ?: FileHashUtil.calculateMD5(applicationContext, file.uri)
                
                // Double-check for duplicates (in case file was uploaded during this run)
                val isDuplicate = if (checksum != null) {
                    database.uploadedMediaDao().isUploadedByChecksum(checksum)
                } else false
                
                if (isDuplicate) {
                    Log.d("UploadWorker", "Skipping duplicate: ${file.name} (checksum: $checksum)")
                    skippedCount++
                    try {
                        setForegroundAsync(createForegroundInfo(index + 1, total, file.name, "Skipped (duplicate)"))
                    } catch (e: Exception) { }
                    return@forEachIndexed
                }
                
                // Validate file size
                val validation = validateFileSize(file)
                val sendAsDocument = when (validation) {
                    is ValidationResult.TooLarge -> {
                        Log.w("UploadWorker", "Skipping ${file.name}: too large (${formatFileSize(file.size)}, max: ${formatFileSize(validation.maxSize)})")
                        skippedCount++
                        try {
                            setForegroundAsync(createForegroundInfo(
                                index + 1, total, file.name, 
                                "Skipped (too large: ${formatFileSize(file.size)})"
                            ))
                        } catch (e: Exception) { }
                        return@forEachIndexed
                    }
                    is ValidationResult.SendAsDocument -> {
                        Log.i("UploadWorker", "Sending ${file.name} as document (${formatFileSize(file.size)})")
                        true
                    }
                    ValidationResult.Valid -> false
                }
                
                // Update Notification - Uploading phase
                try {
                    setForegroundAsync(createForegroundInfo(index + 1, total, file.name, "Uploading"))
                } catch (e: Exception) { }
                
                // Declare tempFile outside try block for finally cleanup
                var tempFile: File? = null
                
                try {
                    // Copy stream to temp file because Retrofit needs a File or RequestBody from stream
                    tempFile = createTempFileFromUri(file.uri) ?: return@forEachIndexed

                    // Extract metadata for caption
                    val caption = extractMetadata(file.uri, file.name)
                    val captionPart = caption?.toRequestBody(MultipartBody.FORM)

                    // Wrap request body with progress tracking
                    val requestFile = tempFile.asRequestBody(null)
                    val fileSize = tempFile.length()
                    val progressRequestBody = ProgressRequestBody(requestFile) { uploadedBytes, totalBytes, percent, speedBps, etaSeconds ->
                        // Update progress data with detailed upload information
                        try {
                            setProgressAsync(workDataOf(
                                "current" to index + 1,
                                "total" to total,
                                "filename" to (file.name ?: "File"),
                                "uri" to file.uri.toString(),
                                "uploadPercent" to percent,
                                "uploadedBytes" to uploadedBytes,
                                "totalBytes" to totalBytes,
                                "uploadSpeedBps" to speedBps,
                                "etaSeconds" to etaSeconds
                            ))
                            
                            // Also update notification with detailed progress
                            setForegroundAsync(createForegroundInfo(
                                current = index + 1,
                                total = total,
                                fileName = file.name,
                                status = "Uploading",
                                uploadPercent = percent,
                                uploadedBytes = uploadedBytes,
                                totalBytes = totalBytes,
                                uploadSpeedBps = speedBps,
                                etaSeconds = etaSeconds
                            ))
                        } catch (e: Exception) {
                            // Ignore if progress update fails
                        }
                    }
                    
                    // Determine part name based on file type and size
                    val partName = when {
                        sendAsDocument -> "document"
                        file.type == MediaType.VIDEO -> "video"
                        else -> "photo"
                    }
                    
                    val body = MultipartBody.Part.createFormData(
                        partName,
                        file.name ?: "file",
                        progressRequestBody
                    )
                    val chatIdPart = chatId.toRequestBody(MultipartBody.FORM)

                    val response = when {
                        sendAsDocument -> {
                            val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "sendDocument")
                            api.sendDocument(url, chatIdPart, body, captionPart)
                        }
                        file.type == MediaType.VIDEO -> {
                            val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "sendVideo")
                            api.sendVideo(url, chatIdPart, body, captionPart)
                        }
                        else -> {
                            val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "sendPhoto")
                            api.sendPhoto(url, chatIdPart, body, captionPart)
                        }
                    }

                    if (response.isSuccessful) {
                        uploadedCount++
                        // Mark as Uploaded with checksum
                        database.uploadedMediaDao().insertUploaded(
                            UploadedMediaEntity(
                                mediaId = if (file.mediaId != 0L) file.mediaId else System.currentTimeMillis(),
                                contentUri = file.uri.toString(),
                                checksum = checksum,
                                size = file.size
                            )
                        )
                    } else {
                        Log.e("UploadWorker", "Upload failed: ${response.errorBody()?.string()}")
                        skippedCount++
                    }

                } catch (e: OutOfMemoryError) {
                    Log.e("UploadWorker", "Out of memory uploading ${file.name}", e)
                    skippedCount++
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e("UploadWorker", "Timeout uploading ${file.name}", e)
                    skippedCount++
                } catch (e: java.io.IOException) {
                    Log.e("UploadWorker", "IO error uploading ${file.name}: ${e.message}", e)
                    skippedCount++
                } catch (e: Exception) {
                    Log.e("UploadWorker", "Error uploading ${file.name}: ${e.message}", e)
                    skippedCount++
                } finally {
                    // Ensure temp file is always deleted
                    try {
                        tempFile?.delete()
                    } catch (e: Exception) {
                        Log.w("UploadWorker", "Failed to delete temp file", e)
                    }
                }
            }
        
        Log.i("UploadWorker", "Sync complete: $uploadedCount uploaded, $skippedCount skipped")

        if (isAutoSync) scheduleNextAutoSync()
        return Result.success()
    }

    private fun createForegroundInfo(
        current: Int, 
        total: Int, 
        fileName: String?, 
        status: String? = null,
        uploadPercent: Int? = null,
        uploadedBytes: Long? = null,
        totalBytes: Long? = null,
        uploadSpeedBps: Long? = null,
        etaSeconds: Long? = null
    ): ForegroundInfo {
        val title = "TeleDrop Syncing"
        
        val text = if (current > 0) {
            buildString {
                // File counter
                append("$current of $total")
                if (fileName != null) {
                    append(" - $fileName")
                }
                
                // Add detailed progress if available
                if (uploadPercent != null && uploadPercent > 0) {
                    append(" ($uploadPercent%)")
                }
                
                // Status prefix (Uploading, Checking, etc.)
                if (status != null && status != "Uploading") {
                    append(" - $status")
                }
            }
        } else {
            "Starting upload..."
        }
        
        // Build detailed subtext with speed and ETA
        val subText = if (uploadedBytes != null && totalBytes != null && totalBytes > 0) {
            buildString {
                append("${formatFileSize(uploadedBytes)} / ${formatFileSize(totalBytes)}")
                
                if (uploadSpeedBps != null && uploadSpeedBps > 0) {
                    append(" • ${formatSpeed(uploadSpeedBps)}")
                }
                
                if (etaSeconds != null && etaSeconds > 0) {
                    append(" • ${formatDuration(etaSeconds)} left")
                }
            }
        } else null
        
        // Progress bar shows current file progress if available, otherwise overall progress
        val progress = if (uploadPercent != null && uploadPercent > 0) {
            uploadPercent
        } else if (total > 0) {
            ((current.toFloat() / total.toFloat()) * 100).toInt()
        } else {
            0
        }
        
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, TeleDropApp.UPLOAD_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .apply {
                    if (subText != null) {
                        setSubText(subText)
                    }
                }
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
                .setContentTitle(title)
                .setContentText(text)
                .apply {
                    if (subText != null) {
                        setSubText(subText)
                    }
                }
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
        val type: MediaType = MediaType.PHOTO,
        val checksum: String? = null
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
    
    // Validate file size against Telegram API limits
    private fun validateFileSize(file: PendingFile): ValidationResult {
        return when (file.type) {
            MediaType.PHOTO -> {
                when {
                    file.size <= MAX_PHOTO_SIZE -> ValidationResult.Valid
                    file.size <= MAX_DOCUMENT_SIZE -> ValidationResult.SendAsDocument
                    else -> ValidationResult.TooLarge("Photo", MAX_DOCUMENT_SIZE)
                }
            }
            MediaType.VIDEO -> {
                when {
                    file.size <= MAX_VIDEO_SIZE -> ValidationResult.Valid
                    else -> ValidationResult.TooLarge("Video", MAX_VIDEO_SIZE)
                }
            }
        }
    }
}

// Sealed class for file validation results
sealed class ValidationResult {
    object Valid : ValidationResult()
    object SendAsDocument : ValidationResult()
    data class TooLarge(val type: String, val maxSize: Long) : ValidationResult()
}
