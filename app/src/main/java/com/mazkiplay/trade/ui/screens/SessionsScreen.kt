package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.domain.session.SessionManager
import com.mazkiplay.trade.ui.components.MeterBar
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.marketViewModel
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.theme.Orange

/** Real-time status of the four major FX sessions plus a liquidity read. */
@Composable
fun SessionsScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = marketViewModel(app)
    val sessions by vm.sessions.collectAsState()
    val liquidity by vm.liquidity.collectAsState()
    val openCount = sessions.count { it.isOpen }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.sessions, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("$openCount dari 4 sesi sedang buka", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SectionCard(title = "Skor Likuiditas", subtitle = "Gabungan sesi yang aktif saat ini") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$liquidity",
                        style = MaterialTheme.typography.displaySmall,
                        color = when {
                            liquidity >= 80 -> Bull
                            liquidity >= 55 -> Gold
                            else -> Orange
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        MeterBar(value = liquidity.toDouble(), color = if (liquidity >= 55) Bull else Orange, height = 10.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = when {
                                liquidity >= 85 -> "Overlap London-New York \u2014 volatilitas tertinggi"
                                liquidity >= 55 -> "Likuiditas sehat untuk trading intraday"
                                else -> "Likuiditas tipis \u2014 spread melebar"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(sessions) { session ->
            SectionCard(
                title = session.name,
                subtitle = "${session.city} \u2022 ${session.tzLabel}",
                trailing = { com.mazkiplay.trade.ui.components.Pill(text = if (session.isOpen) "BUKA" else "TUTUP", color = if (session.isOpen) Bull else MaterialTheme.colorScheme.outline, filled = session.isOpen) }
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Buka (UTC)", value = String.format(java.util.Locale.US, "%02d:%02d", session.openUtcMinutes / 60, session.openUtcMinutes % 60), modifier = Modifier.weight(1f))
                        StatTile(label = "Tutup (UTC)", value = String.format(java.util.Locale.US, "%02d:%02d", session.closeUtcMinutes / 60, session.closeUtcMinutes % 60), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    MeterBar(value = (session.progress * 100.0), color = if (session.isOpen) Bull else MaterialTheme.colorScheme.outline, height = 8.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(SessionManager.describe(session, prefs.language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SectionCard(title = "Catatan Sesi", subtitle = "Karakter tiap sesi") {
                Column {
                    listOf(
                        "Sydney: pergerakan awal, cocok untuk pair AUD/NZD.",
                        "Tokyo: range sempit, breakout USDJPY dan AUDJPY.",
                        "London: likuiditas besar, sering jadi arah harian.",
                        "New York: data AS rilis, volatilitas tinggi.",
                        "Overlap London-New York adalah jendela terbaik untuk entry."
                    ).forEach { line ->
                        Text("\u2022 $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}
