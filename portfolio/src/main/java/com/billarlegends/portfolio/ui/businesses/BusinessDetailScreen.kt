package com.billarlegends.portfolio.ui.businesses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.billarlegends.portfolio.data.local.entity.MovementEntity
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.ui.displayName
import com.billarlegends.portfolio.ui.formatDate
import com.billarlegends.portfolio.ui.formatUsd

private val MANUAL_TYPES = listOf(MovementType.INGRESO, MovementType.GASTO, MovementType.DEPOSITO, MovementType.RETIRO)

@Composable
fun BusinessDetailScreen(businessName: String, viewModel: BusinessDetailViewModel) {
    val movements by viewModel.movements.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val invested = movements.filter { it.type == MovementType.DEPOSITO }.sumOf { it.amountUsd } -
        movements.filter { it.type == MovementType.RETIRO }.sumOf { it.amountUsd }
    val profit = movements.filter { it.type == MovementType.INGRESO }.sumOf { it.amountUsd } -
        movements.filter { it.type == MovementType.GASTO }.sumOf { it.amountUsd }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Agregar movimiento") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(businessName, style = MaterialTheme.typography.titleMedium)
                        Text("Capital invertido: ${formatUsd(invested)}")
                        Text(
                            "Ganancia operativa: ${formatUsd(profit)}",
                            color = if (profit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        )
                    }
                }
            }

            if (movements.isEmpty()) {
                item { Text("Sin movimientos todavía.") }
            } else {
                items(movements) { movement ->
                    MovementRow(movement, onDelete = { viewModel.deleteMovement(movement) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddMovementDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, amount, currency, note ->
                viewModel.addMovement(type, amount, currency, note)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun MovementRow(movement: MovementEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${movement.type.displayName()} · ${formatUsd(movement.amountUsd)}", style = MaterialTheme.typography.titleSmall)
                Text(formatDate(movement.timestampMillis), style = MaterialTheme.typography.bodySmall)
                movement.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") }
        }
    }
}

@Composable
private fun AddMovementDialog(
    onDismiss: () -> Unit,
    onConfirm: (MovementType, Double, String, String?) -> Unit,
) {
    var type by remember { mutableStateOf(MovementType.INGRESO) }
    var expanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = type.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MANUAL_TYPES.forEach { option ->
                            DropdownMenuItem(text = { Text(option.displayName()) }, onClick = { type = option; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Moneda (ej. USD)") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota (opcional)") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(type, amount, currency.ifBlank { "USD" }, note.ifBlank { null })
                },
                enabled = amountText.toDoubleOrNull() != null,
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
