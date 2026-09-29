package com.mazkiplay.trade.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.AiProviderStatus
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold

@Composable
fun AiCenterScreen(app: MazkiplayApp) {
    var selected by remember { mutableStateOf<AiProviderStatus?>(null) }
    var credential by remember { mutableStateOf("") }
    val context = LocalContext.current
    val statuses = app.aiProviders.statuses()
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mazkiplay AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Engine menghitung angka · AI menjelaskan · user tetap mengonfirmasi", style = MaterialTheme.typography.bodySmall) }
        item { SectionCard(title = "AI Router", subtitle = "Local first → cloud fallback", trailing = { Pill("SAFE ROUTING", Bull, filled = true) }) { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("AUTO", color = Bull, fontWeight = FontWeight.Bold); Text("Prioritas: Local Qwen/Gemma → provider cloud yang dikonfigurasi → fallback berikutnya. LLM tidak memiliki akses ke private broker/exchange API.", style = MaterialTheme.typography.bodySmall); Text("Technical, fundamental, news, dan risk engine tetap menjadi sumber angka kritis.", style = MaterialTheme.typography.labelSmall, color = Gold) } } }
        item { SectionCard(title = "Model Manager", subtitle = "Model tidak dibundel ke APK utama") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Column(Modifier.weight(1f)) { Text("Qwen Small · GGUF"); Text("Runtime llama.cpp · LOCAL/OFFLINE", style = MaterialTheme.typography.labelSmall) }; Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/Qwen"))) }) { Text("BUKA MODEL") } } } }
        item { SectionCard(title = "Provider Connection", subtitle = "API key disimpan terenkripsi dengan Android Keystore") { Text("Tidak ada developer key di source. Gunakan provider milik Anda sendiri dan hapus credential kapan saja.", style = MaterialTheme.typography.bodySmall) } }
        items(statuses) { status ->
            SectionCard(title = status.config.name, subtitle = "${status.config.protocol} · ${status.config.baseUrl.ifBlank { "local model" }}", trailing = { Pill(status.state, if (status.state == "READY") Bull else Gold, filled = status.state == "READY") }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { selected = status; credential = "" }) { Text(if (status.state == "READY") "UPDATE" else "CONFIGURE") }; if (status.state == "READY") TextButton(onClick = { app.aiProviders.deleteCredential(status.config.id) }) { Text("DELETE") } }
            }
        }
    }
    selected?.let { status -> AlertDialog(onDismissRequest = { selected = null }, title = { Text("Configure ${status.config.name}") }, text = { OutlinedTextField(value = credential, onValueChange = { credential = it }, label = { Text(if (status.config.id == "local") "Model path / reference" else "API key") }, singleLine = true) }, confirmButton = { Button(onClick = { app.aiProviders.saveCredential(if (status.config.id == "local") "local_model" else status.config.id, credential); selected = null }) { Text("SAVE VAULT") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("CANCEL") } }) }
}
