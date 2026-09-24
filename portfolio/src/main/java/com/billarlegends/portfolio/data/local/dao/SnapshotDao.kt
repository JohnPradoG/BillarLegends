package com.billarlegends.portfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.billarlegends.portfolio.data.local.entity.SnapshotEntity
import com.billarlegends.portfolio.domain.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE source = :source AND ownerId = :ownerId ORDER BY timestampMillis ASC")
    fun observeForOwner(source: Source, ownerId: Long): Flow<List<SnapshotEntity>>

    @Insert
    suspend fun insert(snapshot: SnapshotEntity): Long
}
