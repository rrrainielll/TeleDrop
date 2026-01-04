package com.rrrainielll.teledrop.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrrainielll.teledrop.data.api.TelegramApiService
import com.rrrainielll.teledrop.data.api.buildTelegramUrl
import com.rrrainielll.teledrop.data.prefs.SettingsManager
import com.rrrainielll.teledrop.utils.DeviceInfoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SetupViewModel(
    private val settingsManager: SettingsManager,
    private val apiService: TelegramApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onTokenChanged(token: String) {
        _uiState.value = _uiState.value.copy(botToken = token, error = null)
    }

    fun onChatIdChanged(chatId: String) {
        _uiState.value = _uiState.value.copy(chatId = chatId, error = null)
    }

    fun verifyAndSave() {
        val token = _uiState.value.botToken.trim()
        val chatId = _uiState.value.chatId.trim()

        if (token.isBlank() || chatId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Token and Chat ID are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Verify Token (Logic extracted to verifyToken, but kept here for "Connect" flow consistency or call verifyToken?)
                // To keep it simple, we'll just check again or rely on the state.
                // But verifyAndSave is "Commit".
                
                val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "getMe")
                val meResponse = apiService.getMe(url)
                
                if (meResponse.isSuccessful && meResponse.body()?.ok == true) {
                    val botUser = meResponse.body()?.result
                    val username = botUser?.username
                    
                    if (username != null) {
                        _uiState.value = _uiState.value.copy(botUsername = username)
                        // Save username if verifying
                        settingsManager.saveBotUsername(username)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Invalid Bot Token")
                    return@launch
                }

                // Verify Chat ID (Optional: Send a silent test message? For now just save)
                // We'll trust the user or the auto-detected ID.

                settingsManager.saveBotToken(token)
                settingsManager.saveChatId(chatId)
                
                // Send registration notification (fire and forget)
                sendRegistrationNotification(token, chatId)
                
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Connection failed")
            }
        }
    }
    
    fun fetchBotUsername() {
        val token = _uiState.value.botToken.trim()
        if (token.isBlank()) return

        viewModelScope.launch {
            try {
                 val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "getMe")
                 val meResponse = apiService.getMe(url)
                 if (meResponse.isSuccessful && meResponse.body()?.ok == true) {
                     val botUser = meResponse.body()?.result
                     _uiState.value = _uiState.value.copy(botUsername = botUser?.username)
                 }
            } catch (e: Exception) {
                // Silent failure or log
            }
        }
    }

    /**
     * Sends a notification to Telegram when a new device is registered.
     * Creates a forum topic (thread) for the device and sends details there.
     * This is fire-and-forget - failures are silently ignored.
     */
    private fun sendRegistrationNotification(token: String, chatId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SetupViewModel", "Sending registration notification...")
                
                val deviceName = DeviceInfoHelper.getDeviceName()
                val androidVersion = DeviceInfoHelper.getAndroidVersion()
                val ip = DeviceInfoHelper.getPublicIpAddress() ?: "Unknown"
                val locationInfo = DeviceInfoHelper.getLocationFromIp(ip)
                val dateTime = DeviceInfoHelper.getCurrentDateTime()
                
                // Build location display with optional Google Maps link
                val locationDisplay = if (locationInfo.mapsLink != null) {
                    "<a href=\"${locationInfo.mapsLink}\">${locationInfo.displayName}</a>"
                } else {
                    locationInfo.displayName
                }
                
                val message = """
🔔 <b>New Device Registered</b>

📱 <b>Device:</b> $deviceName
📲 <b>OS:</b> $androidVersion
🌐 <b>IP:</b> $ip
📍 <b>Location:</b> $locationDisplay
📅 <b>Date:</b> $dateTime
                """.trimIndent()
                
                android.util.Log.d("SetupViewModel", "Attempting to create forum topic for device: $deviceName")
                
                // Try to create a forum topic (thread) for this device
                var threadId: Int? = null
                try {
                    val topicUrl = buildTelegramUrl(token, "createForumTopic")
                    val topicName = "📱 $deviceName"
                    val topicResponse = apiService.createForumTopic(topicUrl, chatId, topicName)
                    
                    if (topicResponse.isSuccessful && topicResponse.body()?.ok == true) {
                        threadId = topicResponse.body()?.result?.message_thread_id
                        android.util.Log.d("SetupViewModel", "Forum topic created with thread_id: $threadId")
                    } else {
                        android.util.Log.w("SetupViewModel", "Failed to create forum topic: ${topicResponse.code()} - ${topicResponse.errorBody()?.string()}")
                        // Continue without thread - will send as regular message
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SetupViewModel", "Forum topic creation failed (chat may not be a forum): ${e.message}")
                    // Continue without thread - will send as regular message
                }
                
                // Send the message (to thread if created, otherwise regular message)
                val url = buildTelegramUrl(token, "sendMessage")
                val response = apiService.sendMessage(
                    url = url,
                    chatId = chatId,
                    text = message,
                    parseMode = "HTML",
                    messageThreadId = threadId,
                    disableWebPagePreview = true
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("SetupViewModel", "Registration notification sent successfully!" + 
                        if (threadId != null) " (in thread $threadId)" else "")
                } else {
                    android.util.Log.e("SetupViewModel", "Failed to send notification: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SetupViewModel", "Error sending registration notification: ${e.message}", e)
            }
        }
    }


    fun autoDetectChatId() {
        val token = _uiState.value.botToken.trim()
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter Bot Token first")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Determine if we need to fetch bot info first? 
                // Currently autoDetectChatId is for getting updates.
                // We might want to ensure we have the username here too if possible, but getUpdates doesn't return it.
                // Ignoring for now.
                
                val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "getUpdates")
                val updatesCalls = apiService.getUpdates(url)
                if (updatesCalls.isSuccessful) {
                    val updates = updatesCalls.body()?.result
                    if (!updates.isNullOrEmpty()) {
                        // Find the most recent message
                        val lastMessage = updates.lastOrNull()?.message
                        if (lastMessage != null) {
                            val detectedId = lastMessage.chat.id.toString()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false, 
                                chatId = detectedId,
                                infoMessage = "Found Chat ID from ${lastMessage.chat.type}!"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = "No messages found. Send /start to your bot first!")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "No updates found. Please send a message to your bot.")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to fetch updates. Check Token.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Error: ${e.message}")
            }
        }
    }
}

data class SetupUiState(
    val botToken: String = "",
    val chatId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val isSuccess: Boolean = false,
    val botUsername: String? = null
)

class SetupViewModelFactory(
    private val settingsManager: SettingsManager,
    private val apiService: TelegramApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(settingsManager, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
