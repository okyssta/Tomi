package com.tomi.app.reminder

import kotlinx.coroutines.sync.Mutex

object MovementCheckpointLock {
    val mutex = Mutex()
}
