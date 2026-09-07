package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReactorSupplyComposeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun claimAt130PercentFontShowsExactRewardAndPreventsDuplicate() {
        var state by mutableStateOf(ReactorSupplyUiState(quantity = 0))
        var claims = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                ChemTableTheme {
                    ReactorSupplyEntry(state, onClaim = {
                        claims++
                        state = ReactorSupplyUiState(quantity = 1, claimed = true)
                    }, onReload = {})
                }
            }
        }
        compose.onNodeWithTag("reactor_supply_open").performClick()
        compose.onNodeWithText("첫 무료 보급: 연습 조각 1개").assertExists()
        compose.onNodeWithTag("reactor_supply_claim").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("reactor_supply_balance").assertTextContains("보관 중: 연습 조각 1개")
        compose.onNodeWithTag("reactor_supply_claim").assertIsNotEnabled()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithTag("reactor_supply_open").performClick()
        compose.onNodeWithTag("reactor_supply_claim").performScrollTo().assertIsNotEnabled()
        assertEquals(1, claims)
    }

    @Test fun failedStorageOffersRetryWithoutInventingZeroBalance() {
        var retries = 0
        compose.setContent {
            ChemTableTheme {
                ReactorSupplyEntry(ReactorSupplyUiState(error = true), {}, { retries++ })
            }
        }
        compose.onNodeWithTag("reactor_supply_open").performClick()
        compose.onNodeWithTag("reactor_supply_balance").assertTextContains("다시 확인해 주세요.", substring = true)
        compose.onNodeWithTag("reactor_supply_claim").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("reactor_supply_retry").performScrollTo().performClick()
        assertEquals(2, retries) // Opening refresh + explicit retry.
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithTag("reactor_supply_open").assertExists()
    }

    @Test fun pendingSaveDisablesClaimButDoesNotTrapPlayerInDialog() {
        compose.setContent { ChemTableTheme { ReactorSupplyEntry(ReactorSupplyUiState(busy = true), {}, {}) } }
        compose.onNodeWithTag("reactor_supply_open").performClick()
        compose.onNodeWithTag("reactor_supply_claim").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithTag("reactor_supply_open").assertExists()
    }
}
