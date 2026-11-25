package com.tomi.app.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayContext(val date: LocalDate, val zone: ZoneId)

class CurrentDay(
    private val clock: () -> Instant = Instant::now,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
) {
    private val _context = MutableStateFlow(snapshot())
    val context: StateFlow<DayContext> = _context.asStateFlow()

    fun refresh() {
        _context.value = snapshot()
    }

    private fun snapshot(): DayContext {
        val zone = zoneProvider()
        return DayContext(clock().atZone(zone).toLocalDate(), zone)
    }
}
