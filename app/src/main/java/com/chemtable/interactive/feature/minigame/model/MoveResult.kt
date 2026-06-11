package com.chemtable.interactive.feature.minigame.model

/**
 * 한 번의 move(swipe) 처리 결과.
 *
 * - moved: 유효한 이동(압착/조합으로 보드가 바뀜)이었는지. false 면 spawn 없음.
 * - mergedFormulas: 이번 move 에서 생성된 분자 화학식 목록.
 * - spawned / spawnedPosition: 유효 move 후 새 원소가 스폰되었는지와 위치.
 */
data class MoveResult(
    val board: BoardState,
    val moved: Boolean,
    val mergedFormulas: List<String>,
    val gainedScore: Int,
    val spawned: Boolean,
    val spawnedPosition: Position?,
    val isGameOver: Boolean,
)

/**
 * 세션 종료 결과(최소 형태). Phase 1B 의 Result overlay 에서 사용 예정.
 */
data class GameResult(
    val finalScore: Int,
    val success: Boolean,
    val moleculesMade: List<String>,
)
