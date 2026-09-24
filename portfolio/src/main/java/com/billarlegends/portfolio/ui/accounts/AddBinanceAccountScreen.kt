package com.billarlegends.portfolio.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AddBinanceAccountScreen(viewModel: AccountsViewModel, onDone: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Importante", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Crea en Binance una API key con SOLO el permiso 'Enable Reading'. " +
                        "NO actives 'Enable Withdrawals' ni 'Enable Spot & Margin Trading'. " +
                        "La app verifica esto automáticamente y rechaza keys con permiso de retiro.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Nombre de la cuenta (ej. Binance principal)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        OutlinedTextField(
            value = apiSecret,
            onValueChange = { apiSecret = it },
            label = { Text("API Secret") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        uiState.errorMessage?.let { Text(it, color = Color(0xFFC62828)) }

        if (uiState.isAddingAccount) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.addBinanceAccount(label.trim(), apiKey.trim(), apiSecret.trim()) { success ->
                        if (success) onDone()
                    }
                },
                enabled = label.isNotBlank() && apiKey.isNotBlank() && apiSecret.isNotBlank(),
            ) { Text("Conectar") }
        }
    }
}
