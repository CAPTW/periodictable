package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.MassReferenceSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.model.SettlingBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReactorSettlingProfileTest {

    private val profile = MassReferenceSettlingProfile()

    @Test
    fun defaultProfileUsesExplicitPrototypeReferenceAndRoundingTolerance() {
        assertEquals(32.0, profile.referenceMass, 0.0)
        assertEquals(0.01, profile.epsilon, 0.0)
    }

    @Test
    fun massBelowEqualAndAboveReferenceMapToRiseNeutralAndSink() {
        assertEquals(SettlingBehavior.RISE, profile.evaluate(18.015).behavior)
        assertEquals(SettlingBehavior.NEUTRAL, profile.evaluate(32.0).behavior)
        assertEquals(SettlingBehavior.SINK, profile.evaluate(44.009).behavior)
    }

    @Test
    fun epsilonBoundaryIsInclusiveAndOutsideValuesRemainDirectional() {
        val boundaryProfile = MassReferenceSettlingProfile(referenceMass = 32.0, epsilon = 0.125)

        assertEquals(SettlingBehavior.RISE, boundaryProfile.evaluate(31.75).behavior)
        assertEquals(SettlingBehavior.NEUTRAL, boundaryProfile.evaluate(31.875).behavior)
        assertEquals(SettlingBehavior.NEUTRAL, boundaryProfile.evaluate(32.125).behavior)
        assertEquals(SettlingBehavior.SINK, boundaryProfile.evaluate(32.25).behavior)
    }

    @Test
    fun currentSixRecipeMassesHaveRequiredPrototypeBehaviors() {
        val expected = mapOf(
            "H2" to (2.016 to SettlingBehavior.RISE),
            "H2O" to (18.015 to SettlingBehavior.RISE),
            "N2" to (28.014 to SettlingBehavior.RISE),
            "O2" to (31.998 to SettlingBehavior.NEUTRAL),
            "CO2" to (44.009 to SettlingBehavior.SINK),
            "NaCl" to (58.44 to SettlingBehavior.SINK),
        )

        expected.forEach { (_, massAndBehavior) ->
            assertEquals(massAndBehavior.second, profile.evaluate(massAndBehavior.first).behavior)
        }
    }

    @Test
    fun alternateReferenceIsInjectedWithoutChangingClassificationRules() {
        val alternate = MassReferenceSettlingProfile(referenceMass = 20.0, epsilon = 0.5)

        assertEquals(-2.0, alternate.evaluate(18.0).settlingIndex, 0.0)
        assertEquals(SettlingBehavior.RISE, alternate.evaluate(18.0).behavior)
        assertEquals(SettlingBehavior.NEUTRAL, alternate.evaluate(20.5).behavior)
        assertEquals(SettlingBehavior.SINK, alternate.evaluate(21.0).behavior)
    }

    @Test
    fun nonFiniteOrNegativeMassCannotEnterTheGameProfile() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.001).forEach { mass ->
            assertThrows(IllegalArgumentException::class.java) {
                profile.evaluate(mass)
            }
        }
    }

    @Test
    fun repeatedEvaluationIsStructurallyDeterministic() {
        assertEquals(profile.evaluate(44.009), profile.evaluate(44.009))
    }
}
