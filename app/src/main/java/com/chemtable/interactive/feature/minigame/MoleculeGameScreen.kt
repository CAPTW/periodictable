package com.chemtable.interactive.feature.minigame

import android.os.SystemClock
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.input.pointer.positionChange
import com.chemtable.interactive.feature.minigame.model.SelectedMoleculeSheet
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableColors
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import com.chemtable.interactive.core.designsystem.theme.CustomShapes
import com.chemtable.interactive.core.model.ElementCategory
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.Difficulty
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.DiscoveredMolecule
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameEffect
import com.chemtable.interactive.feature.minigame.model.GameEvent
import com.chemtable.interactive.feature.minigame.model.GameMode
import com.chemtable.interactive.feature.minigame.model.GamePhase
import com.chemtable.interactive.feature.minigame.model.GameUiState
import com.chemtable.interactive.feature.minigame.model.MissionTarget
import com.chemtable.interactive.feature.minigame.model.MoleculeElementLink
import com.chemtable.interactive.feature.minigame.model.MoleculeGlossaryLink
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import com.chemtable.interactive.feature.minigame.model.Position
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/** 화면 전용 일회성 피드백(짧게 표시 후 자동 사라짐). */
internal sealed interface FeedbackUi {
    data class Success(val label: String, val gained: Int) : FeedbackUi
    data object Rejected : FeedbackUi
}

private val BoardTapThreshold = 18.dp
private val BoardSwipeThreshold = 40.dp
private const val TimeAttackTickIntervalMillis = 250L

private data class ModeOptionUi(
    val mode: GameMode,
    val label: String,
    val description: String,
)

private val ModeOptions = listOf(
    ModeOptionUi(GameMode.MISSION, "미션", "목표 분자를 달성해 보세요"),
    ModeOptionUi(GameMode.ENDLESS, "엔들리스", "보드가 막힐 때까지 최고점수에 도전해요"),
    ModeOptionUi(GameMode.TIME_ATTACK, "타임어택", "제한 시간 안에 최고점수에 도전해요"),
)

private data class DifficultyOptionUi(
    val difficulty: Difficulty,
    val label: String,
    val description: String,
)

private val DifficultyOptions = listOf(
    DifficultyOptionUi(Difficulty.BEGINNER, "초급", "H/O 중심, 물과 소금부터"),
    DifficultyOptionUi(Difficulty.INTERMEDIATE, "중급", "C/N 등장, CO₂ 목표"),
    DifficultyOptionUi(Difficulty.ADVANCED, "고급", "이동 제한과 균형 잡힌 원소 풀"),
)

/**
 * 미니게임 진입점(하단바 없는 풀스크린 전제). Hilt ViewModel 을 연결하고
 * 일회성 effect 를 화면 피드백(배너/보드 흔들림)으로 변환해 stateless content 에 전달한다.
 */
@Composable
fun MoleculeGameScreen(
    onExit: () -> Unit = {},
    onOpenCalculator: (String) -> Unit = {},
    onOpenElement: (Int) -> Unit = {},
    onOpenGlossary: (String) -> Unit = {},
    viewModel: MoleculeGameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var feedback by remember { mutableStateOf<FeedbackUi?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GameEffect.NavigateBack -> onExit()
                is GameEffect.NavigateToCalculator -> onOpenCalculator(effect.formula)
                is GameEffect.NavigateToElement -> onOpenElement(effect.atomicNumber)
                is GameEffect.NavigateToGlossary -> onOpenGlossary(effect.termId)
                is GameEffect.MergeSuccess -> feedback = FeedbackUi.Success(effect.label, effect.gained)
                GameEffect.MergeRejected -> {
                    feedback = FeedbackUi.Rejected
                    shakeTrigger++
                }
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1300)
            feedback = null
        }
    }

    MoleculeGameContent(
        state = state,
        onEvent = viewModel::onEvent,
        onExit = onExit,
        feedback = feedback,
        shakeTrigger = shakeTrigger,
    )
}

