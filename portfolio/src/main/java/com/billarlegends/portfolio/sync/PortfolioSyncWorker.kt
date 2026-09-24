package com.billarlegends.portfolio.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.billarlegends.portfolio.PortfolioApp
import kotlinx.coroutines.flow.first

/**
 * Sincroniza todas las cuentas de Binance y conexiones de Exness en segundo plano.
 * No hace nada con los negocios manuales: esos no tienen nada que sincronizar.
 *
 * Se ejecuta de forma periódica (ver [PortfolioSyncScheduler]) para que el resumen y el
 * historial se mantengan al día sin que el usuario tenga que abrir la app y sincronizar
 * cuenta por cuenta a mano. Android puede retrasar o agrupar esta ejecución (Doze,
 * optimización de batería, restricciones de red): no es un push en tiempo real, es "lo
 * más automático que permite background execution en Android sin infraestructura propia".
 */
class PortfolioSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PortfolioApp

        val binanceAccounts = app.binanceRepository.observeAccounts().first()
        binanceAccounts.forEach { account -> app.binanceRepository.sync(account.id) }

        val exnessConnections = app.exnessRepository.observeConnections().first()
        exnessConnections.forEach { connection -> app.exnessRepository.sync(connection.id) }

        // Si una cuenta puntual falla (ej. el puente MT5 apagado), no se reintenta todo el
        // trabajo: cada sync() ya guarda su propio error y las demás cuentas siguen
        // actualizándose con normalidad.
        return Result.success()
    }
}
