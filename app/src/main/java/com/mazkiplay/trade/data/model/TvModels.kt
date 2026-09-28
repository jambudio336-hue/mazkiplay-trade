package com.mazkiplay.trade.data.model

import kotlin.math.abs

/**
 * Live-feed models sourced from TradingView's public JSON endpoints.
 *
 * These are deliberately separate from the Yahoo-backed [Quote] / [Candle] models:
 * both feeds run side by side, TradingView is the primary reading for prices and
 * technicals, and the Yahoo path stays as the documented fallback when TradingView
 * is unreachable.
 */

/** Connection health of the live feed, surfaced in the UI header. */
enum class FeedStatus(val label: String) {
    CONNECTING("MENGHUBUNGKAN"),
    LIVE("LIVE"),
    STALE("TERTUNDA"),
    OFFLINE("OFFLINE");

    val isHealthy: Boolean get() = this == LIVE || this == STALE
}

/** A live quote straight from the scanner: price, session range and the move. */
data class TvQuote(
    val symbol: String,
    val ticker: String,
    val name: String,
    val close: Double = 0.0,
    val changePercent: Double = 0.0,
    val changeAbs: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val volume: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isUp: Boolean get() = changePercent >= 0.0
    val hasData: Boolean get() = close > 0.0

    /** Where the price sits inside the session range, 0 (low) .. 1 (high). */
    val rangePosition: Float
        get() {
            val span = high - low
            if (span <= 0.0) return 0.5f
            return ((close - low) / span).coerceIn(0.0, 1.0).toFloat()
    }
}

/**
 * A transparent market-wide snapshot derived only from quotes resolved by the live feed.
 * It intentionally carries no synthetic prices: unavailable symbols are counted in
 * [unresolved] and never included in the breadth calculations.
 */
data class MarketPulse(
    val total: Int = 0,
    val advancing: Int = 0,
    val declining: Int = 0,
    val unchanged: Int = 0,
    val unresolved: Int = 0,
    val averageChangePercent: Double = 0.0,
    val topGainer: TvQuote? = null,
    val topLoser: TvQuote? = null,
    val mostVolatile: TvQuote? = null,
    val updatedAt: Long = 0L
) {
    val breadthPercent: Int
        get() = if (total == 0) 0 else ((advancing.toDouble() / total) * 100.0).toInt()
    val hasData: Boolean get() = total > 0
}

/**
 * The scanner's technical reading for one symbol.
 *
 * `recommendAll` is a -1..1 score (positive = buy) and is the headline number the
 * TradingView gauge is built from; the three sub-scores let the signal panel explain
 * *why* the verdict came out the way it did.
 */
data class TvTechnical(
    val symbol: String,
    val recommendAll: Double = 0.0,
    val recommendMa: Double = 0.0,
    val recommendOther: Double = 0.0,
    val rsi: Double = 50.0,
    val atr: Double = 0.0,
    val ema20: Double = 0.0,
    val ema50: Double = 0.0,
    val ema200: Double = 0.0,
    val adx: Double = 0.0,
    val cci: Double = 0.0,
    val volatility: Double = 0.0
) {
    val bias: SignalBias get() = SignalBias.fromScore(recommendAll)

    /** 0..100 strength of the reading, driven by how far the score is from neutral. */
    val confidence: Int
        get() {
            val magnitude = abs(recommendAll).coerceIn(0.0, 1.0)
            val agreement = abs(recommendMa - recommendOther).let { 1.0 - (it / 2.0) }
            return ((magnitude * 78.0) + (agreement * 22.0)).toInt().coerceIn(0, 100)
        }

    val rsiState: String
        get() = when {
            rsi >= 70.0 -> "OVERBOUGHT"
            rsi <= 30.0 -> "OVERSOLD"
            rsi >= 55.0 -> "BULLISH"
            rsi <= 45.0 -> "BEARISH"
            else -> "NETRAL"
        }

    val trend: String
        get() = when {
            ema20 > ema50 && ema50 > ema200 -> "NAIK KUAT"
            ema20 > ema50 -> "NAIK"
            ema20 < ema50 && ema50 < ema200 -> "TURUN KUAT"
            ema20 < ema50 -> "TURUN"
            else -> "SIDEWAYS"
        }

    /** Human-readable support for the verdict, built from the numbers themselves. */
    fun reasons(): List<String> {
        val out = mutableListOf<String>()
        out += when {
            recommendMa > 0.25 -> "Rata-rata bergerak mayoritas bullish (skor ${fmt(recommendMa)})"
            recommendMa < -0.25 -> "Rata-rata bergerak mayoritas bearish (skor ${fmt(recommendMa)})"
            else -> "Rata-rata bergerak campuran (skor ${fmt(recommendMa)})"
        }
        out += when {
            recommendOther > 0.25 -> "Oscillator mendukung beli (skor ${fmt(recommendOther)})"
            recommendOther < -0.25 -> "Oscillator mendukung jual (skor ${fmt(recommendOther)})"
            else -> "Oscillator netral (skor ${fmt(recommendOther)})"
        }
        out += "RSI ${fmt(rsi)} \u2014 $rsiState"
        if (adx > 0.0) {
            out += if (adx >= 25.0) "Tren kuat (ADX ${fmt(adx)})" else "Tren lemah (ADX ${fmt(adx)})"
        }
        out += "Struktur EMA: $trend"
        if (atr > 0.0) out += "Volatilitas ATR ${fmt(atr)}"
        return out
    }

    private fun fmt(v: Double): String = String.format(java.util.Locale.US, "%.2f", v)
}

