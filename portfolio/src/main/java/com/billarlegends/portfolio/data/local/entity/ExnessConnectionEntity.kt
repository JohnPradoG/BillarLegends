package com.billarlegends.portfolio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Conexión a un puente local de MetaTrader 5 (ver módulo mt5-bridge/) para una cuenta Exness.
 * El token de autenticación del puente se guarda cifrado, referenciado por [id].
 */
@Entity(tableName = "exness_connections")
data class ExnessConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val baseUrl: String,
    val createdAtMillis: Long,
    val lastSyncAtMillis: Long? = null,
    val lastSyncError: String? = null,
    val lastValueUsd: Double = 0.0,
    val lastNetInvestedUsd: Double = 0.0,
)
