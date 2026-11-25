package com.tomi.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class LicenseAssetsTest {

    @Test
    fun `app bundles its own and dependency license notices`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notices = context.assets.open("licenses.txt").bufferedReader().use { it.readText() }

        assertTrue(notices.contains("MIT License"))
        assertTrue(notices.contains("Apache License"))
        assertTrue(notices.contains("Version 2.0, January 2004"))
        assertTrue(notices.contains("Jetpack Compose"))
    }
}
