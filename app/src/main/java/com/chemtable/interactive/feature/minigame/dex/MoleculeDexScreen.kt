package com.chemtable.interactive.feature.minigame.dex

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import com.chemtable.interactive.feature.minigame.model.MoleculeElementLink
import com.chemtable.interactive.feature.minigame.model.MoleculeGlossaryLink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun MoleculeDexScreen(
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onPlayMiniGame: () -> Unit,
    onOpenCalculator: (String) -> Unit,
    onOpenElement: (Int) -> Unit,
    onOpenGlossary: (String) -> Unit,
    viewModel: MoleculeDexViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    MoleculeDexContent(
        innerPadding = innerPadding,
        state = state,
        onNavigateBack = onNavigateBack,
        onPlayMiniGame = onPlayMiniGame,
        onOpenCalculator = onOpenCalculator,
        onOpenElement = onOpenElement,
        onOpenGlossary = onOpenGlossary,
    )
}

@Composable
internal fun MoleculeDexContent(
    innerPadding: PaddingValues,
    state: MoleculeDexUiState,
    onNavigateBack: () -> Unit,
    onPlayMiniGame: () -> Unit,
    onOpenCalculator: (String) -> Unit,
    onOpenElement: (Int) -> Unit,
    onOpenGlossary: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            MoleculeDexHeader(onNavigateBack = onNavigateBack)
        }
        item {
            MoleculeDexSummary(state = state)
        }
        if (state.errorMessage != null) {
            item {
                ErrorDexState(message = state.errorMessage, onPlayMiniGame = onPlayMiniGame)
            }
        } else if (state.isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else if (state.isEmpty) {
            item {
                EmptyDexState(onPlayMiniGame = onPlayMiniGame)
            }
        } else {
            item {
                Text("발견한 분자", style = MaterialTheme.typography.titleMedium)
            }
            items(state.discoveries, key = { item -> item.formula }) { item ->
                MoleculeDexRow(
                    item = item,
                    onOpenCalculator = onOpenCalculator,
                    onOpenElement = onOpenElement,
                    onOpenGlossary = onOpenGlossary,
                )
            }
        }
        if (!state.isLoading && state.recentSessions.isNotEmpty()) {
            item {
                RecentSessionsCard(sessions = state.recentSessions)
            }
        }
    }
}

@Composable
private fun MoleculeDexHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
            )
        }
        Column {
            Text("분자 도감", style = MaterialTheme.typography.titleLarge)
            Text(
                "게임에서 만든 분자를 다시 살펴보세요",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MoleculeDexSummary(state: MoleculeDexUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryMetric(label = "발견", value = "${state.discoveredCount}개")
            SummaryMetric(label = "최고점수", value = state.highScore?.let { "${it}점" } ?: "-")
            SummaryMetric(
                label = "최근 점수",
                value = state.recentSessions.firstOrNull()?.let { "${it.score}점" } ?: "-",
            )
        }
    }
}

@Composable
private fun RowScope.SummaryMetric(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyDexState(onPlayMiniGame: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("아직 발견한 분자가 없어요.", style = MaterialTheme.typography.titleMedium)
            Text(
                "분자 만들기 게임에서 첫 분자를 만들어 보세요.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onPlayMiniGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "분자 만들기 게임 시작" },
            ) {
                Text("분자 만들기 시작")
            }
        }
    }
}

@Composable
private fun ErrorDexState(message: String, onPlayMiniGame: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("분자 도감을 불러올 수 없어요.", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onPlayMiniGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "분자 만들기 게임으로 이동" },
            ) {
                Text("분자 만들기")
            }
        }
    }
}

