package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.BotChannel
import com.mazkiplay.trade.data.model.UnifiedSignalPayload
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun BotCenterScreen(app: MazkiplayApp) {
    val scope = rememberCoroutineScope()
    var channel by remember { mutableStateOf<BotChannel?>(null) }
    var result by remember { mutableStateOf("") }
    val payload = UnifiedSignalPayload("EURUSD", "M15", "WAIT", "LIVE", "--", "--", "--", "--", "1%", "1:2", "--", "Technical context tersedia", "Fundamental context tersedia", "Macro context tersedia", "No immediate release", "WAIT / conflict check", "No critical warning", "Contoh format preview; sinyal production hanya dikirim dari data aktual.")
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mazkiplay Signal Bot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Unified Market Decision Engine → Telegram · WhatsApp Business · Discord", style = MaterialTheme.typography.bodySmall) }
        item { SectionCard(title = "Decision pipeline", subtitle = "Satu laporan konfluensi, bukan indikator terpisah") { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Market Data → Technical → Fundamental → Macro → News → Live Event → Signal → Risk → AI Explanation", color = Gold, fontWeight = FontWeight.Bold); Text("Bot tidak mengarang angka, tidak mengirim setiap tick, dan tidak mengeksekusi order.", style = MaterialTheme.typography.bodySmall) } } }
        item { SectionCard(title = "Channels", subtitle = "Credential disimpan terenkripsi Android Keystore") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { BotChannel.values().forEach { target -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(target.label, Modifier.weight(1f)); Pill(if (app.botIntegration.configured(target)) "CONFIGURED" else "NOT CONFIGURED", if (app.botIntegration.configured(target)) Bull else Gold, filled = app.botIntegration.configured(target)); Button(onClick = { channel = target }) { Text("CONFIGURE") } } } } } }
        item { SectionCard(title = "Unified signal preview", subtitle = "Template resmi sebelum koneksi dikirim") { Text(app.botIntegration.format(payload), style = MaterialTheme.typography.bodySmall); Button(onClick = { channel = BotChannel.TELEGRAM; result = "Pilih channel dan simpan credential dulu." }) { Text("SETUP / TEST") } } }
        if (result.isNotBlank()) item { SectionCard(title = "Delivery status") { Text(result, color = if (result.startsWith("OK")) Bull else Bear) } }
        item { SectionCard(title = "Event-driven triggers", subtitle = "Anti-spam + dedup window 2 menit") { Text("New BUY/SELL · WAIT · breakout · reversal · TP1/TP2 · SL · spread · volatility · breaking news · high-impact countdown · LIVE NOW · released · post-news reanalysis · macro regime", style = MaterialTheme.typography.bodySmall) } }
    }
    channel?.let { target -> BotConfigDialog(app, target, onClose = { channel = null }, onResult = { result = it }) }
}

@Composable
private fun BotConfigDialog(app: MazkiplayApp, channel: BotChannel, onClose: () -> Unit, onResult: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var third by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val title = when (channel) { BotChannel.TELEGRAM -> "Bot token"; BotChannel.WHATSAPP -> "Access token"; BotChannel.DISCORD -> "Webhook URL" }
    AlertDialog(onDismissRequest = onClose, title = { Text("Configure ${channel.label}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(first, { first = it }, label = { Text(title) }, singleLine = true)
            when (channel) {
                BotChannel.TELEGRAM -> OutlinedTextField(second, { second = it }, label = { Text("Chat ID / @channel") }, singleLine = true)
                BotChannel.WHATSAPP -> { OutlinedTextField(second, { second = it }, label = { Text("Phone Number ID") }, singleLine = true); OutlinedTextField(third, { third = it }, label = { Text("Recipient phone") }, singleLine = true) }
                BotChannel.DISCORD -> Text("Gunakan Incoming Webhook URL dari channel Discord.", style = MaterialTheme.typography.labelSmall)
            }
            Text("Test mengirim hanya pesan preview WAIT. Jangan masukkan credential akun pribadi atau broker.", style = MaterialTheme.typography.labelSmall, color = Gold)
        }
    }, confirmButton = { Button(onClick = {
        val values = when (channel) { BotChannel.TELEGRAM -> mapOf("token" to first, "chatId" to second); BotChannel.WHATSAPP -> mapOf("token" to first, "phoneNumberId" to second, "recipient" to third); BotChannel.DISCORD -> mapOf("webhookUrl" to first) }
        app.botIntegration.save(channel, values)
        scope.launch {
            val payload = UnifiedSignalPayload("EURUSD", "M15", "WAIT", "LIVE", "--", "--", "--", "--", "1%", "1:2", "--", "Preview", "Preview", "Preview", "Preview", "WAIT", "Preview only", "Konfigurasi berhasil disimpan; ini pesan test.")
            val sent = app.botIntegration.send(channel, payload, force = true)
            onResult(if (sent.success) "OK · ${sent.message}" else "FAILED · ${sent.message}")
        }
        onClose()
    }) { Text("SAVE + TEST") } }, dismissButton = { TextButton(onClick = onClose) { Text("CANCEL") } })
}
