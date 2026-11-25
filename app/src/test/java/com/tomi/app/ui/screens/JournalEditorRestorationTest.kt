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
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
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

import com.tomi.app.data.local.JournalEntity
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
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
class JournalEditorRestorationTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `unsaved journal fields survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalEditorSheet(
                    entry = JournalEntity(
                        id = 1L,
                        title = "Saved title",
                        body = "Saved body",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        onNode(hasSetTextAction() and hasText("Title")).performTextInput(" draft")
        onNode(hasSetTextAction() and hasText("Write what is on your mind")).performTextInput(" details")
        onNode(hasSetTextAction() and hasText("Tags")).performTextInput("work")
        onNodeWithText("🙂 Good").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Pin entry").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("🙂 Good").assertIsSelected()
        onNodeWithText("Pin entry").assertIsOn()

        restorationTester.emulateSaveAndRestore()

        val title = onNode(hasSetTextAction() and hasText("Title"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        val body = onNode(hasSetTextAction() and hasText("Write what is on your mind"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        val tags = onNode(hasSetTextAction() and hasText("Tags"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertTrue(title.contains("draft"))
        assertTrue(body.contains("details"))
        assertEquals("work", tags)
        onNodeWithText("🙂 Good").assertIsSelected()
        onNodeWithText("Pin entry").assertIsOn()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `open journal editor waits for restored ViewModel state`() = runComposeUiTest {
        val entry = JournalEntity(id = 10L, title = "Saved entry", body = "Saved body", createdAt = 1L, updatedAt = 1L)
        val restorationTester = StateRestorationTester(this)
        var entries by mutableStateOf(listOf(entry))
        var recreateWithEmptyState by mutableStateOf(false)
        restorationTester.setContent {
            DisposableEffect(Unit) {
                onDispose { recreateWithEmptyState = true }
            }
            TomiTheme(settings = TomiSettings()) {
                JournalScreen(
                    entries = if (recreateWithEmptyState) emptyList() else entries,
                    onSaveEntry = { _, _ -> },
                    onDeleteEntry = {},
                    onTogglePin = {},
                )
            }
        }

        onNodeWithContentDescription("Journal options").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Edit").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasSetTextAction() and hasText("Title")).performTextInput(" pending")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()
        onNodeWithText("Edit entry").assertDoesNotExist()
        runOnIdle {
            recreateWithEmptyState = false
            entries = listOf(entry)
        }
        waitForIdle()

        onNodeWithText("Edit entry").assertIsDisplayed()
        val draft = onNode(hasSetTextAction() and hasText("Title"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertTrue(draft.contains("pending"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `in flight save state does not lock the restored journal draft`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val entry = JournalEntity(id = 1L, title = "Entry", body = "Body", createdAt = 1L, updatedAt = 1L)
        var saveCalls = 0
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalEditorSheet(
                    entry = entry,
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
    fun `journal filter and delete dialog survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val entry = JournalEntity(id = 10L, title = "Saved entry", body = "Body", createdAt = 1L, updatedAt = 1L)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalScreen(
                    entries = listOf(entry),
                    onSaveEntry = { _, _ -> },
                    onDeleteEntry = {},
                    onTogglePin = {},
                )
            }
        }

        onNodeWithContentDescription("Journal options").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Delete journal entry?").assertIsDisplayed()
        onNodeWithText("Pinned").performClick()
        onNodeWithText("Pinned").assertIsSelected()
        onNodeWithText("Delete journal entry?").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Pinned").assertIsSelected()
        onNodeWithText("Delete journal entry?").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `new journal editor identity and draft survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalScreen(
                    entries = emptyList(),
                    onSaveEntry = { _, _ -> },
                    onDeleteEntry = {},
                    onTogglePin = {},
                )
            }
        }

        onNodeWithText("New entry", useUnmergedTree = true).performClick()
        onNode(hasSetTextAction() and hasText("Title")).performTextInput("New draft")
        waitForIdle()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Delete").assertDoesNotExist()
        val draft = onNode(hasSetTextAction() and hasText("Title"))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("New draft", draft)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `completed journal save closes the current editor after recreation`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val entry = JournalEntity(id = 10L, title = "Saved entry", body = "Body", createdAt = 1L, updatedAt = 1L)
        val pendingResults = mutableListOf<(Boolean) -> Unit>()
        var saveCalls = 0
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalScreen(
                    entries = listOf(entry),
                    onSaveEntry = { _, onSaved ->
                        saveCalls++
                        pendingResults += onSaved
                    },
                    onDeleteEntry = {},
                    onTogglePin = {},
                )
            }
        }

        onNodeWithContentDescription("Journal options").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Edit").performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasSetTextAction() and hasText("Title")).performTextInput(" draft")
        onNodeWithText("Save").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, saveCalls)

        restorationTester.emulateSaveAndRestore()
        onNodeWithText("Edit entry").assertIsDisplayed()

        runOnIdle { pendingResults.single().invoke(true) }
        waitForIdle()

        onNodeWithText("Edit entry").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `old journal save completion cannot dismiss a newly opened editor`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val firstEntry = JournalEntity(id = 10L, title = "First entry", body = "First body", createdAt = 1L, updatedAt = 1L)
        val secondEntry = JournalEntity(id = 11L, title = "Second entry", body = "Second body", createdAt = 1L, updatedAt = 1L)
        var entries by mutableStateOf(listOf(firstEntry, secondEntry))
        val pendingResults = mutableListOf<(Boolean) -> Unit>()
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalScreen(
                    entries = entries,
                    onSaveEntry = { _, onSaved -> pendingResults += onSaved },
                    onDeleteEntry = {},
                    onTogglePin = {},
                    editorContent = { entry, onDismiss, onSave, _ ->
                        Column {
                            Text("Edit entry")
                            Text(entry.title)
                            Button(onClick = { onSave(entry) {} }) { Text("Save") }
                            Button(onClick = onDismiss) { Text("Dismiss test") }
                        }
                    },
                )
            }
        }

        onAllNodesWithContentDescription("Journal options")[0].performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasText("Edit", substring = false) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Save").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Edit entry").assertExists()
        onNodeWithText("Dismiss test").performSemanticsAction(SemanticsActions.OnClick)
        onAllNodesWithContentDescription("Journal options")[1].performSemanticsAction(SemanticsActions.OnClick)
        onNode(hasText("Edit", substring = false) and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)

        runOnIdle { pendingResults.single().invoke(true) }
        waitForIdle()

        onNodeWithText("Edit entry").assertExists()
        onAllNodesWithText("Second entry").assertCountEquals(2)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `rapid journal save clicks cannot create duplicate entries`() = runComposeUiTest {
        var saveCalls = 0
        setContent {
            TomiTheme(settings = TomiSettings()) {
                JournalScreen(
                    entries = emptyList(),
                    onSaveEntry = { _, _ -> saveCalls++ },
                    onDeleteEntry = {},
                    onTogglePin = {},
                )
            }
        }

        onNodeWithText("New entry", useUnmergedTree = true).performClick()
        onNode(hasSetTextAction() and hasText("Title")).performTextInput("Only once")
        val save = onNodeWithText("Save").performScrollTo().fetchSemanticsNode()
        repeat(3) {
            save.config[SemanticsActions.OnClick].action?.invoke()
        }
        waitForIdle()

        assertEquals(1, saveCalls)
    }
}
