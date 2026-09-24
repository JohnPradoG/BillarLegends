package com.billarlegends.portfolio.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billarlegends.portfolio.data.local.entity.BinanceAccountEntity
import com.billarlegends.portfolio.data.local.entity.ExnessConnectionEntity
import com.billarlegends.portfolio.data.repository.BinanceRepository
import com.billarlegends.portfolio.data.repository.ExnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val syncingIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val isAddingAccount: Boolean = false,
)

class AccountsViewModel(
    private val binanceRepository: BinanceRepository,
    private val exnessRepository: ExnessRepository,
) : ViewModel() {

    val binanceAccounts: StateFlow<List<BinanceAccountEntity>> = binanceRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exnessConnections: StateFlow<List<ExnessConnectionEntity>> = exnessRepository.observeConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    fun addBinanceAccount(label: String, apiKey: String, apiSecret: String, onDone: (Boolean) -> Unit) {
        _uiState.value = _uiState.value.copy(isAddingAccount = true, errorMessage = null)
        viewModelScope.launch {
            val result = binanceRepository.addAccount(label, apiKey, apiSecret)
            _uiState.value = _uiState.value.copy(
                isAddingAccount = false,
                errorMessage = result.exceptionOrNull()?.message,
            )
            if (result.isSuccess) syncBinance(result.getOrThrow())
            onDone(result.isSuccess)
        }
    }

    fun addExnessConnection(label: String, baseUrl: String, token: String, onDone: (Boolean) -> Unit) {
        _uiState.value = _uiState.value.copy(isAddingAccount = true, errorMessage = null)
        viewModelScope.launch {
            val result = exnessRepository.addConnection(label, baseUrl, token)
            _uiState.value = _uiState.value.copy(
                isAddingAccount = false,
                errorMessage = result.exceptionOrNull()?.message,
            )
            if (result.isSuccess) syncExness(result.getOrThrow())
            onDone(result.isSuccess)
        }
    }

    fun syncBinance(accountId: Long) = withSyncing(accountId) { binanceRepository.sync(accountId) }

    fun syncExness(connectionId: Long) = withSyncing(connectionId) { exnessRepository.sync(connectionId) }

    fun syncAll() {
        binanceAccounts.value.forEach { syncBinance(it.id) }
        exnessConnections.value.forEach { syncExness(it.id) }
    }

    fun removeBinanceAccount(account: BinanceAccountEntity) {
        viewModelScope.launch { binanceRepository.removeAccount(account) }
    }

    fun removeExnessConnection(connection: ExnessConnectionEntity) {
        viewModelScope.launch { exnessRepository.removeConnection(connection) }
    }

    private fun withSyncing(id: Long, block: suspend () -> Result<Unit>) {
        _uiState.value = _uiState.value.copy(syncingIds = _uiState.value.syncingIds + id)
        viewModelScope.launch {
            val result = block()
            _uiState.value = _uiState.value.copy(
                syncingIds = _uiState.value.syncingIds - id,
                errorMessage = result.exceptionOrNull()?.message ?: _uiState.value.errorMessage,
            )
        }
    }
}
