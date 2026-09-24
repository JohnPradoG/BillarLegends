package com.billarlegends.portfolio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadatos de una cuenta de Binance conectada en modo solo lectura.
 * El API key y el API secret NUNCA se guardan aquí: viven cifrados en
 * [com.billarlegends.portfolio.security.SecureCredentialStore], referenciados por [id].
 */
@Entity(tableName = "binance_accounts")
data class BinanceAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val createdAtMillis: Long,
    val lastSyncAtMillis: Long? = null,
    val lastSyncError: String? = null,
    val isReadOnlyVerified: Boolean = false,
    val lastValueUsd: Double = 0.0,
    val lastNetInvestedUsd: Double = 0.0,
)
