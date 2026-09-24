package com.billarlegends.portfolio.domain

data class SourceSummary(
    val source: Source,
    val ownerId: Long,
    val label: String,
    val valueUsd: Double,
    val investedUsd: Double,
) {
    val profitUsd: Double get() = valueUsd - investedUsd
}

data class PortfolioSummary(
    val bySource: List<SourceSummary>,
) {
    val totalValueUsd: Double get() = bySource.sumOf { it.valueUsd }
    val totalInvestedUsd: Double get() = bySource.sumOf { it.investedUsd }
    val totalProfitUsd: Double get() = totalValueUsd - totalInvestedUsd
}

/** Movimiento del historial unificado ya con el nombre de su cuenta/negocio de origen. */
data class LabeledMovement(
    val id: Long,
    val source: Source,
    val ownerLabel: String,
    val type: MovementType,
    val timestampMillis: Long,
    val amount: Double,
    val assetOrCurrency: String,
    val amountUsd: Double,
    val note: String?,
)
