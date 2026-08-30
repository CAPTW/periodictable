package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureBand
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent

const val ReactorPressureDisclaimer =
    "압력은 보드 혼잡도와 공급 차단을 나타내는 게임용 지표이며 실제 화학 반응기 압력이 아닙니다."

@Composable
fun ReactorP3Hud(
    state: ReactorFoundationUiState,
    onEmergencyVent: () -> Unit,
) {
    ReactorFeedRail(preview = state.feedPreview, pending = state.pendingFeed)
    ReactorPressureLine(state = state)
    if (state.operationalState == ReactorOperationalState.OVERFLOW) {
        ReactorOverflowCard(state = state, onEmergencyVent = onEmergencyVent)
    }
}

@Composable
private fun ReactorFeedRail(
    preview: List<ReactorFeedSpecification>,
    pending: ReactorFeedSpecification?,
) {
    val symbols = preview.map { it.symbol }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reactor_feed_rail")
            .semantics {
                contentDescription = "다음 공급 " +
                    symbols.mapIndexed { index, symbol ->
                        if (index == 0) "대기 $symbol" else "${index + 1}번째 $symbol"
                    }.joinToString(", ")
            },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("다음 공급", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.forEachIndexed { index, spec ->
                    val pendingMark = spec == pending || index == 0
                    Card(
                        modifier = Modifier
                            .testTag("reactor_feed_preview_$index")
                            .semantics {
                                contentDescription = if (pendingMark) {
                                    "다음 공급 대기 ${spec.symbol}"
                                } else {
                                    "다음 공급 미리보기 ${index + 1} ${spec.symbol}"
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (pendingMark) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Text(
                            text = spec.symbol,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = if (pendingMark) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactorPressureLine(state: ReactorFoundationUiState) {
    val bandLabel = pressureBandLabel(state.pressureBand)
    val top = state.pressureBreakdown?.topRowOccupied ?: 0
    val blocked = (state.pressureBreakdown?.feedBlocked ?: 0) == 1
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reactor_pressure_line")
            .semantics {
                contentDescription = "압력 ${state.pressure} / 100, 상태 $bandLabel, " +
                    "윗줄 점유 $top, " +
                    if (blocked) "공급 차단" else "공급 가능"
            },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "압력 ${state.pressure} / 100",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("reactor_pressure_value"),
            )
            LinearProgressIndicator(
                progress = { state.pressure / 100f },
                modifier = Modifier.fillMaxWidth().testTag("reactor_pressure_progress"),
            )
            Text(
                text = bandLabel,
                modifier = Modifier.testTag("reactor_pressure_band"),
            )
            Text(
                text = "실패 ${state.failureCount} · 복구 ${state.recoveryCount}",
                modifier = Modifier
                    .testTag("reactor_failure_recovery_counts")
                    .semantics {
                        contentDescription =
                            "실패 횟수 ${state.failureCount}, 복구 횟수 ${state.recoveryCount}"
                    },
            )
        }
    }
}

@Composable
private fun ReactorOverflowCard(
    state: ReactorFoundationUiState,
    onEmergencyVent: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reactor_overflow_overlay")
            .semantics {
                contentDescription = "반응조 오버플로, 압력 ${state.pressure}, 보드 스와이프 잠김"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("반응조 오버플로", fontWeight = FontWeight.Bold)
            Text("윗줄이 막혀 공급이 차단되었습니다. 현재 압력 ${state.pressure}.")
            Text("긴급 배출은 광고나 결제 없이 윗줄 혼잡을 제거합니다.")
            Button(
                onClick = onEmergencyVent,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reactor_emergency_vent")
                    .semantics { contentDescription = "긴급 배출, 광고나 결제 없이 사용" },
            ) {
                Text("긴급 배출")
            }
        }
    }
}

fun pressureBandLabel(band: ReactorPressureBand): String = when (band) {
    ReactorPressureBand.NORMAL -> "정상"
    ReactorPressureBand.CAUTION -> "주의"
    ReactorPressureBand.CRITICAL -> "위험"
    ReactorPressureBand.OVERFLOW -> "오버플로"
}

fun p3EventSummary(event: ReactorTurnEvent): String = when (event) {
    is ReactorTurnEvent.FeedAttempted -> "공급 시도 ${event.symbol}"
    is ReactorTurnEvent.FeedPlaced -> "공급 배치 ${event.formula}"
    is ReactorTurnEvent.FeedBlocked -> "공급 차단 ${event.symbol}"
    is ReactorTurnEvent.PressureChanged ->
        "압력 ${event.oldPressure} → ${event.newPressure}"
    is ReactorTurnEvent.OverflowTriggered -> "오버플로 발생, 실패 ${event.failureCount}"
    is ReactorTurnEvent.RecoveryRequested -> "긴급 배출 요청"
    is ReactorTurnEvent.EmergencyVentApplied -> "긴급 배출 적용"
    is ReactorTurnEvent.EntityVented -> "배출 ${event.formula}"
    is ReactorTurnEvent.RecoveryCompleted -> "복구 완료, 압력 ${event.newPressure}"
    else -> ""
}
