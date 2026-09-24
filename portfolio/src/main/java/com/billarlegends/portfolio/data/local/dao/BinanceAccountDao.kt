package com.billarlegends.portfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.billarlegends.portfolio.data.local.entity.BinanceAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BinanceAccountDao {
    @Query("SELECT * FROM binance_accounts ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<BinanceAccountEntity>>

    @Query("SELECT * FROM binance_accounts WHERE id = :id")
    suspend fun getById(id: Long): BinanceAccountEntity?

    @Insert
    suspend fun insert(account: BinanceAccountEntity): Long

    @Update
    suspend fun update(account: BinanceAccountEntity)

    @Delete
    suspend fun delete(account: BinanceAccountEntity)
}
