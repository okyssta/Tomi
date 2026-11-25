package com.tomi.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText

import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest

import com.tomi.app.data.local.TaskEntity
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class TaskEditorRestorationTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `unsaved task fields survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(
                        id = 1L,
                        title = "Saved title",
                        notes = "Saved notes",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        onNode(hasSetTextAction() and hasText("Task")).performTextInput(" draft")
        onNode(hasSetTextAction() and hasText("Notes")).performTextInput(" details")
        onNode(hasSetTextAction() and hasText("Category")).performTextInput("Work")
        onNodeWithText("High").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("High").assertIsSelected()

        restorationTester.emulateSaveAndRestore()

        val title = onNode(hasSetTextAction() and hasText("Task"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        val notes = onNode(hasSetTextAction() and hasText("Notes"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        val category = onNode(hasSetTextAction() and hasText("Category"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertTrue(title.contains("draft"))
        assertTrue(notes.contains("details"))
        assertEquals("Work", category)
        onNodeWithText("High").assertIsSelected()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `unsaved due date and time survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val dueAt = LocalDate.of(2026, 11, 25)
            .atTime(18, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(id = 1L, title = "Task", dueAt = dueAt, createdAt = 1L, updatedAt = 1L),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        onNodeWithContentDescription("Choose due date").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNode(hasText("November 26, 2026", substring = true) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("OK").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("Thu, Nov 26, 2026").performScrollTo().assertIsDisplayed()

        onNodeWithContentDescription("Choose due time").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithContentDescription("19 hours").performClick()
        waitForIdle()
        onNodeWithContentDescription("Select minutes").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNode(
            hasContentDescription("0 minutes") and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.Role),
        ).performClick()
        onNodeWithText("OK").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("19:00").performScrollTo().assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Thu, Nov 26, 2026").performScrollTo().assertIsDisplayed()
        onNodeWithText("19:00").performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `open task editor waits for restored ViewModel state`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val task = TaskEntity(id = 8L, title = "Saved task", createdAt = 1L, updatedAt = 1L)
        var tasks by mutableStateOf(listOf(task))
        var recreateWithEmptyState by mutableStateOf(false)
        restorationTester.setContent {
            DisposableEffect(Unit) {
                onDispose { recreateWithEmptyState = true }
            }
            TomiTheme(settings = TomiSettings()) {
                TasksScreen(
                    tasks = if (recreateWithEmptyState) emptyList() else tasks,
                    onToggleTask = {},
                    onSaveTask = { _, _ -> },
                    onDeleteTask = {},
                )
            }
        }

        onNodeWithContentDescription("Task options").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Edit").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasSetTextAction() and hasText("Task")).performTextInput(" pending")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        onNodeWithText("Edit task").assertDoesNotExist()
        runOnIdle {
            recreateWithEmptyState = false
            tasks = listOf(task)
        }
        waitForIdle()

        onNodeWithText("Edit task").assertIsDisplayed()
        val draft = onNode(hasSetTextAction() and hasText("Task"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertTrue(draft.contains("pending"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `in flight save state does not lock the restored draft`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val task = TaskEntity(id = 1L, title = "Task", createdAt = 1L, updatedAt = 1L)
        var saveCalls = 0
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = task,
                    onDismiss = {},
                    onSave = { _, _ -> saveCalls++ },
                    onDelete = {},
                )
            }
        }

        onNodeWithText("Save").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Saving…").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Save").performScrollTo().assertIsDisplayed().assertIsEnabled()
        val save = onNodeWithText("Save").fetchSemanticsNode()
        save.config[SemanticsActions.OnClick].action?.invoke()
        assertEquals(2, saveCalls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `task filter and delete dialog survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val task = TaskEntity(id = 8L, title = "Saved task", createdAt = 1L, updatedAt = 1L)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TasksScreen(
                    tasks = listOf(task),
                    onToggleTask = {},
                    onSaveTask = { _, _ -> },
                    onDeleteTask = {},
                )
            }
        }

        onNodeWithContentDescription("Task options").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete task?").assertIsDisplayed()
        onNodeWithText("Done").performClick()
        onNodeWithText("Delete task?").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Done").assertIsSelected()
        onNodeWithText("Delete task?").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `task picker selection and open time picker survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TaskEditorSheet(
                    task = TaskEntity(id = 1L, title = "Task", createdAt = 1L, updatedAt = 1L),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        onNodeWithContentDescription("Choose due date").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasText("November 26, 2026", substring = true) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("OK").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithContentDescription("Choose due time").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithContentDescription("19 hours").performClick()
        onNodeWithText("OK").performSemanticsAction(SemanticsActions.OnClick)

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Thu, Nov 26, 2026").performScrollTo().assertIsDisplayed()
        onNodeWithText("19:00").performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `new task editor identity and draft survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TasksScreen(
                    tasks = emptyList(),
                    onToggleTask = {},
                    onSaveTask = { _, _ -> },
                    onDeleteTask = {},
                )
            }
        }

        onNodeWithContentDescription("Add task").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasSetTextAction() and hasText("Task")).performTextInput("New draft")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("New task").assertIsDisplayed()
        val draft = onNode(hasSetTextAction() and hasText("Task"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("New draft", draft)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `completed task save closes the current editor after recreation`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val task = TaskEntity(id = 8L, title = "Saved task", createdAt = 1L, updatedAt = 1L)
        val pendingResults = mutableListOf<(Boolean) -> Unit>()
        var saveCalls = 0
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TasksScreen(
                    tasks = listOf(task),
                    onToggleTask = {},
                    onSaveTask = { _, onSaved ->
                        saveCalls++
                        pendingResults += onSaved
                    },
                    onDeleteTask = {},
                )
            }
        }

        onNodeWithContentDescription("Task options").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Edit").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasSetTextAction() and hasText("Task")).performTextInput(" draft")
        onNodeWithText("Save").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, saveCalls)

        restorationTester.emulateSaveAndRestore()
        onNodeWithText("Edit task").assertIsDisplayed()

        runOnIdle { pendingResults.single().invoke(true) }
        waitForIdle()

        onNodeWithText("Edit task").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `old task save completion cannot dismiss a newly opened editor`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val firstTask = TaskEntity(id = 8L, title = "First task", createdAt = 1L, updatedAt = 1L)
        val secondTask = TaskEntity(id = 9L, title = "Second task", createdAt = 1L, updatedAt = 1L)
        var tasks by mutableStateOf(listOf(firstTask, secondTask))
        val pendingResults = mutableListOf<(Boolean) -> Unit>()
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                TasksScreen(
                    tasks = tasks,
                    onToggleTask = {},
                    onSaveTask = { _, onSaved -> pendingResults += onSaved },
                    onDeleteTask = {},
                    editorContent = { task, onDismiss, onSave, _ ->
                        Column {
                            Text("Edit task")
                            Text(task.title)
                            Button(onClick = { onSave(task) {} }) { Text("Save") }
                            Button(onClick = onDismiss) { Text("Dismiss test") }
                        }
                    },
                )
            }
        }

        onAllNodesWithContentDescription("Task options")[0].performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasText("Edit", substring = false) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Save").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Edit task").assertExists()
        onNodeWithText("Dismiss test").performSemanticsAction(SemanticsActions.OnClick)
        onAllNodesWithContentDescription("Task options")[1].performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasText("Edit", substring = false) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)

        runOnIdle { pendingResults.single().invoke(true) }
        waitForIdle()

        onNodeWithText("Edit task").assertExists()
        onAllNodesWithText("Second task").assertCountEquals(2)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `rapid task save clicks cannot create duplicate tasks`() = runComposeUiTest {
        var saveCalls = 0
        setContent {
            TomiTheme(settings = TomiSettings()) {
                TasksScreen(
                    tasks = emptyList(),
                    onToggleTask = {},
                    onSaveTask = { _, _ -> saveCalls++ },
                    onDeleteTask = {},
                )
            }
        }

        onNodeWithContentDescription("Add task").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasSetTextAction() and hasText("Task")).performTextInput("Only once")
        val save = onNodeWithText("Save").performScrollTo().fetchSemanticsNode()
        repeat(3) {
            save.config[SemanticsActions.OnClick].action?.invoke()
        }
        waitForIdle()

        assertEquals(1, saveCalls)
    }
}
