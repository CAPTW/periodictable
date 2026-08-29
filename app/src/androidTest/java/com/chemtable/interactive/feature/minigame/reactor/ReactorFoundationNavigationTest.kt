package com.chemtable.interactive.feature.minigame.reactor

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chemtable.interactive.MainActivity
import org.junit.Rule
import org.junit.Test

class ReactorFoundationNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun visualizationKeepsClassicAndDexCardsAndOpensDistinctReactorFoundation() {
        composeRule.onNodeWithContentDescription("시각화", useUnmergedTree = true).performClick()

        composeRule.onNodeWithText("분자 만들기").assertExists()
        composeRule.onNodeWithText("분자 도감").assertExists()
        composeRule.onNodeWithTag("visualization_reactor_foundation_card")
            .assertExists()
            .performClick()

        composeRule.onNodeWithText(ReactorFoundationTitle).assertExists()
        composeRule.onNodeWithText(ReactorFoundationDisclaimer).assertExists()
        composeRule.onNodeWithTag("reactor_board", useUnmergedTree = true).assertExists()
    }
}
