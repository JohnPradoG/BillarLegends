package com.billarlegends.portfolio.ui

import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.Source
import java.text.SimpleDateFormat
import java.util.Locale

fun formatUsd(value: Double): String {
    val sign = if (value < 0) "-" else ""
    return "$sign$${"%,.2f".format(Math.abs(value))}"
}

fun formatAmount(value: Double): String = "%,.6f".format(value).trimEnd('0').trimEnd('.')

fun formatDate(timestampMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES")).format(timestampMillis)

fun Source.displayName(): String = when (this) {
    Source.BINANCE -> "Binance"
    Source.EXNESS -> "Exness"
    Source.MANUAL -> "Negocio"
}

fun MovementType.displayName(): String = when (this) {
    MovementType.DEPOSITO -> "Depósito"
    MovementType.RETIRO -> "Retiro"
    MovementType.TRADE -> "Operación"
    MovementType.INGRESO -> "Ingreso"
    MovementType.GASTO -> "Gasto"
}
