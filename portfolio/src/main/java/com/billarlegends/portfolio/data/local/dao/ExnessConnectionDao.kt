package com.billarlegends.portfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.billarlegends.portfolio.data.local.entity.ExnessConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExnessConnectionDao {
    @Query("SELECT * FROM exness_connections ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<ExnessConnectionEntity>>

    @Query("SELECT * FROM exness_connections WHERE id = :id")
    suspend fun getById(id: Long): ExnessConnectionEntity?

    @Insert
    suspend fun insert(connection: ExnessConnectionEntity): Long

    @Update
    suspend fun update(connection: ExnessConnectionEntity)

    @Delete
    suspend fun delete(connection: ExnessConnectionEntity)
}
