package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import org.junit.Rule
import org.junit.Test

class ReactorItemExperimentComposeTest {
    @get:Rule val compose = createComposeRule()
    private fun click(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick() }
    private fun start() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.3f)) {
                ChemTableTheme { ReactorItemExperimentEntry() }
            }
        }
        compose.onNodeWithTag("p5_open").performClick()
    }
    @Test fun linkRejectWrongEnzymeCleaveAndReuseAt130Percent() {
        start()
        click("p5_cell_0"); click("p5_cell_2"); click("p5_link")
        compose.onNodeWithTag("p5_budget").assertTextEquals("무료 실험 자원: 5/6")
        click("p5_cell_0"); click("p5_cell_1"); click("p5_enzyme_B")
        compose.onNodeWithTag("p5_result").assertTextContains("효소와 기질이 맞지 않습니다.",substring=true)
        compose.onNodeWithTag("p5_budget").assertTextEquals("무료 실험 자원: 5/6")
        click("p5_enzyme_A")
        compose.onNodeWithTag("p5_cell_1").assertTextContains("A1")
        click("p5_cell_0"); click("p5_cell_1"); click("p5_link")
        compose.onNodeWithTag("p5_budget").assertTextEquals("무료 실험 자원: 3/6")
        click("p5_close")
        compose.onNodeWithTag("p5_open").performClick()
        compose.onNodeWithTag("p5_budget").assertTextEquals("무료 실험 자원: 3/6")
        click("p5_reset")
        compose.onNodeWithTag("p5_budget").assertTextEquals("무료 실험 자원: 6/6")
        click("p5_close")
    }
    @Test fun syntheticAndInvalidSelectionsLeaveBudgetUnchanged() {
        start()
        compose.onNodeWithTag("p5_link").assertIsNotEnabled()
        click("p5_cell_24"); click("p5_cell_23"); click("p5_enzyme_A")
        compose.onNodeWithTag("p5_result").assertTextContains("합성 기질에는 효소를 사용할 수 없습니다.",substring=true)
        compose.onNodeWithTag("p5_budget").assertTextEquals("무료 실험 자원: 6/6")
        click("p5_close")
    }
}
