package com.billarlegends.portfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.billarlegends.portfolio.data.local.entity.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id")
    suspend fun getById(id: Long): BusinessEntity?

    @Insert
    suspend fun insert(business: BusinessEntity): Long

    @Delete
    suspend fun delete(business: BusinessEntity)
}
