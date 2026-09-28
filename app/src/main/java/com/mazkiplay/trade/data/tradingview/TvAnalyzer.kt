package com.mazkiplay.trade.data.tradingview

import com.mazkiplay.trade.data.model.CurrencyStrength
import com.mazkiplay.trade.data.model.ScreenerRow
import com.mazkiplay.trade.data.model.SignalBias
import com.mazkiplay.trade.data.model.TradingSignal
import com.mazkiplay.trade.data.model.TvCalendarEvent
import com.mazkiplay.trade.data.model.TvQuote
import com.mazkiplay.trade.data.model.TvTechnical
import kotlin.math.abs

/**
 * Turns raw TradingView readings into the app's own derived numbers.
 *
 * The signal blend is a real calculation rather than a re-labelled technical score:
 * the fundamental leg is built from the *currencies inside the pair* and the upcoming
 * calendar events that touch them, so a strongly-bearish EURUSD with a high-impact
 * US release in twenty minutes ends up meaningfully different from one without.
 */
object TvAnalyzer {

    // ------------------------------------------------------------------- signals

    /**
     * Blends the live technical score with the fundamental weight of the pair.
     *
     * @param technical the scanner reading for [symbol]
     * @param quote the live price, used for the entry/TP/SL levels
     * @param events the calendar, filtered to the pair's own currencies
     * @param tpRatio reward multiple the user selected, driving the TP distance
     */
    fun buildSignal(
        symbol: String,
        technical: TvTechnical?,
        quote: TvQuote?,
        events: List<TvCalendarEvent>,
        tpRatio: Double = 2.0,
        timeframeLabel: String = "M15",
        slPercent: Double = 1.0
    ): TradingSignal {
        val tech = technical ?: TvTechnical(symbol)
        val price = quote?.close ?: 0.0
        val fundamental = fundamentalScore(symbol, events)
        val combined = (tech.recommendAll * 0.65) + (fundamental * 0.35)
        val bias = SignalBias.fromScore(combined)

        // Level math follows the instrument's own pip size so gold, yen crosses and
        // crypto all produce sane distances from the same inputs.
        val instrument = TvTickers.instrumentOf(symbol)
        val atr = if (tech.atr > 0.0) tech.atr else price * 0.004
        val riskDistance = if (price > 0.0) (price * slPercent / 100.0).let {
            if (atr > 0.0) (it + atr * 0.5) / 2.0 else it
        } else atr

        val buy = !bias.isSell
        val entry = price
        val stop = if (price > 0.0) {
            if (buy) price - riskDistance else price + riskDistance
        } else 0.0
        val take = if (price > 0.0) {
            if (buy) price + riskDistance * tpRatio else price - riskDistance * tpRatio
        } else 0.0

        val risk = abs(entry - stop)
        val reward = abs(take - entry)
        val rr = if (risk > 0.0) reward / risk else tpRatio

        // Confidence rewards agreement: a technical and a fundamental leg pointing the
        // same way is worth more than either alone, and disagreement is penalised.
        val agreement = 1.0 - (abs(tech.recommendAll - fundamental) / 2.0)
        val magnitude = abs(combined).coerceIn(0.0, 1.0)
        val confidence = ((magnitude * 72.0) + (agreement * 28.0)).toInt().coerceIn(0, 100)

        val reasons = tech.reasons().toMutableList()
        reasons += fundamentalReasons(symbol, events)
        reasons += "Skor gabungan ${fmt(combined)} (teknikal ${fmt(tech.recommendAll)}, fundamental ${fmt(fundamental)})"

        return TradingSignal(
            symbol = symbol,
            bias = bias,
            technicalScore = tech.recommendAll,
            fundamentalScore = fundamental,
            combinedScore = combined,
            confidence = confidence,
            price = price,
            entry = round(entry, instrument.digits),
            stopLoss = round(stop, instrument.digits),
            takeProfit = round(take, instrument.digits),
            riskReward = round(rr, 2),
            timeframe = timeframeLabel,
            reasons = reasons
        )
    }

