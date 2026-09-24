package com.billarlegends.portfolio.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.billarlegends.portfolio.data.local.entity.BinanceAccountEntity
import com.billarlegends.portfolio.data.local.entity.ExnessConnectionEntity
import com.billarlegends.portfolio.ui.formatDate
import com.billarlegends.portfolio.ui.formatUsd

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onAddBinance: () -> Unit,
    onAddExness: () -> Unit,
) {
    val binanceAccounts by viewModel.binanceAccounts.collectAsState()
    val exnessConnections by viewModel.exnessConnections.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddChoice by remember { mutableStateOf(false) }
    var accountPendingDelete by remember { mutableStateOf<BinanceAccountEntity?>(null) }
    var connectionPendingDelete by remember { mutableStateOf<ExnessConnectionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddChoice = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Agregar cuenta") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Binance", style = MaterialTheme.typography.titleMedium) }
            if (binanceAccounts.isEmpty()) {
                item { Text("Sin cuentas de Binance conectadas.") }
            } else {
                items(binanceAccounts) { account ->
                    BinanceAccountCard(
                        account = account,
                        isSyncing = account.id in uiState.syncingIds,
                        onSync = { viewModel.syncBinance(account.id) },
                        onDelete = { accountPendingDelete = account },
                    )
                }
            }

            item { Text("Exness (vía puente MT5)", style = MaterialTheme.typography.titleMedium) }
            if (exnessConnections.isEmpty()) {
                item { Text("Sin conexiones de Exness configuradas.") }
            } else {
                items(exnessConnections) { connection ->
                    ExnessConnectionCard(
                        connection = connection,
                        isSyncing = connection.id in uiState.syncingIds,
                        onSync = { viewModel.syncExness(connection.id) },
                        onDelete = { connectionPendingDelete = connection },
                    )
                }
            }

            uiState.errorMessage?.let { error ->
                item { Text(error, color = Color(0xFFC62828)) }
            }
        }
    }

    if (showAddChoice) {
        AlertDialog(
            onDismissRequest = { showAddChoice = false },
            title = { Text("¿Qué cuenta quieres agregar?") },
            text = { Text("Binance se conecta directo por API. Exness requiere el puente MT5 (ver README).") },
            confirmButton = {
                TextButton(onClick = { showAddChoice = false; onAddBinance() }) { Text("Binance") }
            },
            dismissButton = {
                TextButton(onClick = { showAddChoice = false; onAddExness() }) { Text("Exness") }
            },
        )
    }

    accountPendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountPendingDelete = null },
            title = { Text("¿Eliminar ${account.label}?") },
            text = { Text("Se borrará su historial de movimientos guardado en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeBinanceAccount(account)
                    accountPendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { accountPendingDelete = null }) { Text("Cancelar") } },
        )
    }

    connectionPendingDelete?.let { connection ->
        AlertDialog(
            onDismissRequest = { connectionPendingDelete = null },
            title = { Text("¿Eliminar ${connection.label}?") },
            text = { Text("Se borrará su historial de movimientos guardado en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeExnessConnection(connection)
                    connectionPendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { connectionPendingDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun BinanceAccountCard(
    account: BinanceAccountEntity,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(account.label, style = MaterialTheme.typography.titleSmall)
                Row {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        IconButton(onClick = onSync) { Icon(Icons.Default.Refresh, "Sincronizar") }
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") }
                }
            }
            Text("Valor: ${formatUsd(account.lastValueUsd)}  ·  Invertido: ${formatUsd(account.lastNetInvestedUsd)}")
            account.lastSyncAtMillis?.let { Text("Última sincronización: ${formatDate(it)}", style = MaterialTheme.typography.bodySmall) }
            account.lastSyncError?.let { Text("Error: $it", color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ExnessConnectionCard(
    connection: ExnessConnectionEntity,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(connection.label, style = MaterialTheme.typography.titleSmall)
                Row {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        IconButton(onClick = onSync) { Icon(Icons.Default.Refresh, "Sincronizar") }
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") }
                }
            }
            Text("Valor: ${formatUsd(connection.lastValueUsd)}  ·  Invertido: ${formatUsd(connection.lastNetInvestedUsd)}")
            connection.lastSyncAtMillis?.let { Text("Última sincronización: ${formatDate(it)}", style = MaterialTheme.typography.bodySmall) }
            connection.lastSyncError?.let { Text("Error: $it", color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
