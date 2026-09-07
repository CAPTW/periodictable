package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chemtable.interactive.feature.minigame.reactor.items.ExperimentSubstrate
import com.chemtable.interactive.feature.minigame.reactor.items.ItemExperimentAction
import com.chemtable.interactive.feature.minigame.reactor.items.ItemExperimentState
import com.chemtable.interactive.feature.minigame.reactor.items.ReactorItemExperiment

@Composable
internal fun ReactorItemExperimentEntry() {
    var open by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ItemExperimentState.sample()) }
    var selection by remember { mutableStateOf(emptyList<Int>()) }
    var message by remember { mutableStateOf("두 칸을 차례로 선택하세요.") }
    fun apply(action: ItemExperimentAction) {
        val result = ReactorItemExperiment.resolve(state, action)
        state = result.state
        message = result.status.message
        if (result.applied) selection = emptyList()
    }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag("p5_open")) {
        Text("아이템 효과 실험 · P5")
    }
    if (open) Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("아이템 효과 실험 · P5")
                Text("A·B는 가상 기질입니다. 실제 화학 반응식이나 효소 종류를 나타내지 않습니다. S는 효소로 분해하지 않는 합성 기질입니다.")
                Text("같은 행·열의 거리 2 이내 두 칸을 선택하세요. 연결은 같은 기질만 최대 4조각, 분해는 대상 묶음 다음 빈 칸을 선택합니다.")
                Text("무료 실험 자원: ${state.remainingActions}/6", modifier = Modifier.testTag("p5_budget"))
                repeat(5) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { column ->
                            val index = row * 5 + column
                            val piece = state.cells[index]
                            val label = piece?.let { "${if (it.substrate == ExperimentSubstrate.SYNTHETIC) "S" else it.substrate.name}${it.units}" } ?: "·"
                            OutlinedButton(
                                onClick = {
                                    selection = if (index in selection) selection - index else (selection + index).takeLast(2)
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("p5_cell_$index").semantics {
                                    selected = index in selection
                                    contentDescription = "${row + 1}행 ${column + 1}열, ${if (piece == null) "빈 칸" else label}, 선택 순서 ${selection.indexOf(index) + 1}"
                                },
                            ) { Text(label) }
                        }
                    }
                }
                Text("선택: ${selection.joinToString { "${it / 5 + 1}행 ${it % 5 + 1}열" }}")
                Button(onClick = { if (selection.size == 2) apply(ItemExperimentAction.Link(selection[0], selection[1])) }, enabled = selection.size == 2 && state.remainingActions > 0, modifier = Modifier.fillMaxWidth().testTag("p5_link")) { Text("촉매 연결 · 자원 1") }
                listOf(ExperimentSubstrate.A, ExperimentSubstrate.B).forEach { enzyme ->
                    Button(onClick = { if (selection.size == 2) apply(ItemExperimentAction.Cleave(selection[0], selection[1], enzyme)) }, enabled = selection.size == 2 && state.remainingActions > 0, modifier = Modifier.fillMaxWidth().testTag("p5_enzyme_$enzyme")) { Text("효소 $enzyme 부분 분해 · 자원 1") }
                }
                Text(message, modifier = Modifier.testTag("p5_result").semantics { liveRegion = LiveRegionMode.Polite })
                Text("성공할 때만 자원 1을 사용합니다. 무료 초기화로 다시 실험할 수 있습니다. 이 실험은 기존 반응조와 보관함에 영향을 주지 않으며, 화면을 떠나거나 앱을 재시작하면 초기화됩니다.")
                TextButton(onClick = { state = ItemExperimentState.sample(); selection = emptyList(); message = "무료 실험을 초기화했습니다." }, modifier = Modifier.testTag("p5_reset")) { Text("무료 실험 초기화") }
                TextButton(onClick = { open = false }, modifier = Modifier.testTag("p5_close")) { Text("닫기") }
            }
        }
    }
}
