package com.rrrainielll.teledrop.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        private val BOT_TOKEN = stringPreferencesKey("bot_token")
        private val CHAT_ID = stringPreferencesKey("chat_id")
    }

    val botToken: Flow<String?> = context.dataStore.data.map { it[BOT_TOKEN] }
    val chatId: Flow<String?> = context.dataStore.data.map { it[CHAT_ID] }

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
}
