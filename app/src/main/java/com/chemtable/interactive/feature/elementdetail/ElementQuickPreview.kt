package com.chemtable.interactive.feature.elementdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.core.model.Element

@Composable
fun ElementQuickPreview(modifier: Modifier = Modifier, element: Element) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${element.symbol} (${element.atomicNumber})", style = MaterialTheme.typography.titleSmall)
            Text("${element.nameKo} · ${element.name}")
            Text("상태: ${element.stateOfMatter.name.lowercase()}")
            Text("원자량: ${element.molarMass} g/mol")
        }
    }
}
