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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.mazkiplay.trade.data.model.TradeDirection
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.EmptyHint
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.util.Formatters

/** Open-position ticket: pick a direction, a TP ratio and a risk, get lot + TP/SL. */
@Composable
fun TradeScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = tradeViewModel(app)

    val symbol by vm.ticketSymbol.collectAsState()
    val direction by vm.ticketDirection.collectAsState()
    val risk by vm.ticketRisk.collectAsState()
    val sl by vm.ticketSl.collectAsState()
    val tp by vm.ticketTp.collectAsState()
    val sized by vm.ticketResult.collectAsState()
    val positions by vm.positions.collectAsState()
    val message by vm.message.collectAsState()

    val instrument = Instruments.bySymbol(symbol)
    val openPositions = positions.filter { it.isOpen }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Buka Posisi", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Lot, TP dan SL dihitung otomatis dari modal dan risiko", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (message != null) {
            item {
                SectionCard { Text(message!!, style = MaterialTheme.typography.bodyMedium, color = Bull) }
            }
        }

        item {
            SectionCard(title = "Instrumen", subtitle = instrument.displayName) {
                Column {
                    Instruments.all.chunked(3).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { instr ->
                                val selected = instr.symbol == symbol
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) Bull.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { vm.selectTicketSymbol(instr.symbol) }
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
            SectionCard(title = "Arah Posisi") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.setTicketDirection(TradeDirection.BUY) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (direction == TradeDirection.BUY) Bull else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (direction == TradeDirection.BUY) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(s.buy) }
                    Button(
                        onClick = { vm.setTicketDirection(TradeDirection.SELL) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (direction == TradeDirection.SELL) Bear else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (direction == TradeDirection.SELL) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(s.sell) }
                }
            }
        }

        item {
            SectionCard(title = "Manajemen Risiko", subtitle = "Risiko $risk% dari modal ${Formatters.money(prefs.balance)}") {
                Column {
                    Text("Risiko per transaksi (%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5, 1.0, 2.0, 3.0, 5.0).forEach { value ->
                            val selected = kotlin.math.abs(value - risk) < 0.01
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bull else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTicketRisk(value) }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text("$value%", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Jarak Stop Loss (%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.25, 0.5, 1.0, 1.5, 2.0).forEach { value ->
                            val selected = kotlin.math.abs(value - sl) < 0.01
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Bear else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTicketSl(value) }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text("$value%", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(s.tpRatio, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0, 1.5, 2.0, 3.0).forEach { value ->
                            val selected = kotlin.math.abs(value - tp) < 0.01
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setTicketTp(value) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("1:${value.toInt()}", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            val ticket = sized
            SectionCard(title = "Hasil Perhitungan", subtitle = "Dihitung dari harga live ${instrument.symbol}") {
                if (ticket == null) {
                    EmptyHint(text = "Menunggu harga pasar...")
                } else {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(label = s.entry, value = Formatters.price(ticket.entryPrice, instrument.digits), modifier = Modifier.weight(1f))
                            StatTile(label = s.lotSize, value = Formatters.lot(ticket.lot), valueColor = Bull, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(label = s.stopLoss, value = Formatters.price(ticket.stopLossPrice, instrument.digits), valueColor = Bear, caption = "${Formatters.pips(ticket.slDistancePips)} pips", modifier = Modifier.weight(1f))
                            StatTile(label = s.takeProfit, value = Formatters.price(ticket.takeProfitPrice, instrument.digits), valueColor = Bull, caption = "${Formatters.pips(ticket.tpDistancePips)} pips", modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(label = "Margin", value = Formatters.money(ticket.marginRequired), modifier = Modifier.weight(1f))
                            StatTile(label = "Potensi Profit", value = Formatters.money(ticket.potentialProfit), valueColor = Bull, modifier = Modifier.weight(1f))
                            StatTile(label = "Potensi Loss", value = Formatters.money(ticket.potentialLoss), valueColor = Bear, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(label = "Nilai Pip", value = Formatters.money(ticket.pipValuePerLot), modifier = Modifier.weight(1f))
                            StatTile(label = "Unit", value = String.format(java.util.Locale.US, "%,.0f", ticket.units), modifier = Modifier.weight(1f))
                            StatTile(label = "R:R", value = "1:${ticket.riskReward.toInt()}", valueColor = Gold, modifier = Modifier.weight(1f))
                        }
                        if (ticket.warnings.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            ticket.warnings.forEach { warning ->
                                Text("⚠ $warning", style = MaterialTheme.typography.bodySmall, color = Gold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.openPosition() }, modifier = Modifier.fillMaxWidth()) {
                            Text(s.openPosition)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = s.positions, subtitle = "${openPositions.size} posisi terbuka", trailing = { Pill(text = s.open.uppercase(), color = Bull) }) {
                if (openPositions.isEmpty()) {
                    EmptyHint(text = s.noPositions)
                } else {
                    Column {
                        openPositions.forEach { position ->
                            val pnl = vm.floatingPnl(position)
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${position.symbol} ${position.direction.label.uppercase()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${Formatters.lot(position.lot)} lot @ ${Formatters.price(position.entryPrice, 5)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("SL ${Formatters.price(position.stopLoss, 5)} • TP ${Formatters.price(position.takeProfit, 5)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Formatters.money(pnl), style = MaterialTheme.typography.titleMedium, color = if (pnl >= 0) Bull else Bear)
                                    OutlinedButton(onClick = { vm.closePosition(position) }) { Text(s.closePosition, style = MaterialTheme.typography.labelSmall) }
                                }
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
