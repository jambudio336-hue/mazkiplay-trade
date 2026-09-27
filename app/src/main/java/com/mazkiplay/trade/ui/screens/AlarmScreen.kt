package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.util.Formatters

/**
 * Entry alarms.
 *
 * Alarms can be set either as a relative offset from now (the common "remind me
 * before the news") or pinned to a specific hour of the day, optionally repeating
 * every day for a session open.
 */
@Composable
fun AlarmScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = tradeViewModel(app)

    val alarms by vm.alarms.collectAsState()
    val message by vm.message.collectAsState()

    var symbol by remember { mutableStateOf("XAUUSD") }
    var note by remember { mutableStateOf("") }
    var repeatDaily by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(-1) }
    var selectedOffset by remember { mutableStateOf(30) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.alarm, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Jadwalkan pengingat entry saat news rilis atau sesi dibuka", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (message != null) {
            item { SectionCard { Text(message!!, style = MaterialTheme.typography.bodyMedium, color = Bull) } }
        }

        item {
            SectionCard(title = "Instrumen") {
                Column {
                    Instruments.all.chunked(3).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { instr ->
                                val selected = instr.symbol == symbol
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) Bull.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { symbol = instr.symbol }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(instr.symbol, style = MaterialTheme.typography.labelSmall, color = if (selected) Bull else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Waktu Alarm", subtitle = "Pilih jeda cepat atau jam tertentu") {
                Column {
                    Text("Jeda dari sekarang", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 15, 30, 60, 120).forEach { minutes ->
                            val selected = selectedHour < 0 && selectedOffset == minutes
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedOffset = minutes; selectedHour = -1 }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (minutes < 60) "$minutes mnt" else "${minutes / 60} jam", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Atau jam tertentu (WIB/device)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 3, 6, 9, 12, 15, 18, 21).forEach { hour ->
                            val selected = selectedHour == hour
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedHour = hour }
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                            ) {
                                Text(String.format(java.util.Locale.US, "%02d:00", hour), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ulangi setiap hari", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Switch(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val triggerAt = if (selectedHour >= 0) {
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                                cal.set(java.util.Calendar.MINUTE, 0)
                                cal.set(java.util.Calendar.SECOND, 0)
                                var time = cal.timeInMillis
                                if (time <= now) time += 86_400_000L
                                time
                            } else {
                                now + selectedOffset * 60_000L
                            }
                            vm.addAlarm(
                                label = "Alarm $symbol",
                                symbol = symbol,
                                triggerAt = triggerAt,
                                note = note.ifBlank { "Pengingat entry $symbol" },
                                repeatDaily = repeatDaily
                            )
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Simpan Alarm") }
                }
            }
        }

        item {
            SectionCard(title = "Daftar Alarm", subtitle = "${alarms.size} alarm terjadwal") {
                if (alarms.isEmpty()) {
                    EmptyHint(text = "Belum ada alarm. Buat alarm untuk mengingatkan entry saat news rilis.")
                } else {
                    Column {
                        alarms.forEach { alarm ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(alarm.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(Formatters.time(alarm.triggerAt, prefs.dateTimePattern), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(alarm.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Pill(text = if (alarm.enabled) "AKTIF" else "OFF", color = if (alarm.enabled) Bull else MaterialTheme.colorScheme.outline, filled = alarm.enabled)
                                    Spacer(Modifier.height(5.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { vm.toggleAlarm(alarm) }) { Text(if (alarm.enabled) "Matikan" else "Aktifkan", style = MaterialTheme.typography.labelSmall) }
                                        OutlinedButton(onClick = { vm.deleteAlarm(alarm.id) }) { Text("Hapus", style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Tips", subtitle = "Kapan alarm entry paling berguna") {
                Column {
                    listOf(
                        "Pasang alarm 5 menit sebelum rilis news high impact.",
                        "Pasang alarm saat pembukaan London (15:00 WIB) dan New York (20:00 WIB).",
                        "Gunakan alarm harian untuk disiplin sesi trading.",
                        "Selalu cek kalender ekonomi sebelum entry."
                    ).forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}
