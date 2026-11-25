package com.tomi.app.update

import android.content.Context

object UpdateCheckScheduler {
    suspend fun checkIfDue(context: Context, force: Boolean = false) = Unit
    fun scheduleDaily(context: Context) = Unit
}
