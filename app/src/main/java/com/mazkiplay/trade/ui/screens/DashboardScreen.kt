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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.Quote
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.domain.session.SessionManager
import com.mazkiplay.trade.ui.Routes
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.MeterBar
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.SessionTile
import com.mazkiplay.trade.ui.components.Sparkline
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.copyTradeViewModel
import com.mazkiplay.trade.ui.marketViewModel
import com.mazkiplay.trade.ui.newsViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Aqua
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.theme.Orange
import com.mazkiplay.trade.ui.theme.Violet
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.util.AppConstants
import com.mazkiplay.trade.util.Formatters

/**
 * Main dashboard: a dense but readable overview of everything that matters right
 * now — money, live prices, sessions, imminent news and the day's best signal.
 */
@Composable
fun DashboardScreen(
    app: MazkiplayApp,
    prefs: UserPreferences,
    onNavigate: (String) -> Unit
) {
    val s = stringsOf(prefs)
    val market = marketViewModel(app)
    val trade = tradeViewModel(app)
    val news = newsViewModel(app)
    val copy = copyTradeViewModel(app)

    val quotes by market.quotes.collectAsState()
    val positions by trade.positions.collectAsState()
    val events by market.events.collectAsState()
    val sessions by market.sessions.collectAsState()
    val liquidity by market.liquidity.collectAsState()
    val headlines by news.news.collectAsState()
    val traders by copy.traders.collectAsState()

    val openPositions = positions.filter { it.isOpen }
    val floating = openPositions.sumOf { trade.floatingPnl(it) }
    val watchlist = prefs.watchlist
    val upcoming = events.filter { it.dateMillis >= System.currentTimeMillis() }.take(3)
    val bestSignal = market.cachedAnalysis(watchlist.firstOrNull() ?: "XAUUSD")
    val liquidityRows = SessionManager.liquidityScore()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Selamat datang di ${AppConstants.BRAND}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${AppConstants.APP_NAME} • ${s.updated} ${Formatters.time(System.currentTimeMillis(), prefs.timePattern)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = s.balance,
                    value = Formatters.money(prefs.balance),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "P/L ${s.open}",
                    value = Formatters.money(floating),
                    valueColor = if (floating >= 0) Bull else Bear,
                    caption = "${openPositions.size} ${s.positions.lowercase()}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionCard(title = "Likuiditas Pasar", subtitle = "Skor kondisi pasar saat ini") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$liquidityRows",
                        style = MaterialTheme.typography.displaySmall,
                        color = when {
                            liquidityRows >= 80 -> Bull
                            liquidityRows >= 55 -> Gold
                            else -> Orange
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        MeterBar(
                            value = liquidityRows.toDouble(),
                            color = if (liquidityRows >= 55) Bull else Orange,
                            height = 10.dp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (liquidityRows >= 85) "Overlap London-New York: volatilitas tertinggi hari ini"
                            else if (liquidityRows >= 55) "Sesi aktif dengan likuiditas sehat"
                            else "Likuiditas tipis — spread melebar, hati-hati",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = s.watchlist,
                subtitle = "Harga real-time ${watchlist.size} instrumen",
                trailing = {
                    Pill(text = "AUTO 60s", color = Aqua)
                }
            ) {
                Column {
                    watchlist.take(6).forEach { symbol ->
                        val quote = quotes[symbol] ?: Quote(symbol)
                        val instrument = Instruments.bySymbol(symbol)
                        WatchRow(
                            symbol = symbol,
                            name = instrument.displayName,
                            quote = quote,
                            digits = instrument.digits,
                            onClick = { onNavigate(Routes.MARKET) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Ketuk kartu untuk membuka grafik lengkap →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onNavigate(Routes.MARKET) }
                    )
                }
            }
        }

        item {
            SectionCard(title = s.sessions, subtitle = "Status pasar 24 jam") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sessions) { session ->
                        SessionTile(
                            session = session,
                            statusText = SessionManager.describe(session, prefs.language),
                            modifier = Modifier.width(178.dp)
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = s.nextEvent,
                subtitle = "Kalender ekonomi Forex Factory",
                trailing = { Pill(text = "${events.size} event", color = Gold) }
            ) {
                if (upcoming.isEmpty()) {
                    EmptyHint(text = "Belum ada agenda mendatang yang termuat.")
                } else {
                    upcoming.forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Pill(
                                text = event.impact.label,
                                color = when (event.impact) {
                                    com.mazkiplay.trade.data.model.Impact.HIGH -> Bear
                                    com.mazkiplay.trade.data.model.Impact.MEDIUM -> Gold
                                    else -> Bull
                                },
                                filled = event.impact == com.mazkiplay.trade.data.model.Impact.HIGH
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${event.currency} • ${Formatters.time(event.dateMillis, prefs.timePattern)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Aksi Cepat", subtitle = "Semua fitur dalam satu ketukan") {
                val actions = listOf(
                    Triple("Analisa Otomatis", "Teknikal + Fundamental", Routes.ANALYSIS),
                    Triple("Buka Posisi", "TP/SL otomatis", Routes.TRADE),
                    Triple("Kalkulator Forex", "Lot, pip, margin", Routes.CALCULATOR),
                    Triple("Kalkulator Risiko", "Money management", Routes.RISK),
                    Triple("Copy Trade", "${traders.size} trader aktif", Routes.COPY_TRADE),
                    Triple("Alarm Entry", "Jadwal sesi & news", Routes.ALARM),
                    Triple("Analisa Pasar", "Order book & profile", Routes.MARKET_ANALYSIS),
                    Triple("Berita", "${headlines.size} headline", Routes.NEWS)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { (title, caption, route) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onNavigate(route) }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = caption,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Sinyal Terakhir",
                subtitle = "Hasil analisa otomatis terakhir",
                trailing = { Pill(text = "AUTO", color = Violet) }
            ) {
                if (bestSignal == null) {
                    EmptyHint(text = "Jalankan Analisa Otomatis untuk melihat sinyal gabungan.")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = bestSignal.symbol,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${bestSignal.bias.label} • keyakinan ${bestSignal.confidence}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Pill(
                            text = String.format(java.util.Locale.US, "R:R 1:%.1f", bestSignal.riskReward),
                            color = Bull,
                            filled = true
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Berita Terkini",
                subtitle = "Headline fundamental terbaru",
                trailing = { Pill(text = "LIVE", color = Bull, filled = true) }
            ) {
                if (headlines.isEmpty()) {
                    EmptyHint(text = "Memuat berita pasar...")
                } else {
                    headlines.take(4).forEach { item ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Text(
                                text = "${item.source} • ${Formatters.ago(item.publishedAt, prefs.language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Lihat semua berita →",
                        style = MaterialTheme.typography.labelLarge,
                        color = Bull,
                        modifier = Modifier.clickable { onNavigate(Routes.NEWS) }
                    )
                }
            }
        }

        item {
            Text(
                text = s.signalDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

/** One instrument line: name, sparkline and the live price with its change. */
@Composable
private fun WatchRow(
    symbol: String,
    name: String,
    quote: Quote,
    digits: Int,
    onClick: () -> Unit
) {
    val up = quote.change >= 0
    val color = if (up) Bull else Bear
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.take(3),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 9.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (quote.spark.size > 2) {
            Sparkline(
                values = quote.spark,
                color = color,
                modifier = Modifier.width(58.dp).height(26.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Formatters.price(quote.price, digits),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Formatters.percent(quote.changePercent),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
