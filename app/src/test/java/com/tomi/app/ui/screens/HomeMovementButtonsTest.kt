package com.tomi.app.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class HomeMovementButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `quick movement label stays on one line in a narrow column`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                Box(Modifier.width(80.dp)) {
                    QuickMovementButton(
                        label = "Push-ups",
                        icon = Icons.Outlined.FitnessCenter,
                        onClick = {},
                    )
                }
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Push-ups").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayouts ->
            getLayouts(layouts)
        }

        assertEquals(1, layouts.single().lineCount)

        val iconBounds = composeRule.onNodeWithContentDescription("Push-ups movement", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val labelBounds = composeRule.onNodeWithText("Push-ups", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("icon=$iconBounds label=$labelBounds", iconBounds.bottom <= labelBounds.top)
    }
}
