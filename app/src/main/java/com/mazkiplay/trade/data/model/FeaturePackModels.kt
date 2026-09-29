package com.mazkiplay.trade.data.model

/** Deterministic state models shared by the premium screens. */
enum class EventState { UPCOMING, LIVE, ENDED }
enum class RiskProfile(val title: String, val maxRisk: Double, val minRr: Double, val maxPositions: Int) {
    CONSERVATIVE("Conservative", 0.5, 2.0, 2),
    BALANCED("Balanced", 1.0, 1.5, 4),
    AGGRESSIVE("Aggressive", 2.0, 1.2, 6),
    CUSTOM("Custom", 1.0, 1.5, 3)
}
enum class MarketRegime(val label: String) { TRENDING("TRENDING"), RANGING("RANGING"), HIGH_VOLATILITY("HIGH VOLATILITY"), LOW_VOLATILITY("LOW VOLATILITY"), BREAKOUT("BREAKOUT"), REVERSAL("REVERSAL") }

data class LiveEconomicEvent(
    val id: String,
    val title: String,
    val institution: String,
    val currency: String,
    val scheduledAt: Long,
    val impact: Impact,
    val officialUrl: String,
    val state: EventState = EventState.UPCOMING
)

data class DataHealth(
    val market: String = "LIVE",
    val crypto: String = "LIVE",
    val news: String = "LIVE",
    val calendar: String = "LIVE",
    val fundamental: String = "LIVE",
    val activeProvider: String = "TradingView → Yahoo fallback",
    val lastUpdateMillis: Long = System.currentTimeMillis()
) {
    val signalPaused: Boolean get() = listOf(market, crypto, news, calendar, fundamental).any { it == "STALE" || it == "OFFLINE" }
}

data class MarketBrief(
    val usd: String,
    val eur: String,
    val gold: String,
    val btc: String,
    val highImpactEvents: Int,
    val liveEvents: Int,
    val activeSignals: Int,
    val warning: String?
)

data class WhatIfResult(val riskAmount: Double, val rewardAmount: Double, val positionSize: Double, val rr: Double)

data class AcademyLesson(val id: String, val title: String, val icon: String, val summary: String, val sections: List<String>, val version: String = "1.0")