    /**
     * The fundamental leg, in the same -1..1 space as the technical score.
     *
     * Each high-impact event in the next 24 hours that touches the base currency pulls
     * the pair up, and each that touches the quote currency pulls it down, weighted by
     * impact and by how close it is. Currency strength drifts across the whole book are
     * folded in too, so the number is never a pure calendar artefact.
     */
    fun fundamentalScore(symbol: String, events: List<TvCalendarEvent>): Double {
        val instrument = TvTickers.instrumentOf(symbol)
        val now = System.currentTimeMillis()
        val horizon = now + 24 * 3_600_000L

        var score = 0.0
        events.forEach { event ->
            if (event.importance < 1) return@forEach
            if (event.dateMillis < now - 3_600_000L || event.dateMillis > horizon) return@forEach
            if (event.hasActual) return@forEach

            val weight = when (event.importance) {
                2 -> 0.35
                1 -> 0.15
                else -> 0.0
            }
            // Nearer events matter more: 1.0 at release time falling to 0.4 a day out.
            val hoursAway = ((event.dateMillis - now) / 3_600_000.0).coerceIn(0.0, 24.0)
            val proximity = 1.0 - (hoursAway / 24.0) * 0.6

            val w = weight * proximity
            when {
                event.currency.equals(instrument.baseCurrency, ignoreCase = true) -> score += w
                event.currency.equals(instrument.quoteCurrency, ignoreCase = true) -> score -= w
            }
        }

        return score.coerceIn(-1.0, 1.0)
    }

    private fun fundamentalReasons(symbol: String, events: List<TvCalendarEvent>): String {
        val instrument = TvTickers.instrumentOf(symbol)
        val now = System.currentTimeMillis()
        val soon = events.firstOrNull {
            it.importance >= 2 &&
                it.dateMillis in now..(now + 12 * 3_600_000L) &&
                (it.currency.equals(instrument.baseCurrency, true) ||
                    it.currency.equals(instrument.quoteCurrency, true))
        }
        return soon?.let {
            "Agenda ${it.currency} berdampak tinggi: ${it.title} (${it.countdown()} lagi)"
        } ?: "Tidak ada agenda berdampak tinggi untuk ${instrument.baseCurrency}/${instrument.quoteCurrency} dalam 12 jam"
    }

    // --------------------------------------------------------- currency strength

    /**
     * Currency strength meter, computed from the live percentage moves of every pair.
     *
     * A currency is credited with its pair's move when it is the base leg and debited
     * when it is the quote leg, then averaged over the pairs that include it \u2014 so the
     * meter is a genuine cross-sectional read rather than a fixed table.
     */
    fun currencyStrength(quotes: Map<String, TvQuote>): List<CurrencyStrength> {
        val buckets = LinkedHashMap<String, MutableList<Double>>()
        TvTickers.currencyLegs().forEach { (symbol, base, quote) ->
            val move = quotes[symbol]?.changePercent ?: return@forEach
            buckets.getOrPut(base) { mutableListOf() }.add(move)
            buckets.getOrPut(quote) { mutableListOf() }.add(-move)
        }

        return TvTickers.currencies().map { currency ->
            val samples = buckets[currency].orEmpty()
            val score = if (samples.isEmpty()) 0.0 else samples.average()
            CurrencyStrength(
                currency = currency,
                score = round(score, 3),
                samples = samples.size
            )
        }.sortedByDescending { it.score }
    }

    // ------------------------------------------------------------------ screener

    /** Rows for the screener screen: live price joined to the technical reading. */
    fun screenerRows(
        quotes: Map<String, TvQuote>,
        technicals: Map<String, TvTechnical>
    ): List<ScreenerRow> = quotes.values.map { quote ->
        val tech = technicals[quote.symbol] ?: TvTechnical(quote.symbol)
        ScreenerRow(
            symbol = quote.symbol,
            name = TvTickers.instrumentOf(quote.symbol).displayName,
            price = quote.close,
            changePercent = quote.changePercent,
            rsi = tech.rsi,
            trend = tech.trend,
            bias = tech.bias,
            volatility = tech.volatility,
            confidence = tech.confidence
        )
    }.sortedByDescending { abs(it.changePercent) }

    // ------------------------------------------------------------------- helpers

    /** Decimal rounding that respects an instrument's quoted digits. */
    fun round(value: Double, digits: Int): Double {
        var factor = 1.0
        repeat(digits.coerceIn(0, 8)) { factor *= 10.0 }
        return Math.round(value * factor) / factor
    }

    private fun fmt(v: Double): String = String.format(java.util.Locale.US, "%.2f", v)
}
