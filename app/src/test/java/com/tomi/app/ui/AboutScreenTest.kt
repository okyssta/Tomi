package com.tomi.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tomi.app.data.settings.TomiSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class AboutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `about screen exposes version and bundled notices`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                AboutScreen(
                    versionName = "1.1.0",
                    onOpenRepository = {},
                    onOpenPrivacy = {},
                )
            }
        }

        composeRule.onNodeWithText("Tomi 1.1.0").assertIsDisplayed()
        composeRule.onNodeWithText("Open-source licenses").assertIsDisplayed()
    }
}
