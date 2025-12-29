package com.rrrainielll.teledrop.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.rrrainielll.teledrop.data.db.SyncFolderEntity
import com.rrrainielll.teledrop.data.model.MediaModel
import com.rrrainielll.teledrop.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    suspend fun getMediaFolders(): List<SyncFolderEntity> = withContext(Dispatchers.IO) {
        val folders = mutableMapOf<String, SyncFolderEntity>()
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        // Query Images
        queryFolders(folders, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection)
        // Query Videos
        queryFolders(folders, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection)

        folders.values.toList().sortedBy { it.name }
    }

    private fun queryFolders(
        folders: MutableMap<String, SyncFolderEntity>,
        uri: android.net.Uri,
        projection: Array<String>
    ) {
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                
                // Get parent directory path
                val parentFile = File(path).parentFile
                val parentPath = parentFile?.absolutePath ?: continue

                if (!folders.containsKey(parentPath)) {
                    folders[parentPath] = SyncFolderEntity(
                        path = parentPath,
                        name = bucketName
                    )
                }
            }
        }
    }

    suspend fun getMediaInFolder(folderPath: String): List<MediaModel> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaModel>()
        
        // Photos
        queryMedia(
            mediaList,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaType.PHOTO,
            folderPath
        )
        
        // Videos
        queryMedia(
            mediaList,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaType.VIDEO,
            folderPath
        )

        mediaList.sortedByDescending { it.dateAdded }
    }

    private fun queryMedia(
        list: MutableList<MediaModel>,
        contentUri: android.net.Uri,
        type: MediaType,
        folderPath: String
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA,
            if (type == MediaType.VIDEO) MediaStore.Video.Media.DURATION else MediaStore.MediaColumns.DATE_ADDED
        )

        // Filter by path using LIKE (Not perfect but standard for bucket path filtering)
        // A more robust way is querying by BUCKET_ID, but path is easier for our generic logic right now since we store path in DB.
        val selection = "${MediaStore.MediaColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%")

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val durationColumn = if (type == MediaType.VIDEO) cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mime = cursor.getString(mimeColumn)
                val date = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L

                val uri = ContentUris.withAppendedId(contentUri, id)

                list.add(
                    MediaModel(
                        id = id,
                        uri = uri,
                        name = name ?: "Unknown",
                        mimeType = mime ?: "application/octet-stream",
                        dateAdded = date,
                        size = size,
                        type = type,
                        duration = duration
                    )
                )
            }
        }
    }
}
