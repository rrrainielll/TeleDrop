package com.rrrainielll.teledrop.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_folders")
data class SyncFolderEntity(
    @PrimaryKey
    val path: String, // Absolute path to the folder/bucket, e.g., "DCIM/Camera"
    val name: String,
    val isAutoSync: Boolean = false,
    val lastSyncTime: Long = 0
)
