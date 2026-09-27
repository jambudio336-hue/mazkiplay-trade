package com.mazkiplay.trade.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.TechnicalSnapshot
import com.mazkiplay.trade.domain.analysis.Indicators
import com.mazkiplay.trade.ui.theme.Aqua
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold
import com.mazkiplay.trade.ui.theme.Violet
import kotlin.math.abs

/**
 * Candlestick chart with indicator overlays, drawn directly on a Compose Canvas.
 *
 * Rendering is intentionally self-contained: no charting library means the release
 * APK stays small and the drawing code is fully under our control. The overlays are
 * driven by the same [TechnicalSnapshot] the analyser produces, so what the trader
 * sees on the chart is exactly what the signal engine measured.
 *
 * @param showEma draw the EMA 20/50/200 stack
 * @param showFib draw the Fibonacci retracement ladder
 * @param showZones draw supply/demand bands and fair value gaps
 */
@Composable
fun PriceChart(
    candles: List<Candle>,
    snapshot: TechnicalSnapshot?,
    digits: Int,
    modifier: Modifier = Modifier,
    showEma: Boolean = true,
    showFib: Boolean = true,
    showZones: Boolean = true
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (candles.size < 2) {
            Text(
                text = "Menunggu data pasar...",
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Canvas(Modifier.fillMaxWidth().height(260.dp).padding(6.dp)) {
            val visible = candles.takeLast(minOf(candles.size, 90))

            // ---- price domain, widened to keep any overlay inside the viewport ----
            var min = visible.minOf { it.low }
            var max = visible.maxOf { it.high }
            showFib.takeIf { it }?.let {
                snapshot?.fibLevels?.forEach { level ->
                    if (level.price.isFinite()) {
                        min = minOf(min, level.price)
                        max = maxOf(max, level.price)
                    }
                }
            }
            showZones.takeIf { it }?.let {
                snapshot?.supplyZones?.forEach { zone ->
                    min = minOf(min, zone.lower)
                    max = maxOf(max, zone.upper)
                }
                snapshot?.demandZones?.forEach { zone ->
                    min = minOf(min, zone.lower)
                    max = maxOf(max, zone.upper)
                }
            }
            val padding = (max - min) * 0.06
            min -= padding
            max += padding
            val span = (max - min).takeIf { it > 0.0 } ?: 1.0

            val leftAxis = 6f
            val rightPad = size.width * 0.12f
            val plotWidth = size.width - leftAxis - rightPad
            val n = visible.size
            val stepX = plotWidth / n
            val candleWidth = (stepX * 0.62f).coerceAtLeast(1.4f)

            fun yOf(price: Double): Float =
                (size.height - ((price - min) / span) * size.height).toFloat()

            // ---- grid + right-hand price axis ----
            val gridLines = 5
            repeat(gridLines + 1) { i ->
                val y = size.height * i / gridLines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f
                )
                val priceAtLine = max - span * (i.toDouble() / gridLines)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(190, 150, 170, 185)
                        textSize = 22f
                        isAntiAlias = true
                    }
                    drawText(
                        String.format(java.util.Locale.US, "%,.${minOf(digits, 4)}f", priceAtLine),
                        size.width - rightPad + 6f,
                        y + 7f,
                        paint
                    )
                }
            }

            // ---- supply / demand zones behind the candles ----
            if (showZones && snapshot != null) {
                snapshot.supplyZones.forEach { zone ->
                    val top = yOf(zone.upper)
                    val bottom = yOf(zone.lower)
                    drawRect(
                        color = Bear.copy(alpha = 0.10f + zone.strength * 0.02f),
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(size.width - rightPad, bottom - top)
                    )
                }
                snapshot.demandZones.forEach { zone ->
                    val top = yOf(zone.upper)
                    val bottom = yOf(zone.lower)
                    drawRect(
                        color = Bull.copy(alpha = 0.10f + zone.strength * 0.02f),
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(size.width - rightPad, bottom - top)
                    )
                }
            }

            // ---- fibonacci ladder ----
            if (showFib && snapshot != null) {
                snapshot.fibLevels.forEach { level ->
                    val y = yOf(level.price)
                    if (y in 0f..size.height) {
                        drawLine(
                            color = Violet.copy(alpha = 0.45f),
                            start = Offset(0f, y),
                            end = Offset(size.width - rightPad, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }
                }
            }

            // ---- fair value gaps ----
            if (showZones && snapshot != null) {
                snapshot.fvgs.forEach { gap ->
                    val top = yOf(gap.upper)
                    val bottom = yOf(gap.lower)
                    if (bottom > 0f && top < size.height) {
                        drawRect(
                            color = (if (gap.bullish) Aqua else Gold).copy(alpha = 0.16f),
                            topLeft = Offset(0f, top),
                            size = androidx.compose.ui.geometry.Size(size.width - rightPad, bottom - top)
                        )
                    }
                }
            }

            // ---- candles ----
            visible.forEachIndexed { i, candle ->
                val cx = leftAxis + stepX * (i + 0.5f)
                val color = if (candle.bullish) Bull else Bear
                val highY = yOf(candle.high)
                val lowY = yOf(candle.low)
                drawLine(
                    color = color.copy(alpha = 0.85f),
                    start = Offset(cx, highY),
                    end = Offset(cx, lowY),
                    strokeWidth = 1.4f
                )
                val openY = yOf(candle.open)
                val closeY = yOf(candle.close)
                val bodyTop = minOf(openY, closeY)
                val bodyHeight = abs(closeY - openY).coerceAtLeast(1.5f)
                drawRect(
                    color = color,
                    topLeft = Offset(cx - candleWidth / 2f, bodyTop),
                    size = androidx.compose.ui.geometry.Size(candleWidth, bodyHeight)
                )
            }

            // ---- EMA stack ----
            if (showEma) {
                val closes = candles.map { it.close }
                val offset = candles.size - visible.size
                fun drawOverlay(period: Int, color: Color, width: Float) {
                    val series = Indicators.emaSeries(closes, period)
                    if (series.size < visible.size) return
                    val window = series.drop(offset)
                    val path = Path()
                    window.forEachIndexed { i, v ->
                        val x = leftAxis + stepX * (i + 0.5f)
                        val y = yOf(v)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = color, style = Stroke(width = width))
                }
                drawOverlay(20, Aqua, 2.2f)
                drawOverlay(50, Gold, 2.2f)
                drawOverlay(200, Violet, 1.8f)
            }

            // ---- support / resistance rails ----
            snapshot?.supports?.take(2)?.forEach { level ->
                val y = yOf(level)
                if (y in 0f..size.height) {
                    drawLine(
                        color = Bull.copy(alpha = 0.75f),
                        start = Offset(0f, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.6f
                    )
                }
            }
            snapshot?.resistances?.take(2)?.forEach { level ->
                val y = yOf(level)
                if (y in 0f..size.height) {
                    drawLine(
                        color = Bear.copy(alpha = 0.75f),
                        start = Offset(0f, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.6f
                    )
                }
            }
        }

        // Legend pinned to the top-left corner of the chart.
        Text(
            text = "EMA20 " + "EMA50 " + "EMA200" + "  |  FIB  |  FVG  |  S/D",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
            color = Aqua.copy(alpha = 0.9f),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        )
    }
}
