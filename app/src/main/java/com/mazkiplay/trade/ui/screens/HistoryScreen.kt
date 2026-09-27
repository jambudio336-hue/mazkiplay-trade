package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.util.Formatters

/** Trade journal: every open and closed position with its realised result. */
@Composable
fun HistoryScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = tradeViewModel(app)
    val positions by vm.positions.collectAsState()

    val open = positions.filter { it.isOpen }
    val closed = positions.filter { !it.isOpen }
    val realised = closed.sumOf { it.pnl ?: 0.0 }
    val wins = closed.count { (it.pnl ?: 0.0) > 0 }
    val winRate = if (closed.isEmpty()) 0.0 else wins.toDouble() / closed.size * 100.0

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.history, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("${positions.size} transaksi tercatat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.mazkiplay.trade.ui.components.StatTile(label = "Realisasi", value = Formatters.money(realised), valueColor = if (realised >= 0) Bull else Bear, modifier = Modifier.weight(1f))
                com.mazkiplay.trade.ui.components.StatTile(label = "Win Rate", value = String.format(java.util.Locale.US, "%.0f%%", winRate), valueColor = Bull, modifier = Modifier.weight(1f))
                com.mazkiplay.trade.ui.components.StatTile(label = "Terbuka", value = "${open.size}", modifier = Modifier.weight(1f))
            }
        }

        item {
            SectionCard(title = "Posisi Terbuka", subtitle = "${open.size} posisi") {
                if (open.isEmpty()) {
                    EmptyHint(text = s.noPositions)
                } else {
                    Column {
                        open.forEach { position ->
                            val pnl = vm.floatingPnl(position)
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${position.symbol} ${position.direction.label}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${Formatters.lot(position.lot)} lot \u2022 ${Formatters.time(position.openedAt, prefs.dateTimePattern)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Formatters.money(pnl), style = MaterialTheme.typography.titleMedium, color = if (pnl >= 0) Bull else Bear)
                                    OutlinedButton(onClick = { vm.closePosition(position) }) { Text(s.closePosition, style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Riwayat Tertutup", subtitle = "${closed.size} transaksi selesai") {
                if (closed.isEmpty()) {
                    EmptyHint(text = "Belum ada transaksi yang ditutup.")
                } else {
                    Column {
                        closed.forEach { position ->
                            val pnl = position.pnl ?: 0.0
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${position.symbol} ${position.direction.label}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = "${Formatters.lot(position.lot)} lot \u2022 tutup ${position.closePrice?.let { Formatters.price(it, 5) } ?: "-"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(Formatters.time(position.closedAt ?: position.openedAt, prefs.dateTimePattern), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Pill(text = if (pnl >= 0) s.profit.uppercase() else s.loss.uppercase(), color = if (pnl >= 0) Bull else Bear, filled = true)
                                    Spacer(Modifier.height(5.dp))
                                    Text(Formatters.money(pnl), style = MaterialTheme.typography.titleMedium, color = if (pnl >= 0) Bull else Bear)
                                    OutlinedButton(onClick = { vm.deletePosition(position.id) }) { Text("Hapus", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(s.signalDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
