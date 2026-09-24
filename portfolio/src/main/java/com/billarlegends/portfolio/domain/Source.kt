package com.billarlegends.portfolio.domain

/** Origen de una cuenta o de un movimiento dentro del portafolio consolidado. */
enum class Source {
    BINANCE,
    EXNESS,
    MANUAL,
}

/** Tipo de movimiento dentro del historial unificado. */
enum class MovementType {
    DEPOSITO,
    RETIRO,
    TRADE,
    INGRESO,
    GASTO,
}
