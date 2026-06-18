package com.chemtable.interactive.feature.minigame

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeAttackFormatterTest {

    @Test
    fun formatTimeAttackClock_formatsMillisAsMinutesAndSeconds() {
        assertEquals("02:00", formatTimeAttackClock(120_000L))
        assertEquals("01:30", formatTimeAttackClock(90_000L))
        assertEquals("00:06", formatTimeAttackClock(5_900L))
        assertEquals("00:01", formatTimeAttackClock(999L))
        assertEquals("00:00", formatTimeAttackClock(0L))
    }

    @Test
    fun formatTimeAttackClock_clampsNegativeMillisToZero() {
        assertEquals("00:00", formatTimeAttackClock(-1L))
    }

    @Test
    fun formatTimeAttackAccessibilityTime_returnsPlainKoreanTime() {
        assertEquals("2분 0초", formatTimeAttackAccessibilityTime(120_000L))
        assertEquals("1분 30초", formatTimeAttackAccessibilityTime(90_000L))
        assertEquals("0분 6초", formatTimeAttackAccessibilityTime(5_900L))
    }
}
