package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorMoleculeEntity
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorPosition

data class ReactorTurnResult(
    val previousTurnIndex: Int,
    val resultingTurnIndex: Int,
    val phaseUsed: Int,
    val nextPhase: Int,
    val board: ReactorBoardState,
    val events: List<ReactorTurnEvent>,
    val playerCompressionChanged: Boolean,
    val mergeOccurred: Boolean,
    val settlingChanged: Boolean,
    val anyEntityMoved: Boolean,
    val formulasCreated: List<String>,
)

/** Pure Kotlin Reactor turn engine. It has no Classic BoardEngine, Android, or persistence dependency. */
class ReactorBoardEngine(
    private val reactionCatalog: ReactorReactionCatalog,
    private val massAuthority: ReactorMassAuthority,
    private val settlingProfile: ReactorSettlingProfile,
    private val idFactory: ReactorEntityIdFactory,
) {
    private val settlingResolver = ReactorSettlingResolver(settlingProfile)

    fun resolveTurn(
        board: ReactorBoardState,
        direction: ReactorDirection,
    ): ReactorTurnResult {
        require(board.validate().isValid) { "Invalid Reactor board before turn" }
        val action = compressAndMerge(board, direction)
        val settling = settlingResolver.resolve(action.board)
        val nextPhase = 1 - board.settlingPhase
        val finalBoard = settling.board.with(
            turnIndex = board.turnIndex + 1,
            settlingPhase = nextPhase,
        )
        val events = action.playerEvents + action.mergeEvents + settling.events
        return ReactorTurnResult(
            previousTurnIndex = board.turnIndex,
            resultingTurnIndex = finalBoard.turnIndex,
            phaseUsed = board.settlingPhase,
            nextPhase = nextPhase,
            board = finalBoard,
            events = events,
            playerCompressionChanged = action.compressionChanged,
            mergeOccurred = action.mergeEvents.isNotEmpty(),
            settlingChanged = settling.changed,
            anyEntityMoved = action.compressionChanged || settling.events.isNotEmpty(),
            formulasCreated = action.mergeEvents.map { it.resultFormula },
        )
    }

    private fun compressAndMerge(
        board: ReactorBoardState,
        direction: ReactorDirection,
    ): PlayerActionOutcome {
        val finalCells = MutableList<ReactorEntityId?>(board.cells.size) { null }
        val playerEvents = mutableListOf<ReactorTurnEvent.PlayerMove>()
        val mergeEvents = mutableListOf<ReactorTurnEvent.Merge>()
        val removedIds = linkedSetOf<ReactorEntityId>()
        val products = mutableListOf<ReactorEntity>()
        var mergeOrdinal = 0
        var compressionChanged = false

        for (lineIndex in 0 until board.dimension) {
            val positions = orientedLinePositions(board.dimension, direction, lineIndex)
            val inputs = positions.mapNotNull { position ->
                board.entityIdAt(position)?.let { entityId ->
                    LineInput(
                        entity = requireNotNull(board.entityStore[entityId]),
                        originalPosition = position,
                    )
                }
            }
            inputs.forEachIndexed { inputIndex, input ->
                if (input.originalPosition != positions[inputIndex]) compressionChanged = true
            }

            var inputIndex = 0
            var outputIndex = 0
            while (inputIndex < inputs.size) {
                val first = inputs[inputIndex]
                val second = inputs.getOrNull(inputIndex + 1)
                val specification = second?.let {
                    reactionCatalog.findProduct(first.entity.composition + it.entity.composition)
                }

                if (second != null && specification != null) {
                    val consumedIds = listOf(first.entity.id, second.entity.id)
                    val mass = massAuthority.molarMassOf(specification)
                    require(mass.isFinite() && mass >= 0.0) {
                        "Reactor mass authority returned invalid mass for ${specification.formula}"
                    }
                    val settling = settlingProfile.evaluate(mass)
                    val product = ReactorMoleculeEntity(
                        id = idFactory.create(
                            turnIndex = board.turnIndex,
                            mergeOrdinal = mergeOrdinal,
                            consumedEntityIds = consumedIds,
                            specification = specification,
                        ),
                        formula = specification.formula,
                        displayName = specification.displayName,
                        composition = specification.composition,
                        molarMass = mass,
                        settlingIndex = settling.settlingIndex,
                        settlingBehavior = settling.behavior,
                    )
                    val resultPosition = positions[outputIndex]
                    finalCells[board.indexOf(resultPosition)] = product.id
                    mergeEvents += ReactorTurnEvent.Merge(
                        consumedEntityIds = consumedIds,
                        consumedPositions = listOf(first.originalPosition, second.originalPosition),
                        compressedPositions = listOf(positions[inputIndex], positions[inputIndex + 1]),
                        resultEntity = product,
                        resultFormula = product.formula,
                        resultPosition = resultPosition,
                    )
                    removedIds += consumedIds
                    products += product
                    mergeOrdinal += 1
                    inputIndex += 2
                    outputIndex += 1
                } else {
                    val resultPosition = positions[outputIndex]
                    finalCells[board.indexOf(resultPosition)] = first.entity.id
                    if (first.originalPosition != resultPosition) {
                        playerEvents += ReactorTurnEvent.PlayerMove(
                            entityId = first.entity.id,
                            from = first.originalPosition,
                            to = resultPosition,
                            direction = direction,
                        )
                    }
                    inputIndex += 1
                    outputIndex += 1
                }
            }
        }

        val resultStore = board.entityStore.removeAndAdd(removedIds, products)
        val resultBoard = board.with(cells = finalCells, entityStore = resultStore)
        return PlayerActionOutcome(
            board = resultBoard,
            playerEvents = playerEvents.toList(),
            mergeEvents = mergeEvents.toList(),
            compressionChanged = compressionChanged,
        )
    }

    private fun orientedLinePositions(
        dimension: Int,
        direction: ReactorDirection,
        lineIndex: Int,
    ): List<ReactorPosition> = when (direction) {
        ReactorDirection.LEFT ->
            (0 until dimension).map { column -> ReactorPosition(lineIndex, column) }
        ReactorDirection.RIGHT ->
            (dimension - 1 downTo 0).map { column -> ReactorPosition(lineIndex, column) }
        ReactorDirection.UP ->
            (0 until dimension).map { row -> ReactorPosition(row, lineIndex) }
        ReactorDirection.DOWN ->
            (dimension - 1 downTo 0).map { row -> ReactorPosition(row, lineIndex) }
    }

    private data class LineInput(
        val entity: ReactorEntity,
        val originalPosition: ReactorPosition,
    )

    private data class PlayerActionOutcome(
        val board: ReactorBoardState,
        val playerEvents: List<ReactorTurnEvent.PlayerMove>,
        val mergeEvents: List<ReactorTurnEvent.Merge>,
        val compressionChanged: Boolean,
    )
}
