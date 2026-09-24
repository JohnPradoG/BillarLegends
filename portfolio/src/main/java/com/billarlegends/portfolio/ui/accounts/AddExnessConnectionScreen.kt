package com.billarlegends.portfolio.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AddExnessConnectionScreen(viewModel: AccountsViewModel, onDone: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Antes de conectar", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Necesitas el puente MT5 corriendo (carpeta mt5-bridge/ del repo, ver su README). " +
                        "Ingresa la URL donde quedó publicado y el mismo BRIDGE_TOKEN que configuraste ahí.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Nombre de la cuenta (ej. Exness principal)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL del puente (ej. http://192.168.1.50:8765/)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token (BRIDGE_TOKEN)") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
        )

        uiState.errorMessage?.let { Text(it, color = Color(0xFFC62828)) }

        if (uiState.isAddingAccount) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.addExnessConnection(label.trim(), baseUrl.trim(), token.trim()) { success ->
                        if (success) onDone()
                    }
                },
                enabled = label.isNotBlank() && baseUrl.isNotBlank() && token.isNotBlank(),
            ) { Text("Conectar") }
        }
    }
}
