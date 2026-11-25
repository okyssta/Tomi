package com.tomi.app.reminder

import android.app.Application
import android.content.Intent
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
class ReminderReceiverManifestTest {

    @Test
    fun `manual clock changes are routed to the reminder receiver`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = Intent(Intent.ACTION_TIME_CHANGED).setPackage(context.packageName)
        val receivers = context.packageManager.queryBroadcastReceivers(intent, 0)

        assertTrue(receivers.any { it.activityInfo.name == BootReceiver::class.java.name })
    }
}
