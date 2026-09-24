package com.billarlegends.portfolio.ui.businesses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.data.repository.BusinessRepository
import com.billarlegends.portfolio.domain.MovementType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusinessDetailViewModel(
    private val businessRepository: BusinessRepository,
    private val businessId: Long,
) : ViewModel() {

    val movements: StateFlow<List<MovementEntity>> = businessRepository.observeMovements(businessId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMovement(type: MovementType, amount: Double, currency: String, note: String?) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            businessRepository.addMovement(
                businessId = businessId,
                type = type,
                amount = amount,
                currency = currency,
                timestampMillis = System.currentTimeMillis(),
                note = note?.trim()?.ifBlank { null },
            )
        }
    }

    fun deleteMovement(movement: MovementEntity) {
        viewModelScope.launch { businessRepository.deleteMovement(movement) }
    }
}
