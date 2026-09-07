package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.ReactorFoundationSession
import com.chemtable.interactive.feature.minigame.reactor.adapter.ClassicRecipeBookReactorAdapter
import com.chemtable.interactive.feature.minigame.reactor.model.*
import org.junit.Assert.*
import org.junit.Test

class ReactorItemTurnResolverTest {
    private val profile = MassReferenceSettlingProfile()
    private val catalog = ReactorElementCatalog { symbol ->
        val i = ReactorFeedSchedule.SYMBOLS.indexOf(symbol)
        if(i<0) null else ReactorElementSpecification(ReactorFeedSchedule.ATOMIC_NUMBERS[i],symbol,symbol,if(symbol=="H")1.0 else 32.0)
    }
    private val engine = ReactorBoardEngine(ClassicRecipeBookReactorAdapter(),ReactorMassAuthority {32.0},profile,DeterministicReactorEntityIdFactory())
    private val p3 = ReactorP3Orchestrator(engine,catalog,profile)
    private val resolver = ReactorItemTurnResolver(p3,profile)
    private val before = ReactorP3ReplayContext(0,0,ReactorOperationalState.ACTIVE,0,0)
    private val link = ReactorItemCommand.Link(0,2)
    private fun context(r: ReactorP3TurnResult) = ReactorP3ReplayContext(r.feedCursor,r.successfulFeedSerial,r.operational.state,r.operational.failureCount,r.recoveryCount)
    private fun units(b: ReactorBoardState) = b.entityStore.entities.values.filterIsInstance<ReactorPolymerEntity>().groupBy{it.substrate}.mapValues{(_,v)->v.sumOf{it.units}}
    private fun session()=ReactorFoundationSession(catalog,ReactorMassAuthority{32.0},profile)

