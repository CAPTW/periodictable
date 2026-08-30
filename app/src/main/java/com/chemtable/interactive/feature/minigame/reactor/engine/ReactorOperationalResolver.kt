package com.chemtable.interactive.feature.minigame.reactor.engine

enum class ReactorOperationalState { ACTIVE, OVERFLOW }

data class ReactorOperationalSnapshot(
    val state: ReactorOperationalState,
    val failureCount: Int,
    val transitionedToOverflow: Boolean,
    val playerMovesDisabled: Boolean,
    val feedDisabled: Boolean,
    val emergencyVentEnabled: Boolean,
    val resetEnabled: Boolean,
)

object ReactorOperationalResolver {
    fun resolve(
        previous: ReactorOperationalState,
        pressure: Int,
        previousFailureCount: Int,
    ): ReactorOperationalSnapshot {
        val next = if (pressure == 100) {
            ReactorOperationalState.OVERFLOW
        } else {
            ReactorOperationalState.ACTIVE
        }
        val transitioned = previous == ReactorOperationalState.ACTIVE &&
            next == ReactorOperationalState.OVERFLOW
        val overflow = next == ReactorOperationalState.OVERFLOW
        return ReactorOperationalSnapshot(
            state = next,
            failureCount = previousFailureCount + if (transitioned) 1 else 0,
            transitionedToOverflow = transitioned,
            playerMovesDisabled = overflow,
            feedDisabled = overflow,
            emergencyVentEnabled = overflow,
            resetEnabled = true,
        )
    }
}
