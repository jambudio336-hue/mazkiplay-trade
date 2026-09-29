package com.mazkiplay.trade.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.FeedStatus
import com.mazkiplay.trade.data.model.CryptoMarketCoin
import com.mazkiplay.trade.data.model.MarketPulse
import com.mazkiplay.trade.data.model.ScreenerFilter
import com.mazkiplay.trade.data.model.Timeframe
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.data.repository.CryptoFeedStatus
import com.mazkiplay.trade.data.tradingview.TvTickers
import com.mazkiplay.trade.ui.components.ChangeChip
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.FeedStatusBadge
import com.mazkiplay.trade.ui.components.MarketColors
import com.mazkiplay.trade.ui.components.MetricBar
import com.mazkiplay.trade.ui.components.OfflineBanner
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.SignalBiasChip
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.components.Sparkline
import com.mazkiplay.trade.ui.components.clockLabel
import com.mazkiplay.trade.ui.liveViewModel
import com.mazkiplay.trade.ui.widgets.TradingViewStrip
import com.mazkiplay.trade.ui.widgets.TradingViewWidget
import com.mazkiplay.trade.ui.widgets.advancedChartConfig
import com.mazkiplay.trade.ui.widgets.economicCalendarConfig
import com.mazkiplay.trade.ui.widgets.newsTimelineConfig
import com.mazkiplay.trade.ui.widgets.technicalGaugeConfig
import com.mazkiplay.trade.ui.widgets.tickerTapeConfig
import com.mazkiplay.trade.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The TradingView-backed live market desk.
 *
 * One screen, six panes, all fed by the single [com.mazkiplay.trade.data.tradingview.TvRepository]
 * poll so nothing re-fetches when the user switches panes:
 *
 *  - **Chart** \u2014 the official Advanced Real-Time Chart in a WebView (tick-by-tick), the
 *    technical-analysis gauge, live OHLC stats and the combined auto-analysis.
 *  - **Screener** \u2014 live rows filtered by real predicates (trend, RSI, volatility, signal).
 *  - **Kekuatan** \u2014 the currency-strength heatmap computed from live percentage moves.
 *  - **Kalender** \u2014 TradingView's economic-calendar widget plus a countdown to each
 *    high-impact release.
 *  - **Berita** \u2014 the live news-flow list plus TradingView's timeline widget.
 *  - **Sinyal** \u2014 the blended technical + fundamental verdict with levels and reasons.
 */
@Composable
fun LiveMarketScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val vm = liveViewModel(app)

    val quotes by vm.quotes.collectAsState()
    val technicals by vm.technicals.collectAsState()
    val status by vm.status.collectAsState()
    val lastUpdate by vm.lastUpdate.collectAsState()
    val strength by vm.strength.collectAsState()
    val calendar by vm.calendar.collectAsState()
    val news by vm.news.collectAsState()
    val signal by vm.signal.collectAsState()
    val signals by vm.signals.collectAsState()
    val pulse by vm.pulse.collectAsState()
    val cryptoCoins by vm.cryptoCoins.collectAsState()
    val cryptoStatus by vm.cryptoStatus.collectAsState()
    val cryptoLastUpdated by vm.cryptoLastUpdated.collectAsState()
    val symbol by vm.symbol.collectAsState()
    val timeframe by vm.timeframe.collectAsState()

    val dark = prefs.theme != "light"
    var pane by remember { mutableStateOf(0) }
    val panes = listOf("Chart", "Screener", "Kekuatan", "Kalender", "Berita", "Crypto", "Sinyal")

    val quote = quotes[symbol]
    val technical = technicals[symbol]
    val instrument = TvTickers.instrumentOf(symbol)

    Column(Modifier.fillMaxSize()) {

        // ------------------------------------------------------------- header
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeedStatusBadge(status = status, lastUpdateMillis = lastUpdate)
                Spacer(Modifier.weight(1f))
                Text(
                    text = clockLabel(lastUpdate, prefs.timePattern),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang")
                }
            }
            if (status == FeedStatus.OFFLINE || status == FeedStatus.STALE) {
                OfflineBanner(visible = true)
            }
        }

        // ------------------------------------------- live ticker tape (dashboard strip)
        TradingViewStrip(
            config = tickerTapeConfig(symbols = vm.tradableSymbols().take(14), dark = dark),
            cacheKey = "tape-${vm.tradableSymbols().size}",
            darkTheme = dark,
            heightDp = 76
        )

        MarketPulseCard(pulse = pulse, onSymbolClick = vm::selectSymbol)

        // ------------------------------------------------------------- pane selector
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(panes) { label ->
                val index = panes.indexOf(label)
                Chip(label = label, selected = pane == index) { pane = index }
            }
        }

        Box(Modifier.weight(1f)) {
            when (pane) {
                0 -> ChartPane(
                    vm = vm, dark = dark, symbol = symbol, timeframe = timeframe,
                    quotePrice = quote?.close ?: 0.0,
                    changePercent = quote?.changePercent ?: 0.0,
                    high = quote?.high ?: 0.0, low = quote?.low ?: 0.0,
                    rsi = technical?.rsi ?: 50.0,
                    trend = technical?.trend ?: "-",
                    biasLabel = technical?.bias?.label ?: "NETRAL",
                    confidence = technical?.confidence ?: 0,
                    watchlisted = prefs.watchlist.contains(symbol)
                )
                1 -> ScreenerPane(vm = vm, dark = dark)
                2 -> StrengthPane(strength = strength, dark = dark)
                3 -> CalendarPane(calendar = calendar, dark = dark)
                4 -> NewsPane(news = news, dark = dark)
                5 -> CryptoPane(coins = cryptoCoins, status = cryptoStatus, lastUpdated = cryptoLastUpdated)
                else -> SignalPane(vm = vm, signal = signal, signals = signals, digits = instrument.digits)
            }
        }
    }
}