    @Test fun linkTicksFeedsAndReplaysWithConservedSubstrates() {
        val initial=ReactorItemTurnResolver.sampleBoard();val r=resolver.resolve(initial,link,before,6)
        assertEquals(1,r.continuation.board.turnIndex);assertEquals(1,r.continuation.board.settlingPhase)
        assertEquals(5,r.remainingActions);assertEquals(1,r.continuation.feedCursor)
        assertEquals(1,r.continuation.successfulFeedSerial)
        assertEquals(1,r.events.filterIsInstance<ReactorTurnEvent.FeedPlaced>().size)
        assertEquals(units(initial),units(r.continuation.board))
        assertEquals(ReactorPressureEvaluator.evaluate(r.continuation.board,false),r.continuation.pressure)
        assertTrue(resolver.validate(initial,link,before,6,r));assertTrue(r.events.first() is ReactorTurnEvent.ItemApplied)
    }
    @Test fun cleavageAndSwipeUseSameLiveBoardAndReplay() {
        val initial=ReactorItemTurnResolver.sampleBoard();val linked=resolver.resolve(initial,link,before,6)
        val command=ReactorItemCommand.Cleave(0,2,ReactorSubstrate.A)
        val split=resolver.resolve(linked.continuation.board,command,context(linked.continuation),5)
        assertTrue(resolver.validate(linked.continuation.board,command,context(linked.continuation),5,split))
        assertEquals(2,split.continuation.board.turnIndex);assertEquals(0,split.continuation.board.settlingPhase)
        val t=split.continuation
        val swipe=p3.resolveTurn(t.board,ReactorDirection.DOWN,t.operational.state,t.feedCursor,t.successfulFeedSerial,0,0)
        assertEquals(units(initial),units(swipe.board));assertTrue(swipe.board.validate().isValid)
        assertTrue(ReactorP3EventReplayer().validate(t.board,swipe,context(t)).isValid)
    }
    @Test fun forgedCostEffectFeedPressurePhaseAndCommandAreRejected() {
        val initial=ReactorItemTurnResolver.sampleBoard();val r=resolver.resolve(initial,link,before,6);val t=r.continuation
        listOf(r.copy(remainingActions=6),r.copy(itemEvent=r.itemEvent.copy(actionsAfter=6)),
            r.copy(itemEvent=r.itemEvent.copy(boardAfterEffect=initial)),r.copy(continuation=t.copy(events=t.events.reversed())),
            r.copy(continuation=t.copy(events=t.events.drop(1))),r.copy(continuation=t.copy(feedCursor=9)),r.copy(continuation=t.copy(pressure=t.pressure.copy(pressure=99))),
            r.copy(continuation=t.copy(p2=t.p2.copy(nextPhase=0)))).forEach{assertFalse(resolver.validate(initial,link,before,6,it))}
        assertFalse(resolver.validate(initial,link,before.copy(feedCursor=1),6,r))
        assertFalse(resolver.validate(initial,ReactorItemCommand.Link(2,0),before,6,r))
        assertFalse(resolver.validate(initial,link,before,5,r))
        assertFalse(ReactorEventReplayer().validate(r.itemEvent.boardAfterEffect,t.p2.copy(events=r.events)).isValid)
    }
    @Test fun rejectionPreservesSessionAndResetRestoresP3() {
        val s=session();val original=s.state.board;s.loadItemSample();val initial=s.state
        s.useItem(ReactorItemCommand.Cleave(0,1,ReactorSubstrate.B))
        assertEquals(initial.board,s.state.board);assertEquals(6,s.state.itemActionsRemaining)
        assertEquals(initial.feedCursor,s.state.feedCursor);assertEquals(initial.pressure,s.state.pressure)
        assertFalse(s.state.lastReplayVerified)
        s.useItem(link);assertTrue(s.state.lastReplayVerified);assertEquals(5,s.state.itemActionsRemaining)
        s.swipe(ReactorDirection.DOWN);assertTrue(s.state.lastReplayVerified);assertEquals(5,s.state.itemActionsRemaining)
        s.reset();assertEquals(original,s.state.board);assertEquals(6,s.state.itemActionsRemaining)
    }
    @Test fun blockedFeedOverflowsAndLocksItemsUntilFreeRecovery() {
        val entities=(0..24).map{ReactorPolymerEntity(ReactorEntityId("full-$it"),ReactorSubstrate.A,1)}
        val initial=ReactorBoardState(ReactorBoardSize.FIVE_BY_FIVE,entities.map{it.id},ReactorEntityStore.of(entities))
        val cmd=ReactorItemCommand.Link(20,22);val r=resolver.resolve(initial,cmd,before,6)
        assertEquals(100,r.continuation.pressure.pressure);assertEquals(ReactorOperationalState.OVERFLOW,r.continuation.operational.state)
        assertEquals(0,r.continuation.feedCursor);assertEquals(0,r.continuation.successfulFeedSerial)
        assertEquals(1,r.continuation.operational.failureCount);assertTrue(resolver.validate(initial,cmd,before,6,r))
        assertThrows(IllegalArgumentException::class.java){resolver.resolve(r.continuation.board,cmd,context(r.continuation),5)}
        val recovered=ReactorRecoveryResolver().recover(r.continuation.board,0,0,"H",1,0)
        assertEquals(ReactorOperationalState.ACTIVE,recovered.operational.state)
    }
    @Test fun emptyBudgetSyntheticAndOccupiedOutputNeverMutate() {
        val initial=ReactorItemTurnResolver.sampleBoard()
        assertThrows(IllegalArgumentException::class.java){resolver.resolve(initial,link,before,0)}
        assertThrows(IllegalArgumentException::class.java){resolver.resolve(initial,ReactorItemCommand.Cleave(24,23,ReactorSubstrate.SYNTHETIC),before,6)}
        val r=resolver.resolve(initial,link,before,6)
        assertThrows(IllegalArgumentException::class.java){resolver.resolve(r.continuation.board,ReactorItemCommand.Cleave(0,1,ReactorSubstrate.A),context(r.continuation),5)}
        assertThrows(IllegalArgumentException::class.java){resolver.resolve(r.continuation.board,ReactorItemCommand.Link(1,0),context(r.continuation),5)}
        assertEquals(0,initial.turnIndex);assertEquals(5,initial.entityStore.size)
    }
    @Test fun abstractBundlesNeverReachChemicalRecipeCatalog() {
        val e=ReactorBoardEngine(ReactorReactionCatalog{error("Abstract pair reached chemistry")},ReactorMassAuthority{1.0},profile,DeterministicReactorEntityIdFactory())
        val initial=ReactorItemTurnResolver.sampleBoard();val r=e.resolveTurn(initial,ReactorDirection.LEFT)
        assertEquals(units(initial),units(r.board));assertTrue(r.events.none{it is ReactorTurnEvent.Merge})
    }
    @Test fun liveBundleParticipatesInExactlyOneSettlingSwapBeforeFeed() {
        val a=ReactorPolymerEntity(ReactorEntityId("a"),ReactorSubstrate.A,1)
        val b=ReactorPolymerEntity(ReactorEntityId("b"),ReactorSubstrate.A,1)
        val hProfile=profile.evaluate(1.0)
        val hydrogen=ReactorElementEntity(ReactorEntityId("h"),1,"H","H",1.0,hProfile.settlingIndex,hProfile.behavior)
        val initial=ReactorBoardState.fromPlacements(ReactorBoardSize.FIVE_BY_FIVE,mapOf(
            ReactorPosition(0,0) to a,ReactorPosition(0,2) to b,ReactorPosition(1,0) to hydrogen))
        val r=resolver.resolve(initial,link,before,6)
        assertEquals(ReactorPosition(0,0),r.itemEvent.boardAfterEffect.positionOf(a.id))
        assertEquals(ReactorPosition(1,0),r.continuation.board.positionOf(a.id))
        assertEquals(ReactorPosition(0,0),r.continuation.board.positionOf(hydrogen.id))
        assertEquals(1,r.continuation.p2.events.filterIsInstance<ReactorTurnEvent.SettlingSwap>().size)
        assertTrue(resolver.validate(initial,link,before,6,r))
        assertEquals(units(initial),units(r.continuation.board))
    }

