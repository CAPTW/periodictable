package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityKind
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import java.util.Locale
import kotlin.math.abs

const val ReactorFoundationTitle = "분자 반응조 · 기초 실험"
const val ReactorFoundationExplanation =
    "스와이프하면 블록이 한쪽으로 모이고, 정해진 조합은 분자가 됩니다. " +
        "그 뒤 침강 지수에 따라 정확히 한 단계의 부유·침강이 진행됩니다."
const val ReactorFoundationDisclaimer =
    "침강 지수는 몰질량을 바탕으로 한 게임용 단순화입니다. " +
        "실제 물질의 밀도·부력·침강은 물질 상태, 매질, 온도, 구조 등 여러 조건에 따라 달라집니다."

@Composable
fun ReactorFoundationScreen(
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    viewModel: ReactorFoundationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ReactorFoundationContent(
        state = state,
        onSwipe = viewModel::onSwipe,
        onReset = viewModel::resetSample,
        onEntitySelected = viewModel::selectEntity,
        onEmergencyVent = viewModel::emergencyVent,
        onNavigateBack = onNavigateBack,
        modifier = Modifier.padding(innerPadding),
    )
}

@Composable
fun ReactorFoundationContent(
    state: ReactorFoundationUiState,
    onSwipe: (ReactorDirection) -> Unit,
    onReset: () -> Unit,
    onEntitySelected: (ReactorEntityId?) -> Unit,
    onEmergencyVent: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reactor_foundation_screen")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onNavigateBack) { Text("실험실로 돌아가기") }
            Text(
                text = "실험적 기초 기능 · P2",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = ReactorFoundationTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(ReactorFoundationExplanation, style = MaterialTheme.typography.bodyMedium)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.semantics {
                contentDescription = "침강 지수 게임 모델 안내. $ReactorFoundationDisclaimer"
            },
        ) {
            Text(
                text = ReactorFoundationDisclaimer,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.semantics {
                contentDescription = "압력 게임 모델 안내. $ReactorPressureDisclaimer"
            },
        ) {
            Text(
                text = ReactorPressureDisclaimer,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.board == null -> Text(
                text = state.errorMessage ?: "반응조 샘플을 준비하지 못했습니다.",
                color = MaterialTheme.colorScheme.error,
            )

            else -> ReactorReadyContent(
                state = state,
                board = state.board,
                onSwipe = onSwipe,
                onReset = onReset,
                onEntitySelected = onEntitySelected,
                onEmergencyVent = onEmergencyVent,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ReactorReadyContent(
    state: ReactorFoundationUiState,
    board: ReactorBoardState,
    onSwipe: (ReactorDirection) -> Unit,
    onReset: () -> Unit,
    onEntitySelected: (ReactorEntityId?) -> Unit,
    onEmergencyVent: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("턴 ${board.turnIndex}", modifier = Modifier.testTag("reactor_turn_label"))
        Text(
            "침강 단계 ${board.settlingPhase}",
            modifier = Modifier.testTag("reactor_phase_label"),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("↑ 부유", fontWeight = FontWeight.SemiBold)
        Text("• 중립", fontWeight = FontWeight.SemiBold)
        Text("↓ 침강", fontWeight = FontWeight.SemiBold)
    }
    Text("보드를 위·아래·왼쪽·오른쪽으로 한 번 스와이프하세요.")
    ReactorP3Hud(state = state, onEmergencyVent = onEmergencyVent)
    ReactorBoard(
        board = board,
        selectedEntityId = state.selectedEntityId,
        swipeEnabled = state.operationalState != com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState.OVERFLOW,
        onSwipe = onSwipe,
        onEntitySelected = onEntitySelected,
    )
    state.errorMessage?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.error)
    }
    OutlinedButton(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth().testTag("reactor_reset_sample"),
    ) {
        Text("샘플 초기화")
    }
    ReactorEventLog(state.latestEvents, state.lastReplayVerified)
    state.selectedEntityId?.let { selectedId ->
        board.entityStore[selectedId]?.let { entity ->
            ReactorEntityDetail(entity = entity, onClose = { onEntitySelected(null) })
        }
    }
}

@Composable
private fun ReactorBoard(
    board: ReactorBoardState,
    selectedEntityId: ReactorEntityId?,
    swipeEnabled: Boolean = true,
    onSwipe: (ReactorDirection) -> Unit,
    onEntitySelected: (ReactorEntityId?) -> Unit,
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 40.dp.toPx() }
    val tapThreshold = with(density) { 8.dp.toPx() }
    val currentBoard by rememberUpdatedState(board)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnEntitySelected by rememberUpdatedState(onEntitySelected)
    val currentSwipeEnabled by rememberUpdatedState(swipeEnabled)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("reactor_board")
            .semantics {
                contentDescription = if (swipeEnabled) {
                    "5×5 분자 반응조 보드, 정확히 25칸"
                } else {
                    "5×5 분자 반응조 보드, 오버플로로 스와이프 잠김, 정확히 25칸"
                }
            }
            .pointerInput(swipeEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragAmount = Offset.Zero
                    var isDrag = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == down.id } ?: break
                        if (change.pressed) {
                            dragAmount += change.positionChange()
                            if (dragAmount.getDistance() > tapThreshold) isDrag = true
                            change.consume()
                        } else {
                            if (isDrag && currentSwipeEnabled) {
                                val dx = dragAmount.x
                                val dy = dragAmount.y
                                when {
                                    abs(dx) > abs(dy) && abs(dx) > swipeThreshold ->
                                        currentOnSwipe(
                                            if (dx > 0) ReactorDirection.RIGHT else ReactorDirection.LEFT,
                                        )
                                    abs(dy) >= abs(dx) && abs(dy) > swipeThreshold ->
                                        currentOnSwipe(
                                            if (dy > 0) ReactorDirection.DOWN else ReactorDirection.UP,
                                        )
                                }
                            } else if (size.width > 0 && size.height > 0) {
                                val column = (change.position.x / size.width * currentBoard.dimension)
                                    .toInt().coerceIn(0, currentBoard.dimension - 1)
                                val row = (change.position.y / size.height * currentBoard.dimension)
                                    .toInt().coerceIn(0, currentBoard.dimension - 1)
                                currentOnEntitySelected(
                                    currentBoard.entityIdAt(ReactorPosition(row, column)),
                                )
                            }
                            change.consume()
                            break
                        }
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            repeat(board.dimension) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(board.dimension) { column ->
                        val position = ReactorPosition(row, column)
                        val entity = board.entityAt(position)
                        ReactorCell(
                            row = row,
                            column = column,
                            entity = entity,
                            selected = entity?.id == selectedEntityId,
                            onEntitySelected = onEntitySelected,
                            modifier = Modifier.fillMaxHeight().weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactorCell(
    row: Int,
    column: Int,
    entity: ReactorEntity?,
    selected: Boolean,
    onEntitySelected: (ReactorEntityId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = entity?.let { reactorEntityDescription(row, column, it) }
        ?: "${row + 1}행 ${column + 1}열, 빈 반응조 칸"
    Card(
        modifier = modifier
            .padding(2.dp)
            .testTag("reactor_cell_${row}_${column}")
            .semantics {
                contentDescription = description
                entity?.let { selectedEntity ->
                    onClick(label = "${selectedEntity.visibleLabel} 상세 보기") {
                        onEntitySelected(selectedEntity.id)
                        true
                    }
                }
            },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (entity == null) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                behaviorColor(entity.settlingBehavior)
            },
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (entity == null) {
                Text("·", color = MaterialTheme.colorScheme.outline)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        entity.visibleLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        behaviorArrow(entity.settlingBehavior),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactorEventLog(events: List<ReactorTurnEvent>, replayVerified: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp).testTag("reactor_event_log"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("최근 이벤트", fontWeight = FontWeight.Bold)
            if (events.isEmpty()) {
                Text("아직 실행된 이벤트가 없습니다.")
            } else {
                Text(if (replayVerified) "이벤트 재생 검증 완료" else "이벤트 재생 미검증")
                events.take(24).forEachIndexed { index, event ->
                    Text("${index + 1}. ${eventSummary(event)}", style = MaterialTheme.typography.bodySmall)
                }
                if (events.size > 24) Text("외 ${events.size - 24}개")
            }
        }
    }
}

@Composable
private fun ReactorEntityDetail(entity: ReactorEntity, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("reactor_entity_detail"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${entity.displayName} · ${entity.visibleLabel}", fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("닫기") }
            }
            HorizontalDivider()
            Text("종류: ${if (entity.kind == ReactorEntityKind.ELEMENT) "원소" else "분자"}")
            Text("몰질량: ${formatNumber(entity.molarMass)} g/mol")
            Text("기준 몰질량: 32.00 g/mol")
            Text("침강 지수: ${formatSignedNumber(entity.settlingIndex)}")
            Text("동작: ${behaviorKorean(entity.settlingBehavior)}")
            Text("이 표시는 몰질량 기반 게임용 단순화 모델입니다.")
        }
    }
}

private fun reactorEntityDescription(row: Int, column: Int, entity: ReactorEntity): String =
    "${row + 1}행 ${column + 1}열, ${entity.displayName} ${entity.visibleLabel}, " +
        "${if (entity.kind == ReactorEntityKind.ELEMENT) "원소" else "분자"}, " +
        "몰질량 ${formatNumber(entity.molarMass)} g/mol, " +
        "침강 지수 ${formatSignedNumber(entity.settlingIndex)}, " +
        "${behaviorSemantic(entity.settlingBehavior)}. 몰질량 기반 단순화 게임 모델"

private fun eventSummary(event: ReactorTurnEvent): String = when (event) {
    is ReactorTurnEvent.PlayerMove ->
        "압축 이동 ${event.entityId.value}: ${positionText(event.from)} → ${positionText(event.to)}"
    is ReactorTurnEvent.Merge ->
        "결합 ${event.resultFormula}: ${positionText(event.resultPosition)}"
    is ReactorTurnEvent.SettlingMove ->
        "${behaviorKorean(event.behavior)} 이동: ${positionText(event.from)} → ${positionText(event.to)}"
    is ReactorTurnEvent.SettlingSwap ->
        "침강 교환: ${positionText(event.upperFrom)} ↔ ${positionText(event.lowerFrom)}"
    else -> p3EventSummary(event)
}

private fun positionText(position: ReactorPosition): String =
    "${position.row + 1}행 ${position.column + 1}열"

private fun behaviorArrow(behavior: SettlingBehavior): String = when (behavior) {
    SettlingBehavior.RISE -> "↑ 부유"
    SettlingBehavior.NEUTRAL -> "• 중립"
    SettlingBehavior.SINK -> "↓ 침강"
}

private fun behaviorKorean(behavior: SettlingBehavior): String = when (behavior) {
    SettlingBehavior.RISE -> "부유"
    SettlingBehavior.NEUTRAL -> "중립"
    SettlingBehavior.SINK -> "침강"
}

private fun behaviorSemantic(behavior: SettlingBehavior): String = when (behavior) {
    SettlingBehavior.RISE -> "위로 부유하는 게임 블록"
    SettlingBehavior.NEUTRAL -> "중립인 게임 블록"
    SettlingBehavior.SINK -> "아래로 침강하는 게임 블록"
}

private fun behaviorColor(behavior: SettlingBehavior): Color = when (behavior) {
    SettlingBehavior.RISE -> Color(0xFFE3F2FD)
    SettlingBehavior.NEUTRAL -> Color(0xFFF5F5F5)
    SettlingBehavior.SINK -> Color(0xFFFFF3E0)
}

private fun formatNumber(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun formatSignedNumber(value: Double): String = String.format(Locale.US, "%+.2f", value)
