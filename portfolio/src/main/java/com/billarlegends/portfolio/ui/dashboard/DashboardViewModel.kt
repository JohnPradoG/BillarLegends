package com.billarlegends.portfolio.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billarlegends.portfolio.data.repository.PortfolioRepository
import com.billarlegends.portfolio.domain.LabeledMovement
import com.billarlegends.portfolio.domain.PortfolioSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(portfolioRepository: PortfolioRepository) : ViewModel() {

    val summary: StateFlow<PortfolioSummary> = portfolioRepository.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioSummary(emptyList()))

    val recentMovements: StateFlow<List<LabeledMovement>> = portfolioRepository.observeMovementFeed()
        .map { it.take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
