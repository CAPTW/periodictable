package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import kotlin.math.abs

data class ReactorReplayValidation(
    val isValid: Boolean,
    val replayedBoard: ReactorBoardState?,
    val errors: List<String>,
)

/** Independently reconstructs a turn from its ordered event batches and rejects inconsistencies. */
class ReactorEventReplayer {
    fun validate(
        initial: ReactorBoardState,
        result: ReactorTurnResult,
    ): ReactorReplayValidation {
        val errors = mutableListOf<String>()
        validateMetadata(initial, result, errors)
        validateStageOrder(result.events, errors)
        if (errors.isNotEmpty()) return invalid(errors)

        val playerEvents = result.events.filterIsInstance<ReactorTurnEvent.PlayerMove>()
        val mergeEvents = result.events.filterIsInstance<ReactorTurnEvent.Merge>()
        val settlingEvents = result.events.filter { event ->
            event is ReactorTurnEvent.SettlingMove || event is ReactorTurnEvent.SettlingSwap
        }

        val movedIds = linkedSetOf<ReactorEntityId>()
        playerEvents.forEach { event ->
            if (!movedIds.add(event.entityId)) {
                errors += "Duplicate player movement for ${event.entityId.value}"
            }
            if (!initial.isInBounds(event.from) || initial.entityIdAt(event.from) != event.entityId) {
                errors += "Invalid player source for ${event.entityId.value}"
            }
            if (!initial.isInBounds(event.to)) {
                errors += "Player destination out of bounds for ${event.entityId.value}"
            }
            if (!matchesDirection(event)) {
                errors += "Player movement direction mismatch for ${event.entityId.value}"
            }
        }

        val consumedIds = linkedSetOf<ReactorEntityId>()
        val resultIds = linkedSetOf<ReactorEntityId>()
        mergeEvents.forEach { event ->
            if (event.consumedEntityIds.size != 2 ||
                event.consumedPositions.size != 2 ||
                event.compressedPositions.size != 2
            ) {
                errors += "Merge must contain exactly two inputs"
            } else {
                event.consumedEntityIds.zip(event.consumedPositions).forEach { (id, position) ->
                    if (!consumedIds.add(id)) errors += "Duplicate merge input ${id.value}"
                    if (!initial.isInBounds(position) || initial.entityIdAt(position) != id) {
                        errors += "Merge input/source missing for ${id.value}"
                    }
                }
            }
            if (event.compressedPositions.any { !initial.isInBounds(it) }) {
                errors += "Merge compressed position out of bounds"
            }
            if (!initial.isInBounds(event.resultPosition)) {
                errors += "Merge result position out of bounds"
            }
            if (event.resultFormula != event.resultEntity.visibleLabel) {
                errors += "Merge result formula mismatch"
            }
            if (event.resultEntity.id in initial.entityStore.ids || !resultIds.add(event.resultEntity.id)) {
                errors += "Merge result entity ID collision"
            }
        }
        if ((movedIds intersect consumedIds).isNotEmpty()) {
            errors += "Entity cannot have both player movement and merge consumption events"
        }
        if (result.formulasCreated != mergeEvents.map { it.resultFormula }) {
            errors += "Created formula list mismatch"
        }
        if (errors.isNotEmpty()) return invalid(errors)

        val actionCells = initial.cells.toMutableList()
        (movedIds + consumedIds).forEach { id ->
            initial.positionOf(id)?.let { actionCells[initial.indexOf(it)] = null }
        }
        playerEvents.forEach { event ->
            val destination = initial.indexOf(event.to)
            if (actionCells[destination] != null) {
                errors += "Player destination collision at ${event.to}"
            } else {
                actionCells[destination] = event.entityId
            }
        }
        mergeEvents.forEach { event ->
            val destination = initial.indexOf(event.resultPosition)
            if (actionCells[destination] != null) {
                errors += "Merge destination collision at ${event.resultPosition}"
            } else {
                actionCells[destination] = event.resultEntity.id
            }
        }
        if (errors.isNotEmpty()) return invalid(errors)

        val actionStore = runCatching {
            initial.entityStore.removeAndAdd(consumedIds, mergeEvents.map { it.resultEntity })
        }.getOrElse { error ->
            errors += "Merge entity-store failure: ${error.message}"
            return invalid(errors)
        }
        runCatching {
            ReactorBoardState(
                boardSize = initial.boardSize,
                cells = actionCells,
                entityStore = actionStore,
                turnIndex = initial.turnIndex,
                settlingPhase = initial.settlingPhase,
            )
        }.onFailure { error -> errors += "Player/merge replay mismatch: ${error.message}" }
        if (errors.isNotEmpty()) return invalid(errors)

        val replayCells = actionCells.toMutableList()
        val settledIds = linkedSetOf<ReactorEntityId>()
        settlingEvents.forEach { event ->
            when (event) {
                is ReactorTurnEvent.SettlingMove -> replaySettlingMove(
                    event = event,
                    initial = initial,
                    entityStore = actionStore,
                    cells = replayCells,
                    settledIds = settledIds,
                    errors = errors,
                )

                is ReactorTurnEvent.SettlingSwap -> replaySettlingSwap(
                    event = event,
                    initial = initial,
                    entityStore = actionStore,
                    cells = replayCells,
                    settledIds = settledIds,
                    errors = errors,
                )

                else -> Unit
            }
        }
        if (errors.isNotEmpty()) return invalid(errors)

        val replayed = runCatching {
            ReactorBoardState(
                boardSize = initial.boardSize,
                cells = replayCells,
                entityStore = actionStore,
                turnIndex = result.resultingTurnIndex,
                settlingPhase = result.nextPhase,
            )
        }.getOrElse { error ->
            errors += "Final replay board invalid: ${error.message}"
            return invalid(errors)
        }

        if (replayed != result.board) errors += "Final board mismatch"
        if (result.mergeOccurred != mergeEvents.isNotEmpty()) errors += "Merge flag mismatch"
        if (result.settlingChanged != settlingEvents.isNotEmpty()) errors += "Settling flag mismatch"
        return if (errors.isEmpty()) {
            ReactorReplayValidation(true, replayed, emptyList())
        } else {
            invalid(errors)
        }
    }

