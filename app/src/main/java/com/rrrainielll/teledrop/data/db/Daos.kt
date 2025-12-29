package com.rrrainielll.teledrop.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncFolderDao {
    @Query("SELECT * FROM sync_folders")
    suspend fun getAllFolders(): List<SyncFolderEntity>

    @Query("SELECT * FROM sync_folders WHERE isAutoSync = 1")
    suspend fun getAutoSyncFolders(): List<SyncFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: SyncFolderEntity)
    
    @Query("DELETE FROM sync_folders WHERE path = :path")
    suspend fun deleteFolder(path: String)
}

@Dao
interface UploadedMediaDao {
    @Query("SELECT EXISTS(SELECT 1 FROM uploaded_media WHERE mediaId = :mediaId)")
    suspend fun isUploaded(mediaId: Long): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM uploaded_media WHERE contentUri = :contentUri)")
    suspend fun isUploadedByUri(contentUri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUploaded(media: UploadedMediaEntity)
}
