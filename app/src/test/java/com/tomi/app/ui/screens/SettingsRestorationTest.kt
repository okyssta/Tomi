package com.tomi.app.ui.screens

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.core.app.ActivityOptionsCompat
import com.tomi.app.data.settings.TomiSettings
import com.tomi.app.ui.TomiTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class SettingsRestorationTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `open reminder time picker survives state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                ReminderTimePickerRow(title = "Start time", minuteOfDay = 480, onTimeSelected = {})
            }
        }

        onNodeWithContentDescription("Change reminder start time").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("Reminder start time").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Reminder start time").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `open clear all confirmation survives state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                ClearAllDataRow(onClearAllData = {})
            }
        }

        onNodeWithText("Clear").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("Clear all Tomi data?").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Clear all Tomi data?").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `open background health confirmation survives state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            TomiTheme(settings = TomiSettings()) {
                BackgroundHealthRow(granted = false, onRequestBackgroundPermissions = {})
            }
        }

        onNodeWithText("Allow").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        onNodeWithText("Allow background step access?").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Allow background step access?").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `pending import URI and confirmation survive state restoration`() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val registry = TestActivityResultRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        val backupUri = Uri.parse("content://backups/tomi.json")
        var importedUri: Uri? = null
        restorationTester.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides owner,
            ) {
                TomiTheme(settings = TomiSettings()) {
                    BackupImportRow(onImport = { importedUri = it })
                }
            }
        }

        onNodeWithText("Import").performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        runOnIdle { registry.dispatchResult(backupUri) }
        waitForIdle()
        onNodeWithText("Replace Tomi data?").assertIsDisplayed()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Replace Tomi data?").assertIsDisplayed()
        onNodeWithText("Replace data").performSemanticsAction(SemanticsActions.OnClick)
        runOnIdle { assertEquals(backupUri, importedUri) }
    }
}

private class TestActivityResultRegistry : ActivityResultRegistry() {
    private var requestCode: Int? = null

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        this.requestCode = requestCode
    }

    fun dispatchResult(result: Uri?) {
        checkNotNull(requestCode).let { dispatchResult(it, result) }
    }
}
