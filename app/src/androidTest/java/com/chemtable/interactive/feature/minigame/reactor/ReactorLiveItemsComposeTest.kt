package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import com.chemtable.interactive.feature.minigame.reactor.engine.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReactorLiveItemsComposeTest {
    @get:Rule val compose = createComposeRule()
    private fun session() = ReactorFoundationSession(ReactorElementCatalog { symbol ->
        val i=ReactorFeedSchedule.SYMBOLS.indexOf(symbol)
        if(i<0)null else ReactorElementSpecification(ReactorFeedSchedule.ATOMIC_NUMBERS[i],symbol,symbol,if(symbol=="H")1.0 else 32.0)
    },ReactorMassAuthority{32.0},MassReferenceSettlingProfile())
    private fun ui(s: ReactorFoundationSessionState) = ReactorFoundationUiState(
        board=s.board,isLoading=false,latestEvents=s.latestEvents,lastReplayVerified=s.lastReplayVerified,
        errorMessage=s.errorMessage,itemActionsRemaining=s.itemActionsRemaining,pressure=s.pressure,
        itemRechargeProgress=s.itemRechargeProgress,itemLearningMessage=s.itemLearningMessage,
        pressureBand=s.pressureBand,pressureBreakdown=s.pressureBreakdown,operationalState=s.operationalState,
        pendingFeed=s.pendingFeed,feedPreview=s.feedPreview,failureCount=s.failureCount,recoveryCount=s.recoveryCount)
    private fun show(s: ReactorFoundationSession) {
        compose.setContent {
            var state by remember { mutableStateOf(ui(s.state)) }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.3f)) {
                ChemTableTheme {
                    ReactorFoundationContent(state=state,onSwipe={s.swipe(it);state=ui(s.state)},
                        onReset={s.reset();state=ui(s.state)},onEntitySelected={},onNavigateBack={},
                        onLoadItemSample={s.loadItemSample();state=ui(s.state)},
                        onLoadAdvancedItemSample={s.loadAdvancedItemSample();state=ui(s.state)},
                        onClaimItemRecharge={s.claimItemRecharge();state=ui(s.state)},
                        onUseItem={s.useItem(it);state=ui(s.state)},onEmergencyVent={s.emergencyVent();state=ui(s.state)})
                }
            }
        }
    }
    private fun click(tag:String) { compose.onNodeWithTag(tag).performScrollTo().performClick() }
    @Test fun actualSessionItemCommandsShareTurnFeedPressureAndSwipe() {
        val s=session();show(s);click("p5_live_open");click("p5_live_sample")
        click("p5_live_cell_0");click("p5_live_cell_2");click("p5_live_link")
        compose.onNodeWithTag("p5_live_status").assertTextContains("턴 1",substring=true).assertTextContains("무료 자원 5/6",substring=true)
        assertEquals(1,s.state.feedCursor);assertTrue(s.state.lastReplayVerified)
        click("p5_live_cell_0");click("p5_live_cell_2");click("p5_live_enzyme_B")
        assertEquals(1,s.state.board.turnIndex);assertEquals(5,s.state.itemActionsRemaining)
        click("p5_live_enzyme_A")
        assertEquals(2,s.state.board.turnIndex);assertEquals(2,s.state.feedCursor);assertEquals(4,s.state.itemActionsRemaining)
        compose.onNodeWithTag("p5_live_result").assertTextEquals("이벤트 재생 검증 완료")
        click("p5_live_close")
        compose.onNodeWithTag("reactor_turn_label").assertTextEquals("턴 2")
        compose.onNodeWithTag("reactor_board",useUnmergedTree=true).performScrollTo().performTouchInput{swipeLeft()}
        compose.runOnIdle { assertEquals(3,s.state.board.turnIndex);assertTrue(s.state.lastReplayVerified);assertEquals(4,s.state.itemActionsRemaining) }
    }
    @Test fun overflowDisablesItemsAndExistingFreeVentStillRecovers() {
        val s=session();s.loadItemSample()
        repeat(30) { if(s.state.operationalState!=ReactorOperationalState.OVERFLOW)s.swipe(ReactorDirection.UP) }
        assertEquals(ReactorOperationalState.OVERFLOW,s.state.operationalState)
        show(s);click("p5_live_open");click("p5_live_cell_0");click("p5_live_cell_2")
        compose.onNodeWithTag("p5_live_link").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("p5_live_enzyme_A").performScrollTo().assertIsNotEnabled()
        click("p5_live_close");click("reactor_emergency_vent")
        compose.runOnIdle { assertEquals(ReactorOperationalState.ACTIVE,s.state.operationalState);assertEquals(6,s.state.itemActionsRemaining) }
    }

    @Test fun earnedFreeResourceClaimKeepsLiveBoardAtLargeFont() {
        val s=session();s.loadItemSample();s.useItem(ReactorItemCommand.Link(0,2))
        s.swipe(ReactorDirection.DOWN);s.swipe(ReactorDirection.LEFT);s.swipe(ReactorDirection.RIGHT)
        assertEquals(3,s.state.itemRechargeProgress);val before=s.state
        show(s);click("p5_live_open")
        compose.onNodeWithTag("p5_learning_guide").performScrollTo().assertIsDisplayed()
        click("p5_recharge_claim")
        compose.runOnIdle { assertEquals(before.copy(itemActionsRemaining=6,itemRechargeProgress=0),s.state) }
        compose.onNodeWithTag("p5_recharge_claim").assertIsNotEnabled()
        compose.onNodeWithTag("p5_recharge_progress").assertTextEquals("무료 보급 준비 0/3")
    }

    @Test fun advancedPublicSampleSupportsFourUnitLearningLoop() {
        val s=session();show(s);click("p5_live_open");click("p5_live_advanced")
        compose.onNodeWithTag("p5_live_cell_0").performScrollTo().assertTextContains("A3")
        click("p5_live_cell_0");click("p5_live_cell_2");click("p5_live_link")
        compose.onNodeWithTag("p5_live_cell_0").performScrollTo().assertTextContains("A4")
        click("p5_live_cell_0");click("p5_live_cell_2");click("p5_live_enzyme_A")
        compose.onNodeWithTag("p5_live_cell_0").performScrollTo().assertTextContains("A3")
        compose.onNodeWithTag("p5_live_cell_2").assertTextContains("A1")
        compose.runOnIdle { assertEquals(2,s.state.board.turnIndex);assertEquals(4,s.state.itemActionsRemaining);assertTrue(s.state.lastReplayVerified) }
    }
}
