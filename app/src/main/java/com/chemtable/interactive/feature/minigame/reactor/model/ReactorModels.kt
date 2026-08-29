package com.chemtable.interactive.feature.minigame.reactor.model

@JvmInline
value class ReactorEntityId(val value: String) {
    init {
        require(value.isNotBlank()) { "Reactor entity ID must not be blank" }
    }
}

enum class ReactorEntityKind { ELEMENT, MOLECULE }

enum class SettlingBehavior { RISE, NEUTRAL, SINK }

enum class ReactorFootprint(val cellCount: Int) {
    SINGLE_CELL(1),
}

enum class ReactorBoardSize(val dimension: Int) {
    FOUR_BY_FOUR(4),
    FIVE_BY_FIVE(5),
    SIX_BY_SIX(6);

    companion object {
        fun fromDimension(dimension: Int): ReactorBoardSize? =
            entries.firstOrNull { it.dimension == dimension }

        fun requireDimension(dimension: Int): ReactorBoardSize =
            requireNotNull(fromDimension(dimension)) {
                "Unsupported Reactor board dimension: $dimension"
            }
    }
}

data class ReactorPosition(val row: Int, val column: Int)

class ReactorComposition private constructor(counts: Map<String, Int>) {
    val counts: Map<String, Int> = counts.toSortedMap()

    init {
        require(this.counts.isNotEmpty()) { "Reactor composition must not be empty" }
        require(this.counts.keys.all { it.isNotBlank() }) {
            "Reactor composition symbols must not be blank"
        }
        require(this.counts.values.all { it > 0 }) {
            "Reactor composition counts must be positive"
        }
    }

    operator fun plus(other: ReactorComposition): ReactorComposition {
        val combined = LinkedHashMap(counts)
        other.counts.forEach { (symbol, count) ->
            combined[symbol] = (combined[symbol] ?: 0) + count
        }
        return of(combined)
    }

    override fun equals(other: Any?): Boolean =
        other is ReactorComposition && counts == other.counts

    override fun hashCode(): Int = counts.hashCode()

    override fun toString(): String = counts.toString()

    companion object {
        fun of(counts: Map<String, Int>): ReactorComposition = ReactorComposition(counts)
    }
}

sealed interface ReactorEntity {
    val id: ReactorEntityId
    val kind: ReactorEntityKind
    val composition: ReactorComposition
    val visibleLabel: String
    val displayName: String
    val molarMass: Double
    val settlingIndex: Double
    val settlingBehavior: SettlingBehavior
    val footprint: ReactorFootprint
        get() = ReactorFootprint.SINGLE_CELL
}

data class ReactorElementEntity(
    override val id: ReactorEntityId,
    val atomicNumber: Int,
    val symbol: String,
    override val displayName: String,
    override val molarMass: Double,
    override val settlingIndex: Double,
    override val settlingBehavior: SettlingBehavior,
) : ReactorEntity {
    init {
        require(atomicNumber > 0) { "Reactor element atomic number must be positive" }
        require(symbol.isNotBlank()) { "Reactor element symbol must not be blank" }
        require(molarMass.isFinite() && molarMass >= 0.0) {
            "Reactor element molar mass must be finite and non-negative"
        }
        require(settlingIndex.isFinite()) { "Reactor settling index must be finite" }
    }

    override val kind: ReactorEntityKind = ReactorEntityKind.ELEMENT
    override val composition: ReactorComposition = ReactorComposition.of(mapOf(symbol to 1))
    override val visibleLabel: String = symbol
}

data class ReactorMoleculeEntity(
    override val id: ReactorEntityId,
    val formula: String,
    override val displayName: String,
    override val composition: ReactorComposition,
    override val molarMass: Double,
    override val settlingIndex: Double,
    override val settlingBehavior: SettlingBehavior,
) : ReactorEntity {
    init {
        require(formula.isNotBlank()) { "Reactor molecule formula must not be blank" }
        require(molarMass.isFinite() && molarMass >= 0.0) {
            "Reactor molecule molar mass must be finite and non-negative"
        }
        require(settlingIndex.isFinite()) { "Reactor settling index must be finite" }
    }

    override val kind: ReactorEntityKind = ReactorEntityKind.MOLECULE
    override val visibleLabel: String = formula
}

class ReactorEntityStore private constructor(entities: Map<ReactorEntityId, ReactorEntity>) {
    val entities: Map<ReactorEntityId, ReactorEntity> = LinkedHashMap(
        entities.entries
            .sortedBy { it.key.value }
            .associate { it.key to it.value },
    )

    val size: Int get() = entities.size
    val ids: Set<ReactorEntityId> get() = entities.keys

    operator fun get(id: ReactorEntityId): ReactorEntity? = entities[id]

    fun removeAndAdd(
        removedIds: Set<ReactorEntityId>,
        additions: Collection<ReactorEntity>,
    ): ReactorEntityStore {
        val remaining = entities.filterKeys { it !in removedIds }.values
        return of(remaining + additions)
    }

    override fun equals(other: Any?): Boolean =
        other is ReactorEntityStore && entities == other.entities

    override fun hashCode(): Int = entities.hashCode()

    override fun toString(): String = entities.toString()

    companion object {
        fun empty(): ReactorEntityStore = ReactorEntityStore(emptyMap())

        fun of(entities: Collection<ReactorEntity>): ReactorEntityStore {
            val duplicates = entities.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            require(duplicates.isEmpty()) { "Duplicate Reactor entity IDs: $duplicates" }
            return ReactorEntityStore(entities.associateBy { it.id })
        }
    }
}

