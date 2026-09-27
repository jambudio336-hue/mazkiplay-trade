package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.Sparkline
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.marketViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.util.Formatters

/** Watchlist manager: pick which instruments the dashboard and alerts track. */
@Composable
fun WatchlistScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = marketViewModel(app)

    val quotes by vm.quotes.collectAsState()
    val watchlist by vm.watchlist.collectAsState()
    val watched = watchlist.watchlist

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.watchlist, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("${watched.size} instrumen dipantau", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SectionCard(title = "Dipantau", subtitle = "Harga live dan perubahan harian") {
                if (watched.isEmpty()) {
                    EmptyHint(text = "Belum ada instrumen. Tambahkan dari daftar di bawah.")
                } else {
                    Column {
                        watched.forEach { symbol ->
                            val quote = quotes[symbol] ?: com.mazkiplay.trade.data.model.Quote(symbol)
                            val instrument = Instruments.bySymbol(symbol)
                            val up = quote.change >= 0
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(instrument.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                if (quote.spark.size > 2) {
                                    Sparkline(values = quote.spark, color = if (up) Bull else Bear, modifier = Modifier.width(60.dp).height(26.dp))
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Formatters.price(quote.price, instrument.digits), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(Formatters.percent(quote.changePercent), style = MaterialTheme.typography.labelSmall, color = if (up) Bull else Bear)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(label = "Naik", value = "${watched.count { (quotes[it]?.change ?: 0.0) >= 0 }}", valueColor = Bull, modifier = Modifier.weight(1f))
                            StatTile(label = "Turun", value = "${watched.count { (quotes[it]?.change ?: 0.0) < 0 }}", valueColor = Bear, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Semua Instrumen", subtitle = "Ketuk untuk menambah atau menghapus") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Instruments.all.chunked(2).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { instrument ->
                                val isWatched = watched.contains(instrument.symbol)
                                Box(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isWatched) Modifier.background(Bull.copy(alpha = 0.18f)).border(1.dp, Bull, RoundedCornerShape(12.dp))
                                            else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                        .clickable { vm.toggleWatchlist(instrument.symbol) }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(instrument.symbol, style = MaterialTheme.typography.titleMedium, color = if (isWatched) Bull else MaterialTheme.colorScheme.onSurface)
                                        Text(instrument.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Notifikasi Harga", subtitle = "Status preferensi") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notifikasi perubahan harga", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Pill(text = if (prefs.priceAlert) "AKTIF" else "OFF", color = if (prefs.priceAlert) Bull else MaterialTheme.colorScheme.outline, filled = prefs.priceAlert)
                }
                Spacer(Modifier.height(6.dp))
                Text("Atur di halaman Pengaturan.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
