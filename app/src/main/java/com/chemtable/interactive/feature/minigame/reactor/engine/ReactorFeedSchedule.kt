package com.chemtable.interactive.feature.minigame.reactor.engine

data class ReactorFeedSpecification(
    val scheduleIndex: Int,
    val atomicNumber: Int,
    val symbol: String,
) {
    init {
        require(scheduleIndex >= 0) { "Reactor feed schedule index must be non-negative" }
        require(atomicNumber > 0) { "Reactor feed atomic number must be positive" }
        require(symbol.isNotBlank()) { "Reactor feed symbol must not be blank" }
    }
}

data class ReactorFeedScheduleState(
    val cursor: Int,
    val successfulFeedSerial: Int,
    val pending: ReactorFeedSpecification,
    val preview: List<ReactorFeedSpecification>,
)

object ReactorFeedSchedule {
    val SYMBOLS: List<String> = listOf("H", "O", "H", "O", "N", "N", "Na", "Cl", "C", "O")
    val ATOMIC_NUMBERS: List<Int> = listOf(1, 8, 1, 8, 7, 7, 11, 17, 6, 8)

    init {
        require(SYMBOLS.size == 10 && ATOMIC_NUMBERS.size == 10)
    }

    fun specificationAt(index: Int): ReactorFeedSpecification {
        val normalized = Math.floorMod(index, SYMBOLS.size)
        return ReactorFeedSpecification(
            scheduleIndex = normalized,
            atomicNumber = ATOMIC_NUMBERS[normalized],
            symbol = SYMBOLS[normalized],
        )
    }

    fun state(cursor: Int, successfulFeedSerial: Int): ReactorFeedScheduleState {
        require(cursor >= 0) { "Reactor feed cursor must be non-negative" }
        require(successfulFeedSerial >= 0) { "Reactor feed serial must be non-negative" }
        val normalized = Math.floorMod(cursor, SYMBOLS.size)
        val pending = specificationAt(normalized)
        return ReactorFeedScheduleState(
            cursor = normalized,
            successfulFeedSerial = successfulFeedSerial,
            pending = pending,
            preview = listOf(0, 1, 2).map { offset -> specificationAt(cursor + offset) },
        )
    }

    fun afterSuccess(cursor: Int, successfulFeedSerial: Int): ReactorFeedScheduleState =
        state(cursor = cursor + 1, successfulFeedSerial = successfulFeedSerial + 1)

    fun afterBlocked(cursor: Int, successfulFeedSerial: Int): ReactorFeedScheduleState =
        state(cursor = cursor, successfulFeedSerial = successfulFeedSerial)

    fun identity(successfulFeedSerial: Int, scheduleIndex: Int): String =
        "p3-feed-$successfulFeedSerial-$scheduleIndex"
}
