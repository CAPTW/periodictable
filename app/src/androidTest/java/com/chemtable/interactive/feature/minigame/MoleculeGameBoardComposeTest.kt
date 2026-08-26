package com.chemtable.interactive.feature.minigame

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import com.chemtable.interactive.core.model.ClassicBoardSize
import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameBlock
import com.chemtable.interactive.feature.minigame.model.GameEvent
import com.chemtable.interactive.feature.minigame.model.GamePhase
import com.chemtable.interactive.feature.minigame.model.GameUiState
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoleculeGameBoardComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fourByFourRendersExactlySixteenTaggedCells() = assertBoardCellCount(
        boardSize = ClassicBoardSize.FOUR_BY_FOUR,
        expectedCount = 16,
    )

    @Test
    fun fiveByFiveRendersExactlyTwentyFiveTaggedCells() = assertBoardCellCount(
        boardSize = ClassicBoardSize.FIVE_BY_FIVE,
        expectedCount = 25,
    )

    @Test
    fun sixBySixRendersExactlyThirtySixTaggedCells() = assertBoardCellCount(
        boardSize = ClassicBoardSize.SIX_BY_SIX,
        expectedCount = 36,
    )

    @Test
    fun introExposesAllBoardSizesAndSelectedSemanticsAtLargestAppFontScale() {
        composeRule.setContent {
            val systemDensity = LocalDensity.current
            var state by mutableStateOf(
                GameUiState.initial().copy(isEngineReady = true)
            )
            CompositionLocalProvider(
                LocalDensity provides Density(systemDensity.density, 1.30f)
            ) {
                ChemTableTheme {
                    MoleculeGameContent(
                        state = state,
                        onEvent = { event ->
                            if (event is GameEvent.SelectBoardSize) {
                                state = state.copy(
                                    boardSize = event.boardSize,
                                    board = BoardState.empty(event.boardSize),
                                )
                            }
                        },
                        onExit = {},
                    )
                }
            }
        }

        listOf(4, 5, 6).forEach { dimension ->
            composeRule.onNodeWithTag("molecule_game_board_size_$dimension")
                .assertExists()
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag("molecule_game_board_size_4").assertIsSelected()

        composeRule.onNodeWithTag("molecule_game_board_size_6").performClick()

        composeRule.onNodeWithTag("molecule_game_board_size_6").assertIsSelected()
    }

    @Test
    fun sixBySixCompactCellsRetainFullAccessibleDescriptions() {
        val rows = MutableList(6) { MutableList<GameBlock?>(6) { null } }
        rows[0][0] = ElementBlock(1L, 1, "H", "수소", 1.008)
        rows[0][1] = MoleculeBlock(2L, "H2O", 18.015, mapOf("H" to 2, "O" to 1), "물")
        val board = BoardState(ClassicBoardSize.SIX_BY_SIX, rows.map { it.toList() })
        setPlayingContent(board)

        composeRule.onNodeWithTag("molecule_game_cell_0_0", useUnmergedTree = true)
            .assertContentDescriptionEquals("1행 1열, 수소 원소 블록, H, 1.0 g/mol")
        composeRule.onNodeWithTag("molecule_game_cell_0_1", useUnmergedTree = true)
            .assertContentDescriptionEquals("1행 2열, 물 분자 블록, H2O, 18.0 g/mol")
        composeRule.onNodeWithTag(MoleculeGameBoardTestTag, useUnmergedTree = true)
            .assertContentDescriptionEquals("6×6 분자 게임 보드, 36칸")
    }

    @Test
    fun onePhysicalSwipeDispatchesExactlyOneMoveEvent() {
        val events = mutableListOf<GameEvent>()
        composeRule.setContent {
            ChemTableTheme {
                MoleculeGameContent(
                    state = playingState(BoardState.empty(ClassicBoardSize.FIVE_BY_FIVE)),
                    onEvent = { events += it },
                    onExit = {},
                )
            }
        }

        composeRule.onNodeWithTag(MoleculeGameBoardTestTag, useUnmergedTree = true)
            .performTouchInput { swipeLeft() }

        assertEquals(listOf(GameEvent.Swipe(Direction.LEFT)), events.filterIsInstance<GameEvent.Swipe>())
    }

    private fun assertBoardCellCount(boardSize: ClassicBoardSize, expectedCount: Int) {
        setPlayingContent(BoardState.empty(boardSize))

        composeRule.onNodeWithTag(MoleculeGameBoardTestTag, useUnmergedTree = true).assertExists()
        composeRule.onAllNodes(BoardCellTagMatcher, useUnmergedTree = true)
            .assertCountEquals(expectedCount)
        composeRule.onNodeWithTag(
            "molecule_game_cell_${boardSize.dimension - 1}_${boardSize.dimension - 1}",
            useUnmergedTree = true,
        ).assertExists()
    }

    private fun setPlayingContent(board: BoardState) {
        composeRule.setContent {
            ChemTableTheme {
                MoleculeGameContent(
                    state = playingState(board),
                    onEvent = {},
                    onExit = {},
                )
            }
        }
    }

    private fun playingState(board: BoardState): GameUiState = GameUiState.initial().copy(
        phase = GamePhase.PLAYING,
        boardSize = board.boardSize,
        board = board,
        isEngineReady = true,
        showTutorial = false,
    )

    private companion object {
        val BoardCellTagMatcher = SemanticsMatcher("molecule game cell test tag") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)
                ?.startsWith("molecule_game_cell_") == true
        }
    }
}
