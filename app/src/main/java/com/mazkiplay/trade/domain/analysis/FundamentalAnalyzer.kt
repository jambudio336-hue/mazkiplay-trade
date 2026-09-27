package com.mazkiplay.trade.domain.analysis

import com.mazkiplay.trade.data.model.EconomicEvent
import com.mazkiplay.trade.data.model.FundamentalSnapshot
import com.mazkiplay.trade.data.model.Impact
import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.NewsItem
import kotlin.math.abs
import kotlin.math.min

/**
 * Fundamental engine: currency strength, economic-calendar pressure and headline
 * sentiment, all folded into a single -1..+1 score for the instrument at hand.
 */
object FundamentalAnalyzer {

    private val positiveWords = listOf(
        "naik", "menguat", "bullish", "surge", "rally", "gain", "rise", "strong", "beat", "optimis",
        "rekor", "positif", "boost", "higher", "improve", "rebound", "growth", "up"
    )
    private val negativeWords = listOf(
        "turun", "melemah", "bearish", "slump", "drop", "fall", "weak", "miss", "pesimis",
        "resesi", "negatif", "crash", "lower", "decline", "recession", "worry", "risk-off", "down"
    )

    fun analyze(
        instrument: Instrument,
        events: List<EconomicEvent>,
        news: List<NewsItem>,
        now: Long = System.currentTimeMillis()
    ): FundamentalSnapshot {
        val notes = ArrayList<String>()

        val dayStart = now - (now % 86_400_000L)
        val todayEvents = events.filter { it.dateMillis >= dayStart && it.dateMillis < dayStart + 86_400_000L }
        val upcoming = events.filter { it.dateMillis >= now }.sortedBy { it.dateMillis }
        val highImpactToday = todayEvents.count { it.impact == Impact.HIGH }

        // --- 1. currency strength from the calendar ----------------------
        val strengthMap = currencyStrength(events, now)
        val base = strengthMap[instrument.baseCurrency] ?: 0.0
        val quote = strengthMap[instrument.quoteCurrency] ?: 0.0
        var score = (base - quote).coerceIn(-1.0, 1.0) * 0.5

        notes += "Kekuatan mata uang: ${instrument.baseCurrency} ${fmt(base)} vs ${instrument.quoteCurrency} ${fmt(quote)}"

        // --- 2. headline sentiment ---------------------------------------
        val sentiment = headlineSentiment(news)
        score += sentiment * 0.3
        notes += when {
            sentiment > 0.15 -> "Sentimen berita condong positif untuk aset berisiko"
            sentiment < -0.15 -> "Sentimen berita condong negatif / risk-off"
            else -> "Sentimen berita relatif netral"
        }

        // --- 3. calendar pressure ----------------------------------------
        if (highImpactToday > 0) {
            notes += "$highImpactToday agenda dampak tinggi hari ini — volatilitas tinggi"
            score *= 0.85
        }
        val nextEvent = upcoming.firstOrNull()
        nextEvent?.let {
            val mins = (it.dateMillis - now) / 60_000L
            if (mins in 0..60) {
                notes += "${it.title} (${it.currency}) rilis dalam $mins menit"
                score *= 0.9
            }
        }

        return FundamentalSnapshot(
            currencyStrength = strengthMap,
            highImpactToday = highImpactToday,
            upcomingCount = upcoming.size,
            nextEvent = nextEvent,
            sentiment = sentiment,
            score = score.coerceIn(-1.0, 1.0),
            notes = notes
        )
    }

    /**
     * Currency strength 0..100 derived from the *surprise* of released events:
     * actual beating forecast lifts the currency, missing it drags it down. The
     * score decays with age so last week's data does not dominate.
     */
    fun currencyStrength(events: List<EconomicEvent>, now: Long): Map<String, Double> {
        val buckets = HashMap<String, MutableList<Double>>()
        events.forEach { ev ->
            val actual = ev.actual.toDoubleOrNull()
            val forecast = ev.forecast.toDoubleOrNull()
            if (actual == null || forecast == null) return@forEach
            val ageHours = ((now - ev.dateMillis) / 3_600_000L).coerceAtLeast(0L)
            if (ageHours > 96) return@forEach
            val decay = 1.0 - ageHours / 120.0
            val surprise = if (abs(forecast) < 1e-9) 0.0 else ((actual - forecast) / abs(forecast)).coerceIn(-3.0, 3.0)
            val weighted = surprise * decay * ev.impact.level / 3.0
            buckets.getOrPut(ev.currency) { mutableListOf() }.add(weighted)
        }
        val result = HashMap<String, Double>()
        buckets.forEach { (currency, values) ->
            val raw = values.average()
            // Map the surprise average onto a 0..100 strength band centred at 50.
            result[currency] = (50.0 + raw * 12.0).coerceIn(5.0, 95.0)
        }
        // Fill the gap for majors without releases so they render at neutral.
        listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "NZD", "XAU").forEach { c ->
            result.putIfAbsent(c, 50.0)
        }
        return result
    }

    /** Weighted lexicon sentiment over the newest headlines. */
    fun headlineSentiment(news: List<NewsItem>): Double {
        if (news.isEmpty()) return 0.0
        val scored = news.take(40).map { item ->
            val text = "${item.title} ${item.summary}".lowercase()
            var hits = 0.0
            positiveWords.forEach { if (text.contains(it)) hits += 1.0 }
            negativeWords.forEach { if (text.contains(it)) hits -= 1.0 }
            val freshness = 1.0 - min(24.0, (System.currentTimeMillis() - item.publishedAt) / 3_600_000.0) / 48.0
            hits.coerceIn(-3.0, 3.0) * freshness
        }
        val average = scored.average()
        return (average / 3.0).coerceIn(-1.0, 1.0)
    }

    private fun fmt(value: Double): String = String.format(java.util.Locale.US, "%.0f", value)
}
