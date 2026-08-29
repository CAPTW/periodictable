package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReactorFoundationComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun foundationAtLargestAppFontExposesAuthorityCopyLegendExactCellsAndNoProhibitedUi() {
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
                        onNavigateBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("reactor_foundation_screen").assertExists()
        composeRule.onNodeWithText("분자 반응조 · 기초 실험").assertExists()
        composeRule.onNodeWithText("실험적 기초 기능", substring = true).assertExists()
        composeRule.onNodeWithText(
            "침강 지수는 몰질량을 바탕으로 한 게임용 단순화입니다.",
            substring = true,
        ).assertExists()
        composeRule.onNodeWithContentDescription("침강 지수 게임 모델 안내", substring = true)
            .assertExists()
        listOf(
            Triple("↑ 부유", 12, 11),
            Triple("• 중립", 1, 0),
            Triple("↓ 침강", 2, 1),
        ).forEach { (label, screenCount, boardCount) ->
            composeRule.onAllNodesWithText(label, useUnmergedTree = true)
                .assertCountEquals(screenCount)
            composeRule.onAllNodes(
                hasText(label) and hasAnyAncestor(hasTestTag("reactor_board")),
                useUnmergedTree = true,
            ).assertCountEquals(boardCount)
        }
        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true).assertExists()
        composeRule.onAllNodes(ReactorCellTagMatcher, useUnmergedTree = true)
            .assertCountEquals(25)
        composeRule.onNodeWithTag("reactor_cell_4_4", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("reactor_reset_sample").assertExists()
        composeRule.onNodeWithTag("reactor_turn_label").assertTextEquals("턴 0")
        composeRule.onNodeWithTag("reactor_phase_label").assertTextEquals("침강 단계 0")

        listOf("점수", "압력", "아이템", "광고", "결제").forEach { forbidden ->
            composeRule.onAllNodesWithText(forbidden, substring = true).assertCountEquals(0)
        }
    }

    @Test
    fun onePhysicalSwipePublishesOneVerifiedTurnAndResetRestoresInitialState() {
        val session = session()
        var swipeCount = 0
        composeRule.setContent {
            var state by mutableStateOf(session.state.toUiState())
            ChemTableTheme {
                ReactorFoundationContent(
                    state = state,
                    onSwipe = { direction ->
                        swipeCount += 1
                        session.swipe(direction)
                        state = session.state.toUiState()
                    },
                    onReset = {
                        session.reset()
                        state = session.state.toUiState()
                    },
                    onEntitySelected = {},
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true)
            .performScrollTo()
            .performTouchInput { swipeLeft() }

        assertEquals(1, swipeCount)
        composeRule.onNodeWithTag("reactor_turn_label").assertTextEquals("턴 1")
        composeRule.onNodeWithTag("reactor_phase_label").assertTextEquals("침강 단계 1")
        val eventLogDescendant = hasAnyAncestor(hasTestTag("reactor_event_log"))
        composeRule.onAllNodes(
            hasText("결합", substring = true) and eventLogDescendant,
            useUnmergedTree = true,
        ).assertCountEquals(4)
        listOf("H2", "O2", "N2", "NaCl").forEach { formula ->
            composeRule.onAllNodes(
                hasText("결합 $formula:", substring = true) and eventLogDescendant,
                useUnmergedTree = true,
            ).assertCountEquals(1)
        }

        composeRule.onNodeWithTag("reactor_reset_sample").performScrollTo().performClick()

        composeRule.onNodeWithTag("reactor_turn_label").assertTextEquals("턴 0")
        composeRule.onNodeWithTag("reactor_phase_label").assertTextEquals("침강 단계 0")
        composeRule.onAllNodes(
            hasText("아직 실행된 이벤트가 없습니다", substring = true) and eventLogDescendant,
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }

    @Test
    fun occupiedCellSemanticsAndSelectionExposeNonColorOnlyGameModelDetails() {
        val session = session()
        composeRule.setContent {
            var state by mutableStateOf(session.state.toUiState())
            ChemTableTheme {
                ReactorFoundationContent(
                    state = state,
                    onSwipe = {},
                    onReset = {},
                    onEntitySelected = { entityId ->
                        session.selectEntity(entityId)
                        state = session.state.toUiState()
                    },
                    onNavigateBack = {},
                )
            }
        }

        val firstHydrogenCell = composeRule.onNodeWithTag("reactor_cell_0_0", useUnmergedTree = true)
        firstHydrogenCell
            .assertContentDescriptionContains("1행 1열", substring = true)
            .assertContentDescriptionContains("수소 H", substring = true)
            .assertContentDescriptionContains("위로 부유하는 게임 블록", substring = true)
        composeRule.onNodeWithTag("reactor_cell_0_2", useUnmergedTree = true)
            .assertContentDescriptionContains("1행 3열", substring = true)
            .assertContentDescriptionContains("수소 H", substring = true)
            .assertContentDescriptionContains("위로 부유하는 게임 블록", substring = true)
        firstHydrogenCell.performClick()

        composeRule.onNodeWithTag("reactor_entity_detail").assertExists()
        composeRule.onNodeWithText("수소 · H", substring = true).assertExists()
        composeRule.onNode(
            hasText("게임용 단순화", substring = true) and
                hasAnyAncestor(hasTestTag("reactor_entity_detail")),
        ).assertExists()
    }

    private fun ReactorFoundationSessionState.toUiState() = ReactorFoundationUiState(
        board = board,
        latestEvents = latestEvents,
        selectedEntityId = selectedEntityId,
        lastReplayVerified = lastReplayVerified,
        errorMessage = errorMessage,
        isLoading = false,
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

    private companion object {
        val ReactorCellTagMatcher = SemanticsMatcher("reactor cell test tag") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)
                ?.startsWith("reactor_cell_") == true
        }
    }
}