    private fun validateMetadata(
        initial: ReactorBoardState,
        result: ReactorTurnResult,
        errors: MutableList<String>,
    ) {
        if (result.previousTurnIndex != initial.turnIndex) errors += "Previous turn index mismatch"
        if (result.resultingTurnIndex != initial.turnIndex + 1) errors += "Resulting turn index mismatch"
        if (result.phaseUsed != initial.settlingPhase) errors += "Settling phase metadata mismatch"
        if (result.nextPhase != 1 - initial.settlingPhase) errors += "Next phase metadata mismatch"
    }

    private fun validateStageOrder(
        events: List<ReactorTurnEvent>,
        errors: MutableList<String>,
    ) {
        var previousStage = -1
        events.forEach { event ->
            val stage = when (event) {
                is ReactorTurnEvent.ItemApplied -> { errors += "Item receipts require the P5 replay validator"; -1 }
                is ReactorTurnEvent.PlayerMove -> 0
                is ReactorTurnEvent.Merge -> 1
                is ReactorTurnEvent.SettlingMove,
                is ReactorTurnEvent.SettlingSwap,
                -> 2
                is ReactorTurnEvent.FeedAttempted -> 3
                is ReactorTurnEvent.FeedPlaced,
                is ReactorTurnEvent.FeedBlocked,
                -> 4
                is ReactorTurnEvent.PressureChanged -> 5
                is ReactorTurnEvent.OverflowTriggered -> 6
                is ReactorTurnEvent.RecoveryRequested -> 7
                is ReactorTurnEvent.EmergencyVentApplied -> 8
                is ReactorTurnEvent.EntityVented -> 9
                is ReactorTurnEvent.RecoveryCompleted -> 10
            }
            if (stage < previousStage) errors += "Turn event stage order mismatch"
            previousStage = stage
        }
    }

