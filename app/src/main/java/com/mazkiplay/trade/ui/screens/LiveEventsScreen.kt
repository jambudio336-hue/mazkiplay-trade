package com.mazkiplay.trade.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.EventState
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.Formatters

@Composable
fun LiveEventsScreen(app: MazkiplayApp) {
    val context = LocalContext.current
    val events = app.featurePack.events
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mazkiplay Live", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Official central-bank events + market context", style = MaterialTheme.typography.bodySmall) }
        item {
            SectionCard(title = "Live Event Center", subtitle = "UPCOMING → LIVE → ENDED", trailing = { Pill("OFFICIAL SOURCES", Bull) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    events.forEach { event ->
                        val color = when (event.state) { EventState.LIVE -> Bear; EventState.ENDED -> Gold; EventState.UPCOMING -> Bull }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(event.state.name, color, filled = event.state == EventState.LIVE)
                            Column(Modifier.weight(1f)) {
                                Text(event.title, fontWeight = FontWeight.SemiBold)
                                Text("${event.institution} · ${event.currency} · ${Formatters.time(event.scheduledAt, "HH:mm")}", style = MaterialTheme.typography.labelSmall)
                                Text(if (event.state == EventState.UPCOMING) "Reminder: event akan mengubah volatility mode saat dimulai" else if (event.state == EventState.LIVE) "NEWS MODE · threshold diperketat · spread/liquidity check aktif" else "Event selesai · reaction summary menunggu data aktual", style = MaterialTheme.typography.labelSmall, color = color)
                            }
                            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.officialUrl))) }) { Text("Official") }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Live + Chart + Reaction", subtitle = "Tidak mengklaim embed jika sumber resmi tidak mengizinkan") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saat player resmi tersedia, pengguna diarahkan ke source resmi; chart dan market reaction tetap berasal dari feed aktual yang tersedia di aplikasi.")
                    Text("FOMC / ECB / BOE → event detector → NEWS MODE → signal threshold → Risk Guardian", color = Gold, fontWeight = FontWeight.Bold)
                    Text("Market reaction: DXY, EURUSD, GBPUSD, XAUUSD · angka hanya ditampilkan setelah quote aktual masuk.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SectionCard(title = "Transcript & Watchwords", subtitle = "Lapisan rule-based, bukan AI wajib") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Inflation" to Bear, "Rate" to Gold, "Employment" to Bull, "Recession" to Bear, "Growth" to Gold).forEach { (word, color) -> Row(Modifier.fillMaxWidth()) { Text(word, Modifier.weight(1f)); Pill("MONITOR", color) } }
                    Text("Transcript hanya dipakai bila caption/transcript resmi tersedia dan sesuai hak penggunaan.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
