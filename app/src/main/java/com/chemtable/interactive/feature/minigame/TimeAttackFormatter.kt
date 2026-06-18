package com.chemtable.interactive.feature.minigame

internal fun formatTimeAttackClock(timeLeftMillis: Long?): String {
    val totalSeconds = timeAttackSeconds(timeLeftMillis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

internal fun formatTimeAttackAccessibilityTime(timeLeftMillis: Long?): String {
    val totalSeconds = timeAttackSeconds(timeLeftMillis)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}분 ${seconds}초"
}

private fun timeAttackSeconds(timeLeftMillis: Long?): Long =
    ((timeLeftMillis ?: 0L).coerceAtLeast(0L) + 999L) / 1_000L
