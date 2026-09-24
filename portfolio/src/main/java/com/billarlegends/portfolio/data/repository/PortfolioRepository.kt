package com.billarlegends.portfolio.data.repository

import com.billarlegends.portfolio.data.local.dao.BinanceAccountDao
import com.billarlegends.portfolio.data.local.dao.BusinessDao
import com.billarlegends.portfolio.data.local.dao.ExnessConnectionDao
import com.billarlegends.portfolio.data.local.dao.MovementDao
import com.billarlegends.portfolio.domain.LabeledMovement
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.PortfolioSummary
import com.billarlegends.portfolio.domain.Source
import com.billarlegends.portfolio.domain.SourceSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Combina Binance + Exness + negocios manuales en un solo resumen y un solo historial. */
class PortfolioRepository(
    private val binanceAccountDao: BinanceAccountDao,
    private val exnessConnectionDao: ExnessConnectionDao,
    private val businessDao: BusinessDao,
    private val movementDao: MovementDao,
) {
    fun observeSummary(): Flow<PortfolioSummary> =
        combine(
            binanceAccountDao.observeAll(),
            exnessConnectionDao.observeAll(),
            businessDao.observeAll(),
            movementDao.observeAll(),
        ) { binanceAccounts, exnessConnections, businesses, movements ->
            val summaries = mutableListOf<SourceSummary>()

            binanceAccounts.forEach { account ->
                summaries += SourceSummary(
                    source = Source.BINANCE,
                    ownerId = account.id,
                    label = account.label,
                    valueUsd = account.lastValueUsd,
                    investedUsd = account.lastNetInvestedUsd,
                )
            }

            exnessConnections.forEach { connection ->
                summaries += SourceSummary(
                    source = Source.EXNESS,
                    ownerId = connection.id,
                    label = connection.label,
                    valueUsd = connection.lastValueUsd,
                    investedUsd = connection.lastNetInvestedUsd,
                )
            }

            businesses.forEach { business ->
                val businessMovements = movements.filter {
                    it.source == Source.MANUAL && it.ownerId == business.id
                }
                val invested = businessMovements.filter { it.type == MovementType.DEPOSITO }.sumOf { it.amountUsd } -
                    businessMovements.filter { it.type == MovementType.RETIRO }.sumOf { it.amountUsd }
                val operatingProfit = businessMovements.filter { it.type == MovementType.INGRESO }.sumOf { it.amountUsd } -
                    businessMovements.filter { it.type == MovementType.GASTO }.sumOf { it.amountUsd }

                summaries += SourceSummary(
                    source = Source.MANUAL,
                    ownerId = business.id,
                    label = business.name,
                    // "Valor" de un negocio manual = capital aportado + ganancia operativa acumulada.
                    valueUsd = invested + operatingProfit,
                    investedUsd = invested,
                )
            }

            PortfolioSummary(bySource = summaries)
        }

    fun observeMovementFeed(): Flow<List<LabeledMovement>> =
        combine(
            binanceAccountDao.observeAll(),
            exnessConnectionDao.observeAll(),
            businessDao.observeAll(),
            movementDao.observeAll(),
        ) { binanceAccounts, exnessConnections, businesses, movements ->
            val binanceLabels = binanceAccounts.associate { it.id to it.label }
            val exnessLabels = exnessConnections.associate { it.id to it.label }
            val businessLabels = businesses.associate { it.id to it.name }

            movements.mapNotNull { m ->
                val label = when (m.source) {
                    Source.BINANCE -> binanceLabels[m.ownerId]
                    Source.EXNESS -> exnessLabels[m.ownerId]
                    Source.MANUAL -> businessLabels[m.ownerId]
                } ?: return@mapNotNull null

                LabeledMovement(
                    id = m.id,
                    source = m.source,
                    ownerLabel = label,
                    type = m.type,
                    timestampMillis = m.timestampMillis,
                    amount = m.amount,
                    assetOrCurrency = m.assetOrCurrency,
                    amountUsd = m.amountUsd,
                    note = m.note,
                )
            }
        }
}
