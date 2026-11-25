package com.tomi.app.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `weekday chips expose full localized accessibility names`() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            var toggled: DayOfWeek? = null
            composeRule.setContent {
                TomiTheme(settings = TomiSettings()) {
                    WeekdayChips(
                        activeDays = setOf(DayOfWeek.MONDAY),
                        onDayToggle = { toggled = it },
                    )
                }
            }

            DayOfWeek.entries.forEach { day ->
                val name = day.getDisplayName(TextStyle.FULL, Locale.US)
                composeRule.onNodeWithContentDescription(name).assertExists()
            }
            composeRule.onNodeWithContentDescription("Monday").assertIsSelected()
            composeRule.onNodeWithContentDescription("Wednesday").performClick()
            composeRule.runOnIdle { assertEquals(DayOfWeek.WEDNESDAY, toggled) }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun `privacy policy has one click target instead of nested row and button`() {
        var opened = 0
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                PrivacyPolicyRow(onOpenPrivacy = { opened += 1 })
            }
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNode(hasText("Open") and hasClickAction()).performClick()
        composeRule.runOnIdle { assertEquals(1, opened) }
    }
}
