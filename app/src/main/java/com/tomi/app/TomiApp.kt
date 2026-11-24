package com.tomi.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.tomi.app.data.settings.SettingsRepository
import com.tomi.app.reminder.MovementDetectionReceiver
import com.tomi.app.reminder.ReminderNotifier
import com.tomi.app.reminder.ReminderScheduler
import com.tomi.app.reminder.ReminderWorker
import com.tomi.app.update.UpdateCheckScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TomiApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(MovementDetectionReceiver(), filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(MovementDetectionReceiver(), filter)
        }
        ReminderWorker.enqueue(this)
        UpdateCheckScheduler.scheduleDaily(this)
        scope.launch {
            ReminderScheduler(
                this@TomiApp,
                SettingsRepository.from(this@TomiApp),
                ReminderNotifier(this@TomiApp),
            ).reschedule()
        }
    }
}
