package com.tomi.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TaskDueDateTest {

    @Test
    fun `date picker uses UTC calendar days`() {
        val date = LocalDate.of(2026, 11, 24)

        val millis = TaskDueDate.toPickerMillis(date)
        val restored = TaskDueDate.fromPickerMillis(millis)

        assertEquals(date, restored)
        assertEquals(Instant.parse("2026-11-24T00:00:00Z"), Instant.ofEpochMilli(millis))
    }
}
