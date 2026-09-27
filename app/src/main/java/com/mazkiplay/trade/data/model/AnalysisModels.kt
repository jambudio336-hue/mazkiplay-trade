package com.mazkiplay.trade.data.model

/** Directional bias produced by the automatic analyser. */
enum class Bias(val label: String, val score: Double) {
    STRONG_BUY("Strong Buy", 1.0),
    BUY("Buy", 0.5),
    NEUTRAL("Neutral", 0.0),
    SELL("Sell", -0.5),
    STRONG_SELL("Strong Sell", -1.0);

    val isBullish: Boolean get() = score > 0.0
    val isBearish: Boolean get() = score < 0.0

    companion object {
        fun fromScore(score: Double): Bias = when {
            score >= 0.55 -> STRONG_BUY
            score >= 0.18 -> BUY
            score <= -0.55 -> STRONG_SELL
            score <= -0.18 -> SELL
            else -> NEUTRAL
        }
    }
}

/** Everything the technical engine measured on the last candle series. */
data class TechnicalSnapshot(
    val price: Double = 0.0,
    val ema20: Double = 0.0,
    val ema50: Double = 0.0,
    val ema200: Double = 0.0,
    val rsi: Double = 50.0,
    val atr: Double = 0.0,
    val macd: Double = 0.0,
    val macdSignal: Double = 0.0,
    val macdHistogram: Double = 0.0,
    val bollingerUpper: Double = 0.0,
    val bollingerLower: Double = 0.0,
    val trend: String = "-",
    val structure: String = "-",
    val score: Double = 0.0,
    val supports: List<Double> = emptyList(),
    val resistances: List<Double> = emptyList(),
    val fibLevels: List<FibLevel> = emptyList(),
    val fvgs: List<Fvg> = emptyList(),
    val supplyZones: List<Zone> = emptyList(),
    val demandZones: List<Zone> = emptyList(),
    val signals: List<String> = emptyList()
)

data class FundamentalSnapshot(
    val currencyStrength: Map<String, Double> = emptyMap(),
    val highImpactToday: Int = 0,
    val upcomingCount: Int = 0,
    val nextEvent: EconomicEvent? = null,
    val sentiment: Double = 0.0,
    val score: Double = 0.0,
    val notes: List<String> = emptyList()
)

data class AnalysisResult(
    val symbol: String = "",
    val bias: Bias = Bias.NEUTRAL,
    val confidence: Int = 50,
    val entry: Double = 0.0,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val riskReward: Double = 0.0,
    val reasons: List<String> = emptyList(),
    val technical: TechnicalSnapshot = TechnicalSnapshot(),
    val fundamental: FundamentalSnapshot = FundamentalSnapshot(),
    val generatedAt: Long = 0L
)
