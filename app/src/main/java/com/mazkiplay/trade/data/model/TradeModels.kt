package com.mazkiplay.trade.data.model

enum class TradeDirection(val label: String) { BUY("Buy"), SELL("Sell") }

enum class PositionStatus(val label: String) { OPEN("Open"), CLOSED("Closed") }

/** A trading position stored locally (Room). */
data class Position(
    val id: Long = 0L,
    val symbol: String,
    val direction: TradeDirection,
    val lot: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val tpRatio: Double,
    val slPercent: Double,
    val riskAmount: Double,
    val openedAt: Long,
    val status: PositionStatus = PositionStatus.OPEN,
    val closePrice: Double? = null,
    val closedAt: Long? = null,
    val pnl: Double? = null,
    val note: String = ""
) {
    val isOpen: Boolean get() = status == PositionStatus.OPEN

    val rrPlanned: Double
        get() {
            val risk = kotlin.math.abs(entryPrice - stopLoss)
            val reward = kotlin.math.abs(takeProfit - entryPrice)
            return if (risk <= 0.0) 0.0 else reward / risk
        }

    fun distanceInPips(current: Double, instrument: Instrument): Double =
        kotlin.math.abs(current - entryPrice) / instrument.pipSize
}

/** A manually scheduled entry alarm. */
data class EntryAlarm(
    val id: Long = 0L,
    val label: String,
    val symbol: String,
    val triggerAt: Long,
    val note: String = "",
    val enabled: Boolean = true,
    val repeatDaily: Boolean = false
) {
    val isPast: Boolean get() = triggerAt < System.currentTimeMillis()
}

data class JournalEntry(
    val id: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val symbol: String,
    val title: String,
    val body: String,
    val mood: String = "Netral"
)

/** A public signal provider offered by the Copy Trade module. */
data class CopyTrader(
    val id: String,
    val name: String,
    val country: String,
    val flag: String,
    val strategy: String,
    val riskLevel: String,
    val monthlyReturn: Double,
    val totalReturn: Double,
    val maxDrawdown: Double,
    val winRate: Double,
    val followers: Int,
    val aum: Double,
    val trades: Int,
    val rewardPercent: Double,
    val equityCurve: List<Double>,
    val topSymbols: List<String>,
    val verified: Boolean,
    val dataSource: String = "Katalog lokal",
    val dataStatus: String = "TIDAK_REALTIME",
    val lastVerifiedAt: Long = 0L,
    val openPositions: List<CopyPosition> = emptyList()
) {
    val score: Double
        get() = (monthlyReturn * 0.4) + (winRate / 100.0 * 30.0) - (maxDrawdown * 0.6) + (if (verified) 6.0 else 0.0)
}

data class CopyPosition(
    val symbol: String,
    val direction: TradeDirection,
    val entry: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val openedAt: Long = 0L
)

/** Result of the position sizing engine. */
data class SizedTrade(
    val symbol: String,
    val direction: TradeDirection,
    val balance: Double,
    val riskPercent: Double,
    val riskAmount: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val entryPrice: Double,
    val slDistancePips: Double,
    val tpDistancePips: Double,
    val pipValuePerLot: Double,
    val lot: Double,
    val units: Double,
    val marginRequired: Double,
    val potentialProfit: Double,
    val potentialLoss: Double,
    val riskReward: Double,
    val valid: Boolean,
    val warnings: List<String> = emptyList()
)
