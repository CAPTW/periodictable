package com.chemtable.interactive.feature.glossary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.component.ChemSearchBar
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.GlossaryTerm

@Composable
fun GlossaryScreen(
    innerPadding: PaddingValues,
    onTermClick: (String) -> Unit,
    viewModel: GlossaryViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val terms by viewModel.filteredTerms.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("화학 사전", style = MaterialTheme.typography.titleLarge)
        ChemSearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            onSearch = {}
        )
        Text("검색 결과 ${terms.size}건", style = MaterialTheme.typography.titleSmall)

        if (terms.isEmpty()) {
            Text("검색 결과가 없습니다.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(terms, key = { it.id }) { term ->
                    GlossaryTermCard(
                        term = term,
                        onClick = { onTermClick(term.id) },
                        onToggleBookmark = { viewModel.toggleBookmark(term) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlossaryTermCard(
    term: GlossaryTerm,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${term.termKo} (${term.termEn})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onToggleBookmark,
                    label = { Text(if (term.isBookmarked) "북마크됨" else "북마크") }
                )
            }
            Text(
                text = term.simpleExplanation,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "카테고리: ${term.category.label()}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun GlossaryCategory.label(): String = when (this) {
    GlossaryCategory.ATOMIC_STRUCTURE -> "원자 구조"
    GlossaryCategory.BONDING -> "결합"
    GlossaryCategory.REACTIONS -> "반응"
    GlossaryCategory.THERMODYNAMICS -> "열역학"
    GlossaryCategory.NUCLEAR -> "핵화학"
    GlossaryCategory.ORGANIC -> "유기화학"
    GlossaryCategory.GENERAL -> "일반"
}