@Composable
private fun CryptoPane(
    coins: List<CryptoMarketCoin>,
    status: CryptoFeedStatus,
    lastUpdated: Long
) {
    var group by remember { mutableStateOf("Spot") }
    val visibleCoins = coins.filter { group == "Semua" || it.marketGroup == group }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionCard(
                title = "Crypto Market",
                subtitle = "CoinGecko public API • ${status.label} • ${visibleCoins.size} coin tampil"
            ) {
                Text(
                    text = if (lastUpdated > 0) "Update ${Formatters.time(lastUpdated, "HH:mm:ss")}" else "Menunggu data publik",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Cakupan public/free terbatas dan dapat terkena rate limit. Ini bukan feed order book exchange.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Spot", "Meme Coin", "Semua").forEach { option ->
                        TextButton(onClick = { group = option }) {
                            Text(option, color = if (group == option) MarketColors.Up else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (coins.isEmpty()) {
            item { EmptyHint("Belum ada data CoinGecko. Coba muat ulang saat koneksi tersedia.") }
        } else {
            items(visibleCoins, key = { it.id }) { coin ->
                CryptoCoinRow(coin)
            }
        }
    }
}

@Composable
private fun CryptoCoinRow(coin: CryptoMarketCoin) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = coin.marketCapRank?.toString() ?: "--",
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.weight(1f)) {
            Text("${coin.symbol} • ${coin.marketGroup}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(coin.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (coin.sparkline7d.size > 2) {
            Sparkline(values = coin.sparkline7d, color = if (coin.isUp) MarketColors.Up else MarketColors.Down, modifier = Modifier.width(72.dp).height(28.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = coin.currentPrice?.let { Formatters.money(it, if (it < 1.0) 6 else 2) } ?: "--",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = coin.priceChangePercentage24h?.let { Formatters.percent(it) } ?: "--",
                style = MaterialTheme.typography.labelSmall,
                color = if (coin.isUp) MarketColors.Up else MarketColors.Down
            )
        }
    }
}

@Composable
private fun MarketPulseCard(
    pulse: MarketPulse,
    onSymbolClick: (String) -> Unit
) {
    SectionCard(
        title = "Market Pulse",
        subtitle = if (pulse.hasData) {
            "Breadth dari ${pulse.total} quote live • ${pulse.unresolved} belum tersedia"
        } else {
            "Menunggu snapshot quote live"
        }
    ) {
        if (!pulse.hasData) {
            EmptyHint("Ringkasan muncul setelah feed mengirim data valid.")
            return@SectionCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = "Naik",
                value = pulse.advancing.toString(),
                valueColor = MarketColors.Up,
                modifier = Modifier.weight(1f),
                caption = "${pulse.breadthPercent}% breadth"
            )
            StatTile(
                label = "Turun",
                value = pulse.declining.toString(),
                valueColor = MarketColors.Down,
                modifier = Modifier.weight(1f),
                caption = "${pulse.unchanged} datar"
            )
            StatTile(
                label = "Rata-rata",
                value = Formatters.percent(pulse.averageChangePercent),
                valueColor = if (pulse.averageChangePercent >= 0) MarketColors.Up else MarketColors.Down,
                modifier = Modifier.weight(1f),
                caption = "perubahan sesi"
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoverChip(
                title = "Top gainer",
                quote = pulse.topGainer,
                color = MarketColors.Up,
                modifier = Modifier.weight(1f),
                onClick = onSymbolClick
            )
            MoverChip(
                title = "Top loser",
                quote = pulse.topLoser,
                color = MarketColors.Down,
                modifier = Modifier.weight(1f),
                onClick = onSymbolClick
            )
        }
    }
}

@Composable
private fun MoverChip(
    title: String,
    quote: com.mazkiplay.trade.data.model.TvQuote?,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    val symbol = quote?.symbol ?: "--"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = quote != null) { quote?.let { onClick(it.symbol) } }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(symbol, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = quote?.let { Formatters.percent(it.changePercent) } ?: "--",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ---------------------------------------------------------------------------- panes

@Composable
private fun ChartPane(
    vm: com.mazkiplay.trade.ui.viewmodel.LiveViewModel,
    dark: Boolean,
    symbol: String,
    timeframe: Timeframe,
    quotePrice: Double,
    changePercent: Double,
    high: Double,
    low: Double,
    rsi: Double,
    trend: String,
    biasLabel: String,
    confidence: Int,
    watchlisted: Boolean
) {
    val digits = TvTickers.instrumentOf(symbol).digits
    val activity = LocalContext.current as? Activity
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------------------------------------------------- symbol & timeframe
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.tradableSymbols()) { item ->
                    Chip(label = item, selected = item == symbol) { vm.selectSymbol(item) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Timeframe.defaults) { tf ->
                        Chip(label = tf.label, selected = tf == timeframe) { vm.selectTimeframe(tf) }
                    }
                }
                IconButton(onClick = { vm.toggleWatchlist(symbol) }) {
                    Icon(
                        imageVector = if (watchlisted) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Watchlist",
                        tint = if (watchlisted) MarketColors.Up
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // -------------------------------------------------------- the real chart
        item {
            SectionCard(
                title = "$symbol \u00b7 ${timeframe.label}",
                subtitle = "TradingView Advanced Real-Time Chart \u2014 tick live"
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { activity?.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }) {
                        Text(if (landscape) "KEMBALI PORTRAIT" else "BUKA LANDSCAPE")
                    }
                }
                TradingViewWidget(
                    config = advancedChartConfig(symbol, vm.chartInterval(), dark),
                    cacheKey = "$symbol-${timeframe.label}",
                    darkTheme = dark,
                    modifier = Modifier.fillMaxWidth().height(if (landscape) 520.dp else 380.dp)
                )
            }
        }

        // ------------------------------------------------------------- live stats
        item {
            SectionCard(title = "Statistik Live", subtitle = "Diperbarui tiap 10 detik") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%,.${digits}f", quotePrice),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(10.dp))
                        ChangeChip(changePercent)
                        Spacer(Modifier.weight(1f))
                        SignalBiasChip(
                            bias = com.mazkiplay.trade.data.model.SignalBias.fromScore(
                                (confidence / 100.0) * (if (biasLabel.contains("JUAL")) -1.0 else 1.0)
                            )
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            label = "High",
                            value = String.format(Locale.US, "%,.${digits}f", high),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Low",
                            value = String.format(Locale.US, "%,.${digits}f", low),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "RSI",
                            value = String.format(Locale.US, "%.1f", rsi),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Struktur EMA: $trend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ------------------------------------------------ technical gauge widget
        item {
            SectionCard(
                title = "Rekomendasi Teknikal TradingView",
                subtitle = "Gauge resmi \u2014 beli / netral / jual"
            ) {
                TradingViewWidget(
                    config = technicalGaugeConfig(symbol, vm.chartInterval(), dark),
                    cacheKey = "gauge-$symbol-${timeframe.label}",
                    darkTheme = dark,
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            }
        }

        // ---------------------------------------------------- auto analysis action
        item {
            SectionCard(
                title = "Analisa Otomatis Gabungan",
                subtitle = "Fundamental + teknikal dari data live"
            ) {
                Column {
                    Text(
                        text = "Menggabungkan skor teknikal TradingView dengan bobot agenda " +
                            "berdampak tinggi untuk ${TvTickers.instrumentOf(symbol).baseCurrency}" +
                            "/${TvTickers.instrumentOf(symbol).quoteCurrency}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { vm.analyseNow() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Analisa Sekarang")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenerPane(vm: com.mazkiplay.trade.ui.viewmodel.LiveViewModel, dark: Boolean) {
    val rows by vm.filteredRows.collectAsState()
    val filter by vm.filter.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ScreenerFilter.entries.toList()) { item ->
                Chip(label = item.label, selected = item == filter) { vm.selectFilter(item) }
            }
        }
        if (rows.isEmpty()) {
            EmptyHint("Tidak ada instrumen yang cocok dengan filter ${filter.label}.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows) { row ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = row.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${row.name} \u00b7 ${row.trend}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(
                                        Locale.US, "%,.${TvTickers.instrumentOf(row.symbol).digits}f",
                                        row.price
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MarketColors.of(row.changePercent)
                                )
                                ChangeChip(row.changePercent)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RSI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", row.rsi),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            SignalBiasChip(row.bias)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthPane(
    strength: List<com.mazkiplay.trade.data.model.CurrencyStrength>,
    dark: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Currency Strength Meter",
                subtitle = "Dihitung dari pergerakan persen live seluruh pair"
            ) {
                if (strength.isEmpty()) {
                    EmptyHint("Menunggu data kekuatan mata uang dari feed...")
                } else {
                    Column {
                        strength.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.currency,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MarketColors.of(item.score),
                                    modifier = Modifier.width(46.dp)
                                )
                                MetricBar(
                                    fraction = ((50.0 + item.score * 50.0) / 100.0).toFloat(),
                                    color = MarketColors.of(item.score),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.US, "%+.2f%%", item.score),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MarketColors.of(item.score),
                                    modifier = Modifier.width(62.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Heatmap Mata Uang", subtitle = "Kuat = hijau, lemah = merah") {
                Column {
                    strength.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MarketColors.of(item.score)
                                                .copy(alpha = 0.16f)
                                        )
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = item.currency,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MarketColors.of(item.score)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%+.2f%%", item.score),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) {
                                Box(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarPane(
    calendar: List<com.mazkiplay.trade.data.model.TvCalendarEvent>,
    dark: Boolean
) {
    val upcoming = remember(calendar) {
        calendar.filter { it.dateMillis >= System.currentTimeMillis() }.take(30)
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Economic Calendar TradingView",
                subtitle = "Widget resmi \u2014 agenda fundamental real-time"
            ) {
                TradingViewWidget(
                    config = economicCalendarConfig(dark),
                    cacheKey = "calendar",
                    darkTheme = dark,
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )
            }
        }
        item {
            SectionCard(
                title = "Hitung Mundur Rilis",
                subtitle = "${upcoming.size} agenda berikutnya"
            ) {
                if (upcoming.isEmpty()) {
                    EmptyHint("Belum ada agenda pada rentang yang dipantau.")
                } else {
                    Column {
                        upcoming.take(12).forEach { event -> CalendarRow(event) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(event: com.mazkiplay.trade.data.model.TvCalendarEvent) {
    val impactColor = when {
        event.importance >= 2 -> MarketColors.Down
        event.importance == 1 -> com.mazkiplay.trade.ui.theme.Gold
        else -> MarketColors.Neutral
    }
    val stamp = remember(event.dateMillis) {
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(event.dateMillis))
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(impactColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = event.currency.ifBlank { event.country },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = impactColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(3.dp))
        Row {
            Text(
                text = "$stamp \u00b7 ${event.countdown()} lagi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            val values = listOfNotNull(
                event.actual.takeIf { it.isNotBlank() }?.let { "Aktual $it" },
                event.forecast.takeIf { it.isNotBlank() }?.let { "Prakiraan $it" },
                event.previous.takeIf { it.isNotBlank() }?.let { "Sebelumnya $it" }
            )
            if (values.isNotEmpty()) {
                Text(
                    text = values.joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NewsPane(
    news: List<com.mazkiplay.trade.data.model.TvNewsItem>,
    dark: Boolean
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "Timeline Berita TradingView",
                subtitle = "Feed berita pasar langsung"
            ) {
                TradingViewWidget(
                    config = newsTimelineConfig(dark),
                    cacheKey = "news-timeline",
                    darkTheme = dark,
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )
            }
        }
        item {
            SectionCard(
                title = "Headline Terbaru",
                subtitle = "${news.size} berita dari feed"
            ) {
                if (news.isEmpty()) {
                    EmptyHint("Menunggu berita terbaru dari feed...")
                } else {
                    Column {
                        news.take(20).forEach { item -> NewsRow(item, uriHandler) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsRow(
    item: com.mazkiplay.trade.data.model.TvNewsItem,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val stamp = remember(item.publishedAt) {
        if (item.publishedAt <= 0L) "-"
        else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.publishedAt))
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${item.source} \u00b7 $stamp",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (item.symbols.isNotEmpty()) {
                Text(
                    text = item.symbols.take(3).joinToString(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MarketColors.Up
                )
            }
            TextButton(onClick = { runCatching { uriHandler.openUri(item.url) } }) {
                Text("Buka")
            }
        }
    }
}

@Composable
private fun SignalPane(
    vm: com.mazkiplay.trade.ui.viewmodel.LiveViewModel,
    signal: com.mazkiplay.trade.data.model.TradingSignal?,
    signals: Map<String, com.mazkiplay.trade.data.model.TradingSignal>,
    digits: Int
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (signal == null) {
            item { EmptyHint("Menunggu data sinyal pertama dari feed...") }
        } else {
            item {
                SectionCard(
                    title = "Panel Sinyal \u00b7 ${signal.symbol}",
                    subtitle = "Teknikal TradingView + fundamental otomatis"
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SignalBiasChip(signal.bias)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${signal.actionLabel} · ${signal.confidence}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "R:R 1:${String.format(Locale.US, "%.1f", signal.riskReward)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = when {
                                !signal.hasLivePrice -> "Data harga belum live. Sinyal dikunci demi keamanan."
                                signal.isFresh -> "Snapshot live · diperbarui ${Formatters.time(signal.generatedAt, "HH:mm:ss")}"
                                else -> "Snapshot tertunda · muat ulang untuk konfirmasi terbaru"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (signal.hasLivePrice && signal.isFresh) MarketColors.Up else MarketColors.Down
                        )
                        Spacer(Modifier.height(10.dp))
                        MetricBar(
                            fraction = signal.confidence / 100f,
                            color = if (signal.bias.isSell) MarketColors.Down else MarketColors.Up
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(
                                label = "Entry",
                                value = String.format(Locale.US, "%,.${digits}f", signal.entry),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Stop Loss",
                                value = String.format(Locale.US, "%,.${digits}f", signal.stopLoss),
                                valueColor = MarketColors.Down,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Take Profit",
                                value = String.format(Locale.US, "%,.${digits}f", signal.takeProfit),
                                valueColor = MarketColors.Up,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(
                                label = "Skor Teknikal",
                                value = String.format(Locale.US, "%+.2f", signal.technicalScore),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Skor Fundamental",
                                value = String.format(Locale.US, "%+.2f", signal.fundamentalScore),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Skor Gabungan",
                                value = String.format(Locale.US, "%+.2f", signal.combinedScore),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(onClick = vm::analyseNow) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Analisa ulang sekarang")
                        }
                    }
                }
            }
            item {
                val topSignals = signals.values
                    .filter { it.hasLivePrice }
                    .sortedWith(
                        compareByDescending<com.mazkiplay.trade.data.model.TradingSignal> { it.confidence }
                            .thenByDescending { kotlin.math.abs(it.combinedScore) }
                    )
                    .take(6)
                SectionCard(
                    title = "Top Signals",
                    subtitle = "Ranking live · teknikal 65% + fundamental 35%"
                ) {
                    if (topSignals.isEmpty()) {
                        EmptyHint("Menunggu quote live untuk menyusun ranking sinyal.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            topSignals.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { vm.selectSymbol(item.symbol) }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "${item.bias.label} · ${item.timeframe}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "${item.confidence}%",
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.bias.isSell) MarketColors.Down else MarketColors.Up
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("R:R 1:${String.format(Locale.US, "%.1f", item.riskReward)}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Alasan Analisa", subtitle = "${signal.reasons.size} faktor") {
                    Column {
                        signal.reasons.forEach { reason ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "\u2022",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MarketColors.Up
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- chip

/** A small selectable pill; avoids the experimental Material chip APIs. */
@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) accent.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
