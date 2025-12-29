package com.rrrainielll.teledrop.data.model

import android.net.Uri

enum class MediaType {
    PHOTO, VIDEO
}

data class MediaModel(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateAdded: Long,
    val size: Long,
    val type: MediaType,
    val duration: Long = 0 // For videos
)
