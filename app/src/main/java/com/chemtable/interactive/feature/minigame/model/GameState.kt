package com.chemtable.interactive.feature.minigame.model

/** 게임 화면 단계. 결과/일시정지는 별도 라우트가 아니라 이 phase 로 표현한다(Phase 0 spec). */
enum class GamePhase { INTRO, PLAYING, PAUSED, RESULT }

/** Mission 난이도. Phase 3B.1 부터 Intro 선택과 config 분리에 사용한다. */
enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

/** 목표 분자 미션. 예: H2O 를 2개 만들기. */
data class MissionTarget(
    val formula: String,
    val count: Int,
    val progress: Int = 0,
) {
    val isComplete: Boolean get() = progress >= count
}

/** 플레이 중 분자 블록을 탭했을 때 표시할 Action Sheet 전용 정보 모델. */
data class SelectedMoleculeSheet(
    val blockId: Long,
    val formula: String,
    val molarMass: Double,
    val elementLinks: List<MoleculeElementLink>,
    val glossaryLinks: List<MoleculeGlossaryLink>
)

/** 게임 화면 UI 상태(단일 소스). */
data class GameUiState(
    val phase: GamePhase,
    val board: BoardState,
    val score: Int,
    val combo: Int,
    val missionTarget: MissionTarget?,
    val movesLeft: Int?,
    val discoveredMolecules: List<DiscoveredMolecule>,
    val showTutorial: Boolean,
    val isEngineReady: Boolean,
    val resultSuccess: Boolean,
    val difficulty: Difficulty,
    val selectedMoleculeSheet: SelectedMoleculeSheet? = null,
) {
    companion object {
        fun initial(): GameUiState = GameUiState(
            phase = GamePhase.INTRO,
            board = BoardState.empty(4),
            score = 0,
            combo = 0,
            missionTarget = null,
            movesLeft = null,
            discoveredMolecules = emptyList(),
            showTutorial = false,
            isEngineReady = false,
            resultSuccess = false,
            difficulty = Difficulty.BEGINNER,
            selectedMoleculeSheet = null,
        )
    }
}
