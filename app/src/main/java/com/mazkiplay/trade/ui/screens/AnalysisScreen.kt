package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.data.model.TradeDirection
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.domain.trade.PositionCalculator
import com.mazkiplay.trade.ui.analysisViewModel
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.ui.components.AnalysisPanel
import com.mazkiplay.trade.ui.components.CurrencyStrengthRow
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.PanelLabels
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.PriceChart
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.Formatters

/** Automatic combined analysis: technical, fundamental and the blended verdict. */
@Composable
fun AnalysisScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = analysisViewModel(app)
    val tradeVm = tradeViewModel(app)

    val symbol by vm.symbol.collectAsState()
    val timeframe by vm.timeframe.collectAsState()
    val tpRatio by vm.tpRatio.collectAsState()
    val result by vm.result.collectAsState()
    val busy by vm.busy.collectAsState()
    val fundamental by vm.fundamental.collectAsState()
    val candles by vm.candles.collectAsState()
    val snapshot by vm.snapshot.collectAsState()
    val auto by vm.auto.collectAsState()

    val instrument = Instruments.bySymbol(symbol)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.analysis, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("Teknikal + Fundamental • auto ${if (auto) "aktif" else "manual"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        item {
            SectionCard(title = "Instrumen", subtitle = "Pilih pair lalu jalankan analisa") {
                Column {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Instruments.all.forEach { instr ->
                            val selected = instr.symbol == symbol
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setSymbol(instr.symbol) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(instr.symbol, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Timeframe.entries.forEach { tf ->
                            val selected = tf == timeframe
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTimeframe(tf) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(tf.label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto analisa berkala", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Switch(checked = auto, onCheckedChange = { vm.toggleAuto() })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Rasio TP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.mazkiplay.trade.util.AppConstants.PREFERRED_TP_RATIOS.forEach { ratio ->
                            val selected = ratio == tpRatio
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTpRatio(ratio) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (ratio % 1.0 == 0.0) "1:${ratio.toInt()}" else "1:$ratio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { vm.runAnalysis() }, modifier = Modifier.fillMaxWidth()) {
                        Text(s.autoAnalyse)
                    }
                }
            }
        }

        item {
            SectionCard(title = "Grafik + Indikator", subtitle = "${instrument.displayName}") {
                PriceChart(candles = candles, snapshot = snapshot, digits = instrument.digits)
            }
        }

        item {
            SectionCard(title = s.analysis, subtitle = "Bias, level, alasan dan keyakinan") {
                if (result == null) {
                    EmptyHint(text = "Tekan Analisa Otomatis untuk memuat hasil.")
                } else {
                    AnalysisPanel(
                        result = result!!,
                        digits = instrument.digits,
                        labels = PanelLabels(confidence = s.confidence, entry = "Entry", stopLoss = "SL", takeProfit = "TP", reasons = s.reasons)
                    )
                }
            }
        }

        item {
            val sniper = result
            SectionCard(title = "Sniper Entry", subtitle = "Setup otomatis dari feed live • risk sizing mengikuti balance") {
                if (sniper == null || sniper.entry <= 0.0 || sniper.bias == com.mazkiplay.trade.data.model.Bias.NEUTRAL) {
                    EmptyHint("Jalankan analisa dan tunggu bias BUY/SELL sebelum membuat setup sniper.")
                } else {
                    val direction = if (sniper.bias.isBullish) TradeDirection.BUY else TradeDirection.SELL
                    val sized = PositionCalculator.size(
                        instrument = instrument,
                        direction = direction,
                        entry = sniper.entry,
                        balance = prefs.balance,
                        riskPercent = prefs.riskPercent,
                        tpRatio = prefs.tpRatio,
                        slPercent = prefs.slPercent,
                        leverage = prefs.leverage
                    )
                    Text("${direction.label} • confidence ${sniper.confidence}% • balance ${Formatters.money(prefs.balance)}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.mazkiplay.trade.ui.components.StatTile(label = "Entry", value = Formatters.price(sized.entryPrice, instrument.digits), modifier = Modifier.weight(1f))
                        com.mazkiplay.trade.ui.components.StatTile(label = "SL", value = Formatters.price(sized.stopLossPrice, instrument.digits), valueColor = Bear, modifier = Modifier.weight(1f))
                        com.mazkiplay.trade.ui.components.StatTile(label = "TP", value = Formatters.price(sized.takeProfitPrice, instrument.digits), valueColor = Bull, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Lot ${Formatters.lot(sized.lot)} • risiko ${Formatters.money(sized.riskAmount)} (${prefs.riskPercent}%) • R:R 1:${prefs.tpRatio}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        tradeVm.addAlarm(
                            label = "Sniper ${direction.label}",
                            symbol = symbol,
                            triggerAt = System.currentTimeMillis() + 60_000L,
                            note = "Setup ${direction.label}: Entry ${sized.entryPrice}, SL ${sized.stopLossPrice}, TP ${sized.takeProfitPrice}, lot ${sized.lot}. Cek ulang feed live sebelum entry.",
                            repeatDaily = false
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text("Simpan alarm sniper 1 menit") }
                }
            }
        }

        item {
            SectionCard(title = s.technical, subtitle = "Snapshot indikator terbaru") {
                val snap = snapshot
                if (snap == null) {
                    EmptyHint(text = "Belum ada data teknikal.")
                } else {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.mazkiplay.trade.ui.components.StatTile(label = "Tren", value = snap.trend, modifier = Modifier.weight(1f))
                            com.mazkiplay.trade.ui.components.StatTile(label = "RSI", value = String.format(java.util.Locale.US, "%.1f", snap.rsi), modifier = Modifier.weight(1f))
                            com.mazkiplay.trade.ui.components.StatTile(label = "Struktur", value = snap.structure.take(14), modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.mazkiplay.trade.ui.components.StatTile(label = "EMA20", value = Formatters.price(snap.ema20, instrument.digits), modifier = Modifier.weight(1f))
                            com.mazkiplay.trade.ui.components.StatTile(label = "EMA50", value = Formatters.price(snap.ema50, instrument.digits), modifier = Modifier.weight(1f))
                            com.mazkiplay.trade.ui.components.StatTile(label = "EMA200", value = Formatters.price(snap.ema200, instrument.digits), modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        snap.signals.take(12).forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = s.fundamental, subtitle = "Kekuatan mata uang, sentimen dan agenda") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.mazkiplay.trade.ui.components.StatTile(label = "Sentimen", value = Formatters.percent(fundamental.sentiment * 100), valueColor = if (fundamental.sentiment >= 0) Bull else Bear, modifier = Modifier.weight(1f))
                        com.mazkiplay.trade.ui.components.StatTile(label = s.highImpact, value = "${fundamental.highImpactToday}", valueColor = Gold, modifier = Modifier.weight(1f))
                        com.mazkiplay.trade.ui.components.StatTile(label = "Agenda", value = "${fundamental.upcomingCount}", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(s.currencyStrength, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    fundamental.currencyStrength.entries.sortedByDescending { it.value }.take(8).forEach { (currency, strength) ->
                        CurrencyStrengthRow(currency = currency, strength = strength)
                        Spacer(Modifier.height(5.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    fundamental.nextEvent?.let { event ->
                        Pill(text = s.nextEvent.uppercase(), color = Gold, filled = true)
                        Spacer(Modifier.height(5.dp))
                        Text("${event.title} • ${event.currency} • ${Formatters.time(event.dateMillis, prefs.dateTimePattern)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(8.dp))
                    fundamental.notes.forEach { note ->
                        Text("• $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text(s.signalDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
