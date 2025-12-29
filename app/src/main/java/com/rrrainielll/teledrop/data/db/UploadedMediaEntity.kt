package com.rrrainielll.teledrop.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "uploaded_media",
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class UploadedMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long, // MediaStore ID consistency check
    val contentUri: String,
    val checksum: String? = null, // Optional for stricter dedup
    val size: Long,
    val uploadDate: Long = System.currentTimeMillis()
)
