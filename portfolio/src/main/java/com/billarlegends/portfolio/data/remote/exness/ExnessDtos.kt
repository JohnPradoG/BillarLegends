package com.billarlegends.portfolio.data.remote.exness

import kotlinx.serialization.Serializable

/**
 * Contrato que debe cumplir el puente local (ver mt5-bridge/) montado por el usuario sobre
 * su terminal de MetaTrader 5. Ver mt5-bridge/README.md para el detalle de cada campo.
 */
@Serializable
data class ExnessAccountResponse(
    val login: Long,
    val balance: Double,
    val equity: Double,
    val currency: String = "USD",
    val server: String = "",
)

@Serializable
data class ExnessDeal(
    val ticket: Long,
    /** Epoch millis. */
    val timeMillis: Long,
    /** "BALANCE" (depósito/retiro), "BUY" o "SELL" (operación cerrada). */
    val type: String,
    val volume: Double = 0.0,
    val symbol: String? = null,
    val profit: Double = 0.0,
    val comment: String? = null,
)