/** 상태를 외부에서 주입받는 stateless 컨테이너. @Preview 로 단독 렌더 가능. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoleculeGameContent(
    state: GameUiState,
    onEvent: (GameEvent) -> Unit,
    onExit: () -> Unit,
    feedback: FeedbackUi? = null,
    shakeTrigger: Int = 0,
) {
    // back 정책: Sheet 오픈 -> Sheet 닫기, Playing -> Pause, Paused -> Resume, Result -> Exit. (Intro 는 기본 동작으로 종료)
    BackHandler(enabled = state.selectedMoleculeSheet != null) { onEvent(GameEvent.CloseMoleculeSheet) }
    BackHandler(enabled = state.selectedMoleculeSheet == null && state.phase == GamePhase.PLAYING) { onEvent(GameEvent.Pause) }
    BackHandler(enabled = state.selectedMoleculeSheet == null && state.phase == GamePhase.PAUSED) { onEvent(GameEvent.Resume) }
    BackHandler(enabled = state.selectedMoleculeSheet == null && state.phase == GamePhase.RESULT) { onExit() }

    LaunchedEffect(state.phase, state.mode, state.showTutorial) {
        if (state.phase == GamePhase.PLAYING && state.mode == GameMode.TIME_ATTACK && !state.showTutorial) {
            while (true) {
                onEvent(GameEvent.TimerTick(SystemClock.elapsedRealtime()))
                delay(TimeAttackTickIntervalMillis)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        when (state.phase) {
            GamePhase.INTRO -> IntroContent(state = state, onEvent = onEvent)
            else -> PlayingContent(state = state, onEvent = onEvent, shakeTrigger = shakeTrigger)
        }

        // 일회성 피드백 배너(보드 위, 오버레이 아래).
        if (feedback != null && state.phase == GamePhase.PLAYING) {
            FeedbackBanner(feedback, Modifier.align(Alignment.TopCenter))
        }

        if (state.phase == GamePhase.PAUSED) {
            PauseOverlay(onEvent = onEvent, onExit = onExit)
        }
        if (state.phase == GamePhase.RESULT) {
            ResultOverlay(state = state, onEvent = onEvent, onExit = onExit)
        }
        if (state.showTutorial) {
            TutorialCoachMarks(
                onSkip = { onEvent(GameEvent.SkipTutorial) },
                onDone = { onEvent(GameEvent.SkipTutorial) },
            )
        }

        // 인게임 분자 블록 상세 Bottom Sheet
        if (state.selectedMoleculeSheet != null) {
            ModalBottomSheet(
                onDismissRequest = { onEvent(GameEvent.CloseMoleculeSheet) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                MoleculeSheetContent(
                    sheet = state.selectedMoleculeSheet,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun IntroContent(state: GameUiState, onEvent: (GameEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("분자 만들기", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "원소를 모아 분자를 만드는 게임이에요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "원자량·분자량이 큰(무거운) 블록일수록 아래로 가라앉아요.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            "정해진 조합만 분자가 돼요. 실제 반응 조건은 게임을 위해 단순화했어요.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        ModeSelector(
            selected = state.mode,
            onSelect = { onEvent(GameEvent.SelectMode(it)) },
        )
        Spacer(Modifier.height(12.dp))
        DifficultySelector(
            selected = state.difficulty,
            onSelect = { onEvent(GameEvent.SelectDifficulty(it)) },
        )
        Spacer(Modifier.height(12.dp))
        BoardSizeSelector(
            selected = state.boardSize,
            onSelect = { onEvent(GameEvent.SelectBoardSize(it)) },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = boardSizeExplanation(state.boardSize),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onEvent(GameEvent.StartGame) },
            enabled = state.isEngineReady,
        ) {
            Text(if (state.isEngineReady) "시작" else "원소 데이터 불러오는 중…")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onEvent(GameEvent.ShowTutorial) }) {
            Text("게임 방법 보기")
        }
    }
}

@Composable
private fun PlayingContent(state: GameUiState, onEvent: (GameEvent) -> Unit, shakeTrigger: Int) {
    Column(modifier = Modifier.fillMaxSize()) {
        HudBar(state = state, onPause = { onEvent(GameEvent.Pause) })
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val boardExtent = minOf(maxWidth, maxHeight)
            GameBoardView(
                board = state.board,
                enabled = state.phase == GamePhase.PLAYING,
                shakeTrigger = shakeTrigger,
                onSwipe = { onEvent(GameEvent.Swipe(it)) },
                onBlockTap = { onEvent(GameEvent.BlockTapped(it)) },
                modifier = Modifier.size(boardExtent),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "상하좌우로 스와이프해 같은 줄의 블록을 모으세요",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HudBar(state: GameUiState, onPause: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("점수 ${state.score}", style = MaterialTheme.typography.titleMedium)
            val mission = state.missionTarget
            if (mission != null) {
                Text(
                    text = buildAnnotatedString {
                        append("목표 ")
                        appendFormula(mission.formula)
                        append(" ${mission.progress}/${mission.count}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics {
                        contentDescription = "목표 ${mission.formula} ${mission.progress}/${mission.count}"
                    },
                )
            } else if (state.mode == GameMode.ENDLESS) {
                Text(
                    text = "엔들리스 · 최고점수 도전",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics {
                        contentDescription = "엔들리스 모드, 최고점수 도전"
                    },
                )
            } else if (state.mode == GameMode.TIME_ATTACK) {
                val clock = formatTimeAttackClock(state.timeLeftMillis)
                val accessibilityTime = formatTimeAttackAccessibilityTime(state.timeLeftMillis)
                Text(
                    text = "타임어택 · 남은 시간 $clock",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics {
                        contentDescription = "타임어택 모드, 남은 시간 $accessibilityTime"
                    },
                )
            }
            if (state.combo > 1) {
                Text("콤보 x${state.combo}", style = MaterialTheme.typography.labelSmall)
            }
            Text("난이도 ${state.difficulty.labelKo()}", style = MaterialTheme.typography.labelSmall)
            Text("보드 ${state.boardSize.displayLabel}", style = MaterialTheme.typography.labelSmall)
            state.movesLeft?.let { moves ->
                Text("남은 이동 $moves", style = MaterialTheme.typography.labelSmall)
            }
        }
        OutlinedButton(onClick = onPause) { Text("일시정지") }
    }
}

@Composable
internal fun BoardSizeSelector(
    selected: ClassicBoardSize,
    onSelect: (ClassicBoardSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("molecule_game_board_size_selector"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("보드 크기", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            ClassicBoardSize.entries.forEach { boardSize ->
                FilterChip(
                    selected = boardSize == selected,
                    onClick = { onSelect(boardSize) },
                    modifier = Modifier
                        .testTag("molecule_game_board_size_${boardSize.dimension}")
                        .semantics {
                            contentDescription = "${boardSize.accessibilityLabel} 선택"
                        },
                    label = { Text(boardSize.displayLabel, maxLines = 1) },
                )
            }
        }
    }
}

private fun boardSizeExplanation(boardSize: ClassicBoardSize): String = when (boardSize) {
    ClassicBoardSize.FOUR_BY_FOUR -> "빠르게 즐기는 기본 Classic 보드예요."
    ClassicBoardSize.FIVE_BY_FIVE -> "같은 규칙을 더 넓은 5×5 보드에서 즐겨요."
    ClassicBoardSize.SIX_BY_SIX -> "같은 규칙을 가장 넓은 6×6 보드에서 즐겨요."
}

@Composable
private fun ModeSelector(
    selected: GameMode,
    onSelect: (GameMode) -> Unit,
) {
    val selectedOption = ModeOptions.first { it.mode == selected }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("모드", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeOptions.forEach { option ->
                FilterChip(
                    selected = option.mode == selected,
                    onClick = { onSelect(option.mode) },
                    modifier = Modifier.semantics {
                        contentDescription = "${option.label} 모드 선택, ${option.description}"
                    },
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = selectedOption.description,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DifficultySelector(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
) {
    val selectedOption = DifficultyOptions.first { it.difficulty == selected }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("난이도", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DifficultyOptions.forEach { option ->
                FilterChip(
                    selected = option.difficulty == selected,
                    onClick = { onSelect(option.difficulty) },
                    modifier = Modifier.semantics {
                        contentDescription = "${option.label} 난이도 선택, ${option.description}"
                    },
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = selectedOption.description,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun GameBoardView(
    board: BoardState,
    enabled: Boolean,
    shakeTrigger: Int,
    onSwipe: (Direction) -> Unit,
    onBlockTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tapThreshold = with(density) { BoardTapThreshold.toPx() }
    val swipeThreshold = with(density) { BoardSwipeThreshold.toPx() }
    val visualProfile = boardCellVisualProfile(board.boardSize)
    val cellHitPadding = with(density) { visualProfile.outerPaddingDp.dp.toPx() }

    val currentBoard by rememberUpdatedState(board)
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val currentOnBlockTap by rememberUpdatedState(onBlockTap)

    // 조합 실패(MergeRejected) 시 보드 좌우 흔들림 피드백.
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            listOf(14f, -14f, 10f, -10f, 6f, -6f, 0f).forEach { target ->
                shakeX.animateTo(target, tween(durationMillis = 45))
            }
        }
    }

    Box(
        modifier = modifier
            .testTag(MoleculeGameBoardTestTag)
            .semantics {
                contentDescription = boardRootContentDescription(board.boardSize)
            }
            .offset { IntOffset(shakeX.value.roundToInt(), 0) }
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragAmount = Offset.Zero
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == down.id }
                        if (change == null) {
                            break
                        }

                        if (change.pressed) {
                            val positionChange = change.positionChange()
                            dragAmount += positionChange
                            if (dragAmount.getDistance() > tapThreshold) {
                                isDrag = true
                            }
                            change.consume()
                        } else {
                            // Up
                            if (!isDrag) {
                                val position = boardCellForTap(
                                    tapX = change.position.x,
                                    tapY = change.position.y,
                                    viewWidth = size.width.toFloat(),
                                    viewHeight = size.height.toFloat(),
                                    boardSize = currentBoard.size,
                                    cellPaddingPx = cellHitPadding,
                                )
                                if (position != null) {
                                    val block = currentBoard.blockAt(position.row, position.col)
                                    if (block is MoleculeBlock) {
                                        currentOnBlockTap(block.id)
                                    }
                                }
                            } else {
                                val dx = dragAmount.x
                                val dy = dragAmount.y
                                when {
                                    abs(dx) > abs(dy) && abs(dx) > swipeThreshold ->
                                        currentOnSwipe(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                                    abs(dy) >= abs(dx) && abs(dy) > swipeThreshold ->
                                        currentOnSwipe(if (dy > 0) Direction.DOWN else Direction.UP)
                                }
                            }
                            change.consume()
                            break
                        }
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until board.size) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    for (c in 0 until board.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(visualProfile.outerPaddingDp.dp)
                                .testTag(boardCellTestTag(r, c))
                                .semantics(mergeDescendants = true) {
                                    contentDescription = boardCellContentDescription(board, r, c)
                                },
                        ) {
                            when (val block = board.blockAt(r, c)) {
                                is ElementBlock -> ElementBlockView(
                                    block = block,
                                    visualProfile = visualProfile,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag(boardBlockTestTag(r, c)),
                                )
                                is MoleculeBlock -> MoleculeBlockView(
                                    block = block,
                                    visualProfile = visualProfile,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag(boardBlockTestTag(r, c)),
                                    onOpenOptions = { currentOnBlockTap(block.id) },
                                )
                                null -> EmptyCell(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCell(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = CustomShapes.elementCell,
        colors = CardDefaults.cardColors(containerColor = Color(0x14000000)),
    ) {}
}

/** 원소 블록 — ElementCell 과 동일한 디자인 토큰(카테고리 색 + elementCell shape). */
@Composable
private fun ElementBlockView(
    block: ElementBlock,
    visualProfile: BoardCellVisualProfile,
    modifier: Modifier = Modifier,
) {
    val displayName = block.nameKo.ifBlank { block.symbol }
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$displayName 원소 블록, ${block.symbol}, ${molarMassDescription(block.molarMass)}"
        },
        shape = CustomShapes.elementCell,
        colors = CardDefaults.cardColors(containerColor = categoryColor(block.category)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(visualProfile.contentPaddingDp.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = block.symbol,
                style = when (visualProfile.outerPaddingDp) {
                    3f -> MaterialTheme.typography.titleLarge
                    2f -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (visualProfile.showSecondaryMass) {
                Text(
                    text = formatMass(block.molarMass),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 분자 블록 — 원소 블록과 명확히 구분(둥근 card shape + 강조색 + 흰 테두리). 화학식은 아래첨자로. */
@Composable
private fun MoleculeBlockView(
    block: MoleculeBlock,
    visualProfile: BoardCellVisualProfile,
    modifier: Modifier = Modifier,
    onOpenOptions: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${block.formula} 분자 블록, ${molarMassDescription(block.massScore)}"
            if (onOpenOptions != null) {
                onClick(label = "분자 옵션 열기") {
                    onOpenOptions.invoke()
                    true
                }
            }
        },
        shape = CustomShapes.card,
        border = BorderStroke(2.dp, Color.White),
        colors = CardDefaults.cardColors(containerColor = ChemTableColors.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(visualProfile.contentPaddingDp.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formulaToAnnotated(block.formula),
                style = if (visualProfile.outerPaddingDp >= 3f) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold,
                color = ChemTableColors.onPrimary,
                maxLines = 1,
            )
            if (visualProfile.showSecondaryMass) {
                Text(
                    text = formatMass(block.massScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = ChemTableColors.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FeedbackBanner(feedback: FeedbackUi, modifier: Modifier = Modifier) {
    val container: Color
    val onContainer: Color
    val message: AnnotatedString
    when (feedback) {
        is FeedbackUi.Success -> {
            container = MaterialTheme.colorScheme.primaryContainer
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer
            message = buildAnnotatedString {
                appendFormula(feedback.label)
                append(" 생성!  +${feedback.gained}")
            }
        }
        FeedbackUi.Rejected -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer
            message = buildAnnotatedString { append("정해진 조합만 분자가 돼요 (벌점 없음)") }
        }
    }
    Card(
        modifier = modifier
            .padding(top = 4.dp)
            .semantics {
                contentDescription = when (feedback) {
                    is FeedbackUi.Success -> "${feedback.label} 생성, ${feedback.gained}점 획득"
                    FeedbackUi.Rejected -> "정해진 조합만 분자가 됩니다. 벌점은 없습니다."
                }
            },
        shape = CustomShapes.chip,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Text(
            text = message,
            color = onContainer,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PauseOverlay(onEvent: (GameEvent) -> Unit, onExit: () -> Unit) {
    Scrim {
        OverlayCard(title = "일시정지") {
            Button(onClick = { onEvent(GameEvent.Resume) }, modifier = Modifier.fillMaxWidth()) {
                Text("계속하기")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onEvent(GameEvent.Restart) }, modifier = Modifier.fillMaxWidth()) {
                Text("다시 시작")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("나가기")
            }
        }
    }
}

@Composable
private fun ResultOverlay(state: GameUiState, onEvent: (GameEvent) -> Unit, onExit: () -> Unit) {
    Scrim {
        OverlayCard(title = resultTitleFor(state)) {
            Text("점수 ${state.score}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.discoveredMolecules.isNotEmpty()) {
                Text("만든 분자", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 각 분자마다 "계산기에서 보기" CTA — 클릭 시 해당 식을 계산기로 프리필 전달.
                    state.discoveredMolecules.forEach { molecule ->
                        MoleculeResultRow(molecule = molecule, onEvent = onEvent)
                    }
                }
            } else {
                Text("만든 분자가 없습니다.", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onEvent(GameEvent.Restart) }, modifier = Modifier.fillMaxWidth()) {
                Text("다시 하기")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("나가기")
            }
        }
    }
}

private fun resultTitleFor(state: GameUiState): String = when {
    state.mode == GameMode.ENDLESS -> "엔들리스 종료"
    state.mode == GameMode.TIME_ATTACK -> "타임어택 종료"
    state.resultSuccess -> "미션 성공!"
    else -> "게임 종료"
}

@Composable
private fun MoleculeResultRow(molecule: DiscoveredMolecule, onEvent: (GameEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formulaToAnnotated(molecule.formula),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "${molecule.formula} 분자"
                },
            )
            TextButton(onClick = { onEvent(GameEvent.OpenCalculator(molecule.formula)) }) {
                Text("계산기에서 보기")
            }
        }
        if (molecule.elementLinks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                molecule.elementLinks.forEach { link ->
                    ElementLinkChip(link = link, onClick = { onEvent(GameEvent.OpenElement(link.atomicNumber)) })
                }
            }
        }
        if (molecule.glossaryLinks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                molecule.glossaryLinks.forEach { link ->
                    GlossaryLinkChip(link = link, onClick = { onEvent(GameEvent.OpenGlossary(link.termId)) })
                }
            }
        }
    }
}

@Composable
private fun ElementLinkChip(link: MoleculeElementLink, onClick: () -> Unit) {
    val displayName = link.displayName?.takeIf { it.isNotBlank() } ?: link.symbol
    AssistChip(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = "$displayName 상세 보기"
        },
        label = {
            Text(
                text = if (link.count > 1) "${link.symbol} ×${link.count}" else link.symbol,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

@Composable
private fun GlossaryLinkChip(link: MoleculeGlossaryLink, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = "${link.label} 용어 보기"
        },
        label = {
            Text(
                text = link.label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

/** 튜토리얼 코치마크 — 3단계. 건너뛰기/다음/시작. (재방문 자동표시 정책은 ViewModel 의 세션 플래그가 유지) */
@Composable
private fun TutorialCoachMarks(onSkip: () -> Unit, onDone: () -> Unit) {
    val steps = listOf(
        "무거운 블록은 아래로" to
            "원자량·분자량이 큰(무거운) 블록일수록 아래로 가라앉아요. 밀도가 아니라 질량 기준이에요.",
        "스와이프로 모으기" to
            "상하좌우로 밀어 같은 줄의 블록을 한쪽으로 모으세요.",
        "조합하면 분자" to
            "정해진 조합이 만나면 분자 블록으로 합쳐져요. 예: H + H → H₂. 실제 반응 조건은 단순화했어요.",
    )
    var step by remember { mutableIntStateOf(0) }
    val isLast = step >= steps.lastIndex

    Scrim {
        OverlayCard(title = steps[step].first) {
            Text(steps[step].second, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("${step + 1} / ${steps.size}", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { if (isLast) onDone() else step++ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isLast) "시작" else "다음")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("건너뛰기")
            }
        }
    }
}

/** 오버레이 배경 입력은 차단하고, 카드 내부 스크롤/버튼 입력은 유지한다. */
@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0x99000000))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        )
        content()
    }
}

@Composable
private fun OverlayCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(8.dp),
        shape = CustomShapes.card,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** 화학식 문자열의 숫자를 아래첨자로 렌더링해 builder 에 추가한다. 예: "H2O" -> H₂O */
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

private fun categoryColor(category: ElementCategory?): Color = when (category) {
    ElementCategory.ALKALI_METAL -> ChemTableColors.alkaliMetal
    ElementCategory.ALKALINE_EARTH -> ChemTableColors.alkalineEarth
    ElementCategory.TRANSITION_METAL -> ChemTableColors.transitionMetal
    ElementCategory.POST_TRANSITION_METAL -> ChemTableColors.postTransitionMetal
    ElementCategory.METALLOID -> ChemTableColors.metalloid
    ElementCategory.NONMETAL -> ChemTableColors.nonmetal
    ElementCategory.HALOGEN -> ChemTableColors.halogen
    ElementCategory.NOBLE_GAS -> ChemTableColors.nobleGas
    ElementCategory.LANTHANIDE -> ChemTableColors.lanthanide
    ElementCategory.ACTINIDE -> ChemTableColors.actinide
    ElementCategory.UNKNOWN, null -> ChemTableColors.primary
}

private fun formatMass(value: Double): String =
    if (value <= 0.0) "" else String.format(Locale.ROOT, "%.1f", value)

private fun molarMassDescription(value: Double): String =
    if (value <= 0.0) "몰 질량 정보 없음" else "${formatMass(value)} g/mol"

private fun Difficulty.labelKo(): String = when (this) {
    Difficulty.BEGINNER -> "초급"
    Difficulty.INTERMEDIATE -> "중급"
    Difficulty.ADVANCED -> "고급"
}

internal fun boardCellForTap(
    tapX: Float,
    tapY: Float,
    viewWidth: Float,
    viewHeight: Float,
    boardSize: Int,
    cellPaddingPx: Float,
): Position? {
    if (boardSize <= 0 || viewWidth <= 0f || viewHeight <= 0f) return null
    if (tapX < 0f || tapY < 0f || tapX >= viewWidth || tapY >= viewHeight) return null

    val cellWidth = viewWidth / boardSize
    val cellHeight = viewHeight / boardSize
    val col = (tapX / cellWidth).toInt().coerceIn(0, boardSize - 1)
    val row = (tapY / cellHeight).toInt().coerceIn(0, boardSize - 1)
    val localX = tapX - (col * cellWidth)
    val localY = tapY - (row * cellHeight)
    val paddingX = cellPaddingPx.coerceAtMost(cellWidth / 2f)
    val paddingY = cellPaddingPx.coerceAtMost(cellHeight / 2f)

    if (
        localX < paddingX ||
        localX > cellWidth - paddingX ||
        localY < paddingY ||
        localY > cellHeight - paddingY
    ) {
        return null
    }

    return Position(row = row, col = col)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun MoleculeGamePreview() {
    val sampleBoard = BoardState(
        size = 4,
        grid = listOf(
            listOf(null, ElementBlock(1, 1, "H", "수소", 1.008, ElementCategory.NONMETAL), null, null),
            listOf(null, null, MoleculeBlock(2, "H2O", 18.0, mapOf("H" to 2, "O" to 1), "물"), null),
            listOf(ElementBlock(3, 8, "O", "산소", 16.0, ElementCategory.NONMETAL), null, null, null),
            listOf(
                ElementBlock(4, 11, "Na", "소듐", 22.99, ElementCategory.ALKALI_METAL),
                null, null,
                ElementBlock(5, 17, "Cl", "염소", 35.45, ElementCategory.HALOGEN),
            ),
        ),
    )
    val state = GameUiState.initial().copy(
        phase = GamePhase.RESULT,
        board = sampleBoard,
        score = 152,
        combo = 2,
        missionTarget = MissionTarget("H2O", 2, 2),
        discoveredMolecules = listOf(
            DiscoveredMolecule(
                formula = "H2O",
                elementLinks = listOf(
                    MoleculeElementLink("H", 2, 1, "수소"),
                    MoleculeElementLink("O", 1, 8, "산소"),
                ),
                glossaryLinks = listOf(
                    MoleculeGlossaryLink("compound", "화합물"),
                    MoleculeGlossaryLink("molar_mass", "몰 질량"),
                    MoleculeGlossaryLink("covalent_bond", "공유 결합"),
                ),
            ),
            DiscoveredMolecule(
                formula = "NaCl",
                elementLinks = listOf(
                    MoleculeElementLink("Na", 1, 11, "나트륨"),
                    MoleculeElementLink("Cl", 1, 17, "염소"),
                ),
                glossaryLinks = listOf(
                    MoleculeGlossaryLink("ionic_bond", "이온 결합"),
                    MoleculeGlossaryLink("electronegativity", "전기음성도"),
                    MoleculeGlossaryLink("lattice_energy", "격자 에너지"),
                ),
            ),
        ),
        isEngineReady = true,
        resultSuccess = true,
    )
    ChemTableTheme {
        MoleculeGameContent(
            state = state,
            onEvent = {},
            onExit = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoleculeSheetContent(
    sheet: SelectedMoleculeSheet,
    onEvent: (GameEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "${sheet.formula} 분자 옵션" }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formulaToAnnotated(sheet.formula),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "${sheet.formula} 분자"
                    },
                )
                Text(
                    text = "${formatMass(sheet.molarMass)} g/mol",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // 계산기에서 보기 CTA
            Button(
                onClick = { onEvent(GameEvent.OpenCalculator(sheet.formula)) },
                modifier = Modifier.semantics {
                    contentDescription = "${sheet.formula}의 몰 질량 계산기에서 보기"
                }
            ) {
                Text("계산기에서 보기")
            }
        }

        HorizontalDivider()

        // 구성 원소
        Text("구성 원소", style = MaterialTheme.typography.titleMedium)
        if (sheet.elementLinks.isEmpty()) {
            Text("연결된 원소가 없습니다.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sheet.elementLinks.forEach { link ->
                    val displayName = link.displayName?.takeIf { it.isNotBlank() } ?: link.symbol
                    AssistChip(
                        onClick = { onEvent(GameEvent.OpenElement(link.atomicNumber)) },
                        modifier = Modifier.semantics {
                            contentDescription = "$displayName 상세 보기"
                        },
                        label = {
                            Text(
                                text = if (link.count > 1) "${link.symbol} ×${link.count}" else link.symbol,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }

        // 관련 용어
        Text("관련 용어", style = MaterialTheme.typography.titleMedium)
        if (sheet.glossaryLinks.isEmpty()) {
            Text("연결된 용어가 없습니다.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sheet.glossaryLinks.forEach { link ->
                    AssistChip(
                        onClick = { onEvent(GameEvent.OpenGlossary(link.termId)) },
                        modifier = Modifier.semantics {
                            contentDescription = "${link.label} 용어 보기"
                        },
                        label = {
                            Text(
                                text = link.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 닫기
        OutlinedButton(
            onClick = { onEvent(GameEvent.CloseMoleculeSheet) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("닫기")
        }
    }
}
