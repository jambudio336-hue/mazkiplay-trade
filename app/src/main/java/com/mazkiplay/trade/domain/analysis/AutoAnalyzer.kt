package com.mazkiplay.trade.domain.analysis

import com.mazkiplay.trade.data.model.AnalysisResult
import com.mazkiplay.trade.data.model.Bias
import com.mazkiplay.trade.data.model.Candle
import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.NewsItem
import kotlin.math.abs
import kotlin.math.max

/**
 * The "Analisa Otomatis" button in a single object: it runs the technical and
 * fundamental engines, blends them, then derives a complete trade plan.
 *
 * Technical carries 60% of the weight because it is computed on live price, while
 * fundamentals shape the bias and cut confidence during high-impact news windows.
 */
object AutoAnalyzer {

    private const val TECHNICAL_WEIGHT = 0.6
    private const val FUNDAMENTAL_WEIGHT = 0.4

    fun analyze(
        symbol: String,
        candles: List<Candle>,
        events: List<EconomicEvent>,
        news: List<NewsItem>,
        tpRatio: Double = 2.0
    ): AnalysisResult {
        val instrument = com.mazkiplay.trade.data.model.Instruments.bySymbol(symbol)
        val technical = TechnicalAnalyzer.analyze(candles, instrument)
        val fundamental = FundamentalAnalyzer.analyze(instrument, events, news)

        val blended = technical.score * TECHNICAL_WEIGHT + fundamental.score * FUNDAMENTAL_WEIGHT
        val bias = Bias.fromScore(blended)
        val (entry, stop, target) = TechnicalAnalyzer.levelsFor(technical, instrument, bias, tpRatio)

        val risk = abs(entry - stop)
        val reward = abs(target - entry)
        val rr = if (risk <= 1e-9) 0.0 else reward / risk

        // Confidence: signal strength, tempered by data quality and news risk.
        var confidence = 50 + (abs(blended) * 45).toInt()
        if (candles.size < 60) confidence -= 8
        if (technical.atr <= 0.0) confidence -= 10
        if (fundamental.highImpactToday >= 3) confidence -= 6
        confidence = confidence.coerceIn(40, 95)

        val reasons = ArrayList<String>()
        reasons += "Bias gabungan: ${bias.label} (skor ${fmt(blended)}) dari teknikal ${fmt(technical.score)} dan fundamental ${fmt(fundamental.score)}"
        reasons += technical.signals
        reasons += fundamental.notes
        if (fundamental.nextEvent != null) {
            reasons += "Agenda terdekat: ${fundamental.nextEvent.title} (${fundamental.nextEvent.currency}) — ${fundamental.nextEvent.impact.label}"
        }
        reasons += "Rencana: entry ${fmtPrice(entry, instrument)}, SL ${fmtPrice(stop, instrument)}, TP${tpRatio.toInt()}:1 ${fmtPrice(target, instrument)} (R:R ${fmt(rr)})"
        reasons += if (bias.isBullish) "Skenario utama: cari konfirmasi buy di area diskon" else if (bias.isBearish) "Skenario utama: cari konfirmasi sell di area premium" else "Skenario utama: tunggu breakout dari range sebelum entry"

        return AnalysisResult(
            symbol = symbol,
            bias = bias,
            confidence = confidence,
            entry = entry,
            stopLoss = stop,
            takeProfit = target,
            riskReward = rr,
            reasons = reasons,
            technical = technical,
            fundamental = fundamental,
            generatedAt = System.currentTimeMillis()
        )
    }

    /** One-line summary used by the dashboard cards and the notification body. */
    fun summaryLine(result: AnalysisResult): String =
        "${result.symbol}: ${result.bias.label} • keyakinan ${result.confidence}% • R:R ${fmt(result.riskReward)}"

    private fun fmt(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)

    private fun fmtPrice(value: Double, instrument: Instrument): String =
        String.format(java.util.Locale.US, "%,.${instrument.digits}f", value)

    /** Distance in pips between two prices on a given instrument. */
    fun pipsBetween(a: Double, b: Double, instrument: Instrument): Double =
        abs(a - b) / instrument.pipSize

    fun safeDiv(a: Double, b: Double): Double = if (max(abs(b), 1e-12) < 1e-12) 0.0 else a / b
}
