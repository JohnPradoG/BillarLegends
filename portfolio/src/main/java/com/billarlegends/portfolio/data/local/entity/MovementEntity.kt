package com.billarlegends.portfolio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.Source

/**
 * Historial unificado de movimientos de todas las fuentes (Binance, Exness, negocios manuales).
 *
 * Semántica común usada para calcular invertido/ganancia en cualquier fuente:
 * - DEPOSITO / RETIRO: entra o sale capital del "bote" (cuenta de trading o caja del negocio).
 * - INGRESO / GASTO: solo aplica a negocios manuales (ventas y gastos operativos).
 * - TRADE: operación informativa (Binance), no se usa para el cálculo de invertido/ganancia
 *   porque su efecto ya está reflejado en el valor de la cuenta.
 *
 * [externalId] permite deduplicar al re-sincronizar (id de depósito/retiro/orden de Binance,
 * ticket de MT5, o el id local de un registro manual).
 */
@Entity(
    tableName = "movements",
    indices = [Index(value = ["source", "ownerId", "externalId"], unique = true)],
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: Source,
    /** id de BinanceAccountEntity, ExnessConnectionEntity o BusinessEntity según [source]. */
    val ownerId: Long,
    val type: MovementType,
    val timestampMillis: Long,
    val amount: Double,
    val assetOrCurrency: String,
    val amountUsd: Double,
    val note: String? = null,
    val externalId: String,
)