/** Buy/sell verdict shared by the technical reading and the combined signal. */
enum class SignalBias(val label: String) {
    STRONG_BUY("BELI KUAT"),
    BUY("BELI"),
    NEUTRAL("NETRAL"),
    SELL("JUAL"),
    STRONG_SELL("JUAL KUAT");

    val isBuy: Boolean get() = this == STRONG_BUY || this == BUY
    val isSell: Boolean get() = this == STRONG_SELL || this == SELL

    companion object {
        /**
         * TradingView's own bucket boundaries for `Recommend.All` are +/-0.1 and
         * +/-0.5; the same cut points are used here so the app and the embedded
         * gauge widget never disagree on screen.
         */
        fun fromScore(score: Double): SignalBias = when {
            score >= 0.5 -> STRONG_BUY
            score >= 0.1 -> BUY
            score <= -0.5 -> STRONG_SELL
            score <= -0.1 -> SELL
            else -> NEUTRAL
        }
    }
}

/**
 * The trading signal shown in the signal panel: the live technical reading blended
 * with the fundamental weight of upcoming high-impact events, with the confidence
 * score recomputed for the blend rather than copied from either side.
 */
data class TradingSignal(
    val symbol: String,
    val bias: SignalBias,
    val technicalScore: Double,
    val fundamentalScore: Double,
    val combinedScore: Double,
    val confidence: Int,
    val price: Double,
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val riskReward: Double,
    val timeframe: String,
    val reasons: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)

/** A headline from the TradingView news flow. */
data class TvNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val urgency: Int = 0,
    val symbols: List<String> = emptyList(),
    val paywalled: Boolean = false
) {
    val isFresh: Boolean get() = System.currentTimeMillis() - publishedAt < 3_600_000L
    val isBreaking: Boolean get() = urgency >= 2
}

/** An economic-calendar row from TradingView, with a live countdown upstream. */
data class TvCalendarEvent(
    val id: String,
    val title: String,
    val country: String,
    val currency: String,
    val indicator: String,
    val dateMillis: Long,
    val importance: Int,
    val actual: String = "",
    val forecast: String = "",
    val previous: String = ""
) {
    val hasActual: Boolean get() = actual.isNotBlank() && actual != "\u2014"

    val impact: Impact
        get() = when {
            importance >= 2 -> Impact.HIGH
            importance == 1 -> Impact.MEDIUM
            importance == 0 -> Impact.LOW
            else -> Impact.HOLIDAY
        }

    val minutesUntil: Long get() = (dateMillis - System.currentTimeMillis()) / 60_000L

    val isImminent: Boolean get() = minutesUntil in 0..60

    /** `H:MM:SS`-style countdown label, or a past marker once the event has fired. */
    fun countdown(): String {
        val diff = dateMillis - System.currentTimeMillis()
        if (diff <= 0L) return "SUDAH RILIS"
        val totalMinutes = diff / 60_000L
        val days = totalMinutes / 1440L
        val hours = (totalMinutes % 1440L) / 60L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L -> "${days}h ${hours}j"
            hours > 0L -> "${hours}j ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/** One row of the currency-strength meter. */
data class CurrencyStrength(
    val currency: String,
    val score: Double,
    val samples: Int = 0
) {
    val isStrong: Boolean get() = score >= 0.0

    /** 0..1 position on the bar, mapping the typical +/-1% band onto the full width. */
    val magnitude: Float get() = (abs(score) / 1.0).coerceIn(0.0, 1.0).toFloat()
}

/** A screener row: live price plus the reading, ready for filtering. */
data class ScreenerRow(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val rsi: Double,
    val trend: String,
    val bias: SignalBias,
    val volatility: Double,
    val confidence: Int
) {
    val isUp: Boolean get() = changePercent >= 0.0
}

/** Filters the screener screen can apply, driving real predicates on live rows. */
enum class ScreenerFilter(val label: String) {
    ALL("Semua"),
    TRENDING_UP("Tren Naik"),
    TRENDING_DOWN("Tren Turun"),
    OVERBOUGHT("Overbought"),
    OVERSOLD("Oversold"),
    HIGH_VOLATILITY("Volatilitas Tinggi"),
    BUY_SIGNAL("Sinyal Beli"),
    SELL_SIGNAL("Sinyal Jual");

    fun test(row: ScreenerRow): Boolean = when (this) {
        ALL -> true
        TRENDING_UP -> row.trend.startsWith("NAIK")
        TRENDING_DOWN -> row.trend.startsWith("TURUN")
        OVERBOUGHT -> row.rsi >= 70.0
        OVERSOLD -> row.rsi <= 30.0
        HIGH_VOLATILITY -> row.volatility >= 1.0
        BUY_SIGNAL -> row.bias.isBuy
        SELL_SIGNAL -> row.bias.isSell
    }
}
