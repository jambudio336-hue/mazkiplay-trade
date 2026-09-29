package com.mazkiplay.trade.data.api

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AiChatApi {
    @POST("chat/completions")
    suspend fun complete(
        @Body body: JsonObject,
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/jambudio336-hue/mazkiplay-trade",
        @Header("X-OpenRouter-Title") title: String = "Mazkiplay Trade"
    ): AiChatResponse
}

data class AiChatResponse(
    val choices: List<AiChoice>? = null,
    val error: AiError? = null
)

data class AiChoice(val message: AiMessage? = null)
data class AiMessage(val role: String? = null, val content: String? = null)
data class AiError(val message: String? = null, val code: Any? = null)
