package com.mazkiplay.trade.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mazkiplay.trade.data.model.Position
import com.mazkiplay.trade.data.model.PositionStatus
import com.mazkiplay.trade.data.model.TradeDirection

/** Room row for a recorded trade. */
@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbol: String,
    val direction: String,
    val lot: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val tpRatio: Double,
    val slPercent: Double,
    val riskAmount: Double,
    val openedAt: Long,
    val status: String,
    val closePrice: Double?,
    val closedAt: Long?,
    val pnl: Double?,
    val note: String
) {
    fun toModel(): Position = Position(
        id = id,
        symbol = symbol,
        direction = runCatching { TradeDirection.valueOf(direction) }.getOrDefault(TradeDirection.BUY),
        lot = lot,
        entryPrice = entryPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        tpRatio = tpRatio,
        slPercent = slPercent,
        riskAmount = riskAmount,
        openedAt = openedAt,
        status = runCatching { PositionStatus.valueOf(status) }.getOrDefault(PositionStatus.OPEN),
        closePrice = closePrice,
        closedAt = closedAt,
        pnl = pnl,
        note = note
    )

    companion object {
        fun fromModel(p: Position): PositionEntity = PositionEntity(
            id = p.id,
            symbol = p.symbol,
            direction = p.direction.name,
            lot = p.lot,
            entryPrice = p.entryPrice,
            stopLoss = p.stopLoss,
            takeProfit = p.takeProfit,
            tpRatio = p.tpRatio,
            slPercent = p.slPercent,
            riskAmount = p.riskAmount,
            openedAt = p.openedAt,
            status = p.status.name,
            closePrice = p.closePrice,
            closedAt = p.closedAt,
            pnl = p.pnl,
            note = p.note
        )
    }
}

/** Room row for a scheduled entry alarm. */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val symbol: String,
    val triggerAt: Long,
    val note: String,
    val enabled: Boolean,
    val repeatDaily: Boolean
)

/** Room row for the free-form trading journal. */
@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val createdAt: Long,
    val symbol: String,
    val title: String,
    val body: String,
    val mood: String
)
