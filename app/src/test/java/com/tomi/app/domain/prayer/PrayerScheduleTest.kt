package com.tomi.app.domain.prayer

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerScheduleTest {
    private val lahoreLatitude = 31.5204
    private val lahoreLongitude = 74.3587
    private val date = LocalDate.of(2026, 11, 26)

    @Test
    fun `karachi method produces five ordered prayers and sunrise`() {
        val day = PrayerSchedule.day(lahoreLatitude, lahoreLongitude, date, "KARACHI", "HANAFI")

        assertEquals(TrackedPrayer.entries.toSet(), day.times.keys)
        assertTrue(day.times.getValue(TrackedPrayer.FAJR) < day.times.getValue(TrackedPrayer.DHUHR))
        assertTrue(day.times.getValue(TrackedPrayer.DHUHR) < day.times.getValue(TrackedPrayer.ASR))
        assertTrue(day.times.getValue(TrackedPrayer.ASR) < day.times.getValue(TrackedPrayer.MAGHRIB))
        assertTrue(day.times.getValue(TrackedPrayer.MAGHRIB) < day.times.getValue(TrackedPrayer.ISHA))
        assertTrue(day.times.getValue(TrackedPrayer.FAJR) < day.sunrise)
        assertTrue(day.sunrise < day.times.getValue(TrackedPrayer.DHUHR))
    }

    @Test
    fun `next prayer rolls over to fajr tomorrow after isha`() {
        val day = PrayerSchedule.day(lahoreLatitude, lahoreLongitude, date, "KARACHI", "HANAFI")
        val afterIsha = day.times.getValue(TrackedPrayer.ISHA).plusSeconds(60)

        val next = PrayerSchedule.next(lahoreLatitude, lahoreLongitude, afterIsha, "KARACHI", "HANAFI")

        assertEquals(TrackedPrayer.FAJR, next.prayer)
        assertTrue(next.tomorrow)
        assertTrue(next.at > afterIsha)
    }

    @Test
    fun `asr differs between hanafi and shafi calculations`() {
        val hanafi = PrayerSchedule.day(lahoreLatitude, lahoreLongitude, date, "KARACHI", "HANAFI")
        val shafi = PrayerSchedule.day(lahoreLatitude, lahoreLongitude, date, "KARACHI", "SHAFI")

        assertTrue(hanafi.times.getValue(TrackedPrayer.ASR) > shafi.times.getValue(TrackedPrayer.ASR))
    }
}
