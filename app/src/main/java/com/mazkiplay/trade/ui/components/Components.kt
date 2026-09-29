package com.mazkiplay.trade.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.data.model.AnalysisResult
import com.mazkiplay.trade.data.model.Bias
import com.mazkiplay.trade.data.model.MarketProfile
import com.mazkiplay.trade.data.model.MarketSession
import com.mazkiplay.trade.data.model.OrderBook
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.util.Formatters

/** Elevated container used by every dashboard block. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    trailing?.invoke()
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

/** Small rounded label. */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (filled) Modifier.background(color)
                else Modifier.border(1.dp, color, RoundedCornerShape(50))
            )
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) MaterialTheme.colorScheme.surface else color,
            maxLines = 1
        )
    }
}

/** Bias chip with the semantic colour of the signal. */
@Composable
fun BiasChip(bias: Bias, modifier: Modifier = Modifier) {
    val color = when {
        bias.isBullish -> Bull
        bias.isBearish -> Bear
        else -> Gold
    }
    Pill(text = bias.label.uppercase(), color = color, modifier = modifier, filled = true)
}

/** Key figure with a caption. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    caption: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            maxLines = 1
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Compact sparkline drawn straight from a price series. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Bull,
    strokeWidth: Float = 2.5f,
    fill: Boolean = true
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (values.size - 1)

        fun yOf(v: Double) = (size.height - ((v - min) / span) * size.height).toFloat()

        val path = Path().apply {
            moveTo(0f, yOf(values.first()))
            values.drop(1).forEachIndexed { i, v -> lineTo(stepX * (i + 1), yOf(v)) }
        }

        if (fill) {
            val area = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, color = color.copy(alpha = 0.14f))
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth))
    }
}

/** Horizontal strength/progress bar with a 0..100 domain. */
@Composable
fun MeterBar(
    value: Double,
    modifier: Modifier = Modifier,
    color: Color = Bull,
    height: androidx.compose.ui.unit.Dp = 7.dp
) {
    val animated by animateFloatAsState(
        targetValue = (value / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "meter"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** Trader's session tile with a live open/closed state and a progress ring. */
@Composable
fun SessionTile(
    session: MarketSession,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val color = if (session.isOpen) Bull else MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = session.tzLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        MeterBar(value = (session.progress * 100.0), color = color, height = 5.dp)
        Spacer(Modifier.height(7.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Currency strength visualisation used by the fundamental panels. */
@Composable
fun CurrencyStrengthRow(
    currency: String,
    strength: Double,
    modifier: Modifier = Modifier
) {
    val color = when {
        strength >= 58 -> Bull
        strength <= 42 -> Bear
        else -> Gold
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(44.dp)
        )
        MeterBar(
            value = strength,
            color = color,
            height = 8.dp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format(java.util.Locale.US, "%.0f", strength),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp)
        )
    }
}

/** Analysis panel: bias, confidence and every reason behind the call. */
@Composable
fun AnalysisPanel(
    result: AnalysisResult,
    digits: Int,
    modifier: Modifier = Modifier,
    labels: PanelLabels = PanelLabels()
) {
    val color = when {
        result.bias.isBullish -> Bull
        result.bias.isBearish -> Bear
        else -> Gold
    }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BiasChip(result.bias)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${labels.confidence} ${result.confidence}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = String.format(java.util.Locale.US, "R:R 1:%.1f", result.riskReward),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        MeterBar(value = result.confidence.toDouble(), color = color, height = 8.dp)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = labels.entry,
                value = Formatters.price(result.entry, digits),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = labels.stopLoss,
                value = Formatters.price(result.stopLoss, digits),
                valueColor = Bear,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = labels.takeProfit,
                value = Formatters.price(result.takeProfit, digits),
                valueColor = Bull,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = labels.reasons,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        result.reasons.take(14).forEach { reason ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "\u2022",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
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

/** Labels injected by the screen so the panel stays locale-aware. */
data class PanelLabels(
    val confidence: String = "Keyakinan",
    val entry: String = "Entry",
    val stopLoss: String = "Stop Loss",
    val takeProfit: String = "Take Profit",
    val reasons: String = "Alasan Analisa"
)

/** Depth ladder reconstruction of the order book. */
@Composable
fun OrderBookView(book: OrderBook, digits: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("BID", style = MaterialTheme.typography.labelSmall, color = Bull, modifier = Modifier.weight(1f))
            Text(
                text = String.format(java.util.Locale.US, "Imbalance %+.1f%%", book.imbalance),
                style = MaterialTheme.typography.labelSmall,
                color = if (book.imbalance >= 0) Bull else Bear
            )
            Text("ASK", style = MaterialTheme.typography.labelSmall, color = Bear, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(6.dp))
        val rows = maxOf(book.bids.size, book.asks.size)
        val maxVolume = (book.bids + book.asks).maxOfOrNull { it.volume } ?: 1.0
        repeat(rows) { i ->
            val bid = book.bids.getOrNull(i)
            val ask = book.asks.getOrNull(i)
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Box(Modifier.weight(1f)) {
                    if (bid != null) {
                        MeterBar(
                            value = bid.volume / maxVolume * 100.0,
                            color = Bull.copy(alpha = 0.55f),
                            height = 16.dp
                        )
                        Text(
                            text = Formatters.price(bid.price, digits),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (ask != null) {
                        MeterBar(
                            value = ask.volume / maxVolume * 100.0,
                            color = Bear.copy(alpha = 0.55f),
                            height = 16.dp
                        )
                        Text(
                            text = Formatters.price(ask.price, digits),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Volume-at-price profile with POC and value area highlights. */
@Composable
fun MarketProfileView(profile: MarketProfile, digits: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text("POC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(Formatters.price(profile.poc, digits), style = MaterialTheme.typography.labelSmall, color = Gold)
            Spacer(Modifier.weight(1f))
            Text("VA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${Formatters.price(profile.valueAreaLow, digits)} - ${Formatters.price(profile.valueAreaHigh, digits)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))
        val maxVolume = profile.rows.maxOfOrNull { it.volume } ?: 1.0
        profile.rows.reversed().forEach { row ->
            val inValueArea = row.price in profile.valueAreaLow..profile.valueAreaHigh
            val isPoc = kotlin.math.abs(row.price - profile.poc) < (profile.rows.getOrNull(1)?.let {
                kotlin.math.abs(it.price - profile.rows.first().price)
            } ?: 0.0) / 2.0
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatters.price(row.price, digits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(78.dp)
                )
                MeterBar(
                    value = row.volume / maxVolume * 100.0,
                    color = when {
                        isPoc -> Gold
                        inValueArea -> Bull.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.outline
                    },
                    height = 12.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = profile.shape,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Empty-state placeholder. */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Section header used between routed screens. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
