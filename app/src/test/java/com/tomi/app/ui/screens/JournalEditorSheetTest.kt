package com.tomi.app.ui.screens

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tomi.app.data.local.JournalEntity
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
class JournalEditorSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `save stays disabled until the entry has content`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalEditorSheet(
                    entry = JournalEntity(title = "", body = "", createdAt = 0L, updatedAt = 0L),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }
}
