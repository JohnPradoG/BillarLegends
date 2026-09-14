package com.billarlegends.nfcprototype.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billarlegends.nfcprototype.card.CardLayout
import com.billarlegends.nfcprototype.nfc.DiagnosticResult
import com.billarlegends.nfcprototype.nfc.SectorDiagnostic

enum class NfcAvailability {
    NOT_SUPPORTED,
    DISABLED,
    READY
}

@Composable
fun DiagnosticScreen(
    availability: NfcAvailability,
    result: DiagnosticResult?
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "DIAGNÓSTICO NFC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (availability) {
                NfcAvailability.NOT_SUPPORTED -> {
                    Text("Este dispositivo no tiene NFC. El prototipo no puede funcionar aquí.")
                }
                NfcAvailability.DISABLED -> {
                    Text("NFC está desactivado. Actívalo en Ajustes y vuelve a esta pantalla.")
                }
                NfcAvailability.READY -> {
                    if (result == null) {
                        Text(
                            "Acerca una tarjeta NFC",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            "Tarjeta detectada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DiagnosticSummary(result)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (result.errorMessage != null) {
                            Text(
                                text = result.errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (result.sectors.isNotEmpty()) {
                            Text(
                                "Sectores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(result.sectors) { sector ->
                                    SectorRow(sector)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSummary(result: DiagnosticResult) {
    Column {
        LabelValue("UID", result.uidHex)
        LabelValue("ATQA", result.atqaHex)
        LabelValue("SAK", result.sakHex)
        LabelValue("Tecnologías detectadas", result.techList.joinToString(", ") { it.substringAfterLast('.') })
        LabelValue("Tipo de tarjeta", result.cardType)
        result.sizeBytes?.let { LabelValue("Tamaño", "$it bytes") }
        result.sectorCount?.let { LabelValue("Sectores", it.toString()) }
        result.blockCount?.let { LabelValue("Bloques", it.toString()) }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = "$label:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
        Text(text = value)
    }
}

@Composable
private fun SectorRow(sector: SectorDiagnostic) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                sector.isManufacturerSector -> Color(0xFFFFF3CD)
                sector.authenticated && sector.canRead -> Color(0xFFE3F5E1)
                else -> Color(0xFFF5E3E3)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Sector ${sector.sector}" + if (sector.isManufacturerSector) " (fabricante)" else "",
                fontWeight = FontWeight.Bold
            )
            Text(text = CardLayout.labelFor(sector.sector), style = MaterialTheme.typography.bodySmall)
            if (sector.isManufacturerSector) {
                Text("NO MODIFICAR", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "[${if (sector.canRead) "LECTURA OK" else "LECTURA NO"}] " +
                    "[ESCRITURA: NO EVALUADA]"
            )
            Text(text = sector.statusMessage)
            if (sector.authKeyUsed != null) {
                Text(text = "Clave usada (fábrica, pública): ${sector.authKeyUsed}", style = MaterialTheme.typography.bodySmall)
            }
            if (sector.rawDataHex != null) {
                Text(
                    text = "Datos crudos (bloque de datos 1): ${sector.rawDataHex}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
