package com.billarlegends.portfolio.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PortfolioSyncScheduler {

    private const val PERIODIC_WORK_NAME = "portfolio_periodic_sync"
    private const val ONE_TIME_WORK_NAME = "portfolio_sync_now"
    private val SYNC_INTERVAL = 30L to TimeUnit.MINUTES

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Programa la sincronización periódica en segundo plano. Idempotente: seguro de llamar en cada arranque de la app. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<PortfolioSyncWorker>(SYNC_INTERVAL.first, SYNC_INTERVAL.second)
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Dispara una sincronización inmediata (ej. al abrir la app), sin esperar al ciclo periódico. */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PortfolioSyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
