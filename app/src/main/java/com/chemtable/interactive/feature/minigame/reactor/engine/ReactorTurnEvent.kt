package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior

sealed interface ReactorTurnEvent {
    val movedEntityIds: List<ReactorEntityId>

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
}

enum class ReactorDirection { UP, DOWN, LEFT, RIGHT }
