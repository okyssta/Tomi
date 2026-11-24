package com.tomi.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `onboarding pages advance with the standard next button`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                OnboardingScreen(
                    initialInterval = 40,
                    onComplete = {},
                    onRequestNotifications = {},
                )
            }
        }

        composeRule.onNodeWithText("Move a little, often").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("A calmer workday").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your data stays yours").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
    }

}
