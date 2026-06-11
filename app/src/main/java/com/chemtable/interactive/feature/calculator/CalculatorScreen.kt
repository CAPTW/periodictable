package com.chemtable.interactive.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.FormulaComponentResult

@Composable
fun CalculatorScreen(
    innerPadding: PaddingValues,
    onNavigateToPeriodicTable: () -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val formula by viewModel.formula.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val error by viewModel.error.collectAsState()
    val components by viewModel.components.collectAsState()
    val history by viewModel.history.collectAsState()
    val autoCalculate by viewModel.autoCalculate.collectAsState()

    // 프리필 식이 있으면 진입 시 1회만 자동 계산해 결과까지 보여준다.
    LaunchedEffect(autoCalculate) {
        if (autoCalculate) {
            viewModel.calculate()
            viewModel.onPrefillAutoCalculateHandled()
        }
    }

    val quickKeys = listOf("H", "C", "N", "O", "S", "P", "Cl", "Na", "Ca", "Fe", "Cu", "(", ")", "·", "→", "+")
    val digitKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding),
    ) {
        Text(
            text = "몰 질량 계산기",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = formula,
            onValueChange = viewModel::onFormulaChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Formula") },
            placeholder = { Text("예: Ca(OH)₂ + 2HCl") }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (key in quickKeys) {
                OutlinedButton(onClick = { viewModel.appendToken(key) }) {
                    Text(key)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (key in digitKeys) {
                OutlinedButton(onClick = { viewModel.appendToken(key) }) {
                    Text(key)
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = viewModel::deleteLast) { Text("⌫") }
            OutlinedButton(onClick = viewModel::clearFormula) { Text("초기화") }
            OutlinedButton(onClick = onNavigateToPeriodicTable) { Text("원소키보드로") }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        FilledTonalButton(
            onClick = { viewModel.calculate() },
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 110.dp, height = 40.dp)
        ) {
            Text("계산")
        }

        if (isCalculating) {
            LinearProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        if (resultText.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val total = resultText.toDoubleOrNull() ?: 0.0
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("총 몰 질량", style = MaterialTheme.typography.titleMedium)
                    Text("${"%.3f".format(total)} g/mol")
                    if (components.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("원소 기여도", style = MaterialTheme.typography.titleSmall)
                        for (item in components) {
                            Text(
                                text = componentLine(item, total),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "최근 계산",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        for (item in history) {
            TextButton(
                onClick = { viewModel.onFormulaChanged(item.formula) },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "${item.formula} = ${"%.3f".format(item.result)} g/mol",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun componentLine(component: FormulaComponentResult, total: Double): String {
    val percent = if (total <= 0.0) 0.0 else component.percentage
    return "${component.symbol}${component.count}  " +
            "${"%.3f".format(component.totalMass)} g/mol  " +
            "(${"%.1f".format(percent)}%)"
}
