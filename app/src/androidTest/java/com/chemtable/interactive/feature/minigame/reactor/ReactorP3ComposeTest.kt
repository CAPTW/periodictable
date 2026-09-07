package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementSpecification
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorOperationalState
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val ForbiddenActionableMonetizationPhrases = listOf(
    "광고 보기",
    "광고 시청",
    "보상형 광고",
    "보상 광고",
    "광고로 복구",
    "광고로 계속",
    "Watch ad",
    "Rewarded ad",
    "구매",
    "결제",
    "구매로 복구",
    "결제로 복구",
    "Buy",
    "Purchase",
    "상점",
    "스토어",
    "Store",
    "충전",
    "아이템으로 복구",
    "아이템",
    "광고",
    "Paywall",
)

class ReactorP3ComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun feedPreviewAndPressureSemanticsAreDiscoverableWithoutColorAlone() {
        val session = session()
        composeRule.setContent {
            ChemTableTheme {
                ReactorFoundationContent(
                    state = session.state.toUiState(),
                    onSwipe = {},
                    onReset = {},
                    onEntitySelected = {},
                    onEmergencyVent = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.onNodeWithTag("reactor_feed_rail").assertExists()
        composeRule.onNodeWithTag("reactor_feed_preview_0")
            .assertContentDescriptionContains("대기 H", substring = true)
        composeRule.onNodeWithTag("reactor_feed_preview_1")
            .assertContentDescriptionContains("O", substring = true)
        composeRule.onNodeWithTag("reactor_feed_preview_2")
            .assertContentDescriptionContains("H", substring = true)
        composeRule.onNodeWithTag("reactor_pressure_line")
            .assertContentDescriptionContains("압력 45 / 100", substring = true)
            .assertContentDescriptionContains("정상", substring = true)
            .assertContentDescriptionContains("윗줄 점유", substring = true)
        composeRule.onNodeWithText("다음 공급").assertExists()
        composeRule.onNodeWithText("압력 45 / 100").assertExists()
        composeRule.onNodeWithText(ReactorPressureDisclaimer, substring = true).assertExists()
        composeRule.onNodeWithText(ReactorFoundationDisclaimer, substring = true).assertExists()
    }

    @Test
    fun successfulTurnPlacesDeterministicFeedAndAdvancesPreview() {
        val session = session()
        composeRule.setContent {
            var state by mutableStateOf(session.state.toUiState())
            ChemTableTheme {
                ReactorFoundationContent(
                    state = state,
                    onSwipe = { direction ->
                        session.swipe(direction)
                        state = session.state.toUiState()
                    },
                    onReset = {},
                    onEntitySelected = {},
                    onEmergencyVent = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true)
            .performScrollTo()
            .performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("reactor_turn_label").assertTextEquals("턴 1")
        assertTrue(session.state.latestEvents.any { it is ReactorTurnEvent.FeedPlaced })
        assertEquals(1, session.state.feedCursor)
        assertEquals("O", session.state.pendingFeed.symbol)
        composeRule.onNodeWithTag("reactor_feed_preview_0")
            .assertContentDescriptionContains("대기 O", substring = true)
    }

    @Test
    fun overflowShowsLockAndEmergencyVentWithoutPaywall() {
        val session = session()
        forceOverflow(session)
        var swipeCalls = 0
        composeRule.setContent {
            var state by mutableStateOf(session.state.toUiState())
            ChemTableTheme {
                ReactorFoundationContent(
                    state = state,
                    onSwipe = { direction ->
                        swipeCalls += 1
                        session.swipe(direction)
                        state = session.state.toUiState()
                    },
                    onReset = {},
                    onEntitySelected = {},
                    onEmergencyVent = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.onNodeWithText("반응조 오버플로").assertIsDisplayed()
        composeRule.onNodeWithText("긴급 배출은 광고나 결제 없이 윗줄 혼잡을 제거합니다.")
            .assertExists()
        composeRule.onNodeWithText("긴급 배출").assertExists()
        composeRule.onNodeWithTag("reactor_emergency_vent")
            .assertExists()
            .assertIsEnabled()
            .assertHasClickAction()
        composeRule.onNodeWithTag("reactor_reset_sample").assertExists()
        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true)
            .assertContentDescriptionContains("스와이프 잠김", substring = true)
        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true)
            .performScrollTo()
            .performTouchInput { swipeLeft() }
        assertEquals(0, swipeCalls)
        assertEquals(ReactorOperationalState.OVERFLOW, session.state.operationalState)
        ForbiddenActionableMonetizationPhrases.forEach { phrase ->
            composeRule.onAllNodes(
                hasClickAction() and hasText(phrase, substring = true),
                useUnmergedTree = true,
            ).assertCountEqualsSafe(0)
        }
    }

    @Test
    fun emergencyVentPreservesPendingFeedAndUnlocksBoard() {
        val session = session()
        forceOverflow(session)
        val pending = session.state.pendingFeed.symbol
        val cursor = session.state.feedCursor
        composeRule.setContent {
            var state by mutableStateOf(session.state.toUiState())
            ChemTableTheme {
                ReactorFoundationContent(
                    state = state,
                    onSwipe = {},
                    onReset = {},
                    onEntitySelected = {},
                    onEmergencyVent = {
                        session.emergencyVent()
                        state = session.state.toUiState()
                    },
                    onNavigateBack = {},
                )
            }
        }
        composeRule.onNodeWithTag("reactor_emergency_vent").performScrollTo().performClick()
        assertEquals(ReactorOperationalState.ACTIVE, session.state.operationalState)
        assertEquals(pending, session.state.pendingFeed.symbol)
        assertEquals(cursor, session.state.feedCursor)
        assertTrue(session.state.pressure < 100)
        assertEquals(1, session.state.recoveryCount)
        composeRule.onNodeWithText("이벤트 재생 미검증").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("이벤트 재생 검증 완료").assertDoesNotExist()
        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true)
            .assertContentDescriptionContains("정확히 25칸", substring = true)
    }

    @Test
    fun resetClearsFeedPressureAndCounters() {
        val session = session()
        session.swipe(ReactorDirection.LEFT)
        composeRule.setContent {
            var state by mutableStateOf(session.state.toUiState())
            ChemTableTheme {
                ReactorFoundationContent(
                    state = state,
                    onSwipe = {},
                    onReset = {
                        session.reset()
                        state = session.state.toUiState()
                    },
                    onEntitySelected = {},
                    onEmergencyVent = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.onNodeWithTag("reactor_reset_sample").performScrollTo().performClick()
        assertEquals(0, session.state.feedCursor)
        assertEquals(0, session.state.successfulFeedSerial)
        assertEquals(0, session.state.failureCount)
        assertEquals(0, session.state.recoveryCount)
        assertEquals(ReactorOperationalState.ACTIVE, session.state.operationalState)
        assertEquals(listOf("H", "O", "H"), session.state.feedPreview.map { it.symbol })
        composeRule.onNodeWithText("압력 45 / 100").assertExists()
        composeRule.onNodeWithTag("reactor_turn_label").assertTextEquals("턴 0")
    }

    @Test
    fun largestAppFontKeepsFeedAndPressureReadable() {
        val session = session()
        composeRule.setContent {
            val systemDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, 1.30f)) {
                ChemTableTheme {
                    ReactorFoundationContent(
                        state = session.state.toUiState(),
                        onSwipe = {},
                        onReset = {},
                        onEntitySelected = {},
                        onEmergencyVent = {},
                        onNavigateBack = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag("reactor_feed_rail").assertExists()
        composeRule.onNodeWithTag("reactor_pressure_line").assertExists()
        composeRule.onNodeWithText("정상").assertExists()
        composeRule.onNodeWithText("다음 공급").assertExists()
        composeRule.onNodeWithText(ReactorPressureDisclaimer, substring = true).assertExists()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEqualsSafe(
        expected: Int,
    ) {
        assertEquals(expected, fetchSemanticsNodes().size)
    }

    private fun forceOverflow(session: ReactorFoundationSession) {
        var turns = 0
        val directions = listOf(
            ReactorDirection.LEFT,
            ReactorDirection.RIGHT,
            ReactorDirection.UP,
            ReactorDirection.DOWN,
        )
        while (turns < 30 && session.state.operationalState != ReactorOperationalState.OVERFLOW) {
            session.swipe(directions[turns % directions.size])
            turns += 1
        }
        assertEquals(ReactorOperationalState.OVERFLOW, session.state.operationalState)
        assertEquals(100, session.state.pressure)
    }

    private fun ReactorFoundationSessionState.toUiState() = ReactorFoundationUiState(
        board = board,
        latestEvents = latestEvents,
        selectedEntityId = selectedEntityId,
        lastReplayVerified = lastReplayVerified,
        errorMessage = errorMessage,
        isLoading = false,
        feedPreview = feedPreview,
        pendingFeed = pendingFeed,
        pressure = pressure,
        pressureBand = pressureBand,
        pressureBreakdown = pressureBreakdown,
        operationalState = operationalState,
        failureCount = failureCount,
        recoveryCount = recoveryCount,
    )

    private fun session(): ReactorFoundationSession {
        val elements = listOf(
            ReactorElementSpecification(1, "H", "수소", 1.008),
            ReactorElementSpecification(6, "C", "탄소", 12.011),
            ReactorElementSpecification(7, "N", "질소", 14.007),
            ReactorElementSpecification(8, "O", "산소", 15.999),
            ReactorElementSpecification(11, "Na", "나트륨", 22.99),
            ReactorElementSpecification(17, "Cl", "염소", 35.45),
        )
        val masses = mapOf(
            "H2" to 2.016,
            "O2" to 31.998,
            "N2" to 28.014,
            "H2O" to 18.015,
            "CO2" to 44.009,
            "NaCl" to 58.44,
        )
        return ReactorFoundationSession(
            elementCatalog = ReactorElementCatalog { symbol ->
                elements.firstOrNull { it.symbol == symbol }
            },
            massAuthority = ReactorMassAuthority { product ->
                requireNotNull(masses[product.formula])
            },
            settlingProfile = MassReferenceSettlingProfile(),
        )
    }
}
