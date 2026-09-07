package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.items.ItemExperimentAction
import com.chemtable.interactive.feature.minigame.reactor.items.ItemExperimentState
import com.chemtable.interactive.feature.minigame.reactor.items.PolymerBundle
import com.chemtable.interactive.feature.minigame.reactor.items.ReactorItemExperiment
import com.chemtable.interactive.feature.minigame.reactor.model.*

sealed interface ReactorItemCommand {
    val first: Int
    val second: Int
    data class Link(override val first: Int, override val second: Int) : ReactorItemCommand
    data class Cleave(override val first: Int, override val second: Int, val enzyme: ReactorSubstrate) : ReactorItemCommand
}

data class ReactorItemTurnResult(
    val itemEvent: ReactorTurnEvent.ItemApplied,
    val continuation: ReactorP3TurnResult,
    val remainingActions: Int,
) {
    val events: List<ReactorTurnEvent> get() = listOf(itemEvent) + continuation.events
}

/** Resolves the effect, then exactly one settling tick and the unmodified P3 feed/pressure tail. */
class ReactorItemTurnResolver(
    private val orchestrator: ReactorP3Orchestrator,
    private val profile: ReactorSettlingProfile,
) {
    fun resolve(
        initial: ReactorBoardState,
        command: ReactorItemCommand,
        before: ReactorP3ReplayContext,
        remainingActions: Int,
    ): ReactorItemTurnResult {
        require(before.feedCursor in ReactorFeedSchedule.SYMBOLS.indices && before.successfulFeedSerial >= 0 && before.failureCount >= 0 && before.recoveryCount >= 0) { "유효하지 않은 이전 실행 상태입니다." }
        require(before.operationalState == ReactorOperationalState.ACTIVE) { "오버플로에서는 아이템을 사용할 수 없습니다. 무료 긴급 배출을 사용하세요." }
        require(initial.dimension == 5 && initial.turnIndex < Int.MAX_VALUE) { "지원하지 않는 보드 또는 턴입니다." }
        require(command.first in initial.cells.indices && command.second in initial.cells.indices) { "유효한 두 칸을 선택하세요." }
        val source = initial.entityStore[initial.cells[command.first] ?: error("첫 칸에 가상 기질이 필요합니다.")]
        require(source is ReactorPolymerEntity) { "아이템은 가상 기질 A/B/S에만 사용할 수 있습니다." }
        val partner = initial.cells[command.second]?.let(initial.entityStore::get)
        when (command) {
            is ReactorItemCommand.Link -> require(partner is ReactorPolymerEntity) { "두 번째 칸에도 같은 가상 기질이 필요합니다." }
            is ReactorItemCommand.Cleave -> require(partner == null) { "분해 조각을 놓을 두 번째 칸이 비어 있어야 합니다." }
        }
        val abstractCells = initial.cells.map { id -> (id?.let(initial.entityStore::get) as? ReactorPolymerEntity)?.let { PolymerBundle(it.substrate, it.units) } }
        val action = when (command) {
            is ReactorItemCommand.Link -> ItemExperimentAction.Link(command.first,command.second)
            is ReactorItemCommand.Cleave -> ItemExperimentAction.Cleave(command.first,command.second,command.enzyme)
        }
        val effect = ReactorItemExperiment.resolve(ItemExperimentState(abstractCells,remainingActions),action)
        require(effect.applied) { effect.status.message }
        val cells = initial.cells.toMutableList()
        val retained = requireNotNull(effect.state.cells[command.first])
        val additions = mutableListOf(source.copy(units = retained.units))
        val removed = mutableSetOf(source.id)
        if (partner != null) removed += partner.id
        cells[command.second] = null
        effect.state.cells[command.second]?.let { fragment ->
            val id = ReactorEntityId("reactor-item-${initial.turnIndex}-fragment-${command.second}")
            require(id !in initial.entityStore.ids) { "아이템 조각 ID가 충돌했습니다." }
            additions += ReactorPolymerEntity(id,fragment.substrate,fragment.units)
            cells[command.second] = id
        }
        val effectBoard = initial.with(cells = cells, entityStore = initial.entityStore.removeAndAdd(removed, additions))
        val settling = ReactorSettlingResolver(profile).resolve(effectBoard)
        val settled = settling.board.with(turnIndex = initial.turnIndex + 1, settlingPhase = 1 - initial.settlingPhase)
        val p2 = ReactorTurnResult(initial.turnIndex,settled.turnIndex,initial.settlingPhase,settled.settlingPhase,
            settled,settling.events,false,false,settling.changed,settling.changed,emptyList())
        val tail = orchestrator.continueAfterAction(p2,before.operationalState,before.feedCursor,before.successfulFeedSerial,before.failureCount,before.recoveryCount)
        return ReactorItemTurnResult(
            ReactorTurnEvent.ItemApplied(command,initial.turnIndex,remainingActions,effect.state.remainingActions,effectBoard),
            tail,effect.state.remainingActions,
        )
    }

    /** Prior state/command are caller authority; never recover them from the receipt being checked. */
    fun validate(initial: ReactorBoardState, command: ReactorItemCommand, before: ReactorP3ReplayContext,
                 remainingActions: Int, result: ReactorItemTurnResult): Boolean = runCatching {
        val expected = resolve(initial,command,before,remainingActions)
        // Recompute command/effect/tick to reject forged receipts and omitted/mutated events.
        require(result == expected)
        val replay = ReactorP3EventReplayer().validate(expected.itemEvent.boardAfterEffect,result.continuation,before)
        require(replay.isValid && replay.replayedBoard == result.continuation.board)
        true
    }.getOrDefault(false)

    companion object {
        fun sampleBoard(): ReactorBoardState {
            val sample = ItemExperimentState.sample()
            val entities = sample.cells.mapIndexedNotNull { i, piece -> piece?.let {
                ReactorPolymerEntity(ReactorEntityId("reactor-p5-sample-$i"),it.substrate,it.units)
            } }
            return ReactorBoardState(ReactorBoardSize.FIVE_BY_FIVE,
                sample.cells.mapIndexed { i, piece -> piece?.let { ReactorEntityId("reactor-p5-sample-$i") } },
                ReactorEntityStore.of(entities))
        }
    }
}
