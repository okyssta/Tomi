package com.tomi.app.ui.screens

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tomi.app.data.local.TaskEntity
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
class TaskEditorSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `new task editor always exposes both due date and due time`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(title = "", createdAt = 0L, updatedAt = 0L),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Due date").assertExists()
        composeRule.onNodeWithText("Due time").assertExists()
    }

    @Test
    fun `save stays disabled until the task has a title`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(title = "", createdAt = 0L, updatedAt = 0L),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `failed save keeps the draft open`() {
        var dismissed = false
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(title = "", createdAt = 0L, updatedAt = 0L),
                    onDismiss = { dismissed = true },
                    onSave = { _, onSaved -> onSaved(false) },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Task").performTextInput("Keep this draft")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction() and hasText("Task")).assertTextContains("Keep this draft")
        org.junit.Assert.assertFalse(dismissed)
    }

    @Test
    fun `editor delete asks for confirmation`() {
        composeRule.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(id = 1L, title = "Existing task", createdAt = 1L, updatedAt = 1L),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        composeRule.waitForIdle()
        val delete = composeRule.onNodeWithText("Delete").performScrollTo()
        composeRule.waitForIdle()
        delete.performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete task?").assertExists()
    }
}
