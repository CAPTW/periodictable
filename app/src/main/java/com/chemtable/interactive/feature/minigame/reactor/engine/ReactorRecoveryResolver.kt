package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

data class ReactorRecoveryResult(
    val board: ReactorBoardState,
    val vented: List<Pair<ReactorEntityId, ReactorPosition>>,
    val pressure: ReactorPressureBreakdown,
    val operational: ReactorOperationalSnapshot,
    val recoveryCount: Int,
    val cursor: Int,
    val successfulFeedSerial: Int,
    val pending: ReactorFeedSpecification,
    val events: List<ReactorTurnEvent>,
)

class ReactorRecoveryResolver {
    fun recover(
        board: ReactorBoardState,
        cursor: Int,
        successfulFeedSerial: Int,
        pendingSymbol: String,
        previousFailureCount: Int,
        previousRecoveryCount: Int,
    ): ReactorRecoveryResult {
        val pending = ReactorFeedSchedule.specificationAt(cursor)
        require(pending.symbol == pendingSymbol) {
            "Retained pending feed must match cursor authority"
        }
        val candidates = (0 until board.dimension).mapNotNull { column ->
            val position = ReactorPosition(0, column)
            val entity = board.entityAt(position) ?: return@mapNotNull null
            Triple(entity, position, entity.id)
        }.sortedWith(
            compareByDescending<Triple<com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity, ReactorPosition, ReactorEntityId>> {
                it.first.settlingIndex
            }.thenBy { it.second.column }
                .thenBy { it.third.value },
        )
        val selected = candidates.take(2)
        val removedIds = selected.map { it.third }.toSet()
        val cells = board.cells.toMutableList()
        selected.forEach { (_, position, _) ->
            cells[board.indexOf(position)] = null
        }
        val nextBoard = board.with(
            cells = cells,
            entityStore = board.entityStore.removeAndAdd(removedIds, emptyList()),
        )
        val pressure = ReactorPressureEvaluator.evaluate(nextBoard, feedBlocked = false)
        require(pressure.pressure < 100) { "Emergency Vent must reduce pressure below OVERFLOW" }
        val operational = ReactorOperationalResolver.resolve(
            previous = ReactorOperationalState.OVERFLOW,
            pressure = pressure.pressure,
            previousFailureCount = previousFailureCount,
        )
        val recoveryCount = previousRecoveryCount + 1
        val vented = selected.map { it.third to it.second }
        val events = buildList {
            add(
                ReactorTurnEvent.RecoveryRequested(
                    failureCount = previousFailureCount,
                    recoveryCountBefore = previousRecoveryCount,
                ),
            )
            add(ReactorTurnEvent.EmergencyVentApplied(ventedEntityIds = vented.map { it.first }))
            selected.forEach { (entity, position, id) ->
                add(
                    ReactorTurnEvent.EntityVented(
                        entityId = id,
                        formula = entity.visibleLabel,
                        position = position,
                        settlingIndex = entity.settlingIndex,
                    ),
                )
            }
            add(
                ReactorTurnEvent.PressureChanged(
                    oldPressure = 100,
                    newPressure = pressure.pressure,
                    oldBand = ReactorPressureBand.OVERFLOW,
                    newBand = pressure.band,
                    breakdown = pressure,
                ),
            )
            add(
                ReactorTurnEvent.RecoveryCompleted(
                    newPressure = pressure.pressure,
                    newState = operational.state,
                    recoveryCount = recoveryCount,
                ),
            )
        }
        return ReactorRecoveryResult(
            board = nextBoard,
            vented = vented,
            pressure = pressure,
            operational = operational,
            recoveryCount = recoveryCount,
            cursor = cursor,
            successfulFeedSerial = successfulFeedSerial,
            pending = pending,
            events = events,
        )
    }
}
