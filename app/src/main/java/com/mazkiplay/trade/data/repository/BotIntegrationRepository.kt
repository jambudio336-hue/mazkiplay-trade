package com.mazkiplay.trade.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mazkiplay.trade.data.api.NetworkModule
import com.mazkiplay.trade.data.model.BotChannel
import com.mazkiplay.trade.data.model.BotDeliveryResult
import com.mazkiplay.trade.data.model.BotTrigger
import com.mazkiplay.trade.data.model.UnifiedSignalPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Outbound-only unified signal bot. It never receives or executes broker orders. */
class BotIntegrationRepository(private val context: Context) {
    private val refs = context.getSharedPreferences("bot_credential_refs", Context.MODE_PRIVATE)
    private val alias = "mazkiplay_bot_vault"
    private val key: SecretKey by lazy {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    private val lastSent = mutableMapOf<String, Long>()

    fun save(channel: BotChannel, values: Map<String, String>) { values.forEach { (field, value) -> if (value.isNotBlank()) put("${channel.name}_$field", value) } }
    fun configured(channel: BotChannel): Boolean = when (channel) {
        BotChannel.TELEGRAM -> get("TELEGRAM_token").isNotBlank() && get("TELEGRAM_chatId").isNotBlank()
        BotChannel.WHATSAPP -> get("WHATSAPP_token").isNotBlank() && get("WHATSAPP_phoneNumberId").isNotBlank() && get("WHATSAPP_recipient").isNotBlank()
        BotChannel.DISCORD -> get("DISCORD_webhookUrl").startsWith("https://discord.com/api/webhooks/") || get("DISCORD_webhookUrl").startsWith("https://discordapp.com/api/webhooks/")
    }
    fun delete(channel: BotChannel) { refs.all.keys.filter { it.startsWith("${channel.name}_") }.forEach { refs.edit().remove(it).apply() } }

    suspend fun send(channel: BotChannel, payload: UnifiedSignalPayload, trigger: BotTrigger? = null, force: Boolean = false): BotDeliveryResult = withContext(Dispatchers.IO) {
        if (!configured(channel)) return@withContext BotDeliveryResult(channel, false, message = "Belum dikonfigurasi")
        val key = "${channel.name}:${payload.symbol}:${payload.decision}:${trigger?.name ?: "manual"}"
        val now = System.currentTimeMillis()
        if (!force && now - (lastSent[key] ?: 0L) < TimeUnit.MINUTES.toMillis(2)) return@withContext BotDeliveryResult(channel, false, message = "Deduplicated/rate limited")
        val text = format(payload)
        val result = when (channel) {
            BotChannel.TELEGRAM -> postTelegram(text)
            BotChannel.DISCORD -> postDiscord(text)
            BotChannel.WHATSAPP -> postWhatsApp(text)
        }
        if (result.success) lastSent[key] = now
        result
    }

    fun format(payload: UnifiedSignalPayload): String = """🚨 MAZKIPLAY TRADE SIGNAL

${payload.symbol}
⏱ Timeframe: ${payload.timeframe}
📊 Market: ${payload.marketStatus}

${decisionEmoji(payload.decision)} DECISION: ${payload.decision}

Entry: ${payload.entry}
SL: ${payload.stopLoss}
TP1: ${payload.takeProfit1}
TP2: ${payload.takeProfit2}

Risk: ${payload.riskPercent}
R:R: ${payload.rr}
Lot: ${payload.lot}

━━━━━━━━━━━━━━
📊 TECHNICAL: ${payload.technical}
🌎 FUNDAMENTAL: ${payload.fundamental}
🏦 MACRO: ${payload.macro}
📰 NEWS: ${payload.news}
📈 CONFLUENCE: ${payload.confluence}

⚠️ Risk Guardian: ${payload.riskGuardian}
🤖 Mazkiplay: ${payload.explanation}

Status: ${payload.status}

Data-driven notification. Not financial advice. No automatic order execution.""".trimIndent()

    private fun decisionEmoji(decision: String) = when (decision.uppercase()) { "BUY" -> "🟢"; "SELL" -> "🔴"; else -> "🟡" }
    private fun postTelegram(text: String): BotDeliveryResult {
        val body = JSONObject().put("chat_id", get("TELEGRAM_chatId")).put("text", text).put("disable_web_page_preview", true).toString().toRequestBody("application/json".toMediaType())
        return execute(BotChannel.TELEGRAM, Request.Builder().url("https://api.telegram.org/bot${get("TELEGRAM_token")}/sendMessage").post(body).build())
    }
    private fun postDiscord(text: String): BotDeliveryResult {
        val body = JSONObject().put("content", text.take(2000)).put("allowed_mentions", JSONObject().put("parse", emptyList<String>())).toString().toRequestBody("application/json".toMediaType())
        return execute(BotChannel.DISCORD, Request.Builder().url(get("DISCORD_webhookUrl")).post(body).build())
    }
    private fun postWhatsApp(text: String): BotDeliveryResult {
        val body = JSONObject().put("messaging_product", "whatsapp").put("to", get("WHATSAPP_recipient")).put("type", "text").put("text", JSONObject().put("preview_url", false).put("body", text.take(4096))).toString().toRequestBody("application/json".toMediaType())
        return execute(BotChannel.WHATSAPP, Request.Builder().url("https://graph.facebook.com/v20.0/${get("WHATSAPP_phoneNumberId")}/messages").header("Authorization", "Bearer ${get("WHATSAPP_token")}").post(body).build())
    }
    private fun execute(channel: BotChannel, request: Request): BotDeliveryResult = runCatching { NetworkModule.client.newCall(request).execute().use { response -> BotDeliveryResult(channel, response.isSuccessful, response.code, if (response.isSuccessful) "Terkirim" else "HTTP ${response.code}: ${response.body?.string()?.take(180)}") } }.getOrElse { BotDeliveryResult(channel, false, message = it.message ?: "Network error") }
    private fun put(name: String, value: String) {
        val nonce = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce)) }
        refs.edit().putString(name, Base64.encodeToString(nonce + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)).apply()
    }
    private fun get(name: String): String = runCatching {
        val bytes = Base64.decode(refs.getString(name, "") ?: "", Base64.NO_WRAP)
        if (bytes.size < 13) "" else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), StandardCharsets.UTF_8)
        }
    }.getOrDefault("")
}
