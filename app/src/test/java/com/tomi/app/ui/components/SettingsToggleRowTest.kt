package com.tomi.app.ui.components

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class SettingsToggleRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `whole row is one named toggle`() {
        var changed = false
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                SettingsToggleRow(
                    title = "Smart pause",
                    supportingText = "Reset after walking",
                    checked = false,
                    onCheckedChange = { changed = true },
                )
            }
        }

        composeRule.onNodeWithText("Smart pause").assertIsToggleable().assertIsOff().performClick()
        composeRule.runOnIdle { assertTrue(changed) }
    }
}
