package com.mazkiplay.trade.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.Gson
import com.mazkiplay.trade.data.api.AiChatResponse
import com.mazkiplay.trade.data.api.AiChatApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Real inference lane for OpenAI-compatible providers; keys are read only from the vault. */
class AiInferenceRepository(
    private val credentials: AiProviderRepository,
    private val client: OkHttpClient
) {
    private val defaultModels = mapOf(
        "openrouter" to "openrouter/free",
        "openai" to "gpt-4o-mini",
        "groq" to "llama-3.1-8b-instant",
        "mistral" to "mistral-small-latest",
        "deepseek" to "deepseek-chat",
        "custom" to ""
    )

    fun defaultModel(providerId: String): String = defaultModels[providerId].orEmpty()

    suspend fun test(provider: AiProviderConfig): Result<String> = runCatching {
        require(provider.id in setOf("openrouter", "openai", "groq", "mistral", "deepseek", "custom")) {
            "Provider ini memakai protokol native dan belum tersedia di jalur kompatibel."
        }
        complete(provider, "Balas singkat dalam Bahasa Indonesia: koneksi aktif.")
    }

    suspend fun complete(provider: AiProviderConfig, prompt: String): String {
        val key = credentials.readCredential(provider.id)?.takeIf { it.isNotBlank() }
            ?: error("API key ${provider.name} belum dikonfigurasi.")
        val model = provider.model.ifBlank { defaultModel(provider.id) }
            .takeIf { it.isNotBlank() } ?: error("Model belum dipilih untuk ${provider.name}.")
        val baseUrl = provider.baseUrl.trimEnd('/') + "/"
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiChatApi::class.java)
        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", "Anda adalah asisten Mazkiplay Trade. Jelaskan analisa secara ringkas dalam Bahasa Indonesia. Jangan mengarang harga atau menjanjikan profit.")
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", prompt)
            })
        }
        val body = JsonObject().apply {
            addProperty("model", model)
            add("messages", messages)
            addProperty("temperature", 0.2)
            addProperty("max_tokens", 300)
        }
        val response = api.complete(body, "Bearer $key")
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val providerMessage = runCatching {
                Gson().fromJson(errorBody, AiChatResponse::class.java)?.error?.message
            }.getOrNull()
            error("${provider.name} HTTP ${response.code()}: ${providerMessage ?: "permintaan ditolak"}")
        }
        val result = response.body() ?: error("${provider.name} tidak mengembalikan data.")
        result.error?.message?.let { error("${provider.name}: $it") }
        return result.choices?.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
            ?: error("${provider.name} tidak mengembalikan jawaban.")
    }
}
