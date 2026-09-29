package com.mazkiplay.trade.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Provider abstraction: keys are encrypted locally and never logged or committed. */
enum class AiMode(val label: String) { AUTO("AUTO"), LOCAL_ONLY("LOCAL ONLY"), CLOUD_FIRST("CLOUD FIRST") }
data class AiProviderConfig(val id: String, val name: String, val protocol: String, val baseUrl: String, val model: String, val configured: Boolean)
data class AiProviderStatus(val config: AiProviderConfig, val state: String, val latencyMillis: Long? = null, val detail: String = "")

class AiProviderRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("ai_provider_refs", Context.MODE_PRIVATE)
    private val alias = "mazkiplay_ai_vault"
    private val key: SecretKey by lazy {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    val providers: List<AiProviderConfig> = listOf(
        AiProviderConfig("local", "Local Qwen / Gemma", "llama.cpp", "", "qwen-small.gguf", false),
        AiProviderConfig("openrouter", "OpenRouter", "openai-compatible", "https://openrouter.ai/api/v1", "", false),
        AiProviderConfig("openai", "OpenAI", "openai-compatible", "https://api.openai.com/v1", "", false),
        AiProviderConfig("gemini", "Google Gemini", "native", "https://generativelanguage.googleapis.com", "", false),
        AiProviderConfig("anthropic", "Anthropic", "native", "https://api.anthropic.com", "", false),
        AiProviderConfig("groq", "Groq", "openai-compatible", "https://api.groq.com/openai/v1", "", false),
        AiProviderConfig("mistral", "Mistral", "openai-compatible", "https://api.mistral.ai/v1", "", false),
        AiProviderConfig("deepseek", "DeepSeek", "openai-compatible", "https://api.deepseek.com/v1", "", false),
        AiProviderConfig("twelvedata", "Twelve Data", "REST market-data", "https://api.twelvedata.com", "market-data", false),
        AiProviderConfig("custom", "Custom Provider", "openai-compatible", "", "", false)
    )

    fun saveCredential(providerId: String, apiKey: String): Result<Unit> = runCatching {
        require(apiKey.trim().isNotEmpty()) { "Kunci API tidak boleh kosong." }
        val nonce = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        }
        val encrypted = cipher.doFinal(apiKey.trim().toByteArray(StandardCharsets.UTF_8))
        check(prefs.edit().putString(providerId, Base64.encodeToString(nonce + encrypted, Base64.NO_WRAP)).commit()) {
            "Credential gagal ditulis ke penyimpanan aman."
        }
    }

    fun hasCredential(providerId: String): Boolean = !prefs.getString(providerId, null).isNullOrBlank()
    fun deleteCredential(providerId: String) { prefs.edit().remove(providerId).apply() }
    /** Decrypts only at request time; callers must never log or persist the result. */
    fun readCredential(providerId: String): String? = runCatching {
        val packed = prefs.getString(providerId, null) ?: return@runCatching null
        val bytes = Base64.decode(packed, Base64.NO_WRAP)
        if (bytes.size <= 12) return@runCatching null
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }.doFinal(bytes.copyOfRange(12, bytes.size)).toString(StandardCharsets.UTF_8)
    }.getOrNull()
    fun statuses(): List<AiProviderStatus> = providers.map { config ->
        val ready = config.id == "local" && hasCredential("local_model") || hasCredential(config.id)
        AiProviderStatus(config.copy(configured = ready), if (ready) "READY" else "NOT CONFIGURED", detail = if (ready) "Credential reference stored in Keystore vault" else "Add credential/model")
    }
}
