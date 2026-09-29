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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.AnalysisPanel
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.PanelLabels
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.PriceChart
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.Sparkline
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.marketViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.Formatters

/**
 * Market screen: instrument selector, live chart with indicator overlays, the
 * automatic analysis for the selected symbol and a compact order book summary.
 */
@Composable
fun MarketScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = marketViewModel(app)

    val quotes by vm.quotes.collectAsState()
    val candles by vm.candles.collectAsState()
    val symbol by vm.selectedSymbol.collectAsState()
    val timeframe by vm.timeframe.collectAsState()
    val analysis by vm.analysis.collectAsState()
    val loading by vm.loading.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()
    val watchlist by vm.watchlist.collectAsState()
    val indodaxMarkets by app.indodaxRepository.markets.collectAsState()
    val indodaxStatus by app.indodaxRepository.status.collectAsState()
    val indodaxUpdated by app.indodaxRepository.lastUpdated.collectAsState()

    val instrument = Instruments.bySymbol(symbol)
    val quote = quotes[symbol] ?: com.mazkiplay.trade.data.model.Quote(symbol)
    val snapshot = remember(candles, symbol) { vm.snapshot() }
    val book = remember(candles) { vm.orderBook() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = instrument.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Formatters.price(quote.price, instrument.digits),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${Formatters.percent(quote.changePercent)} • H ${Formatters.price(quote.dayHigh, instrument.digits)} / L ${Formatters.price(quote.dayLow, instrument.digits)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (quote.change >= 0) Bull else Bear
                    )
                }
                Sparkline(
                    values = quote.spark,
                    color = if (quote.change >= 0) Bull else Bear,
                    modifier = Modifier.width(92.dp).height(40.dp)
                )
            }
        }

        item {
            SectionCard(
                title = "Market Radar · INDODAX",
                subtitle = "Ticker publik + cached failover",
                trailing = { Pill(text = indodaxStatus.label, color = if (indodaxStatus.label == "LIVE") Bull else Gold, filled = true) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (indodaxMarkets.isEmpty()) {
                        EmptyHint("Menunggu ticker publik Indodax...")
                    } else {
                        indodaxMarkets.take(6).forEach { market ->
                            val move = market.rangePosition - 50.0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(market.pair, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                Text("${market.rangePosition.toInt()}% range", style = MaterialTheme.typography.labelSmall, color = if (move >= 0) Bull else Bear)
                                Spacer(Modifier.width(8.dp))
                                Text("${market.buy} / ${market.sell}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            "Health ${indodaxStatus.label} · ${if (indodaxUpdated > 0) Formatters.time(indodaxUpdated, prefs.timePattern) else "--:--"} · public REST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Read-only: tidak mengirim order. Spread dan imbalance depth dipakai sebagai konteks risiko ketika endpoint depth dipanggil.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Grafik ${timeframe.label}",
                subtitle = "EMA 20/50/200 • Fibonacci • FVG • Supply-Demand",
                trailing = {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (lastUpdated > 0) Formatters.time(lastUpdated, prefs.timePattern) else "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Timeframe.entries.forEach { tf ->
                            val selected = tf == timeframe
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { vm.setTimeframe(tf) }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tf.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PriceChart(
                        candles = candles,
                        snapshot = snapshot,
                        digits = instrument.digits
                    )
                }
            }
        }

        item {
            SectionCard(title = s.autoAnalyse, subtitle = "Gabungan fundamental + teknikal") {
                Column {
                    Button(
                        onClick = { vm.runAnalysis() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(s.autoAnalyse)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (analysis == null) {
                        EmptyHint(text = "Tekan tombol untuk menghitung bias, level dan keyakinan.")
                    } else {
                        AnalysisPanel(
                            result = analysis!!,
                            digits = instrument.digits,
                            labels = PanelLabels(
                                confidence = s.confidence,
                                entry = "Entry",
                                stopLoss = "SL",
                                takeProfit = "TP",
                                reasons = s.reasons
                            )
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Order Book (estimasi likuiditas)", subtitle = "Dihitung dari sebaran transaksi candle terakhir") {
                com.mazkiplay.trade.ui.components.OrderBookView(book, instrument.digits)
            }
        }

        item {
            SectionCard(title = "Pilih Instrumen", subtitle = "${Instruments.all.size} instrumen tersedia") {
                Column {
                    Instruments.all.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                val selected = item.symbol == symbol
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) Bull.copy(alpha = 0.20f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { vm.selectSymbol(item.symbol) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.symbol,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Bull else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = s.watchlist, subtitle = "Kelola instrumen yang dipantau") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InstrumentChips(
                        symbols = Instruments.all.map { it.symbol },
                        watchlist = watchlist.watchlist,
                        onToggle = { vm.toggleWatchlist(it) }
                    )
                }
            }
        }
    }
}

/** Toggle chips used to add or remove watchlist instruments. */
@Composable
private fun InstrumentChips(
    symbols: List<String>,
    watchlist: Set<String>,
    onToggle: (String) -> Unit
) {
    Column {
        symbols.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { symbol ->
                    val watched = watchlist.contains(symbol)
                    OutlinedButton(
                        onClick = { onToggle(symbol) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = (if (watched) "✓ " else "+ ") + symbol,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (watched) Bull else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
