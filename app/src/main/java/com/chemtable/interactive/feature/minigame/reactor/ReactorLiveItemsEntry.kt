package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorItemCommand
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorSubstrate

@Composable
internal fun ReactorLiveItemsEntry(
    state: ReactorFoundationUiState,
    onLoadSample: () -> Unit,
    onUse: (ReactorItemCommand) -> Unit,
    onClaimRecharge: () -> Unit = {},
    onLoadAdvancedSample: () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    var selectedCells by remember(state.board) { mutableStateOf(emptyList<Int>()) }
    OutlinedButton(onClick = { open = true }, enabled = state.board != null,
        modifier = Modifier.fillMaxWidth().testTag("p5_live_open")) { Text("현재 반응조 아이템") }
    if (open) Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("현재 반응조 아이템")
                Text("현재 보드에 즉시 적용됩니다. 성공 시 아이템 → 침강 1회 → 공급 → 압력 판정이 진행되며 턴과 무료 자원 1을 사용합니다.")
                Text("A/B는 가상 기질이며 실제 반응식·효소가 아닙니다. S는 효소 분해 불가. 같은 행·열 거리 2 이내, 연결 최대 4조각. 분해는 대상 다음 빈 칸을 선택하세요.")
                Text("턴 ${state.board?.turnIndex ?: 0} · 압력 ${state.pressure} · 무료 자원 ${state.itemActionsRemaining}/6", Modifier.testTag("p5_live_status"))
                val board = state.board
                if (board != null) repeat(board.dimension) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(board.dimension) { column ->
                            val index = row * board.dimension + column
                            val entity = board.cells[index]?.let(board.entityStore::get)
                            OutlinedButton(onClick = {
                                selectedCells = if (index in selectedCells) selectedCells - index else (selectedCells + index).takeLast(2)
                            }, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).heightIn(min=48.dp)
                                .testTag("p5_live_cell_$index").semantics {
                                    selected = index in selectedCells
                                    contentDescription = "현재 ${row+1}행 ${column+1}열, ${entity?.visibleLabel ?: "빈 칸"}, 선택 순서 ${selectedCells.indexOf(index)+1}"
                                }) { Text(entity?.visibleLabel ?: "·") }
                        }
                    }
                }
                Text("선택: ${selectedCells.joinToString { "${it/5+1}행 ${it%5+1}열" }}")
                val enabled = selectedCells.size == 2 && state.itemActionsRemaining > 0 && state.operationalState != ReactorOperationalState.OVERFLOW
                Button(onClick = { if (selectedCells.size == 2) onUse(ReactorItemCommand.Link(selectedCells[0],selectedCells[1])) },
                    enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("p5_live_link")) { Text("현재 보드 촉매 연결") }
                listOf(ReactorSubstrate.A,ReactorSubstrate.B).forEach { enzyme ->
                    Button(onClick = { if (selectedCells.size == 2) onUse(ReactorItemCommand.Cleave(selectedCells[0],selectedCells[1],enzyme)) },
                        enabled=enabled, modifier=Modifier.fillMaxWidth().testTag("p5_live_enzyme_$enzyme")) { Text("현재 보드 효소 $enzyme") }
                }
                Text("효소 연습: 같은 기질의 묶음과 빈 칸을 고르세요. 맞는 효소로 한 조각을 분리한 뒤, 같은 기질끼리 재연결하며 총량이 보존되는지 관찰하세요. 분해는 공간을 더 쓰므로 압력을 낮추는 구조 도구가 아닙니다.", Modifier.testTag("p5_learning_guide"))
                state.itemLearningMessage?.let { Text(it, Modifier.testTag("p5_learning_feedback")) }
                Text("자원이 6개 미만일 때 현재 반응조의 스와이프 3턴마다 무료 자원 1개를 받을 수 있습니다. 아이템·긴급 배출·거부된 턴은 적립되지 않습니다. 적립은 3턴까지만 보관됩니다.")
                Text("무료 보급 준비 ${state.itemRechargeProgress}/3", Modifier.testTag("p5_recharge_progress"))
                Button(onClick = onClaimRecharge, enabled = state.itemRechargeProgress == 3 && state.itemActionsRemaining < 6,
                    modifier = Modifier.fillMaxWidth().testTag("p5_recharge_claim")) { Text("보드 유지 · 무료 자원 1개 받기") }
                Text("보급은 턴·압력·보드를 바꾸지 않습니다. 세션용 자원이며 보관함과 연결되지 않고, 화면을 떠나거나 앱을 다시 시작하면 복원되지 않습니다.")
                Text(state.errorMessage ?: if (state.lastReplayVerified) "이벤트 재생 검증 완료" else "두 칸을 차례로 선택하세요.",
                    Modifier.testTag("p5_live_result").semantics { liveRegion = LiveRegionMode.Polite })
                if (state.operationalState == ReactorOperationalState.OVERFLOW) Text("오버플로: 닫은 뒤 기존 무료 긴급 배출을 사용하세요.")
                Text("아래 초기화는 현재 반응조 전체를 P5 샘플로 바꿉니다. 턴·공급·압력·실험 자원도 초기화하며, 보관함은 바꾸지 않습니다. 앱 재시작 후 보드는 복원되지 않습니다.")
                TextButton(onClick = { selectedCells=emptyList();onLoadSample() }, modifier=Modifier.testTag("p5_live_sample")) { Text("P5 샘플로 현재 보드 초기화") }
                Text("4조각 연습은 A3+A1, B3+B1로 시작합니다. 같은 기질의 3과 1을 연결해 4를 만든 뒤, 맞는 효소로 3과 1로 나누어 보세요. 아래 버튼도 현재 보드·턴·자원을 모두 초기화합니다.", Modifier.testTag("p5_advanced_guide"))
                TextButton(onClick = { selectedCells=emptyList();onLoadAdvancedSample() }, modifier=Modifier.testTag("p5_live_advanced")) { Text("4조각 학습 샘플로 초기화") }
                TextButton(onClick = { open=false }, modifier=Modifier.testTag("p5_live_close")) { Text("닫기") }
            }
        }
    }
}
