package com.tomi.app.widget

import java.time.Instant
import java.time.ZonedDateTime

object WidgetText {
    fun nextReminder(enabled: Boolean, next: ZonedDateTime?, now: Instant): String {
        if (!enabled) return "Reminders off"
        if (next == null) return "No reminder scheduled"
        val minutes = (next.toInstant().toEpochMilli() - now.toEpochMilli()) / 60_000L
        return when {
            minutes <= 0 -> "Time to move"
            minutes < 60 -> "Next reminder in $minutes min"
            else -> "Next reminder in ${minutes / 60} hr ${minutes % 60} min"
        }
    }
}
