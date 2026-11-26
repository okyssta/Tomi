package com.tomi.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class SettingsCorruptionTest {
    @Test
    fun `corrupt settings file falls back to defaults`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "datastore/tomi_settings.preferences_pb")
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x7f, 0x00, 0x01, 0x02, 0x03))

        val settings = SettingsRepository.from(context).settings.first()

        assertEquals(TomiSettings(), settings)
    }
}
