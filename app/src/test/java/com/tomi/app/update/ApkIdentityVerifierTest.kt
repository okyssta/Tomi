package com.tomi.app.update

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class ApkIdentityVerifierTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `rejects files that are not APKs`() {
        val fakeApk = temporaryFolder.newFile("update.apk").apply { writeText("not an APK") }
        val context = ApplicationProvider.getApplicationContext<Application>()

        assertFalse(
            ApkIdentityVerifier.verify(
                context = context,
                apk = fakeApk,
                expectedPackage = "com.tomi.app",
                expectedVersionName = "1.0.0",
            ),
        )
    }

    @Test
    fun `rejects a missing file`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val missingApk = File(temporaryFolder.root, "missing.apk")

        assertFalse(
            ApkIdentityVerifier.verify(
                context = context,
                apk = missingApk,
                expectedPackage = "com.tomi.app",
                expectedVersionName = "1.0.0",
            ),
        )
    }
}
