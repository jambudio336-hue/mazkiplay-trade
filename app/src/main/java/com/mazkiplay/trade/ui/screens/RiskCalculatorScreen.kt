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
import androidx.compose.material3.MaterialTheme
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
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.MeterBar
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.util.Formatters

/** Risk-first calculator: what can be lost before the stop is hit. */
@Composable
fun RiskCalculatorScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = tradeViewModel(app)

    val symbol by vm.riskSymbol.collectAsState()
    val winRate by vm.riskWinRate.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val plan = remember(symbol, winRate, preferences) { vm.riskPlan() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.risk, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Money management berbasis titik impas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        .clickable { vm.setRiskSymbol(instr.symbol) }
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
            SectionCard(title = "Asumsi", subtitle = "Risiko ${preferences.riskPercent}% \u2022 SL ${preferences.slPercent}% \u2022 TP 1:${preferences.tpRatio.toInt()}") {
                Column {
                    Text("Win rate yang diasumsikan: ${String.format(java.util.Locale.US, "%.0f", winRate)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(40.0, 50.0, 55.0, 65.0, 75.0).forEach { value ->
                            val selected = kotlin.math.abs(value - winRate) < 0.5
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setRiskWinRate(value) }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text("${value.toInt()}%", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Ubah angka ini di halaman Pengaturan.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SectionCard(title = "Titik Impas", subtitle = "Win rate minimum agar tidak merugi") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", plan.breakEvenWinRate),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (winRate >= plan.breakEvenWinRate) Bull else com.mazkiplay.trade.ui.theme.Bear
                        )
                        Spacer(Modifier.height(0.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            MeterBar(value = plan.breakEvenWinRate, color = Gold, height = 10.dp)
                            Spacer(Modifier.height(5.dp))
                            Text("Target Anda ${String.format(java.util.Locale.US, "%.0f", winRate)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Rencana Risiko") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Risiko", value = Formatters.money(plan.riskAmount), valueColor = com.mazkiplay.trade.ui.theme.Bear, modifier = Modifier.weight(1f))
                        StatTile(label = "Reward", value = Formatters.money(plan.rewardAmount), valueColor = Bull, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Lot", value = Formatters.lot(plan.lot), modifier = Modifier.weight(1f))
                        StatTile(label = "Stop (pips)", value = Formatters.pips(plan.stopPips), modifier = Modifier.weight(1f))
                        StatTile(label = "Target (pips)", value = Formatters.pips(plan.targetPips), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    StatTile(label = "Ketahanan Modal", value = "${plan.maxConsecutiveLosses}x kerugian beruntun", caption = "asumsi risiko tetap", modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            SectionCard(title = "Saran") {
                Column {
                    plan.advice.forEach { line ->
                        Text("\u2022 $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        item {
            Text(s.signalDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
