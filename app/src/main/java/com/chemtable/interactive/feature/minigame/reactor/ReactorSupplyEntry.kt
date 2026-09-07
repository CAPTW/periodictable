package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ReactorSupplyEntry(
    state: ReactorSupplyUiState,
    onClaim: () -> Unit,
    onReload: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true; onReload() },
        modifier = Modifier.fillMaxWidth().testTag("reactor_supply_open"),
    ) { Text("무료 보급 · 보관함") }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("무료 보급 · 보관함") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("첫 무료 보급: 연습 조각 1개")
                    Text("첫 보급은 한 번만 받을 수 있습니다. 앱을 다시 열거나 샘플을 초기화해도 보관 수량과 지급 기록은 유지됩니다.")
                    Text("연습 조각은 보관용이며 아직 사용 기능이 없습니다. 보급을 받지 않아도 반응조를 계속 플레이할 수 있습니다.")
                    Text(
                        text = when {
                            state.busy -> "보관함 확인 중…"
                            state.error -> "보관함을 확인하거나 저장하지 못했습니다. 다시 확인해 주세요."
                            state.quantity != null -> "보관 중: 연습 조각 ${state.quantity}개"
                            else -> "보관함 확인 중…"
                        },
                        modifier = Modifier.testTag("reactor_supply_balance")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (state.error) {
                        TextButton(onClick = onReload, modifier = Modifier.testTag("reactor_supply_retry")) {
                            Text("다시 확인")
                        }
                    }
                    Button(
                        onClick = onClaim,
                        enabled = !state.busy && !state.error && state.quantity != null && !state.claimed,
                        modifier = Modifier.fillMaxWidth().testTag("reactor_supply_claim"),
                    ) { Text(if (state.claimed) "첫 보급 지급 완료" else "무료로 1개 받기") }
                    Text("이 기기의 앱 데이터에 저장됩니다. 앱 데이터 삭제 후 복원이나 다른 기기와의 동기화는 지원하지 않습니다.")
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("닫기") } },
        )
    }
}
