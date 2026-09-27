package com.mazkiplay.trade.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PositionEntity::class, AlarmEntity::class, JournalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MazkiplayDatabase : RoomDatabase() {

    abstract fun positionDao(): PositionDao
    abstract fun alarmDao(): AlarmDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var instance: MazkiplayDatabase? = null

        fun get(context: Context): MazkiplayDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MazkiplayDatabase::class.java,
                    "mazkiplay_trade.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
