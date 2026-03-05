package com.chemtable.interactive.feature.elementdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing

@Composable
fun ElementDetailScreen(
    atomicNumber: Int,
    onOpenNotes: (Int) -> Unit,
    onAddNote: (Int) -> Unit,
    viewModel: ElementDetailViewModel = hiltViewModel()
) {
    val element by viewModel.element.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (element == null) {
            item {
                Text("요청한 원소를 찾을 수 없습니다.")
            }
        } else {
            val current = element!!
            item {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    Text("${current.nameKo} (${current.symbol})", style = MaterialTheme.typography.titleLarge)
                    Text("원자번호: ${current.atomicNumber}")
                    Text("원자량: ${current.molarMass} g/mol")
                    Text("전자배치: ${current.electronConfiguration}")
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpenNotes(current.atomicNumber) }
                        ) {
                            Text("메모 보기")
                        }
                        TextButton(
                            onClick = { onAddNote(current.atomicNumber) }
                        ) {
                            Text("메모 작성")
                        }
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))
                Text("12개 핵심 속성")
            }
            val details = listOf(
                "상태" to current.stateOfMatter.name,
                "용융열" to (current.heatOfVaporization?.toString() ?: "N/A"),
                "비열" to (current.specificHeatCapacity?.toString() ?: "N/A"),
                "열팽창계수" to (current.thermalExpansionCoefficient?.toString() ?: "N/A"),
                "반감기" to (current.halfLife ?: "N/A"),
                "중성자단면적" to (current.neutronCrossSection?.toString() ?: "N/A"),
                "barn" to (current.barn?.toString() ?: "N/A"),
                "열전도도" to (current.thermalConductivity?.toString() ?: "N/A"),
                "전기음성도" to (current.electronegativity?.toString() ?: "N/A"),
                "원자반지름" to (current.atomicRadius?.toString() ?: "N/A")
            )
            items(details) { item ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(item.first)
                    Text(item.second, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
