package com.billarlegends.portfolio

import android.app.Application
import com.billarlegends.portfolio.data.local.AppDatabase
import com.billarlegends.portfolio.data.repository.BinanceRepository
import com.billarlegends.portfolio.data.repository.BusinessRepository
import com.billarlegends.portfolio.data.repository.ExnessRepository
import com.billarlegends.portfolio.data.repository.PortfolioRepository
import com.billarlegends.portfolio.security.SecureCredentialStore

/**
 * Contenedor de dependencias simple (sin librería de DI) para un módulo de este tamaño.
 * Todo vive en memoria mientras el proceso está vivo; Room y EncryptedSharedPreferences
 * son quienes realmente persisten los datos.
 */
class PortfolioApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var credentialStore: SecureCredentialStore
        private set
    lateinit var binanceRepository: BinanceRepository
        private set
    lateinit var exnessRepository: ExnessRepository
        private set
    lateinit var businessRepository: BusinessRepository
        private set
    lateinit var portfolioRepository: PortfolioRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        credentialStore = SecureCredentialStore(this)

        binanceRepository = BinanceRepository(
            accountDao = database.binanceAccountDao(),
            movementDao = database.movementDao(),
            credentialStore = credentialStore,
        )
        exnessRepository = ExnessRepository(
            connectionDao = database.exnessConnectionDao(),
            movementDao = database.movementDao(),
            credentialStore = credentialStore,
        )
        businessRepository = BusinessRepository(
            businessDao = database.businessDao(),
            movementDao = database.movementDao(),
        )
        portfolioRepository = PortfolioRepository(
            binanceAccountDao = database.binanceAccountDao(),
            exnessConnectionDao = database.exnessConnectionDao(),
            businessDao = database.businessDao(),
            movementDao = database.movementDao(),
        )
    }
}
