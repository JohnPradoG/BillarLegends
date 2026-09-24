package com.billarlegends.portfolio.data.repository

import com.billarlegends.portfolio.data.local.dao.BinanceAccountDao
import com.billarlegends.portfolio.data.local.dao.MovementDao
import com.billarlegends.portfolio.data.local.entity.BinanceAccountEntity
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.data.remote.binance.BinanceApi
import com.billarlegends.portfolio.data.remote.binance.BinanceSigningInterceptor
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.Source
import com.billarlegends.portfolio.security.SecureCredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private val STABLECOINS = setOf("USDT", "USDC", "BUSD", "FDUSD", "DAI", "TUSD", "USDP")

class BinanceRepository(
    private val accountDao: BinanceAccountDao,
    private val movementDao: MovementDao,
    private val credentialStore: SecureCredentialStore,
) {
    fun observeAccounts(): Flow<List<BinanceAccountEntity>> = accountDao.observeAll()

    fun observeMovements(accountId: Long): Flow<List<MovementEntity>> =
        movementDao.observeForOwner(Source.BINANCE, accountId)

    /**
     * Agrega una cuenta y verifica de inmediato que el API key sea de SOLO LECTURA.
     * Si la key tiene permiso de retiro, se rechaza para proteger al usuario.
     */
    suspend fun addAccount(label: String, apiKey: String, apiSecret: String): Result<Long> {
        return try {
            val api = buildApi(apiKey, apiSecret)
            val restrictions = api.getApiRestrictions()
            if (restrictions.enableWithdrawals) {
                return Result.failure(
                    IllegalArgumentException(
                        "Esta API key tiene permiso de RETIRO habilitado. Por seguridad, crea una " +
                            "key nueva en Binance solo con permiso de lectura (Enable Reading) y sin " +
                            "'Enable Withdrawals' ni 'Enable Spot & Margin Trading'.",
                    ),
                )
            }
            val id = accountDao.insert(
                BinanceAccountEntity(
                    label = label,
                    createdAtMillis = System.currentTimeMillis(),
                    isReadOnlyVerified = true,
                ),
            )
            credentialStore.saveBinanceCredentials(id, apiKey, apiSecret)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAccount(account: BinanceAccountEntity) {
        movementDao.deleteForOwner(Source.BINANCE, account.id)
        credentialStore.deleteBinanceCredentials(account.id)
        accountDao.delete(account)
    }

    /** Descarga balances, precios, depósitos, retiros y trades, y actualiza el resumen local. */
    suspend fun sync(accountId: Long): Result<Unit> {
        val account = accountDao.getById(accountId) ?: return Result.failure(IllegalStateException("Cuenta no encontrada"))
        val apiKey = credentialStore.getBinanceApiKey(accountId)
        val apiSecret = credentialStore.getBinanceApiSecret(accountId)
        if (apiKey == null || apiSecret == null) {
            return Result.failure(IllegalStateException("Credenciales no encontradas para esta cuenta"))
        }

        return try {
            val api = buildApi(apiKey, apiSecret)

            val accountInfo = api.getAccount()
            val prices = api.getAllPrices().associate { it.symbol to it.price.toDouble() }

            val balances = accountInfo.balances
                .map { it.asset to (it.free.toDouble() + it.locked.toDouble()) }
                .filter { it.second > 0.0 }

            var totalValueUsd = 0.0
            for ((asset, amount) in balances) {
                totalValueUsd += valueInUsd(asset, amount, prices)
            }

            val deposits = runCatching { api.getDepositHistory() }.getOrDefault(emptyList())
            val withdrawals = runCatching { api.getWithdrawHistory() }.getOrDefault(emptyList())

            val movements = mutableListOf<MovementEntity>()

            deposits.filter { it.status == 1 }.forEach { d ->
                movements += MovementEntity(
                    source = Source.BINANCE,
                    ownerId = accountId,
                    type = MovementType.DEPOSITO,
                    timestampMillis = d.insertTime,
                    amount = d.amount.toDoubleOrNull() ?: 0.0,
                    assetOrCurrency = d.coin,
                    amountUsd = valueInUsd(d.coin, d.amount.toDoubleOrNull() ?: 0.0, prices),
                    note = "Depósito${if (d.network.isNotBlank()) " (${d.network})" else ""}",
                    externalId = "deposit_${d.txId ?: d.id}_${d.insertTime}",
                )
            }

            withdrawals.filter { it.status == 6 }.forEach { w ->
                movements += MovementEntity(
                    source = Source.BINANCE,
                    ownerId = accountId,
                    type = MovementType.RETIRO,
                    timestampMillis = parseBinanceDate(w.applyTime),
                    amount = w.amount.toDoubleOrNull() ?: 0.0,
                    assetOrCurrency = w.coin,
                    amountUsd = valueInUsd(w.coin, w.amount.toDoubleOrNull() ?: 0.0, prices),
                    note = "Retiro${if (w.network.isNotBlank()) " (${w.network})" else ""}",
                    externalId = "withdraw_${w.id}",
                )
            }

            // Historial de trades: mejor esfuerzo, solo para activos que se tienen actualmente
            // contra su par en USDT. No cubre posiciones ya liquidadas por completo.
            balances.map { it.first }.filterNot { it in STABLECOINS }.forEach { asset ->
                val symbol = "${asset}USDT"
                if (prices.containsKey(symbol)) {
                    val trades = runCatching { api.getMyTrades(symbol) }.getOrDefault(emptyList())
                    trades.forEach { t ->
                        movements += MovementEntity(
                            source = Source.BINANCE,
                            ownerId = accountId,
                            type = MovementType.TRADE,
                            timestampMillis = t.time,
                            amount = t.qty.toDoubleOrNull() ?: 0.0,
                            assetOrCurrency = t.symbol,
                            amountUsd = t.quoteQty.toDoubleOrNull() ?: 0.0,
                            note = if (t.isBuyer) "Compra ${t.symbol}" else "Venta ${t.symbol}",
                            externalId = "trade_${t.symbol}_${t.id}",
                        )
                    }
                }
            }

            if (movements.isNotEmpty()) movementDao.insertAll(movements)

            // Se recalcula sobre TODO el historial guardado (no solo lo nuevo de esta
            // sincronización) para que el acumulado sea correcto.
            val allMovements = movementDao.getForOwner(Source.BINANCE, accountId)
            val totalNetInvested = allMovements
                .filter { it.type == MovementType.DEPOSITO }.sumOf { it.amountUsd } -
                allMovements.filter { it.type == MovementType.RETIRO }.sumOf { it.amountUsd }

            accountDao.update(
                account.copy(
                    lastSyncAtMillis = System.currentTimeMillis(),
                    lastSyncError = null,
                    lastValueUsd = totalValueUsd,
                    lastNetInvestedUsd = totalNetInvested,
                ),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            accountDao.update(account.copy(lastSyncError = e.message ?: "Error desconocido"))
            Result.failure(e)
        }
    }

    private fun valueInUsd(asset: String, amount: Double, prices: Map<String, Double>): Double {
        if (asset in STABLECOINS) return amount
        val direct = prices["${asset}USDT"]
        if (direct != null) return amount * direct
        // Puente vía BTC para activos sin par directo a USDT.
        val viaBtc = prices["${asset}BTC"]
        val btcUsdt = prices["BTCUSDT"]
        if (viaBtc != null && btcUsdt != null) return amount * viaBtc * btcUsdt
        return 0.0
    }

    private fun parseBinanceDate(value: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(value)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun buildApi(apiKey: String, apiSecret: String): BinanceApi {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .addInterceptor(BinanceSigningInterceptor(apiKey, apiSecret))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BinanceApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BinanceApi::class.java)
    }
}
