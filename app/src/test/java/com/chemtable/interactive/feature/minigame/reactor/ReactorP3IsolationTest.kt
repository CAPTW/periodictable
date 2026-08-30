package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorFeedResolver
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorP3Orchestrator
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorPressureEvaluator
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorRecoveryResolver
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Random

class ReactorP3IsolationTest {

    @Test
    fun p3AuthoritiesHaveNoRoomDataStoreRandomClockOrClassicBoardEngineFields() {
        val types = listOf(
            ReactorFoundationSession::class.java,
            ReactorP3Orchestrator::class.java,
            ReactorFeedResolver::class.java,
            ReactorRecoveryResolver::class.java,
            ReactorPressureEvaluator::class.java,
        )
        types.forEach { type ->
            type.declaredFields.forEach { field ->
                val name = field.type.name
                assertFalse("$type uses Room", name.contains("Room"))
                assertFalse("$type uses DataStore", name.contains("DataStore"))
                assertFalse("$type uses Random", field.type == Random::class.java)
                assertFalse("$type uses Clock", name.contains("Clock") || name.contains("Instant"))
                assertFalse(
                    "$type uses Classic BoardEngine",
                    name.endsWith(".BoardEngine") && !name.endsWith("ReactorBoardEngine"),
                )
            }
        }
    }
}
