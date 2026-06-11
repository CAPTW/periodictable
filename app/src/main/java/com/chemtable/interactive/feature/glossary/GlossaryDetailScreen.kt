package com.chemtable.interactive.feature.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.GlossaryCategory
import com.chemtable.interactive.core.model.InteractiveType

@Composable
fun GlossaryDetailScreen(
    innerPadding: PaddingValues,
    termId: String,
    onNavigateBack: () -> Unit,
    onOpenTerm: (String) -> Unit,
    onOpenElement: (Int) -> Unit,
    viewModel: GlossaryViewModel = hiltViewModel()
) {
    val term by viewModel.selectedTerm.collectAsState()
    val relatedTerms by viewModel.relatedTerms.collectAsState()

    LaunchedEffect(termId) {
        viewModel.selectTerm(termId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기"
                )
            }
            Text("용어 상세", style = MaterialTheme.typography.titleLarge)
        }

        if (term == null) {
            Text(
                text = "용어를 찾을 수 없습니다: $termId",
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        val selected = term!!
        val unresolvedIds = selected.relatedTerms.filter { id ->
            relatedTerms.none { it.id == id }
        }

        Text("${selected.termKo} (${selected.termEn})", style = MaterialTheme.typography.titleLarge)
        Text("카테고리: ${selected.category.label()}", style = MaterialTheme.typography.labelLarge)
        if (selected.interactiveType != null) {
            Text("인터랙션 타입: ${selected.interactiveType.label()}", style = MaterialTheme.typography.labelMedium)
        }

        HorizontalDivider()

        Text("정의", style = MaterialTheme.typography.titleMedium)
        Text(selected.definition, style = MaterialTheme.typography.bodyMedium)
        Text("쉬운 설명", style = MaterialTheme.typography.titleMedium)
        Text(selected.simpleExplanation, style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()

        Text("관련 용어", style = MaterialTheme.typography.titleMedium)
        if (relatedTerms.isEmpty() && unresolvedIds.isEmpty()) {
            Text("연결된 용어가 없습니다.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (related in relatedTerms) {
                    AssistChip(
                        onClick = { onOpenTerm(related.id) },
                        label = { Text(related.termKo) }
                    )
                }
                for (id in unresolvedIds) {
                    AssistChip(
                        onClick = { onOpenTerm(id) },
                        label = { Text(id) }
                    )
                }
            }
        }

        Text("관련 원소", style = MaterialTheme.typography.titleMedium)
        if (selected.relatedElements.isEmpty()) {
            Text("연결된 원소가 없습니다.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (atomicNumber in selected.relatedElements.sorted()) {
                    AssistChip(
                        onClick = { onOpenElement(atomicNumber) },
                        label = { Text("#$atomicNumber") }
                    )
                }
            }
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

private fun InteractiveType.label(): String = when (this) {
    InteractiveType.ELECTRON_ANIMATION -> "전자 애니메이션"
    InteractiveType.BOND_VISUALIZATION -> "결합 시각화"
    InteractiveType.DECAY_SIMULATION -> "붕괴 시뮬레이션"
    InteractiveType.ENERGY_DIAGRAM -> "에너지 다이어그램"
    InteractiveType.COMPARISON -> "비교"
}
