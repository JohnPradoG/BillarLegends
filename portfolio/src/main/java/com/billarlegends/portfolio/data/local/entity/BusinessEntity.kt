package com.billarlegends.portfolio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Un negocio no digital que el usuario lleva a mano (efectivo, local físico, etc.). */
@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String? = null,
    val createdAtMillis: Long,
)
