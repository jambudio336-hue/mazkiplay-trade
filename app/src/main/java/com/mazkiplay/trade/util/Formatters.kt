package com.mazkiplay.trade.util

import java.util.Locale

/** Number / price / time formatting shared by every screen. */
object Formatters {

    fun price(value: Double, digits: Int): String =
        String.format(Locale.US, "%,.${digits}f", value)

    fun price(value: Double, instrument: com.mazkiplay.trade.data.model.Instrument): String =
        price(value, instrument.digits)

    fun signed(value: Double, digits: Int): String {
        val sign = if (value > 0) "+" else ""
        return "$sign${price(value, digits)}"
    }

    fun money(value: Double, digits: Int = 2): String {
        val abs = kotlin.math.abs(value)
        val sign = if (value < 0) "-" else ""
        return "$sign\$${String.format(Locale.US, "%,.${digits}f", abs)}"
    }

    fun compactMoney(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000_000 -> String.format(Locale.US, "\$%.2fB", value / 1_000_000_000)
            abs >= 1_000_000 -> String.format(Locale.US, "\$%.2fM", value / 1_000_000)
            abs >= 1_000 -> String.format(Locale.US, "\$%.1fK", value / 1_000)
            else -> String.format(Locale.US, "\$%.2f", value)
        }
    }

    fun percent(value: Double, digits: Int = 2): String =
        String.format(Locale.US, "%+.${digits}f%%", value)

    fun lot(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun pips(value: Double): String = String.format(Locale.US, "%.1f", value)

    /** Time using the user's chosen format, in the device time zone. */
    fun time(millis: Long, format: String): String =
        java.text.SimpleDateFormat(format, Locale.getDefault()).format(java.util.Date(millis))

    /** Relative "x minutes ago" label used across news and events. */
    fun ago(millis: Long, language: String): String {
        val diff = System.currentTimeMillis() - millis
        val mins = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        val id = language == "in"
        return when {
            diff < 60_000L -> if (id) "baru saja" else "just now"
            mins < 60L -> if (id) "$mins mnt lalu" else "$mins min ago"
            hours < 24L -> if (id) "$hours jam lalu" else "$hours h ago"
            days < 7L -> if (id) "$days hari lalu" else "$days d ago"
            else -> time(millis, "dd MMM")
        }
    }
}
