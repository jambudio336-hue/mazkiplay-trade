package com.mazkiplay.trade.data.model

/** Broad grouping used for filtering and for picking the right pip maths. */
enum class InstrumentClass { METAL, MAJOR, MINOR, CRYPTO, INDEX }

/**
 * A tradable instrument. [yahooSymbol] is the public chart feed used for live
 * prices, [pipSize] drives every pip / risk calculation in the app.
 */
data class Instrument(
    val symbol: String,
    val yahooSymbol: String,
    val displayName: String,
    val klass: InstrumentClass,
    val pipSize: Double,
    val digits: Int,
    val contractSize: Double,
    val quoteCurrency: String,
    val baseCurrency: String
) {
    val isJpyQuote: Boolean get() = quoteCurrency == "JPY"

    val badge: String
        get() = when (klass) {
            InstrumentClass.METAL -> if (symbol.startsWith("XAU")) "GOLD" else "XAG"
            InstrumentClass.CRYPTO -> "CRYPTO"
            InstrumentClass.INDEX -> "INDEX"
            InstrumentClass.MAJOR -> "MAJOR"
            InstrumentClass.MINOR -> "CROSS"
        }
}

enum class Timeframe(
    val label: String,
    val interval: String,
    val range: String,
    val aggregate: Int
) {
    M1("M1", "1m", "5d", 1),
    M5("M5", "5m", "1mo", 1),
    M15("M15", "15m", "1mo", 1),
    H1("H1", "1h", "3mo", 1),
    H4("H4", "1h", "6mo", 4),
    D1("D1", "1d", "1y", 1);

    companion object {
        val defaults: List<Timeframe> = listOf(M1, M5, M15, H1, H4, D1)

        /** Resolves a stored label (the DataStore preference) back to a timeframe. */
        fun fromLabel(label: String): Timeframe =
            defaults.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: M15
    }
}

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val bullish: Boolean get() = close >= open
    val body: Double get() = kotlin.math.abs(close - open)
    val span: Double get() = high - low
}

data class Quote(
    val symbol: String,
    val price: Double = 0.0,
    val previousClose: Double = 0.0,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val updatedAt: Long = 0L,
    val spark: List<Double> = emptyList()
) {
    val change: Double get() = if (previousClose == 0.0) 0.0 else price - previousClose
    val changePercent: Double get() = if (previousClose == 0.0) 0.0 else change / previousClose * 100.0
    val isUp: Boolean get() = change >= 0.0
}

enum class Impact(val level: Int, val label: String) {
    HOLIDAY(0, "Holiday"),
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    companion object {
        fun from(raw: String): Impact = when (raw.trim().lowercase()) {
            "high" -> HIGH
            "medium" -> MEDIUM
            "low" -> LOW
            "holiday" -> HOLIDAY
            else -> LOW
        }
    }
}

data class EconomicEvent(
    val id: String,
    val title: String,
    val country: String,
    val currency: String,
    val dateMillis: Long,
    val impact: Impact,
    val actual: String = "",
    val forecast: String = "",
    val previous: String = ""
) {
    val hasActual: Boolean get() = actual.isNotBlank()
}

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val category: String = "Forex"
) {
    val isFresh: Boolean get() = System.currentTimeMillis() - publishedAt < 3_600_000L
}

data class MarketSession(
    val name: String,
    val city: String,
    val tzLabel: String,
    val openUtcMinutes: Int,
    val closeUtcMinutes: Int,
    val isOpen: Boolean,
    val progress: Float,
    val remainingMinutes: Long,
    val nextOpenUtcMinutes: Int
)

data class Zone(val lower: Double, val upper: Double, val strength: Int, val kind: String) {
    val mid: Double get() = (lower + upper) / 2.0
    val height: Double get() = kotlin.math.abs(upper - lower)
}

data class FibLevel(val ratio: Double, val label: String, val price: Double)

data class Fvg(val lower: Double, val upper: Double, val bullish: Boolean, val index: Int)

data class OrderBookLevel(val price: Double, val volume: Double, val side: String)

data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val imbalance: Double
)

data class ProfileRow(val price: Double, val volume: Double)

data class MarketProfile(
    val symbol: String,
    val poc: Double,
    val valueAreaHigh: Double,
    val valueAreaLow: Double,
    val rows: List<ProfileRow>,
    val shape: String
)
