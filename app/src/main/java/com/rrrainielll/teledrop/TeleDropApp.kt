package com.rrrainielll.teledrop

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.rrrainielll.teledrop.data.api.ApiClient
import com.rrrainielll.teledrop.data.api.TelegramApiService
import com.rrrainielll.teledrop.data.db.AppDatabase
import com.rrrainielll.teledrop.data.prefs.SettingsManager
import com.rrrainielll.teledrop.data.repository.MediaRepository

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class TeleDropApp : Application(), ImageLoaderFactory {

    lateinit var settingsManager: SettingsManager
    lateinit var database: AppDatabase
    lateinit var mediaRepository: MediaRepository
    lateinit var apiService: TelegramApiService
    
    // Lazy API client since it might need token headers later (though current impl passes token in method)
    // val apiService by lazy { ApiClient.instance }

    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel for uploads
        createNotificationChannel()
        
        settingsManager = SettingsManager(this)
        database = AppDatabase.getDatabase(this)
        mediaRepository = MediaRepository(this)
        apiService = ApiClient.instance
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPLOAD_CHANNEL_ID,
                "Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows upload progress for photos and videos"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
    
    companion object {
        const val UPLOAD_CHANNEL_ID = "teledrop_upload_channel"
    }
}
