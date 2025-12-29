package com.rrrainielll.teledrop.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrrainielll.teledrop.data.api.TelegramApiService
import com.rrrainielll.teledrop.data.prefs.SettingsManager
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
                // Verify Token
                val url = com.rrrainielll.teledrop.data.api.buildTelegramUrl(token, "getMe")
                val meResponse = apiService.getMe(url)
                if (!meResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Invalid Bot Token")
                    return@launch
                }

                // Verify Chat ID (Optional: Send a silent test message? For now just save)
                // We'll trust the user or the auto-detected ID.

                settingsManager.saveBotToken(token)
                settingsManager.saveChatId(chatId)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Connection failed")
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
    val isSuccess: Boolean = false
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
