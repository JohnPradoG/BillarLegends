package com.billarlegends.portfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.billarlegends.portfolio.data.local.dao.BinanceAccountDao
import com.billarlegends.portfolio.data.local.dao.BusinessDao
import com.billarlegends.portfolio.data.local.dao.ExnessConnectionDao
import com.billarlegends.portfolio.data.local.dao.MovementDao
import com.billarlegends.portfolio.data.local.dao.SnapshotDao
import com.billarlegends.portfolio.data.local.entity.BinanceAccountEntity
import com.billarlegends.portfolio.data.local.entity.BusinessEntity
import com.billarlegends.portfolio.data.local.entity.ExnessConnectionEntity
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.data.local.entity.SnapshotEntity

@Database(
    entities = [
        BinanceAccountEntity::class,
        ExnessConnectionEntity::class,
        BusinessEntity::class,
        MovementEntity::class,
        SnapshotEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun binanceAccountDao(): BinanceAccountDao
    abstract fun exnessConnectionDao(): ExnessConnectionDao
    abstract fun businessDao(): BusinessDao
    abstract fun movementDao(): MovementDao
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "portfolio.db",
                ).build().also { instance = it }
            }
    }
}
