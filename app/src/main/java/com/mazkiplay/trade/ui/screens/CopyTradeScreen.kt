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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.AnalysisPanel
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.PanelLabels
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.Sparkline
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.copyTradeViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.util.Formatters

/** Public signal desk: ranked providers with portfolio stats and one-tap copying. */
@Composable
fun CopyTradeScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = copyTradeViewModel(app)

    val traders by vm.traders.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()
    val detail by vm.detail.collectAsState()
    val analysis by vm.detailAnalysis.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val ranked = traders.sortedByDescending { it.score }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.copyTrade, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("${traders.size} trader publik \u2022 update ${Formatters.time(lastUpdated, prefs.timePattern)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        item {
            SectionCard(title = "Status sumber copy-trade", subtitle = "Katalog publik transparan") {
                Text(
                    "Data di bawah adalah katalog lokal/edukasi, bukan feed trader real-time. Open position, SL, dan TP tidak ditampilkan sebelum provider resmi menyediakan data tersebut.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (message != null) {
            item { SectionCard { Text(message!!, style = MaterialTheme.typography.bodyMedium, color = Bull) } }
        }

        items(ranked, key = { it.id }) { trader ->
            SectionCard(
                title = "${trader.name} ${trader.flag}",
                subtitle = "${trader.strategy} \u2022 ${trader.country}",
                trailing = { Pill(text = "KATALOG", color = Gold) }
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Return/bln", value = Formatters.percent(trader.monthlyReturn), valueColor = if (trader.monthlyReturn >= 0) Bull else Bear, modifier = Modifier.weight(1f))
                        StatTile(label = "Total", value = Formatters.percent(trader.totalReturn), valueColor = Bull, modifier = Modifier.weight(1f))
                        StatTile(label = "Max DD", value = String.format(java.util.Locale.US, "%.1f%%", trader.maxDrawdown), valueColor = Bear, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Win rate", value = String.format(java.util.Locale.US, "%.1f%%", trader.winRate), modifier = Modifier.weight(1f))
                        StatTile(label = "Followers", value = String.format(java.util.Locale.US, "%,d", trader.followers), modifier = Modifier.weight(1f))
                        StatTile(label = "Reward", value = "${trader.rewardPercent.toInt()}%", valueColor = Gold, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${trader.dataSource} \u2022 ${trader.dataStatus}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("AUM ${Formatters.compactMoney(trader.aum)} \u2022 ${trader.trades} trade", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(trader.topSymbols.joinToString(" \u2022 "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Sparkline(values = trader.equityCurve, color = if (trader.monthlyReturn >= 0) Bull else Bear, modifier = Modifier.width(84.dp).height(30.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.openDetail(trader) }, modifier = Modifier.weight(1f)) { Text("Analisa", style = MaterialTheme.typography.labelSmall) }
                        val subscribed = vm.isSubscribed(trader)
                        Button(onClick = { vm.toggleSubscription(trader) }, modifier = Modifier.weight(1f)) {
                            Text(if (subscribed) s.unsubscribeCopy else s.subscribe, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (detail != null) {
            item {
                val trader = detail!!
                val symbol = trader.topSymbols.firstOrNull() ?: "XAUUSD"
                SectionCard(
                    title = "Analisa ${trader.name}",
                    subtitle = "Gabungan fundamental + teknikal pada $symbol",
                    trailing = { Pill(text = trader.riskLevel.uppercase(), color = Gold) }
                ) {
                    Column {
                        if (analysis == null) {
                            EmptyHint(text = if (busy) s.analysing else "Menjalankan analisa...")
                        } else {
                            AnalysisPanel(
                                result = analysis!!,
                                digits = Instruments.bySymbol(symbol).digits,
                                labels = PanelLabels(confidence = s.confidence, entry = "Entry", stopLoss = "SL", takeProfit = "TP", reasons = s.reasons)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { vm.closeDetail() }, modifier = Modifier.fillMaxWidth()) { Text("Tutup panel") }
                    }
                }
            }
        }

        item {
            Text(s.signalDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
