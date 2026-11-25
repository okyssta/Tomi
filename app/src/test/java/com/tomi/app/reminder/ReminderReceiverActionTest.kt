package com.tomi.app.reminder

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderReceiverActionTest {

    @Test
    fun `manual clock broadcast uses the Android TIME_SET action`() {
        assertEquals("android.intent.action.TIME_SET", Intent.ACTION_TIME_CHANGED)
    }
}