    private fun replaySettlingMove(
        event: ReactorTurnEvent.SettlingMove,
        initial: ReactorBoardState,
        entityStore: com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityStore,
        cells: MutableList<ReactorEntityId?>,
        settledIds: MutableSet<ReactorEntityId>,
        errors: MutableList<String>,
    ) {
        if (!settledIds.add(event.entityId)) errors += "Duplicate settling movement for ${event.entityId.value}"
        if (!initial.isInBounds(event.from) || !initial.isInBounds(event.to)) {
            errors += "Settling move out of bounds"
            return
        }
        if (event.from.column != event.to.column || abs(event.from.row - event.to.row) != 1) {
            errors += "Settling move must span exactly one vertical cell"
            return
        }
        val source = initial.indexOf(event.from)
        val destination = initial.indexOf(event.to)
        if (cells[source] != event.entityId) errors += "Invalid settling source for ${event.entityId.value}"
        if (cells[destination] != null) errors += "Settling destination collision at ${event.to}"
        val entity = entityStore[event.entityId]
        if (entity == null || entity.settlingBehavior != event.behavior || entity.settlingIndex != event.settlingIndex) {
            errors += "Settling entity metadata mismatch for ${event.entityId.value}"
        }
        if (event.phase != initial.settlingPhase) errors += "Settling move phase mismatch"
        if (event.behavior == SettlingBehavior.SINK && event.to.row <= event.from.row) {
            errors += "SINK event must move downward"
        }
        if (event.behavior == SettlingBehavior.RISE && event.to.row >= event.from.row) {
            errors += "RISE event must move upward"
        }
        if (errors.isEmpty()) {
            cells[source] = null
            cells[destination] = event.entityId
        }
    }

    private fun replaySettlingSwap(
        event: ReactorTurnEvent.SettlingSwap,
        initial: ReactorBoardState,
        entityStore: com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityStore,
        cells: MutableList<ReactorEntityId?>,
        settledIds: MutableSet<ReactorEntityId>,
        errors: MutableList<String>,
    ) {
        if (!settledIds.add(event.upperEntityId)) errors += "Duplicate settling movement for ${event.upperEntityId.value}"
        if (!settledIds.add(event.lowerEntityId)) errors += "Duplicate settling movement for ${event.lowerEntityId.value}"
        val positions = listOf(event.upperFrom, event.upperTo, event.lowerFrom, event.lowerTo)
        if (positions.any { !initial.isInBounds(it) }) {
            errors += "Settling swap out of bounds"
            return
        }
        if (event.upperFrom.column != event.lowerFrom.column ||
            event.lowerFrom.row - event.upperFrom.row != 1 ||
            event.upperTo != event.lowerFrom ||
            event.lowerTo != event.upperFrom
        ) {
            errors += "Settling swap must use one adjacent vertical pair"
            return
        }
        val upperSource = initial.indexOf(event.upperFrom)
        val lowerSource = initial.indexOf(event.lowerFrom)
        if (cells[upperSource] != event.upperEntityId || cells[lowerSource] != event.lowerEntityId) {
            errors += "Invalid settling swap source"
        }
        val upper = entityStore[event.upperEntityId]
        val lower = entityStore[event.lowerEntityId]
        if (upper?.settlingIndex != event.upperSettlingIndex ||
            lower?.settlingIndex != event.lowerSettlingIndex
        ) {
            errors += "Settling swap metadata mismatch"
        }
        if (event.phase != initial.settlingPhase) errors += "Settling swap phase mismatch"
        if (errors.isEmpty()) {
            cells[upperSource] = event.lowerEntityId
            cells[lowerSource] = event.upperEntityId
        }
    }

    private fun matchesDirection(event: ReactorTurnEvent.PlayerMove): Boolean = when (event.direction) {
        ReactorDirection.LEFT -> event.from.row == event.to.row && event.to.column < event.from.column
        ReactorDirection.RIGHT -> event.from.row == event.to.row && event.to.column > event.from.column
        ReactorDirection.UP -> event.from.column == event.to.column && event.to.row < event.from.row
        ReactorDirection.DOWN -> event.from.column == event.to.column && event.to.row > event.from.row
    }

    private fun invalid(errors: List<String>): ReactorReplayValidation =
        ReactorReplayValidation(false, null, errors.toList())
}
