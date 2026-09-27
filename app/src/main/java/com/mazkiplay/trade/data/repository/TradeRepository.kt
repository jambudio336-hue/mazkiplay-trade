package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.local.MazkiplayDatabase
import com.mazkiplay.trade.data.local.AlarmEntity
import com.mazkiplay.trade.data.local.PositionEntity
import com.mazkiplay.trade.data.model.EntryAlarm
import com.mazkiplay.trade.data.model.Instruments
import com.mazkiplay.trade.data.model.Position
import com.mazkiplay.trade.data.model.SizedTrade
import com.mazkiplay.trade.data.model.TradeDirection
import com.mazkiplay.trade.domain.trade.PositionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Everything persisted about trading: open/closed positions and entry alarms.
 *
 * Profit and loss is always recomputed from the live price on read, so a stored
 * position shows a correct floating result without needing a write per tick.
 */
class TradeRepository(private val database: MazkiplayDatabase) {

    val positions: Flow<List<Position>> = database.positionDao().observeAll()
        .map { rows -> rows.map { it.toModel() } }

    val openPositions: Flow<List<Position>> = database.positionDao().observeOpen()
        .map { rows -> rows.map { it.toModel() } }

    val alarms: Flow<List<EntryAlarm>> = database.alarmDao().observeAll()
        .map { rows ->
            rows.map {
                EntryAlarm(
                    id = it.id,
                    label = it.label,
                    symbol = it.symbol,
                    triggerAt = it.triggerAt,
                    note = it.note,
                    enabled = it.enabled,
                    repeatDaily = it.repeatDaily
                )
            }
        }

    suspend fun savePosition(sized: SizedTrade, price: Double, note: String = ""): Long =
        database.positionDao().insert(
            PositionEntity.fromModel(
                Position(
                    symbol = sized.symbol,
                    direction = sized.direction,
                    lot = sized.lot,
                    entryPrice = if (price > 0.0) price else sized.entryPrice,
                    stopLoss = sized.stopLossPrice,
                    takeProfit = sized.takeProfitPrice,
                    tpRatio = sized.riskReward,
                    slPercent = 0.0,
                    riskAmount = sized.riskAmount,
                    openedAt = System.currentTimeMillis(),
                    note = note
                )
            )
        )

    /** Close a position at [price] and store the realised result. */
    suspend fun closePosition(position: Position, price: Double) {
        val instrument = Instruments.bySymbol(position.symbol)
        val pipValue = PositionCalculator.pipValuePerLot(instrument, price)
        val pips = (price - position.entryPrice) / instrument.pipSize
        val signedPips = if (position.direction == TradeDirection.BUY) pips else -pips
        val pnl = signedPips * pipValue * position.lot
        database.positionDao().close(
            id = position.id,
            closePrice = price,
            closedAt = System.currentTimeMillis(),
            pnl = pnl
        )
    }

    /** Floating P/L of an open position at the current market price. */
    fun floatingPnl(position: Position, price: Double): Double {
        if (price <= 0.0) return 0.0
        val instrument = Instruments.bySymbol(position.symbol)
        val pipValue = PositionCalculator.pipValuePerLot(instrument, price)
        val pips = (price - position.entryPrice) / instrument.pipSize
        val signedPips = if (position.direction == TradeDirection.BUY) pips else -pips
        return signedPips * pipValue * position.lot
    }

    suspend fun deletePosition(id: Long) = database.positionDao().delete(id)

    suspend fun saveAlarm(alarm: EntryAlarm): Long = database.alarmDao().insert(
        AlarmEntity(
            label = alarm.label,
            symbol = alarm.symbol,
            triggerAt = alarm.triggerAt,
            note = alarm.note,
            enabled = alarm.enabled,
            repeatDaily = alarm.repeatDaily
        )
    )

    suspend fun setAlarmEnabled(id: Long, enabled: Boolean) = database.alarmDao().setEnabled(id, enabled)

    suspend fun rescheduleAlarm(id: Long, triggerAt: Long) = database.alarmDao().reschedule(id, triggerAt)

    suspend fun deleteAlarm(id: Long) = database.alarmDao().delete(id)

    suspend fun pendingAlarms(): List<EntryAlarm> = database.alarmDao().enabledAlarms().map {
        EntryAlarm(
            id = it.id,
            label = it.label,
            symbol = it.symbol,
            triggerAt = it.triggerAt,
            note = it.note,
            enabled = it.enabled,
            repeatDaily = it.repeatDaily
        )
    }
}
