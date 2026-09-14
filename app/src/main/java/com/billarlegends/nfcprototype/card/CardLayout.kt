package com.billarlegends.nfcprototype.card

/**
 * CAPA DE TARJETA — mapa de sectores para nuestra MIFARE Classic 1K.
 *
 * MIFARE Classic 1K tiene 16 sectores (0-15), 4 bloques cada uno
 * (3 de datos + 1 "trailer" con claves/permisos). El sector 0 es de
 * fábrica (UID en el bloque 0) y esta app NUNCA escribe en él.
 *
 * Esta asignación es la propuesta inicial (fase de diseño, sin
 * escribir todavía). Sectores 5-15 quedan libres para funciones
 * futuras y no se tocan en las fases actuales.
 */
object CardLayout {

    const val SECTOR_MANUFACTURER = 0 // Solo lectura. Nunca se escribe.

    const val SECTOR_IDENTITY = 1   // card_id + versión de esquema
    const val SECTOR_BALANCE = 2    // saldo experimental + timestamp
    const val SECTOR_COUNTER = 3    // contador monotónico + último transaction_id corto
    const val SECTOR_EXTRA = 4      // reservado para datos adicionales (tarifas, estado)

    val PROJECT_SECTORS = listOf(SECTOR_IDENTITY, SECTOR_BALANCE, SECTOR_COUNTER, SECTOR_EXTRA)

    val RESERVED_FOR_FUTURE = (5..15).toList()

    fun labelFor(sector: Int): String = when (sector) {
        SECTOR_MANUFACTURER -> "Fabricante (UID) — solo lectura"
        SECTOR_IDENTITY -> "Identidad de la tarjeta (card_id)"
        SECTOR_BALANCE -> "Saldo experimental"
        SECTOR_COUNTER -> "Contador de transacciones"
        SECTOR_EXTRA -> "Datos adicionales"
        in RESERVED_FOR_FUTURE -> "Reservado para uso futuro"
        else -> "Sector desconocido"
    }
}
