package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorElementEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

data class ReactorFeedResult(
    val board: ReactorBoardState,
    val placed: Boolean,
    val blocked: Boolean,
    val cursor: Int,
    val successfulFeedSerial: Int,
    val pending: ReactorFeedSpecification,
    val preview: List<ReactorFeedSpecification>,
    val placedEntityId: ReactorEntityId?,
    val placedPosition: ReactorPosition?,
    val events: List<ReactorTurnEvent>,
)

class ReactorFeedResolver(
    private val elementCatalog: ReactorElementCatalog,
    private val settlingProfile: ReactorSettlingProfile,
) {
    fun resolve(
        board: ReactorBoardState,
        resolvedTurn: Int,
        cursor: Int,
        successfulFeedSerial: Int,
    ): ReactorFeedResult {
        val schedule = ReactorFeedSchedule.state(cursor, successfulFeedSerial)
        val startColumn = startColumn(resolvedTurn, cursor)
        val scanned = (0 until board.dimension).map { offset ->
            Math.floorMod(startColumn + offset, board.dimension)
        }
        val attempted = ReactorTurnEvent.FeedAttempted(
            scheduleIndex = schedule.pending.scheduleIndex,
            symbol = schedule.pending.symbol,
            atomicNumber = schedule.pending.atomicNumber,
            resolvedTurn = resolvedTurn,
            feedCursor = cursor,
            startColumn = startColumn,
        )
        val emptyColumn = scanned.firstOrNull { column ->
            board.entityIdAt(ReactorPosition(0, column)) == null
        }
        if (emptyColumn == null) {
            return ReactorFeedResult(
                board = board,
                placed = false,
                blocked = true,
                cursor = cursor,
                successfulFeedSerial = successfulFeedSerial,
                pending = schedule.pending,
                preview = schedule.preview,
                placedEntityId = null,
                placedPosition = null,
                events = listOf(
                    attempted,
                    ReactorTurnEvent.FeedBlocked(
                        scheduleIndex = schedule.pending.scheduleIndex,
                        symbol = schedule.pending.symbol,
                        startColumn = startColumn,
                        scannedColumns = scanned,
                    ),
                ),
            )
        }
        val specification = requireNotNull(elementCatalog.find(schedule.pending.symbol)) {
            "Missing Reactor feed element authority: ${schedule.pending.symbol}"
        }
        val settling = settlingProfile.evaluate(specification.molarMass)
        val entityId = ReactorEntityId(
            ReactorFeedSchedule.identity(successfulFeedSerial, schedule.pending.scheduleIndex),
        )
        val entity = ReactorElementEntity(
            id = entityId,
            atomicNumber = specification.atomicNumber,
            symbol = specification.symbol,
            displayName = specification.displayName,
            molarMass = specification.molarMass,
            settlingIndex = settling.settlingIndex,
            settlingBehavior = settling.behavior,
        )
        val position = ReactorPosition(0, emptyColumn)
        val cells = board.cells.toMutableList()
        cells[board.indexOf(position)] = entityId
        val advanced = ReactorFeedSchedule.afterSuccess(cursor, successfulFeedSerial)
        return ReactorFeedResult(
            board = board.with(
                cells = cells,
                entityStore = board.entityStore.removeAndAdd(emptySet(), listOf(entity)),
            ),
            placed = true,
            blocked = false,
            cursor = advanced.cursor,
            successfulFeedSerial = advanced.successfulFeedSerial,
            pending = advanced.pending,
            preview = advanced.preview,
            placedEntityId = entityId,
            placedPosition = position,
            events = listOf(
                attempted,
                ReactorTurnEvent.FeedPlaced(
                    entityId = entityId,
                    entity = entity,
                    formula = entity.visibleLabel,
                    scheduleIndex = schedule.pending.scheduleIndex,
                    position = position,
                    startColumn = startColumn,
                    scannedColumns = scanned,
                ),
            ),
        )
    }

    companion object {
        fun startColumn(resolvedTurn: Int, feedCursor: Int): Int =
            Math.floorMod(resolvedTurn + feedCursor, 5)
    }
}
