package org.keepassxc.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testUnlockScreenDisplayed() {
        // Initially, the unlock screen should be visible since no DB is unlocked
        composeTestRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
    }
}
