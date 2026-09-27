package com.mazkiplay.trade.domain.session

import com.mazkiplay.trade.data.model.MarketSession
import java.util.Calendar
import java.util.TimeZone

/**
 * Real-time status of the four major FX sessions.
 *
 * Every session is stored as minute-of-day boundaries in UTC. A session whose
 * close is smaller than its open (Sydney) wraps past midnight, which is handled
 * explicitly instead of being special-cased in the UI.
 */
object SessionManager {

    private data class Spec(
        val name: String,
        val city: String,
        val tzLabel: String,
        val open: Int,
        val close: Int
    )

    private val specs = listOf(
        Spec("Sydney", "Sydney", "AEST", 21 * 60, 6 * 60),
        Spec("Tokyo", "Tokyo", "JST", 0, 9 * 60),
        Spec("London", "London", "BST", 7 * 60, 16 * 60),
        Spec("New York", "New York", "EDT", 12 * 60, 21 * 60)
    )

    fun sessions(now: Long = System.currentTimeMillis()): List<MarketSession> {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = now
        val minuteOfDay = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE)

        return specs.map { spec ->
            val wraps = spec.close <= spec.open
            val isOpen = if (wraps) {
                minuteOfDay >= spec.open || minuteOfDay < spec.close
            } else {
                minuteOfDay in spec.open until spec.close
            }

            val totalSpan = if (wraps) (1440 - spec.open) + spec.close else spec.close - spec.open
            val elapsed = when {
                isOpen && wraps && minuteOfDay >= spec.open -> minuteOfDay - spec.open
                isOpen && wraps -> (1440 - spec.open) + minuteOfDay
                isOpen -> minuteOfDay - spec.open
                else -> 0
            }
            val progress = if (!isOpen || totalSpan <= 0) 0f else (elapsed.toFloat() / totalSpan).coerceIn(0f, 1f)
            val remaining = if (isOpen) (totalSpan - elapsed).toLong() else 0L

            MarketSession(
                name = spec.name,
                city = spec.city,
                tzLabel = spec.tzLabel,
                openUtcMinutes = spec.open,
                closeUtcMinutes = spec.close,
                isOpen = isOpen,
                progress = progress,
                remainingMinutes = remaining,
                nextOpenUtcMinutes = spec.open
            )
        }
    }

    /** Sessions currently open — used on the dashboard and by the analyser. */
    fun openSessions(now: Long = System.currentTimeMillis()): List<MarketSession> =
        sessions(now).filter { it.isOpen }

    /**
     * Liquidity score 0..100 for the current instant. The London/New York overlap
     * is the deepest window of the day, so it is weighted accordingly.
     */
    fun liquidityScore(now: Long = System.currentTimeMillis()): Int {
        val open = openSessions(now).map { it.name }
        var score = 20
        if (open.contains("Sydney")) score += 10
        if (open.contains("Tokyo")) score += 20
        if (open.contains("London")) score += 25
        if (open.contains("New York")) score += 25
        if (open.contains("London") && open.contains("New York")) score += 12
        return score.coerceAtMost(100)
    }

    fun describe(session: MarketSession, language: String): String {
        val id = language == "in"
        return if (session.isOpen) {
            val hours = session.remainingMinutes / 60
            val mins = session.remainingMinutes % 60
            if (id) "Buka • tutup dalam ${hours}j ${mins}m" else "Open • closes in ${hours}h ${mins}m"
        } else {
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utc.timeInMillis = System.currentTimeMillis()
            val minuteOfDay = utc.get(Calendar.HOUR_OF_DAY) * 60 + utc.get(Calendar.MINUTE)
            val until = ((session.nextOpenUtcMinutes - minuteOfDay) + 1440) % 1440
            if (id) "Tutup • buka dalam ${until / 60}j ${until % 60}m" else "Closed • opens in ${until / 60}h ${until % 60}m"
        }
    }
}
