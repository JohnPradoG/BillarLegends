package com.billarlegends.portfolio.data.repository

import com.billarlegends.portfolio.data.local.dao.BusinessDao
import com.billarlegends.portfolio.data.local.dao.MovementDao
import com.billarlegends.portfolio.data.local.entity.BusinessEntity
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.Source
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Negocios no digitales (efectivo, local físico, etc.) que el usuario registra a mano.
 * No hay ninguna sincronización automática: todo lo ingresa el propio usuario.
 */
class BusinessRepository(
    private val businessDao: BusinessDao,
    private val movementDao: MovementDao,
) {
    fun observeBusinesses(): Flow<List<BusinessEntity>> = businessDao.observeAll()

    fun observeMovements(businessId: Long): Flow<List<MovementEntity>> =
        movementDao.observeForOwner(Source.MANUAL, businessId)

    suspend fun addBusiness(name: String, category: String?): Long =
        businessDao.insert(
            BusinessEntity(name = name, category = category, createdAtMillis = System.currentTimeMillis()),
        )

    suspend fun deleteBusiness(business: BusinessEntity) {
        movementDao.deleteForOwner(Source.MANUAL, business.id)
        businessDao.delete(business)
    }

    /**
     * Registra un movimiento manual.
     * - DEPOSITO/RETIRO: capital que el dueño aporta o retira del negocio (cuenta como "invertido").
     * - INGRESO/GASTO: operación del día a día del negocio (cuenta para la ganancia operativa).
     */
    suspend fun addMovement(
        businessId: Long,
        type: MovementType,
        amount: Double,
        currency: String,
        timestampMillis: Long,
        note: String?,
    ) {
        movementDao.insertAll(
            listOf(
                MovementEntity(
                    source = Source.MANUAL,
                    ownerId = businessId,
                    type = type,
                    timestampMillis = timestampMillis,
                    amount = amount,
                    assetOrCurrency = currency,
                    amountUsd = amount, // Los negocios manuales se registran directo en la moneda de referencia del usuario.
                    note = note,
                    externalId = UUID.randomUUID().toString(),
                ),
            ),
        )
    }

    suspend fun deleteMovement(movement: MovementEntity) = movementDao.delete(movement)
}
