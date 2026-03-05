package com.chemtable.interactive.feature.glossary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing

@Composable
fun GlossaryScreen(
    innerPadding: PaddingValues,
    onTermClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding)
    ) {
        Text("화학 사전", style = MaterialTheme.typography.titleLarge)
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            placeholder = { Text("용어 검색") }
        )
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp)
        ) {
            val terms = listOf("Electronegativity", "Half-life", "열역학", "주기율", "화학 결합")
                .filter { it.contains(query, ignoreCase = true) }
            items(terms) { term ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = term,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(12.dp)
                            .clickable { onTermClick(term.lowercase()) }
                    )
                }
            }
        }
    }
}

