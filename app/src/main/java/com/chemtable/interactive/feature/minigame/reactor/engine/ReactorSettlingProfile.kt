package com.chemtable.interactive.feature.minigame.reactor.engine

import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import kotlin.math.abs

data class ReactorSettlingEvaluation(
    val settlingIndex: Double,
    val behavior: SettlingBehavior,
)

interface ReactorSettlingProfile {
    val referenceMass: Double
    val epsilon: Double

    fun evaluate(molarMass: Double): ReactorSettlingEvaluation
}

/**
 * P2 gameplay-only settling profile.
 *
 * The index is molar mass minus [referenceMass]. It is not a density, buoyancy, or physical
 * sedimentation model. The 0.01 g/mol default tolerance deliberately absorbs ordinary rounded
 * atomic-weight differences around the 32.0 prototype reference so O2 remains neutral.
 */
class MassReferenceSettlingProfile(
    override val referenceMass: Double = DEFAULT_REFERENCE_MASS,
    override val epsilon: Double = DEFAULT_EPSILON,
) : ReactorSettlingProfile {

    init {
        require(referenceMass.isFinite() && referenceMass >= 0.0) {
            "Reactor reference mass must be finite and non-negative"
        }
        require(epsilon.isFinite() && epsilon > 0.0) {
            "Reactor settling epsilon must be finite and positive"
        }
    }

    override fun evaluate(molarMass: Double): ReactorSettlingEvaluation {
        require(molarMass.isFinite() && molarMass >= 0.0) {
            "Reactor molar mass must be finite and non-negative"
        }
        val index = molarMass - referenceMass
        val behavior = when {
            index < -epsilon -> SettlingBehavior.RISE
            abs(index) <= epsilon -> SettlingBehavior.NEUTRAL
            else -> SettlingBehavior.SINK
        }
        return ReactorSettlingEvaluation(index, behavior)
    }

    companion object {
        const val DEFAULT_REFERENCE_MASS: Double = 32.0
        const val DEFAULT_EPSILON: Double = 0.01
    }
}
