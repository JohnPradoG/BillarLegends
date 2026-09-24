package com.billarlegends.portfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.domain.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE source = :source AND ownerId = :ownerId ORDER BY timestampMillis DESC")
    fun observeForOwner(source: Source, ownerId: Long): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE source = :source AND ownerId = :ownerId ORDER BY timestampMillis DESC")
    suspend fun getForOwner(source: Source, ownerId: Long): List<MovementEntity>

    /** Ignora duplicados (mismo source+ownerId+externalId) para que re-sincronizar sea seguro. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(movements: List<MovementEntity>): List<Long>

    @Delete
    suspend fun delete(movement: MovementEntity)

    @Query("DELETE FROM movements WHERE source = :source AND ownerId = :ownerId")
    suspend fun deleteForOwner(source: Source, ownerId: Long)
}
