package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.R
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.settingsViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.AppConstants
import com.mazkiplay.trade.util.Formatters

/** Settings: appearance, locale, notification switches and trading defaults. */
@Composable
fun SettingsScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = settingsViewModel(app)
    val current by vm.preferences.collectAsState()
    var showDonation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.settings, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("${AppConstants.APP_NAME} v${AppConstants.VERSION} • ${AppConstants.SIGNATURE}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SectionCard(title = s.language, subtitle = "Bahasa antarmuka") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("in" to "Indonesia", "en" to "English").forEach { (code, label) ->
                        val selected = current.language == code
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { vm.setLanguage(code) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = s.theme, subtitle = "Tampilan aplikasi") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (code, label) ->
                        val selected = current.theme == code
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { vm.setTheme(code) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = s.brightness, subtitle = "${(current.brightness * 100).toInt()}%") {
                Slider(
                    value = current.brightness,
                    onValueChange = { vm.setBrightness(it) },
                    valueRange = 0.3f..1.0f
                )
            }
        }

        item {
            SectionCard(title = s.timeFormat, subtitle = "Format waktu dan tanggal") {
                Column {
                    Text("Waktu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("24h" to "24 jam", "12h" to "12 jam").forEach { (code, label) ->
                            val selected = current.timeFormat == code
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTimeFormat(code) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Tanggal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd").forEach { pattern ->
                            val selected = current.dateFormat == pattern
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setDateFormat(pattern) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pattern, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Contoh: ${Formatters.time(System.currentTimeMillis(), current.dateTimePattern)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SectionCard(title = s.notifications, subtitle = "Pilih notifikasi yang ingin diterima") {
                Column {
                    SettingSwitch(label = s.newsAlert, checked = current.newsAlert, onChange = { vm.setNewsAlert(it) })
                    SettingSwitch(label = s.priceAlert, checked = current.priceAlert, onChange = { vm.setPriceAlert(it) })
                    SettingSwitch(label = s.entryAlert, checked = current.entryAlert, onChange = { vm.setEntryAlert(it) })
                    SettingSwitch(label = "Auto refresh latar belakang", checked = current.autoRefresh, onChange = { vm.setAutoRefresh(it) })
                }
            }
        }

        item {
            SectionCard(title = s.tradingPreferences, subtitle = "Nilai default untuk setiap order") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = s.balance, value = Formatters.money(current.balance), modifier = Modifier.weight(1f))
                        StatTile(label = "Risiko", value = "${current.riskPercent}%", valueColor = Gold, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.balance, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1_000.0, 5_000.0, 10_000.0, 50_000.0, 100_000.0).forEach { value ->
                            val selected = kotlin.math.abs(value - current.balance) < 1.0
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setBalance(value) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(Formatters.compactMoney(value), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.defaultRisk, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5, 1.0, 2.0, 3.0, 5.0).forEach { value ->
                            val selected = kotlin.math.abs(value - current.riskPercent) < 0.01
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setRiskPercent(value) }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text("$value%", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Jarak Stop Loss default (%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.25, 0.5, 1.0, 1.5, 2.0).forEach { value ->
                            val selected = kotlin.math.abs(value - current.slPercent) < 0.01
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setSlPercent(value) }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text("$value%", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.preferredTpRatio, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppConstants.PREFERRED_TP_RATIOS.forEach { value ->
                            val selected = kotlin.math.abs(value - current.tpRatio) < 0.01
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTpRatio(value) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(if (value % 1.0 == 0.0) "1:${value.toInt()}" else "1:$value", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Leverage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 50, 100, 500, 1000).forEach { value ->
                            val selected = value == current.leverage
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setLeverage(value) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("1:$value", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Donasi untuk Pengembangan APK",
                subtitle = "Dukung server, data pasar, dan pembaruan fitur",
                modifier = Modifier.clickable { showDonation = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.dana_logo),
                        contentDescription = "Logo DANA",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(1.dp))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Bantu Mazkiplay Trade tetap aktif", fontWeight = FontWeight.Bold)
                        Text("Ketuk untuk melihat QRIS donasi resmi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            SectionCard(title = s.about, subtitle = AppConstants.TAGLINE) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${AppConstants.BRAND}", style = MaterialTheme.typography.titleLarge, color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Pill(text = "v${AppConstants.VERSION}", color = Bull, filled = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${AppConstants.APP_NAME} — aplikasi analisa dan eksekusi trading forex dengan analisa otomatis teknikal dan fundamental.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(AppConstants.SIGNATURE, style = MaterialTheme.typography.titleMedium, color = Bull, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(s.signalDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showDonation) {
        AlertDialog(
            onDismissRequest = { showDonation = false },
            icon = {
                Image(
                    painter = painterResource(R.drawable.dana_logo),
                    contentDescription = "Logo DANA",
                    modifier = Modifier.size(82.dp)
                )
            },
            title = { Text("Donasi untuk Pengembangan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dukungan Anda membantu biaya server, pemeliharaan koneksi data pasar real-time, keamanan, dan pengembangan fitur baru Mazkiplay Trade.")
                    Image(
                        painter = painterResource(R.drawable.donation_qris),
                        contentDescription = "QRIS donasi",
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Text("Cara donasi", fontWeight = FontWeight.Bold)
                    Text("1. Buka aplikasi pembayaran yang mendukung QRIS.\n2. Scan QRIS di atas.\n3. Periksa nama penerima dan nominal sebelum mengonfirmasi.")
                    Text("QRIS resmi • nominal sesuai kebutuhan", style = MaterialTheme.typography.labelMedium, color = Gold, fontWeight = FontWeight.Bold)
                    Text("Developer: ${AppConstants.DEVELOPER_NAME}", style = MaterialTheme.typography.bodySmall)
                    Text("Email: ${AppConstants.DEVELOPER_EMAIL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Pastikan nama penerima dan nominal sudah benar. Donasi bersifat sukarela dan tidak memengaruhi akses fitur aplikasi.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { showDonation = false }) { Text("Selesai") } },
            dismissButton = {
                TextButton(onClick = { showDonation = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
