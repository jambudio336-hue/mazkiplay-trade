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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.components.StatTile
import com.mazkiplay.trade.ui.stringsOf
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.tradeViewModel
import com.mazkiplay.trade.util.Formatters

/** Forex calculator: pip value, margin and profit/loss for any instrument. */
@Composable
fun CalculatorScreen(app: MazkiplayApp, prefs: UserPreferences) {
    val s = stringsOf(prefs)
    val vm = tradeViewModel(app)

    val symbol by vm.calcSymbol.collectAsState()
    val lot by vm.calcLot.collectAsState()
    val pips by vm.calcPips.collectAsState()
    val leverage by vm.calcLeverage.collectAsState()

    var lotText by remember(lot) { mutableStateOf(lot.toString()) }
    var pipText by remember(pips) { mutableStateOf(pips.toString()) }

    val instrument = Instruments.bySymbol(symbol)
    val result = remember(symbol, lot, pips, leverage) { vm.calcResult() }
    val price = app.marketRepository.quoteOf(symbol).price

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(s.calculator, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Hitung nilai pip, margin dan estimasi profit/loss", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SectionCard(title = "Instrumen", subtitle = "${instrument.displayName} \u2022 leverage 1:$leverage") {
                Column {
                    Instruments.all.chunked(3).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { instr ->
                                val selected = instr.symbol == symbol
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) Bull.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { vm.setCalcSymbol(instr.symbol) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(instr.symbol, style = MaterialTheme.typography.labelSmall, color = if (selected) Bull else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Leverage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 50, 100, 500, 1000).forEach { value ->
                            val selected = value == leverage
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (selected) Gold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { vm.setCalcLeverage(value) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("1:$value", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Input", subtitle = "Harga live ${Formatters.price(price, instrument.digits)}") {
                Column {
                    OutlinedTextField(
                        value = lotText,
                        onValueChange = { text -> lotText = text; text.toDoubleOrNull()?.let { vm.setCalcLot(it) } },
                        label = { Text(s.lotSize) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pipText,
                        onValueChange = { text -> pipText = text; text.toDoubleOrNull()?.let { vm.setCalcPips(it) } },
                        label = { Text("Jumlah pip") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Nilai Pip / Lot", value = Formatters.money(result.pipValue), modifier = Modifier.weight(1f))
                        StatTile(label = "Nilai Pip \u00d7 Lot", value = Formatters.money(result.pipValueForLot), valueColor = Bull, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SectionCard(title = "Hasil", subtitle = "Estimasi berdasarkan harga terakhir") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Margin", value = Formatters.money(result.margin), modifier = Modifier.weight(1f))
                        StatTile(label = "Leverage", value = "1:${result.leverage}", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Profit $pips pips", value = Formatters.money(result.profitForPips), valueColor = Bull, modifier = Modifier.weight(1f))
                        StatTile(label = "Loss $pips pips", value = Formatters.money(result.lossForPips), valueColor = com.mazkiplay.trade.ui.theme.Bear, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text(s.signalDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
