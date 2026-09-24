package com.billarlegends.portfolio.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billarlegends.portfolio.domain.LabeledMovement
import com.billarlegends.portfolio.domain.SourceSummary
import com.billarlegends.portfolio.ui.displayName
import com.billarlegends.portfolio.ui.formatDate
import com.billarlegends.portfolio.ui.formatUsd

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val summary by viewModel.summary.collectAsState()
    val movements by viewModel.recentMovements.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TotalsCard(
                totalValueUsd = summary.totalValueUsd,
                totalInvestedUsd = summary.totalInvestedUsd,
                totalProfitUsd = summary.totalProfitUsd,
            )
        }

        if (summary.bySource.isNotEmpty()) {
            item {
                Text(
                    "Por fuente",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(summary.bySource) { source -> SourceSummaryRow(source) }
        }

        item {
            Text(
                "Movimientos recientes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (movements.isEmpty()) {
            item { Text("Todavía no hay movimientos. Agrega una cuenta o un negocio.") }
        } else {
            items(movements) { movement -> MovementRow(movement) }
        }
    }
}

@Composable
private fun TotalsCard(totalValueUsd: Double, totalInvestedUsd: Double, totalProfitUsd: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Valor total", style = MaterialTheme.typography.labelLarge)
            Text(formatUsd(totalValueUsd), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Column {
                Text("Invertido", style = MaterialTheme.typography.labelMedium)
                Text(formatUsd(totalInvestedUsd), style = MaterialTheme.typography.titleLarge)
            }
            Column {
                Text("Ganancia / pérdida", style = MaterialTheme.typography.labelMedium)
                Text(
                    formatUsd(totalProfitUsd),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (totalProfitUsd >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SourceSummaryRow(source: SourceSummary) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${source.source.displayName()} · ${source.label}", style = MaterialTheme.typography.titleSmall)
            Text("Valor: ${formatUsd(source.valueUsd)}  ·  Invertido: ${formatUsd(source.investedUsd)}")
            Text(
                "Ganancia: ${formatUsd(source.profitUsd)}",
                color = if (source.profitUsd >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
            )
        }
    }
}

@Composable
private fun MovementRow(movement: LabeledMovement) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("${movement.source.displayName()} · ${movement.ownerLabel} — ${movement.type.displayName()}")
        Text(formatDate(movement.timestampMillis), style = MaterialTheme.typography.bodySmall)
        Text("${formatUsd(movement.amountUsd)}  (${movement.amount} ${movement.assetOrCurrency})")
        movement.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Divider(modifier = Modifier.padding(top = 6.dp))
    }
}
