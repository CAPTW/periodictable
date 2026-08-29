package com.chemtable.interactive.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReactorFoundationRouteTest {

    @Test
    fun reactorFoundationHasAStableRouteDistinctFromClassicAndDex() {
        assertEquals("reactor/foundation", Screen.ReactorFoundation.route)
        assertNotEquals(Screen.MoleculeGame.route, Screen.ReactorFoundation.route)
        assertNotEquals(Screen.MoleculeDex.route, Screen.ReactorFoundation.route)
    }
}
