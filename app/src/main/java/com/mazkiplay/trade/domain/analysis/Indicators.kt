package com.mazkiplay.trade.domain.analysis

import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.FibLevel
import com.mazkiplay.trade.data.model.Fvg
import com.mazkiplay.trade.data.model.MarketProfile
import com.mazkiplay.trade.data.model.OrderBook
import com.mazkiplay.trade.data.model.OrderBookLevel
import com.mazkiplay.trade.data.model.ProfileRow
import com.mazkiplay.trade.data.model.Zone
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure, dependency-free technical indicator maths.
 * Every function is defensive: empty or too-short inputs return neutral values
 * instead of throwing, so the UI never crashes on a partial market feed.
 */
object Indicators {

    // ---------------------------------------------------------------- averages

    fun sma(values: List<Double>, period: Int): Double {
        if (values.isEmpty() || period <= 0) return 0.0
        val window = values.takeLast(period)
        return window.average()
    }

    /** Exponential moving average series, aligned so `series[i]` belongs to `values[i]`. */
    fun emaSeries(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty() || period <= 0) return emptyList()
        val k = 2.0 / (period + 1.0)
        val out = ArrayList<Double>(values.size)
        var prev = values.first()
        values.forEachIndexed { index, v ->
            prev = if (index == 0) v else v * k + prev * (1 - k)
            out.add(prev)
        }
        return out
    }

    fun ema(values: List<Double>, period: Int): Double =
        emaSeries(values, period).lastOrNull() ?: 0.0

    /** Standard EMA slope over [lookback] bars as a percentage of price. */
    fun emaSlope(values: List<Double>, period: Int, lookback: Int = 5): Double {
        val series = emaSeries(values, period)
        if (series.size <= lookback) return 0.0
        val last = series.last()
        val prior = series[series.size - 1 - lookback]
        if (prior == 0.0) return 0.0
        return (last - prior) / prior * 100.0
    }

    // ------------------------------------------------------------------- rsi

    fun rsi(values: List<Double>, period: Int = 14): Double {
        if (values.size < period + 1) return 50.0
        var gain = 0.0
        var loss = 0.0
        for (i in values.size - period until values.size) {
            val diff = values[i] - values[i - 1]
            if (diff >= 0) gain += diff else loss -= diff
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0

        // Wilder smoothing across the remaining history for a stable reading.
        for (i in (values.size - period - 1) downTo 1) {
            val diff = values[i] - values[i - 1]
            val g = if (diff > 0) diff else 0.0
            val l = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (period - 1) + g) / period
            avgLoss = (avgLoss * (period - 1) + l) / period
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    // ------------------------------------------------------------------- atr

    fun atr(candles: List<Candle>, period: Int = 14): Double {
        val trs = trueRanges(candles)
        if (trs.isEmpty()) return 0.0
        if (trs.size < period) return trs.average()
        var prev = trs.take(period).average()
        for (i in period until trs.size) {
            prev = (prev * (period - 1) + trs[i]) / period
        }
        return prev
    }

    fun trueRanges(candles: List<Candle>): List<Double> {
        if (candles.size < 2) return emptyList()
        val out = ArrayList<Double>(candles.size - 1)
        for (i in 1 until candles.size) {
            val c = candles[i]
            val prevClose = candles[i - 1].close
            out.add(max(c.high - c.low, max(abs(c.high - prevClose), abs(c.low - prevClose))))
        }
        return out
    }

    /** ATR expressed as a percentage of the last close — volatility regime. */
    fun atrPercent(candles: List<Candle>, period: Int = 14): Double {
        val close = candles.lastOrNull()?.close ?: return 0.0
        if (close == 0.0) return 0.0
        return atr(candles, period) / close * 100.0
    }

    // ------------------------------------------------------------------ macd

    data class Macd(val macd: Double, val signal: Double, val histogram: Double)

    fun macd(values: List<Double>, fast: Int = 12, slow: Int = 26, signalPeriod: Int = 9): Macd {
        if (values.size < slow) return Macd(0.0, 0.0, 0.0)
        val fastSeries = emaSeries(values, fast)
        val slowSeries = emaSeries(values, slow)
        val macdSeries = fastSeries.indices.map { fastSeries[it] - slowSeries[it] }
        val signalSeries = emaSeries(macdSeries, signalPeriod)
        val macd = macdSeries.last()
        val signal = signalSeries.last()
        return Macd(macd, signal, macd - signal)
    }

    // ------------------------------------------------------------- bollinger

    data class Bollinger(val upper: Double, val middle: Double, val lower: Double, val width: Double)

    fun bollinger(values: List<Double>, period: Int = 20, multiplier: Double = 2.0): Bollinger {
        if (values.size < 2) return Bollinger(0.0, 0.0, 0.0, 0.0)
        val window = values.takeLast(period)
        val mean = window.average()
        val variance = window.sumOf { (it - mean) * (it - mean) } / window.size
        val sd = sqrt(variance)
        val upper = mean + multiplier * sd
        val lower = mean - multiplier * sd
        val width = if (mean == 0.0) 0.0 else (upper - lower) / mean * 100.0
        return Bollinger(upper, mean, lower, width)
    }

    // ----------------------------------------------------------------- pivots

    data class Pivots(
        val pivot: Double,
        val r1: Double,
        val r2: Double,
        val r3: Double,
        val s1: Double,
        val s2: Double,
        val s3: Double
    )

    /** Classic floor-trader pivots computed from the previous completed candle. */
    fun pivots(candles: List<Candle>): Pivots {
        if (candles.size < 2) return Pivots(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val c = candles[candles.size - 2]
        val p = (c.high + c.low + c.close) / 3.0
        val range = c.high - c.low
        return Pivots(
            pivot = p,
            r1 = 2 * p - c.low,
            r2 = p + range,
            r3 = c.high + 2 * (p - c.low),
            s1 = 2 * p - c.high,
            s2 = p - range,
            s3 = c.low - 2 * (c.high - p)
        )
    }

    // ------------------------------------------------------- swing level scan

    data class Levels(val supports: List<Double>, val resistances: List<Double>)

    /**
     * Fractal swing scan: a pivot high is a candle whose high is the highest of
     * the surrounding [strength] candles on both sides (the mirror holds for lows).
     * Levels are then merged when they sit within 0.15 ATR of each other and ranked
     * by how often price reacted there.
     */
    fun swingLevels(candles: List<Candle>, strength: Int = 3, maxLevels: Int = 4): Levels {
        if (candles.size < strength * 2 + 2) return Levels(emptyList(), emptyList())
        val price = candles.last().close
        val tolerance = max(atr(candles), price * 0.0005)
        val toleranceBand = tolerance * 0.35

        val highs = ArrayList<Double>()
        val lows = ArrayList<Double>()

        for (i in strength until candles.size - strength) {
            val c = candles[i]
            var isHigh = true
            var isLow = true
            for (j in (i - strength)..(i + strength)) {
                if (j == i) continue
                if (candles[j].high >= c.high) isHigh = false
                if (candles[j].low <= c.low) isLow = false
            }
            if (isHigh) highs.add(c.high)
            if (isLow) lows.add(c.low)
        }

        val resistances = mergeLevels(highs, toleranceBand)
            .filter { it >= price - tolerance }
            .sortedBy { it }
            .take(maxLevels)

        val supports = mergeLevels(lows, toleranceBand)
            .filter { it <= price + tolerance }
            .sortedByDescending { it }
            .take(maxLevels)

        return Levels(supports, resistances)
    }

    private fun mergeLevels(values: List<Double>, tolerance: Double): List<Double> {
        if (values.isEmpty()) return emptyList()
        val sorted = values.sorted()
        val clusters = ArrayList<MutableList<Double>>()
        sorted.forEach { v ->
            val current = clusters.lastOrNull()
            if (current != null && abs(v - current.average()) <= tolerance) current.add(v)
            else clusters.add(mutableListOf(v))
        }
        return clusters.sortedByDescending { it.size }.map { it.average() }
    }

    // -------------------------------------------------------------- fibonacci

    /** Retracement ladder measured between the swing low and swing high of the window. */
    fun fibonacci(candles: List<Candle>, lookback: Int = 120): List<FibLevel> {
        if (candles.size < 10) return emptyList()
        val window = candles.takeLast(lookback)
        val high = window.maxOf { it.high }
        val low = window.minOf { it.low }
        val span = high - low
        if (span <= 0.0) return emptyList()
        val rising = window.last().close >= window.first().open
        val ratios = listOf(
            0.0 to "0.0%",
            0.236 to "23.6%",
            0.382 to "38.2%",
            0.5 to "50.0%",
            0.618 to "61.8%",
            0.786 to "78.6%",
            1.0 to "100.0%",
            1.272 to "127.2%",
            1.618 to "161.8%"
        )
        return ratios.map { (ratio, label) ->
            val price = if (rising) high - span * ratio else low + span * ratio
            FibLevel(ratio, label, price)
        }
    }

    // -------------------------------------------------------------------- fvg

    /**
     * Fair Value Gaps: a three-candle imbalance where the gap between candle n-2
     * and candle n was never traded. Unfilled gaps act as magnet levels.
     */
    fun fairValueGaps(candles: List<Candle>, maxGaps: Int = 3): List<Fvg> {
        if (candles.size < 3) return emptyList()
        val price = candles.last().close
        val gaps = ArrayList<Fvg>()
        for (i in 2 until candles.size) {
            val a = candles[i - 2]
            val c = candles[i]
            if (c.low > a.high) {
                gaps.add(Fvg(a.high, c.low, true, i))
            } else if (c.high < a.low) {
                gaps.add(Fvg(c.high, a.low, false, i))
            }
        }
        // Keep only gaps that price has not fully traded back through yet.
        val unfilled = gaps.filter { gap ->
            val subsequent = candles.drop(gap.index)
            if (gap.bullish) subsequent.none { it.low <= gap.lower } else subsequent.none { it.high >= gap.upper }
        }
        return unfilled
            .sortedByDescending { it.index }
            .sortedBy { abs(it.mid - price) }
            .take(maxGaps)
    }

    val Fvg.mid: Double get() = (lower + upper) / 2.0

    // ---------------------------------------------------------- supply demand

    /**
     * Supply / demand zones are base candles (small bodies) immediately followed by
     * an impulsive candle. Strength grows with the size of that impulse.
     */
    fun supplyDemand(candles: List<Candle>, maxZones: Int = 3): Pair<List<Zone>, List<Zone>> {
        if (candles.size < 20) return emptyList<Zone>() to emptyList<Zone>()
        val avgRange = candles.takeLast(50).map { it.span }.average().takeIf { it > 0 } ?: 1e-9
        val supply = ArrayList<Zone>()
        val demand = ArrayList<Zone>()
        val start = max(5, candles.size - 200)
        for (i in start until candles.size - 1) {
            val base = candles[i]
            val impulse = candles[i + 1]
            val impulseRange = impulse.span
            if (base.body > avgRange * 0.6) continue
            if (impulseRange < avgRange * 1.5) continue
            val strength = ((impulseRange / avgRange) * 2).toInt().coerceIn(1, 5)
            if (impulse.close < impulse.open) {
                supply.add(Zone(min(base.low, impulse.low), max(base.high, impulse.high), strength, "Supply"))
            } else {
                demand.add(Zone(min(base.low, impulse.low), max(base.high, impulse.high), strength, "Demand"))
            }
        }
        val price = candles.last().close
        val nearestSupply = supply
            .filter { it.upper >= price }
            .sortedBy { it.lower }
            .take(maxZones)
            .ifEmpty { supply.sortedByDescending { it.upper }.take(maxZones) }
        val nearestDemand = demand
            .filter { it.lower <= price }
            .sortedByDescending { it.upper }
            .take(maxZones)
            .ifEmpty { demand.sortedByDescending { it.upper }.take(maxZones) }
        return nearestSupply to nearestDemand
    }

    // ------------------------------------------------------- market structure

    data class Structure(val label: String, val higherHighs: Boolean, val higherLows: Boolean)

    fun marketStructure(candles: List<Candle>, lookback: Int = 60): Structure {
        if (candles.size < 20) return Structure("Data terbatas", false, false)
        val window = candles.takeLast(lookback)
        val swings = swingLevels(window, strength = 2, maxLevels = 6)
        val resistanceTrend = trendOf(swings.resistances.reversed())
        val supportTrend = trendOf(swings.supports.reversed())
        val higherHighs = resistanceTrend > 0
        val higherLows = supportTrend > 0
        val label = when {
            higherHighs && higherLows -> "Uptrend (HH/HL)"
            !higherHighs && !higherLows -> "Downtrend (LH/LL)"
            higherHighs && !higherLows -> "Bullish lemah / konsolidasi"
            else -> "Bearish lemah / konsolidasi"
        }
        return Structure(label, higherHighs, higherLows)
    }

    private fun trendOf(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val first = values.first()
        val last = values.last()
        if (first == 0.0) return 0.0
        return (last - first) / abs(first)
    }

    // ------------------------------------------------------------- order book

    /**
     * Reconstructed depth ladder. No retail API exposes a real FX order book, so
     * liquidity is estimated from where the recent candles actually traded: levels
     * inside past candle ranges accumulate that candle's volume, everything else
     * decays exponentially with distance from price.
     */
    fun orderBook(candles: List<Candle>, symbol: String, depth: Int = 10): OrderBook {
        if (candles.isEmpty()) return OrderBook(symbol, emptyList(), emptyList(), 0.0)
        val price = candles.last().close
        if (price <= 0.0) return OrderBook(symbol, emptyList(), emptyList(), 0.0)
        val tick = price * 0.00006
        val recent = candles.takeLast(40)
        val decayUnit = price * 0.0025 + 1e-9

        fun volumeAt(level: Double): Double = recent.sumOf { c ->
            val inside = level in c.low..c.high
            if (inside) c.volume.coerceAtLeast(1.0)
            else c.volume.coerceAtLeast(1.0) * 0.06 * exp(-abs(level - c.close) / decayUnit)
        }

        val bids = (1..depth).map { i ->
            val level = price - tick * i
            OrderBookLevel(level, volumeAt(level) * (1.0 + (depth - i) * 0.04), "BID")
        }
        val asks = (1..depth).map { i ->
            val level = price + tick * i
            OrderBookLevel(level, volumeAt(level) * (1.0 + (depth - i) * 0.04), "ASK")
        }
        val bidVolume = bids.sumOf { it.volume }
        val askVolume = asks.sumOf { it.volume }
        val total = bidVolume + askVolume
        val imbalance = if (total == 0.0) 0.0 else (bidVolume - askVolume) / total * 100.0
        return OrderBook(symbol, bids, asks, imbalance)
    }

    // ---------------------------------------------------------- market profile

    /** Volume-at-price histogram with Point of Control and 70% value area. */
    fun marketProfile(candles: List<Candle>, symbol: String, rows: Int = 14): MarketProfile {
        if (candles.isEmpty()) return MarketProfile(symbol, 0.0, 0.0, 0.0, emptyList(), "-")
        val window = candles.takeLast(96)
        val high = window.maxOf { it.high }
        val low = window.minOf { it.low }
        val span = (high - low).takeIf { it > 0.0 } ?: return MarketProfile(symbol, high, high, high, emptyList(), "Flat")
        val step = span / rows
        val buckets = DoubleArray(rows)

        window.forEach { c ->
            val closeBucket = ((c.close - low) / step).toInt().coerceIn(0, rows - 1)
            buckets[closeBucket] += c.volume.coerceAtLeast(1.0)
            val highBucket = ((c.high - low) / step).toInt().coerceIn(0, rows - 1)
            val lowBucket = ((c.low - low) / step).toInt().coerceIn(0, rows - 1)
            buckets[highBucket] += c.volume * 0.25
            buckets[lowBucket] += c.volume * 0.25
        }

        val pocIndex = buckets.indices.maxByOrNull { buckets[it] } ?: 0
        val totalVolume = buckets.sum()
        var collected = buckets[pocIndex]
        var upper = pocIndex
        var lower = pocIndex
        while (collected < totalVolume * 0.7 && (upper < rows - 1 || lower > 0)) {
            val upVolume = if (upper < rows - 1) buckets[upper + 1] else -1.0
            val downVolume = if (lower > 0) buckets[lower - 1] else -1.0
            if (upVolume >= downVolume) {
                upper += 1
                collected += upVolume.coerceAtLeast(0.0)
            } else {
                lower -= 1
                collected += downVolume.coerceAtLeast(0.0)
            }
        }

        val profileRows = (0 until rows).map { i ->
            ProfileRow(low + step * (i + 0.5), buckets[i])
        }
        val relativePoc = pocIndex.toDouble() / (rows - 1).coerceAtLeast(1)
        val shape = when {
            relativePoc > 0.78 -> "P-shape (buying tail)"
            relativePoc < 0.22 -> "b-shape (selling tail)"
            else -> "D-shape (balanced)"
        }
        return MarketProfile(
            symbol = symbol,
            poc = low + step * (pocIndex + 0.5),
            valueAreaHigh = low + step * (upper + 1),
            valueAreaLow = low + step * lower,
            rows = profileRows,
            shape = shape
        )
    }

    // ------------------------------------------------------------- utilities

    /** Merge candles into a higher timeframe (H4 = four H1 candles, and so on). */
    fun aggregate(candles: List<Candle>, factor: Int): List<Candle> {
        if (factor <= 1 || candles.isEmpty()) return candles
        return candles.chunked(factor).mapNotNull { chunk ->
            if (chunk.isEmpty()) null
            else Candle(
                time = chunk.first().time,
                open = chunk.first().open,
                high = chunk.maxOf { it.high },
                low = chunk.minOf { it.low },
                close = chunk.last().close,
                volume = chunk.sumOf { it.volume }
            )
        }
    }

    /** Down-sample a series to [points] values for the compact sparkline widgets. */
    fun sparkline(values: List<Double>, points: Int = 40): List<Double> {
        if (values.size <= points) return values
        val step = values.size.toDouble() / points
        return (0 until points).map { i ->
            val index = (i * step).toInt().coerceIn(0, values.size - 1)
            values[index]
        }
    }
}
