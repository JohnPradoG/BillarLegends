package com.billarlegends.portfolio.ui.businesses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billarlegends.portfolio.data.local.entity.BusinessEntity
import com.billarlegends.portfolio.data.repository.BusinessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusinessesViewModel(private val businessRepository: BusinessRepository) : ViewModel() {

    val businesses: StateFlow<List<BusinessEntity>> = businessRepository.observeBusinesses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBusiness(name: String, category: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { businessRepository.addBusiness(name.trim(), category?.trim()?.ifBlank { null }) }
    }

    fun deleteBusiness(business: BusinessEntity) {
        viewModelScope.launch { businessRepository.deleteBusiness(business) }
    }
}
