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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.domain.analysis.Indicators
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.MarketProfileView
import com.mazkiplay.trade.ui.components.OrderBookView
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.marketViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.Formatters

/** Professional market reading: depth, profile, Fibonacci and price action levels. */
@Composable
fun MarketAnalysisScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = marketViewModel(app)

    val candles by vm.candles.collectAsState()
    val symbol by vm.selectedSymbol.collectAsState()
    val instrument = Instruments.bySymbol(symbol)
    val snapshot = remember(candles, symbol) { vm.snapshot() }
    val book = remember(candles) { vm.orderBook() }
    val profile = remember(candles) { vm.marketProfile() }
    val fib = remember(candles) { Indicators.fibonacci(candles) }
    val fvgs = remember(candles) { Indicators.fairValueGaps(candles) }
    val structure = remember(candles) { Indicators.marketStructure(candles) }
    val pivots = remember(candles) { Indicators.pivots(candles) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.marketAnalysis, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("${symbol} • ${instrument.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Pill(text = "ORDER FLOW", color = Bull)
            }
        }

        if (candles.isEmpty()) {
            item { SectionCard(title = s.marketAnalysis) { EmptyHint(text = "Memuat data pasar...") } }
        } else {
            item {
                SectionCard(title = "Order Book", subtitle = "Estimasi likuiditas dari sebaran volume candle") {
                    OrderBookView(book = book, digits = instrument.digits)
                }
            }

            item {
                SectionCard(title = "Market Profile", subtitle = "Volume at price, POC dan value area") {
                    MarketProfileView(profile = profile, digits = instrument.digits)
                }
            }

            item {
                SectionCard(title = "Market Structure", subtitle = "Higher high / higher low analysis") {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(label = "Struktur", value = structure.label, valueColor = if (structure.higherHighs) Bull else Bear, modifier = Modifier.weight(1f))
                            StatTile(label = "HH", value = if (structure.higherHighs) "Ya" else "Tidak", valueColor = if (structure.higherHighs) Bull else Bear, modifier = Modifier.weight(1f))
                            StatTile(label = "HL", value = if (structure.higherLows) "Ya" else "Tidak", valueColor = if (structure.higherLows) Bull else Bear, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Fibonacci Retracement", subtitle = "Diukur dari swing low-high terakhir") {
                    Column {
                        fib.forEach { level ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(level.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text(Formatters.price(level.price, instrument.digits), style = MaterialTheme.typography.labelLarge, color = Gold)
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Indikator", subtitle = "EMA, RSI, ATR dan MACD dari data live") {
                    val snap = snapshot
                    if (snap == null) {
                        EmptyHint(text = "Indikator belum tersedia.")
                    } else {
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile(label = "EMA 20", value = Formatters.price(snap.ema20, instrument.digits), modifier = Modifier.weight(1f))
                                StatTile(label = "EMA 50", value = Formatters.price(snap.ema50, instrument.digits), modifier = Modifier.weight(1f))
                                StatTile(label = "EMA 200", value = Formatters.price(snap.ema200, instrument.digits), modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile(label = "RSI 14", value = String.format(java.util.Locale.US, "%.1f", snap.rsi), valueColor = if (snap.rsi > 70) Bear else if (snap.rsi < 30) Bull else Gold, modifier = Modifier.weight(1f))
                                StatTile(label = "ATR", value = Formatters.price(snap.atr, instrument.digits), modifier = Modifier.weight(1f))
                                StatTile(label = "MACD", value = Formatters.price(snap.macdHistogram, instrument.digits), valueColor = if (snap.macdHistogram >= 0) Bull else Bear, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Support & Resistance", subtitle = "Dari swing high/low terbaru") {
                    Column {
                        (snapshot?.resistances ?: emptyList()).forEach { level ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Pill(text = "R", color = Bear)
                                Spacer(Modifier.padding(4.dp))
                                Text(Formatters.price(level, instrument.digits), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        (snapshot?.supports ?: emptyList()).forEach { level ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Pill(text = "S", color = Bull)
                                Spacer(Modifier.padding(4.dp))
                                Text(Formatters.price(level, instrument.digits), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Pivot klasik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("P ${Formatters.price(pivots.pivot, instrument.digits)} • R1 ${Formatters.price(pivots.r1, instrument.digits)} • S1 ${Formatters.price(pivots.s1, instrument.digits)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                SectionCard(title = "Supply & Demand + FVG", subtitle = "Zona institusional dan imbalance harga") {
                    Column {
                        (snapshot?.supplyZones ?: emptyList()).forEach { zone ->
                            Text("Supply ${zone.strength}x  ${Formatters.price(zone.lower, instrument.digits)} - ${Formatters.price(zone.upper, instrument.digits)}", style = MaterialTheme.typography.bodySmall, color = Bear, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        (snapshot?.demandZones ?: emptyList()).forEach { zone ->
                            Text("Demand ${zone.strength}x  ${Formatters.price(zone.lower, instrument.digits)} - ${Formatters.price(zone.upper, instrument.digits)}", style = MaterialTheme.typography.bodySmall, color = Bull, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        if (fvgs.isEmpty()) {
                            Text("Tidak ada Fair Value Gap terbuka.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            fvgs.forEach { gap ->
                                Text("FVG ${if (gap.bullish) "Bullish" else "Bearish"}  ${Formatters.price(gap.lower, instrument.digits)} - ${Formatters.price(gap.upper, instrument.digits)}", style = MaterialTheme.typography.bodySmall, color = if (gap.bullish) Bull else Bear, modifier = Modifier.padding(vertical = 2.dp))
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
