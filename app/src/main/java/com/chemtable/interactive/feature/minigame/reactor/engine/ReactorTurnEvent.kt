package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior

sealed interface ReactorTurnEvent {
    val movedEntityIds: List<ReactorEntityId>

    data class ItemApplied(
        val command: ReactorItemCommand,
        val turnBefore: Int,
        val actionsBefore: Int,
        val actionsAfter: Int,
        val boardAfterEffect: com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class PlayerMove(
        val entityId: ReactorEntityId,
        val from: ReactorPosition,
        val to: ReactorPosition,
        val direction: ReactorDirection,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = listOf(entityId)
    }

    data class Merge(
        val consumedEntityIds: List<ReactorEntityId>,
        val consumedPositions: List<ReactorPosition>,
        val compressedPositions: List<ReactorPosition>,
        val resultEntity: ReactorEntity,
        val resultFormula: String,
        val resultPosition: ReactorPosition,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class SettlingMove(
        val entityId: ReactorEntityId,
        val from: ReactorPosition,
        val to: ReactorPosition,
        val behavior: SettlingBehavior,
        val settlingIndex: Double,
        val phase: Int,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = listOf(entityId)
    }

    data class SettlingSwap(
        val upperEntityId: ReactorEntityId,
        val lowerEntityId: ReactorEntityId,
        val upperFrom: ReactorPosition,
        val upperTo: ReactorPosition,
        val lowerFrom: ReactorPosition,
        val lowerTo: ReactorPosition,
        val upperSettlingIndex: Double,
        val lowerSettlingIndex: Double,
        val phase: Int,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> =
            listOf(upperEntityId, lowerEntityId)
    }

    data class FeedAttempted(
        val scheduleIndex: Int,
        val symbol: String,
        val atomicNumber: Int,
        val resolvedTurn: Int,
        val feedCursor: Int,
        val startColumn: Int,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class FeedPlaced(
        val entityId: ReactorEntityId,
        val entity: ReactorEntity,
        val formula: String,
        val scheduleIndex: Int,
        val position: ReactorPosition,
        val startColumn: Int,
        val scannedColumns: List<Int>,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class FeedBlocked(
        val scheduleIndex: Int,
        val symbol: String,
        val startColumn: Int,
        val scannedColumns: List<Int>,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class PressureChanged(
        val oldPressure: Int,
        val newPressure: Int,
        val oldBand: ReactorPressureBand,
        val newBand: ReactorPressureBand,
        val breakdown: ReactorPressureBreakdown,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class OverflowTriggered(
        val pressure: Int,
        val failureCount: Int,
        val oldState: ReactorOperationalState,
        val newState: ReactorOperationalState,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class RecoveryRequested(
        val failureCount: Int,
        val recoveryCountBefore: Int,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class EmergencyVentApplied(
        val ventedEntityIds: List<ReactorEntityId>,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class EntityVented(
        val entityId: ReactorEntityId,
        val formula: String,
        val position: ReactorPosition,
        val settlingIndex: Double,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }

    data class RecoveryCompleted(
        val newPressure: Int,
        val newState: ReactorOperationalState,
        val recoveryCount: Int,
    ) : ReactorTurnEvent {
        override val movedEntityIds: List<ReactorEntityId> = emptyList()
    }
}

enum class ReactorDirection { UP, DOWN, LEFT, RIGHT }