@Composable
private fun MoleculeDexRow(
    item: MoleculeDexItem,
    onOpenCalculator: (String) -> Unit,
    onOpenElement: (Int) -> Unit,
    onOpenGlossary: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val lastDiscoveredText = formatDexTimestamp(item.lastDiscoveredAt)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription =
                                "${item.formula} 분자, 발견 ${item.discoveryCount}회, 최근 $lastDiscoveredText"
                        },
                ) {
                    Text(
                        text = formulaToAnnotated(item.formula),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "발견 ${item.discoveryCount}회 · 최근 $lastDiscoveredText",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = { onOpenCalculator(item.formula) },
                    modifier = Modifier.semantics {
                        contentDescription = "${item.formula} 계산기에서 보기"
                    },
                ) {
                    Text("계산기")
                }
            }
            if (item.elementLinks.isNotEmpty()) {
                LinkChipRow {
                    item.elementLinks.forEach { link ->
                        ElementChip(link = link, onOpenElement = onOpenElement)
                    }
                }
            }
            if (item.glossaryLinks.isNotEmpty()) {
                LinkChipRow {
                    item.glossaryLinks.forEach { link ->
                        GlossaryChip(link = link, onOpenGlossary = onOpenGlossary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun ElementChip(link: MoleculeElementLink, onOpenElement: (Int) -> Unit) {
    val displayName = link.displayName?.takeIf { it.isNotBlank() } ?: link.symbol
    AssistChip(
        onClick = { onOpenElement(link.atomicNumber) },
        modifier = Modifier.semantics {
            contentDescription = "$displayName 상세 보기"
        },
        label = {
            Text(if (link.count > 1) "${link.symbol} ×${link.count}" else link.symbol)
        },
    )
}

@Composable
private fun GlossaryChip(link: MoleculeGlossaryLink, onOpenGlossary: (String) -> Unit) {
    AssistChip(
        onClick = { onOpenGlossary(link.termId) },
        modifier = Modifier.semantics {
            contentDescription = "${link.label} 용어 보기"
        },
        label = { Text(link.label) },
    )
}

@Composable
private fun RecentSessionsCard(sessions: List<MoleculeDexSessionItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("최근 플레이", style = MaterialTheme.typography.titleMedium)
            sessions.forEachIndexed { index, session ->
                if (index > 0) {
                    HorizontalDivider()
                }
                Text(
                    text = "${if (session.success) "성공" else "종료"} · ${session.score}점 · ${formatDexTimestamp(session.playedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                val mission = session.missionFormula
                    ?.takeIf { it.isNotBlank() }
                    ?.let { formula ->
                        session.missionTargetCount?.let { count -> "목표: $formula ${count}개" }
                            ?: "목표: $formula"
                    }
                if (mission != null) {
                    Text(
                        text = mission,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                val made = session.moleculesMade.distinct().joinToString(", ")
                if (made.isNotBlank()) {
                    Text(
                        text = "만든 분자: $made",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendFormula(formula: String) {
    formula.forEach { ch ->
        if (ch.isDigit()) {
            withStyle(SpanStyle(baselineShift = BaselineShift.Subscript)) { append(ch) }
        } else {
            append(ch)
        }
    }
}

private fun formulaToAnnotated(formula: String): AnnotatedString =
    buildAnnotatedString { appendFormula(formula) }

internal fun formatDexTimestamp(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): String =
    SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).apply {
        this.timeZone = timeZone
    }.format(Date(epochMillis))

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun MoleculeDexPreview() {
    val state = MoleculeDexUiState(
        isLoading = false,
        highScore = 220,
        discoveries = listOf(
            MoleculeDexItem(
                formula = "H2O",
                discoveryCount = 3,
                firstDiscoveredAt = 1_700_000_000_000L,
                lastDiscoveredAt = 1_700_000_360_000L,
                elementLinks = listOf(
                    MoleculeElementLink("H", 2, 1, "수소"),
                    MoleculeElementLink("O", 1, 8, "산소"),
                ),
                glossaryLinks = listOf(
                    MoleculeGlossaryLink("molecule", "분자"),
                    MoleculeGlossaryLink("molar_mass", "몰 질량"),
                ),
            ),
        ),
        recentSessions = listOf(
            MoleculeDexSessionItem(
                id = 1L,
                score = 220,
                success = true,
                difficulty = "BEGINNER",
                missionFormula = "H2O",
                missionTargetCount = 2,
                playedAt = 1_700_000_360_000L,
                moleculesMade = listOf("H2O", "CO2"),
            ),
        ),
    )
    ChemTableTheme {
        MoleculeDexContent(
            innerPadding = PaddingValues(0.dp),
            state = state,
            onNavigateBack = {},
            onPlayMiniGame = {},
            onOpenCalculator = {},
            onOpenElement = {},
            onOpenGlossary = {},
        )
    }
}