data class ReactorBoardValidation(
    val errors: List<String>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

class ReactorBoardState(
    val boardSize: ReactorBoardSize,
    cells: List<ReactorEntityId?>,
    val entityStore: ReactorEntityStore,
    val turnIndex: Int = 0,
    val settlingPhase: Int = 0,
) {
    val cells: List<ReactorEntityId?> = cells.toList()
    val dimension: Int get() = boardSize.dimension

    init {
        val validation = validateInternal()
        require(validation.isValid) { validation.errors.joinToString(separator = "; ") }
    }

    fun positions(): List<ReactorPosition> =
        (0 until dimension).flatMap { row ->
            (0 until dimension).map { column -> ReactorPosition(row, column) }
        }

    fun emptyPositions(): List<ReactorPosition> =
        positions().filter { entityIdAt(it) == null }

    fun occupiedPositions(): List<ReactorPosition> =
        positions().filter { entityIdAt(it) != null }

    fun entityIdAt(position: ReactorPosition): ReactorEntityId? =
        if (isInBounds(position)) cells[indexOf(position)] else null

    fun entityAt(position: ReactorPosition): ReactorEntity? =
        entityIdAt(position)?.let(entityStore::get)

    fun positionOf(id: ReactorEntityId): ReactorPosition? =
        cells.indexOf(id).takeIf { it >= 0 }?.let { index ->
            ReactorPosition(index / dimension, index % dimension)
        }

    fun isInBounds(position: ReactorPosition): Boolean =
        position.row in 0 until dimension && position.column in 0 until dimension

    fun indexOf(position: ReactorPosition): Int {
        require(isInBounds(position)) { "Reactor position out of bounds: $position" }
        return position.row * dimension + position.column
    }

    fun with(
        cells: List<ReactorEntityId?> = this.cells,
        entityStore: ReactorEntityStore = this.entityStore,
        turnIndex: Int = this.turnIndex,
        settlingPhase: Int = this.settlingPhase,
    ): ReactorBoardState = ReactorBoardState(
        boardSize = boardSize,
        cells = cells,
        entityStore = entityStore,
        turnIndex = turnIndex,
        settlingPhase = settlingPhase,
    )

    fun validate(): ReactorBoardValidation = validateInternal()

    private fun validateInternal(): ReactorBoardValidation {
        val errors = mutableListOf<String>()
        val expectedCells = dimension * dimension
        if (cells.size != expectedCells) {
            errors += "Reactor board must contain exactly $expectedCells cells"
        }
        if (turnIndex < 0) errors += "Reactor turn index must be non-negative"
        if (settlingPhase !in 0..1) errors += "Reactor settling phase must be 0 or 1"

        if (cells.size == expectedCells) {
            val occupied = cells.filterNotNull()
            if (occupied.distinct().size != occupied.size) {
                errors += "A Reactor entity ID may occupy exactly one cell"
            }
            val occupiedIds = occupied.toSet()
            val missing = occupiedIds - entityStore.ids
            if (missing.isNotEmpty()) errors += "Occupied cells reference missing entities: $missing"
            val orphans = entityStore.ids - occupiedIds
            if (orphans.isNotEmpty()) errors += "Active Reactor entities must occupy exactly one cell: $orphans"
        }
        return ReactorBoardValidation(errors.toList())
    }

    override fun equals(other: Any?): Boolean =
        other is ReactorBoardState &&
            boardSize == other.boardSize &&
            cells == other.cells &&
            entityStore == other.entityStore &&
            turnIndex == other.turnIndex &&
            settlingPhase == other.settlingPhase

    override fun hashCode(): Int {
        var result = boardSize.hashCode()
        result = 31 * result + cells.hashCode()
        result = 31 * result + entityStore.hashCode()
        result = 31 * result + turnIndex
        result = 31 * result + settlingPhase
        return result
    }

    override fun toString(): String =
        "ReactorBoardState(boardSize=$boardSize, cells=$cells, entityStore=$entityStore, " +
            "turnIndex=$turnIndex, settlingPhase=$settlingPhase)"

    companion object {
        fun empty(
            boardSize: ReactorBoardSize,
            turnIndex: Int = 0,
            settlingPhase: Int = 0,
        ): ReactorBoardState = ReactorBoardState(
            boardSize = boardSize,
            cells = List(boardSize.dimension * boardSize.dimension) { null },
            entityStore = ReactorEntityStore.empty(),
            turnIndex = turnIndex,
            settlingPhase = settlingPhase,
        )

        fun fromPlacements(
            boardSize: ReactorBoardSize,
            placements: Map<ReactorPosition, ReactorEntity>,
            turnIndex: Int = 0,
            settlingPhase: Int = 0,
        ): ReactorBoardState {
            require(placements.keys.all { position ->
                position.row in 0 until boardSize.dimension &&
                    position.column in 0 until boardSize.dimension
            }) { "Reactor placement out of bounds" }
            val cells = MutableList<ReactorEntityId?>(boardSize.dimension * boardSize.dimension) { null }
            placements.forEach { (position, entity) ->
                cells[position.row * boardSize.dimension + position.column] = entity.id
            }
            return ReactorBoardState(
                boardSize = boardSize,
                cells = cells,
                entityStore = ReactorEntityStore.of(placements.values),
                turnIndex = turnIndex,
                settlingPhase = settlingPhase,
            )
        }
    }
}
