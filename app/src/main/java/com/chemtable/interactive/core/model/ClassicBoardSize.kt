package com.chemtable.interactive.core.model

/** The only board dimensions supported by the preserved Classic molecule game. */
enum class ClassicBoardSize(
    val dimension: Int,
    val persistenceValue: Int,
    val displayLabel: String,
    val accessibilityLabel: String,
) {
    FOUR_BY_FOUR(
        dimension = 4,
        persistenceValue = 4,
        displayLabel = "4×4",
        accessibilityLabel = "4×4 보드",
    ),
    FIVE_BY_FIVE(
        dimension = 5,
        persistenceValue = 5,
        displayLabel = "5×5",
        accessibilityLabel = "5×5 보드",
    ),
    SIX_BY_SIX(
        dimension = 6,
        persistenceValue = 6,
        displayLabel = "6×6",
        accessibilityLabel = "6×6 보드",
    );

    companion object {
        val DEFAULT: ClassicBoardSize = FOUR_BY_FOUR

        /** Unknown or corrupt local preference values always preserve the historical 4×4 default. */
        fun fromPersistenceValue(value: Int?): ClassicBoardSize =
            entries.firstOrNull { it.persistenceValue == value } ?: DEFAULT

        fun fromDimension(dimension: Int): ClassicBoardSize? =
            entries.firstOrNull { it.dimension == dimension }
    }
}
