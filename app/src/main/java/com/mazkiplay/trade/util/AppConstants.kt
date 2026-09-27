package com.mazkiplay.trade.util

/** Static configuration values used across the app. */
object AppConstants {
    const val APP_NAME = "Mazkiplay Trade"
    const val BRAND = "Nusantara Forex"
    const val SIGNATURE = "By.mazkiplayTrade"
    const val VERSION = "1.0.0"
    const val TAGLINE = "Trading cerdas, analisa otomatis"

    const val DEFAULT_BALANCE = 10_000.0
    const val DEFAULT_RISK_PERCENT = 1.0
    const val DEFAULT_LEVERAGE = 100
    const val DEFAULT_SL_PERCENT = 1.0

    const val PRICE_REFRESH_MINUTES = 15L
    const val NEWS_REFRESH_MINUTES = 30L

    /** Minutes used for the four major FX sessions, expressed in UTC. */
    const val SESSION_SYDNEY_OPEN = 21 * 60
    const val SESSION_SYDNEY_CLOSE = 6 * 60
    const val SESSION_TOKYO_OPEN = 0
    const val SESSION_TOKYO_CLOSE = 9 * 60
    const val SESSION_LONDON_OPEN = 7 * 60
    const val SESSION_LONDON_CLOSE = 16 * 60
    const val SESSION_NEWYORK_OPEN = 12 * 60
    const val SESSION_NEWYORK_CLOSE = 21 * 60

    val TP_RATIOS: List<Double> = listOf(1.0, 2.0, 3.0)
    val PREFERRED_TP_RATIOS: List<Double> = listOf(1.0, 1.5, 2.0, 3.0)
}
