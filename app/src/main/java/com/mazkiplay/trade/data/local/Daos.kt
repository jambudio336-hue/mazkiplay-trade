package com.mazkiplay.trade.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {

    @Query("SELECT * FROM positions ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE status = 'OPEN' ORDER BY openedAt DESC")
    fun observeOpen(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE id = :id")
    suspend fun byId(id: Long): PositionEntity?

    @Insert
    suspend fun insert(position: PositionEntity): Long

    @Query("UPDATE positions SET status = 'CLOSED', closePrice = :closePrice, closedAt = :closedAt, pnl = :pnl WHERE id = :id")
    suspend fun close(id: Long, closePrice: Double, closedAt: Long, pnl: Double)

    @Query("DELETE FROM positions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE positions SET stopLoss = :stopLoss, takeProfit = :takeProfit WHERE id = :id")
    suspend fun updateLevels(id: Long, stopLoss: Double, takeProfit: Double)

    @Query("DELETE FROM positions")
    suspend fun clear()
}

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY triggerAt ASC")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY triggerAt ASC")
    suspend fun enabledAlarms(): List<AlarmEntity>

    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET triggerAt = :triggerAt WHERE id = :id")
    suspend fun reschedule(id: Long, triggerAt: Long)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<JournalEntity>>

    @Insert
    suspend fun insert(entry: JournalEntity): Long

    @Query("DELETE FROM journal WHERE id = :id")
    suspend fun delete(id: Long)
}
