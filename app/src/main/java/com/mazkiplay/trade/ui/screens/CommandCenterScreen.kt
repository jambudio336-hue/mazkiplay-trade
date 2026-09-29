package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.RiskProfile
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import kotlin.math.max

@Composable
fun CommandCenterScreen(app: MazkiplayApp) {
    val lock by app.featurePack.emergencyLock.collectAsState()
    val profile by app.featurePack.riskProfile.collectAsState()
    val health by app.featurePack.health.collectAsState()
    var balance by remember { mutableFloatStateOf(1000f) }
    var risk by remember { mutableFloatStateOf(profile.maxRisk.toFloat()) }
    var slPips by remember { mutableFloatStateOf(20f) }
    var rr by remember { mutableFloatStateOf(profile.minRr.toFloat()) }
    val riskAmount = balance * risk / 100f
    val reward = riskAmount * rr
    val lot = max(0.01f, riskAmount / (slPips * 10f))
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Market Command Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Data → chart → context → signal → risk → journal", style = MaterialTheme.typography.bodySmall) }
        item { SectionCard(title = "Personal Market Brief", subtitle = "Deterministic snapshot, bukan AI tebakan", trailing = { Pill(if (health.signalPaused) "SIGNAL PAUSED" else "MARKET ONLINE", if (health.signalPaused) Bear else Bull, filled = true) }) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("USD  STRONG"); Text("EUR  NEUTRAL"); Text("GOLD  HIGH VOLATILITY"); Text("BTC  TRENDING"); Text("2 HIGH IMPACT EVENTS · 1 LIVE EVENT · 8 ACTIVE SIGNALS", color = Gold, fontWeight = FontWeight.Bold); health.signalPaused.let { if (it) Text("Market data stale/offline. Signal baru ditahan sampai data fresh.", color = Bear) } } } }
        item { SectionCard(title = "Emergency Risk Lock", subtitle = "Stop new orders · cancel pending · disable copy trade") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { app.featurePack.setEmergencyLock(!lock) }) { Text(if (lock) "UNLOCK" else "EMERGENCY STOP") }; Text(if (lock) "ACTIVE: new orders blocked" else "Armed but inactive", color = if (lock) Bear else Bull, modifier = Modifier.padding(top = 12.dp)) }; Text("Posisi terbuka tidak ditutup otomatis tanpa kebijakan/konfirmasi pengguna.", style = MaterialTheme.typography.labelSmall) } }
        item { SectionCard(title = "Personal Trading Profile", subtitle = "Hard limits tetap aktif") { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { RiskProfile.values().forEach { candidate -> Button(onClick = { app.featurePack.setRiskProfile(candidate) }, enabled = candidate != profile) { Text("${candidate.title} · max ${candidate.maxRisk}% · min R:R ${candidate.minRr} · ${candidate.maxPositions} positions") } } } } }
        item { SectionCard(title = "What-If Simulator", subtitle = "Tidak menyentuh akun broker") { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Balance Rp${balance.toInt()}"); Slider(value = balance, onValueChange = { balance = it }, valueRange = 100f..10000f); Text("Risk ${"%.2f".format(risk)}%"); Slider(value = risk, onValueChange = { risk = it.coerceAtMost(profile.maxRisk.toFloat()) }, valueRange = 0.1f..2f); Text("SL ${slPips.toInt()} pips"); Slider(value = slPips, onValueChange = { slPips = it }, valueRange = 5f..100f); Text("R:R 1:${"%.1f".format(rr)}"); Slider(value = rr, onValueChange = { rr = it }, valueRange = 1f..4f); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile("Risk", "${riskAmount.toInt()}", Modifier.weight(1f), Bear); StatTile("Lot", "%.2f".format(lot), Modifier.weight(1f), Gold); StatTile("TP", "${reward.toInt()}", Modifier.weight(1f), Bull) } } } }
        item { SectionCard(title = "Data Health Center", subtitle = health.activeProvider) { listOf("Forex" to health.market, "Crypto" to health.crypto, "News" to health.news, "Calendar" to health.calendar, "Fundamental" to health.fundamental).forEach { (name, state) -> Row(Modifier.fillMaxWidth()) { Text(name, Modifier.weight(1f)); Pill(state, if (state == "LIVE") Bull else Gold) } }; Text("API quota, provider conflict, spread, slippage, and failover are shown only when provider data is available.", style = MaterialTheme.typography.labelSmall) } }
        item { SectionCard(title = "Opportunity Scanner", subtitle = "Qualified setups · rule-based") { Text("Trend · Momentum · Volatility · Fundamental · News · Risk", fontWeight = FontWeight.Bold); Text("Breakout · Reversal · Continuation · Oversold/Overbought · Volume spike · MTF alignment", style = MaterialTheme.typography.bodySmall); Text("Scanner tidak memaksa BUY/SELL ketika konflik atau data stale.", color = Gold) } }
    }
}
