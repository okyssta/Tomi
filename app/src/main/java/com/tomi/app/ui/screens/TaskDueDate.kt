package com.tomi.app.ui.screens

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal object TaskDueDate {
    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
