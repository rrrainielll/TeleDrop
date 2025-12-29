package com.rrrainielll.teledrop.data.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface TelegramApiService {

    @GET
    suspend fun getMe(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun getUpdates(
        @Url url: String,
        @Query("offset") offset: Int? = null
    ): Response<GetUpdatesResponse>

    @Multipart
    @POST
    suspend fun sendPhoto(
        @Url url: String,
        @Part("chat_id") chatId: okhttp3.RequestBody,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: okhttp3.RequestBody? = null
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun sendVideo(
        @Url url: String,
        @Part("chat_id") chatId: okhttp3.RequestBody,
        @Part video: MultipartBody.Part,
        @Part("caption") caption: okhttp3.RequestBody? = null
    ): Response<ResponseBody>
    
    // For general files or backup
    @Multipart
    @POST
    suspend fun sendDocument(
        @Url url: String,
        @Part("chat_id") chatId: okhttp3.RequestBody,
        @Part document: MultipartBody.Part,
        @Part("caption") caption: okhttp3.RequestBody? = null
    ): Response<ResponseBody>
}

// Helper function to build URLs
fun buildTelegramUrl(token: String, method: String): String {
    return "https://api.telegram.org/bot$token/$method"
}

// Minimal response classes for GetUpdates
data class GetUpdatesResponse(
    val ok: Boolean,
    val result: List<UpdateResult>
)

data class UpdateResult(
    val update_id: Int,
    val message: Message?
)

data class Message(
    val message_id: Int,
    val from: User?,
    val chat: Chat,
    val text: String?
)

data class User(
    val id: Long,
    val is_bot: Boolean,
    val first_name: String
)

data class Chat(
    val id: Long,
    val type: String
)
