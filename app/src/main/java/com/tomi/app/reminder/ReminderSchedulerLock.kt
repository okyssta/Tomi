package com.tomi.app.reminder

import kotlinx.coroutines.sync.Mutex

internal object ReminderSchedulerLock {
    val mutex = Mutex()
}
