package com.rrrainielll.teledrop.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Backup interval options
enum class BackupInterval(val displayName: String, val minutes: Long) {
    MANUAL("Manual", 0),
    WHEN_ADDED("When photos are added", -1),  // Special value: content observer trigger
    FIFTEEN_MINUTES("Every 15 minutes", 15),
    THIRTY_MINUTES("Every 30 minutes", 30),
    ONE_HOUR("Every hour", 60),
    SIX_HOURS("Every 6 hours", 360),
    DAILY("Daily", 1440)
}

class SettingsManager(private val context: Context) {
    companion object {
        private val BOT_TOKEN = stringPreferencesKey("bot_token")
        private val CHAT_ID = stringPreferencesKey("chat_id")
        private val BOT_USERNAME = stringPreferencesKey("bot_username")
        private val BACKUP_INTERVAL = stringPreferencesKey("backup_interval")
        private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    }

    val botToken: Flow<String?> = context.dataStore.data.map { it[BOT_TOKEN] }
    val chatId: Flow<String?> = context.dataStore.data.map { it[CHAT_ID] }
    val botUsername: Flow<String?> = context.dataStore.data.map { it[BOT_USERNAME] }
    
    // Backup schedule preference - defaults to MANUAL
    val backupInterval: Flow<BackupInterval> = context.dataStore.data.map { prefs ->
        val intervalName = prefs[BACKUP_INTERVAL] ?: BackupInterval.MANUAL.name
        BackupInterval.entries.find { it.name == intervalName } ?: BackupInterval.MANUAL
    }
    
    // Network preference - defaults to false (allow mobile data)
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WIFI_ONLY] ?: false
    }

    // Check if configuration is complete
    val isConfigured: Flow<Boolean> = context.dataStore.data.map {
        !it[BOT_TOKEN].isNullOrBlank() && !it[CHAT_ID].isNullOrBlank()
    }

    suspend fun saveBotToken(token: String) {
        context.dataStore.edit { it[BOT_TOKEN] = token }
    }

    suspend fun saveChatId(id: String) {
        context.dataStore.edit { it[CHAT_ID] = id }
    }
    
    suspend fun saveBotUsername(username: String) {
        context.dataStore.edit { it[BOT_USERNAME] = username }
    }
    
    suspend fun saveBackupInterval(interval: BackupInterval) {
        context.dataStore.edit { it[BACKUP_INTERVAL] = interval.name }
    }
    
    suspend fun saveWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[WIFI_ONLY] = wifiOnly }
    }
}
