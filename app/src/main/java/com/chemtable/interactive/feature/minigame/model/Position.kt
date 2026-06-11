package com.chemtable.interactive.feature.minigame.model

/**
 * 보드 좌표. row 0 = 최상단, row 가 클수록 화면 아래쪽(중력 방향).
 * col 0 = 왼쪽.
 */
data class Position(val row: Int, val col: Int)
