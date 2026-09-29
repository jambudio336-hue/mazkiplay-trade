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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.AiProviderStatus
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiCenterScreen(app: MazkiplayApp) {
    var selected by remember { mutableStateOf<AiProviderStatus?>(null) }
    var credential by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var providerRefresh by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Make the status card recompose after a successful save/test.
    val statuses = remember(providerRefresh) { app.aiProviders.statuses() }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Mazkiplay AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Engine menghitung angka · AI menjelaskan · pengguna tetap mengonfirmasi", style = MaterialTheme.typography.bodySmall)
        }
        item {
            SectionCard(title = "AI Router", subtitle = "Lokal terlebih dahulu → cloud sebagai cadangan", trailing = { Pill("ROUTING AMAN", Bull, filled = true) }) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("OTOMATIS", color = Bull, fontWeight = FontWeight.Bold)
                    Text("Prioritas: Local Qwen/Gemma → provider cloud yang dikonfigurasi → fallback berikutnya. LLM tidak memiliki akses ke private broker/exchange API.", style = MaterialTheme.typography.bodySmall)
                    Text("Teknikal, fundamental, berita, dan risk engine tetap menjadi sumber angka kritis.", style = MaterialTheme.typography.labelSmall, color = Gold)
                }
            }
        }
        item {
            SectionCard(title = "Pengelola Model", subtitle = "Model tidak dibundel ke APK utama") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Text("Qwen Small · GGUF"); Text("Runtime llama.cpp · LOKAL/OFFLINE", style = MaterialTheme.typography.labelSmall) }
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/Qwen"))) }) { Text("BUKA MODEL") }
                }
            }
        }
        item {
            SectionCard(title = "Koneksi Provider", subtitle = "Kunci API disimpan terenkripsi dengan Android Keystore") {
                Text("Tidak ada kunci developer di source. Setelah disimpan, aplikasi melakukan tes koneksi dan menampilkan statusnya.", style = MaterialTheme.typography.bodySmall)
            }
        }
        items(statuses) { status ->
            SectionCard(
                title = status.config.name,
                subtitle = "${status.config.protocol} · ${status.config.baseUrl.ifBlank { "model lokal" }}",
                trailing = { Pill(status.state, if (status.state == "READY") Bull else Gold, filled = status.state == "READY") }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (status.config.id == "twelvedata" && status.state == "READY") {
                        Text("Credential tersimpan. Tekan tes koneksi untuk memastikan provider merespons.", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            selected = status
                            credential = ""
                            model = status.config.model.ifBlank { app.aiInference.defaultModel(status.config.id) }
                        }) { Text(if (status.state == "READY") "PERBARUI" else "KONFIGURASI") }
                        if (status.state == "READY") {
                            TextButton(onClick = {
                                if (status.config.id == "twelvedata") {
                                    scope.launch {
                                        notice = "Menguji koneksi Twelve Data…"
                                        val result = withContext(Dispatchers.IO) { app.twelveData.testConnection() }
                                        notice = result.fold({ "Twelve Data terhubung dan merespons." }, { "Twelve Data gagal: ${it.message ?: "kesalahan tidak diketahui"}" })
                                    }
                                } else if (status.config.id in setOf("openrouter", "openai", "groq", "mistral", "deepseek", "custom")) {
                                    scope.launch {
                                        notice = "Menguji inferensi ${status.config.name}…"
                                        val provider = status.config.copy(model = status.config.model.ifBlank { app.aiInference.defaultModel(status.config.id) })
                                        val result = withContext(Dispatchers.IO) { app.aiInference.test(provider) }
                                        notice = result.fold({ "${status.config.name} aktif dan berhasil menjawab." }, { "${status.config.name} gagal: ${it.message ?: "periksa key/model/internet"}" })
                                    }
                                }
                            }) { Text(if (status.config.id == "twelvedata") "TES KONEKSI" else if (status.config.id in setOf("openrouter", "openai", "groq", "mistral", "deepseek", "custom")) "TES AI" else "PERIKSA") }
                            TextButton(onClick = { app.aiProviders.deleteCredential(status.config.id); providerRefresh++ }) { Text("HAPUS") }
                        }
                    }
                }
            }
        }
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Status Provider") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("OK") } }
        )
    }

    selected?.let { status ->
        AlertDialog(
            onDismissRequest = { if (!saving) selected = null },
            title = { Text("Konfigurasi ${status.config.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = credential,
                        onValueChange = { credential = it },
                        label = { Text(if (status.config.id == "local") "Lokasi / referensi model" else "Kunci API") },
                        singleLine = true,
                        enabled = !saving
                    )
                    if (status.config.id in setOf("openrouter", "openai", "groq", "mistral", "deepseek", "custom")) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("ID model") },
                            placeholder = { Text(if (status.config.id == "openrouter") "openrouter/free" else "Masukkan ID model provider") },
                            singleLine = true,
                            enabled = !saving
                        )
                        Text(
                            if (status.config.id == "openrouter") "ID yang disarankan: openrouter/free — ketik persis, termasuk garis miring. Model gratis tetap memiliki batas rate limit."
                            else "Masukkan ID model resmi dari provider ini.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text("Kunci hanya disimpan lokal di Android Keystore dan tidak ditampilkan ulang.", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(enabled = !saving, onClick = {
                    scope.launch {
                        saving = true
                        notice = null
                        val keyId = if (status.config.id == "local") "local_model" else status.config.id
                        val result = withContext(Dispatchers.IO) { app.aiProviders.saveCredential(keyId, credential) }
                        saving = false
                        result.fold(
                            onSuccess = {
                                if (status.config.id in setOf("openrouter", "openai", "groq", "mistral", "deepseek", "custom")) {
                                    app.aiProviders.saveModel(status.config.id, model)
                                }
                                selected = null
                                providerRefresh++
                                if (status.config.id == "twelvedata") {
                                    notice = "Kunci tersimpan. Menguji koneksi Twelve Data…"
                                    val connection = withContext(Dispatchers.IO) { app.twelveData.testConnection() }
                                    notice = connection.fold({ "Twelve Data terhubung dan merespons." }, { "Kunci tersimpan, tetapi koneksi gagal: ${it.message ?: "periksa key, paket, atau internet"}" })
                                } else if (status.config.id in setOf("openrouter", "openai", "groq", "mistral", "deepseek", "custom")) {
                                    val provider = status.config.copy(model = model.ifBlank { app.aiInference.defaultModel(status.config.id) })
                                    notice = "Kunci tersimpan. Menguji inferensi ${status.config.name}…"
                                    val response = withContext(Dispatchers.IO) { app.aiInference.test(provider) }
                                    notice = response.fold({ "${status.config.name} aktif dan berhasil menjawab." }, { "Kunci tersimpan, tetapi tes AI gagal: ${it.message ?: "periksa key/model/internet"}" })
                                } else {
                                    notice = "Credential ${status.config.name} berhasil disimpan dengan aman."
                                }
                            },
                            onFailure = { notice = "Gagal menyimpan credential: ${it.message ?: "periksa keamanan perangkat"}" }
                        )
                    }
                }) {
                    if (saving) CircularProgressIndicator(strokeWidth = 2.dp) else Text("SIMPAN KE BRANKAS")
                }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { selected = null }) { Text("BATAL") } }
        )
    }
}
