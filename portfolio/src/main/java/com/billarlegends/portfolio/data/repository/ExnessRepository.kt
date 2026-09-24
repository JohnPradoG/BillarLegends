package com.billarlegends.portfolio.data.repository

import com.billarlegends.portfolio.data.local.dao.ExnessConnectionDao
import com.billarlegends.portfolio.data.local.dao.MovementDao
import com.billarlegends.portfolio.data.local.entity.ExnessConnectionEntity
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.data.remote.exness.ExnessBridgeApi
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.Source
import com.billarlegends.portfolio.security.SecureCredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ExnessRepository(
    private val connectionDao: ExnessConnectionDao,
    private val movementDao: MovementDao,
    private val credentialStore: SecureCredentialStore,
) {
    fun observeConnections(): Flow<List<ExnessConnectionEntity>> = connectionDao.observeAll()

    fun observeMovements(connectionId: Long): Flow<List<MovementEntity>> =
        movementDao.observeForOwner(Source.EXNESS, connectionId)

    suspend fun addConnection(label: String, baseUrl: String, token: String): Result<Long> {
        return try {
            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val api = buildApi(normalizedUrl)
            // Prueba de conexión antes de guardar nada.
            api.getAccount("Bearer $token")

            val id = connectionDao.insert(
                ExnessConnectionEntity(
                    label = label,
                    baseUrl = normalizedUrl,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            credentialStore.saveExnessToken(id, token)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeConnection(connection: ExnessConnectionEntity) {
        movementDao.deleteForOwner(Source.EXNESS, connection.id)
        credentialStore.deleteExnessToken(connection.id)
        connectionDao.delete(connection)
    }

    suspend fun sync(connectionId: Long): Result<Unit> {
        val connection = connectionDao.getById(connectionId)
            ?: return Result.failure(IllegalStateException("Conexión no encontrada"))
        val token = credentialStore.getExnessToken(connectionId)
            ?: return Result.failure(IllegalStateException("Token no encontrado para esta conexión"))

        return try {
            val api = buildApi(connection.baseUrl)
            val bearer = "Bearer $token"

            val accountInfo = api.getAccount(bearer)
            val deals = api.getHistory(bearer)

            val movements = deals.map { deal ->
                when (deal.type.uppercase()) {
                    "BALANCE" -> MovementEntity(
                        source = Source.EXNESS,
                        ownerId = connectionId,
                        type = if (deal.profit >= 0) MovementType.DEPOSITO else MovementType.RETIRO,
                        timestampMillis = deal.timeMillis,
                        amount = kotlin.math.abs(deal.profit),
                        assetOrCurrency = accountInfo.currency,
                        amountUsd = kotlin.math.abs(deal.profit),
                        note = deal.comment ?: "Movimiento de balance",
                        externalId = "deal_${deal.ticket}",
                    )
                    else -> MovementEntity(
                        source = Source.EXNESS,
                        ownerId = connectionId,
                        type = MovementType.TRADE,
                        timestampMillis = deal.timeMillis,
                        amount = deal.volume,
                        assetOrCurrency = deal.symbol ?: "",
                        amountUsd = deal.profit,
                        note = "${deal.type} ${deal.symbol ?: ""}".trim(),
                        externalId = "deal_${deal.ticket}",
                    )
                }
            }

            if (movements.isNotEmpty()) movementDao.insertAll(movements)

            val allMovements = movementDao.getForOwner(Source.EXNESS, connectionId)
            val netInvested = allMovements
                .filter { it.type == MovementType.DEPOSITO }.sumOf { it.amountUsd } -
                allMovements.filter { it.type == MovementType.RETIRO }.sumOf { it.amountUsd }

            connectionDao.update(
                connection.copy(
                    lastSyncAtMillis = System.currentTimeMillis(),
                    lastSyncError = null,
                    lastValueUsd = accountInfo.equity,
                    lastNetInvestedUsd = netInvested,
                ),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            connectionDao.update(connection.copy(lastSyncError = e.message ?: "Error desconocido"))
            Result.failure(e)
        }
    }

    private fun buildApi(baseUrl: String): ExnessBridgeApi {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ExnessBridgeApi::class.java)
    }
}