    @Test fun mixedTraceRepeatsExactly() {
        fun trace(): com.chemtable.interactive.feature.minigame.reactor.ReactorFoundationSessionState {
            val s=session();s.loadItemSample();s.useItem(link);s.useItem(ReactorItemCommand.Cleave(0,2,ReactorSubstrate.A));s.swipe(ReactorDirection.RIGHT);return s.state
        }
        assertEquals(trace(),trace());assertTrue(trace().lastReplayVerified)
    }

    @Test fun rechargePreservesBoardAndCannotDoubleGrant() {
        val s=session();s.loadItemSample();s.claimItemRecharge();assertEquals(6,s.state.itemActionsRemaining)
        s.useItem(link);assertEquals(0,s.state.itemRechargeProgress)
        s.swipe(ReactorDirection.DOWN);s.swipe(ReactorDirection.LEFT)
        val notReady=s.state;s.claimItemRecharge();assertEquals(notReady,s.state)
        s.swipe(ReactorDirection.RIGHT);assertEquals(3,s.state.itemRechargeProgress)
        val ready=s.state;s.claimItemRecharge()
        assertEquals(ready.copy(itemActionsRemaining=6,itemRechargeProgress=0),s.state)
        val claimed=s.state;s.claimItemRecharge();assertEquals(claimed,s.state)
        s.swipe(ReactorDirection.DOWN);assertEquals(0,s.state.itemRechargeProgress)
        s.reset();assertEquals(0,s.state.itemRechargeProgress);assertNull(s.state.itemLearningMessage)
    }
    @Test fun invalidItemsAndOverflowSwipesDoNotEarnRecharge() {
        val s=session();s.loadItemSample();s.useItem(link)
        s.useItem(ReactorItemCommand.Cleave(0,2,ReactorSubstrate.B));assertEquals(0,s.state.itemRechargeProgress)
        repeat(30) { if(s.state.operationalState!=ReactorOperationalState.OVERFLOW)s.swipe(ReactorDirection.UP) }
        val before=s.state.itemRechargeProgress
        s.swipe(ReactorDirection.UP);assertEquals(before,s.state.itemRechargeProgress)
        s.emergencyVent();assertEquals(before,s.state.itemRechargeProgress)
        s.loadItemSample();assertEquals(0,s.state.itemRechargeProgress);assertEquals(6,s.state.itemActionsRemaining)
    }

    @Test fun advancedSampleReachesFourAndCleavesWithoutChangingBasicSample() {
        val s=session();s.loadAdvancedItemSample()
        val totals=units(s.state.board);assertEquals(4,totals[ReactorSubstrate.A]);assertEquals(4,totals[ReactorSubstrate.B])
        s.useItem(link)
        assertEquals(4,(s.state.board.entityAt(ReactorPosition(0,0)) as ReactorPolymerEntity).units)
        s.useItem(ReactorItemCommand.Cleave(0,2,ReactorSubstrate.A))
        assertEquals(3,(s.state.board.entityAt(ReactorPosition(0,0)) as ReactorPolymerEntity).units)
        assertEquals(1,(s.state.board.entityAt(ReactorPosition(0,2)) as ReactorPolymerEntity).units)
        assertEquals(totals,units(s.state.board));assertTrue(s.state.lastReplayVerified)
        s.loadAdvancedItemSample();assertEquals(0,s.state.board.turnIndex);assertEquals(6,s.state.itemActionsRemaining);assertEquals(0,s.state.itemRechargeProgress)
        s.loadItemSample();assertEquals(2,units(s.state.board)[ReactorSubstrate.A])
    }
}
