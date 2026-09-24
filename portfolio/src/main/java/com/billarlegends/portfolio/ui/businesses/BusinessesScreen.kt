package com.billarlegends.portfolio.ui.businesses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.billarlegends.portfolio.data.local.entity.BusinessEntity

@Composable
fun BusinessesScreen(viewModel: BusinessesViewModel, onOpenBusiness: (Long) -> Unit) {
    val businesses by viewModel.businesses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Nuevo negocio") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (businesses.isEmpty()) {
                item { Text("Todavía no registras negocios. Agrega el primero con el botón de abajo.") }
            } else {
                items(businesses) { business -> BusinessRow(business, onClick = { onOpenBusiness(business.id) }) }
            }
        }
    }

    if (showAddDialog) {
        AddBusinessDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category ->
                viewModel.addBusiness(name, category)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun BusinessRow(business: BusinessEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(business.name, style = MaterialTheme.typography.titleSmall)
            business.category?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun AddBusinessDialog(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo negocio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría (opcional)") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, category.ifBlank { null }) }, enabled = name.isNotBlank()) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
