package com.mazkiplay.trade.domain.analysis

import com.mazkiplay.trade.data.model.Bias
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.TechnicalSnapshot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Turns a raw candle series into a scored technical reading.
 *
 * Six independent signals are measured, each mapped to the -1..+1 range and then
 * combined with fixed weights. The weighted total becomes the bias score, and the
 * absolute value feeds the confidence figure shown in the analysis panel.
 */
object TechnicalAnalyzer {

    private const val W_TREND = 0.30
    private const val W_SLOPE = 0.10
    private const val W_RSI = 0.15
    private const val W_MACD = 0.15
    private const val W_STRUCTURE = 0.20
    private const val W_BANDS = 0.10

    fun analyze(candles: List<Candle>, instrument: Instrument): TechnicalSnapshot {
        if (candles.size < 10) {
            return TechnicalSnapshot(price = candles.lastOrNull()?.close ?: 0.0, signals = emptyList())
        }

        val closes = candles.map { it.close }
        val price = closes.last()
        val signals = ArrayList<String>()

        val ema20 = Indicators.ema(closes, 20)
        val ema50 = Indicators.ema(closes, 50)
        val ema200 = Indicators.ema(closes, 200)
        val ema20Slope = Indicators.emaSlope(closes, 20, 5)
        val rsi = Indicators.rsi(closes, 14)
        val atr = Indicators.atr(candles, 14)
        val macd = Indicators.macd(closes)
        val bands = Indicators.bollinger(closes, 20, 2.0)
        val structure = Indicators.marketStructure(candles)
        val levels = Indicators.swingLevels(candles)
        val fibs = Indicators.fibonacci(candles)
        val fvgs = Indicators.fairValueGaps(candles)
        val (supply, demand) = Indicators.supplyDemand(candles)

        // --- 1. EMA stack -------------------------------------------------
        val trendScore = when {
            price > ema20 && ema20 > ema50 && ema50 > ema200 -> {
                signals += "EMA tersusun bullish (harga > EMA20 > EMA50 > EMA200)"
                1.0
            }
            price < ema20 && ema20 < ema50 && ema50 < ema200 -> {
                signals += "EMA tersusun bearish (harga < EMA20 < EMA50 < EMA200)"
                -1.0
            }
            price > ema200 && ema20 > ema50 -> {
                signals += "Struktur EMA masih condong bullish di atas EMA200"
                0.5
            }
            price < ema200 && ema20 < ema50 -> {
                signals += "Struktur EMA masih condong bearish di bawah EMA200"
                -0.5
            }
            else -> {
                signals += "EMA saling berpotongan, arah belum jelas"
                0.0
            }
        }

        // --- 2. EMA20 slope ----------------------------------------------
        val slopeScore = (ema20Slope / 0.35).coerceIn(-1.0, 1.0)
        if (abs(ema20Slope) > 0.08) {
            signals += if (ema20Slope > 0)
                "Slope EMA20 naik ${String.format("%.2f", ema20Slope)}% dalam 5 bar"
            else
                "Slope EMA20 turun ${String.format("%.2f", abs(ema20Slope))}% dalam 5 bar"
        }

        // --- 3. RSI -------------------------------------------------------
        val rsiScore = when {
            rsi >= 70 -> -0.5 + (rsi - 70) / 60.0
            rsi <= 30 -> 0.5 + (rsi - 30) / 60.0
            else -> (rsi - 50.0) / 40.0
        }.coerceIn(-1.0, 1.0)
        signals += when {
            rsi >= 70 -> "RSI ${String.format("%.1f", rsi)} — jenuh beli, waspadai koreksi"
            rsi <= 30 -> "RSI ${String.format("%.1f", rsi)} — jenuh jual, berpotensi rebound"
            rsi > 55 -> "RSI ${String.format("%.1f", rsi)} — momentum bullish"
            rsi < 45 -> "RSI ${String.format("%.1f", rsi)} — momentum bearish"
            else -> "RSI ${String.format("%.1f", rsi)} — netral"
        }

        // --- 4. MACD ------------------------------------------------------
        val macdScale = max(atr, price * 0.0002)
        val macdScore = (macd.histogram / (macdScale * 0.6)).coerceIn(-1.0, 1.0)
        signals += when {
            macd.histogram > 0 && macd.macd > macd.signal -> "MACD histogram positif, garis MACD di atas signal"
            macd.histogram < 0 && macd.macd < macd.signal -> "MACD histogram negatif, garis MACD di bawah signal"
            else -> "MACD mulai menyilang, tunggu konfirmasi"
        }

        // --- 5. Market structure -----------------------------------------
        val structureScore = when {
            structure.higherHighs && structure.higherLows -> 1.0
            !structure.higherHighs && !structure.higherLows -> -1.0
            else -> 0.0
        }
        signals += "Market structure: ${structure.label}"

        // --- 6. Bollinger position ---------------------------------------
        val bandRange = bands.upper - bands.lower
        val bandScore = if (bandRange <= 0.0) 0.0 else {
            // Stretched above / below the envelope mean-reverts.
            val position = (price - bands.middle) / (bandRange / 2.0)
            (-position).coerceIn(-1.0, 1.0)
        }
        if (bandRange > 0.0 && price > bands.upper) signals += "Harga menembus band atas Bollinger"
        if (bandRange > 0.0 && price < bands.lower) signals += "Harga menembus band bawah Bollinger"

        // --- combine ------------------------------------------------------
        val score = (trendScore * W_TREND +
            slopeScore * W_SLOPE +
            rsiScore * W_RSI +
            macdScore * W_MACD +
            structureScore * W_STRUCTURE +
            bandScore * W_BANDS) / (W_TREND + W_SLOPE + W_RSI + W_MACD + W_STRUCTURE + W_BANDS)

        val nearestSupport = levels.supports.minByOrNull { abs(it - price) }
        val nearestResistance = levels.resistances.minByOrNull { abs(it - price) }
        nearestSupport?.let { signals += "Support terdekat di ${round(it, instrument)}" }
        nearestResistance?.let { signals += "Resistance terdekat di ${round(it, instrument)}" }
        if (fvgs.isNotEmpty()) {
            val gap = fvgs.first()
            signals += "FVG ${if (gap.bullish) "bullish" else "bearish"} belum terisi di ${round(gap.lower, instrument)} - ${round(gap.upper, instrument)}"
        }
        supply.firstOrNull()?.let { signals += "Zona supply ${round(it.lower, instrument)} - ${round(it.upper, instrument)} (kekuatan ${it.strength}/5)" }
        demand.firstOrNull()?.let { signals += "Zona demand ${round(it.lower, instrument)} - ${round(it.upper, instrument)} (kekuatan ${it.strength}/5)" }

        return TechnicalSnapshot(
            price = price,
            ema20 = ema20,
            ema50 = ema50,
            ema200 = ema200,
            rsi = rsi,
            atr = atr,
            macd = macd.macd,
            macdSignal = macd.signal,
            macdHistogram = macd.histogram,
            bollingerUpper = bands.upper,
            bollingerLower = bands.lower,
            trend = when {
                price > ema50 && ema50 > ema200 -> "Bullish"
                price < ema50 && ema50 < ema200 -> "Bearish"
                else -> "Sideways"
            },
            structure = structure.label,
            score = score,
            supports = levels.supports,
            resistances = levels.resistances,
            fibLevels = fibs,
            fvgs = fvgs,
            supplyZones = supply,
            demandZones = demand,
            signals = signals
        )
    }

    /** ATR-based stop and take-profit so every instrument gets sane distances. */
    fun levelsFor(snapshot: TechnicalSnapshot, instrument: Instrument, bias: Bias, rr: Double = 2.0): Triple<Double, Double, Double> {
        val entry = snapshot.price
        if (entry <= 0.0) return Triple(0.0, 0.0, 0.0)
        val atrStops = max(snapshot.atr, entry * 0.0012)
        val risk = atrStops * 1.5
        val isBull = bias.score >= 0.0
        val structural = if (isBull) snapshot.supports.firstOrNull() else snapshot.resistances.firstOrNull()

        val stop = if (isBull) {
            min(entry - risk, structural?.let { it - atrStops * 0.3 } ?: (entry - risk))
        } else {
            max(entry + risk, structural?.let { it + atrStops * 0.3 } ?: (entry + risk))
        }
        val distance = abs(entry - stop)
        val target = if (isBull) entry + distance * rr else entry - distance * rr
        return Triple(entry, stop, target)
    }

    private fun round(value: Double, instrument: Instrument): String =
        String.format(java.util.Locale.US, "%,.${instrument.digits}f", value)
}
