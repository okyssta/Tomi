package com.tomi.app.domain.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TrackedPrayer(
    val displayName: String,
    internal val adhan: Prayer,
) {
    FAJR("Fajr", Prayer.FAJR),
    DHUHR("Dhuhr", Prayer.DHUHR),
    ASR("Asr", Prayer.ASR),
    MAGHRIB("Maghrib", Prayer.MAGHRIB),
    ISHA("Isha", Prayer.ISHA),
    ;

    companion object {
        fun fromName(name: String): TrackedPrayer? = entries.firstOrNull { it.name == name }
    }
}

data class PrayerDay(
    val date: LocalDate,
    val times: Map<TrackedPrayer, Instant>,
    val sunrise: Instant,
)

data class NextPrayer(
    val prayer: TrackedPrayer,
    val at: Instant,
    val tomorrow: Boolean,
)

data class PrayerMethodOption(val id: String, val label: String)
data class PrayerMadhabOption(val id: String, val label: String)

object PrayerSchedule {
    val methods = listOf(
        PrayerMethodOption("KARACHI", "Karachi"),
        PrayerMethodOption("MUSLIM_WORLD_LEAGUE", "Muslim World League"),
        PrayerMethodOption("NORTH_AMERICA", "ISNA (North America)"),
    )
    val madhabs = listOf(
        PrayerMadhabOption("HANAFI", "Hanafi"),
        PrayerMadhabOption("SHAFI", "Shafi"),
    )

    fun day(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        methodId: String,
        madhabId: String,
    ): PrayerDay {
        val method = runCatching { CalculationMethod.valueOf(methodId) }
            .getOrDefault(CalculationMethod.KARACHI)
        val parameters = method.parameters.apply {
            madhab = if (madhabId == "HANAFI") Madhab.HANAFI else Madhab.SHAFI
        }
        val times = PrayerTimes(
            Coordinates(latitude, longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters,
        )
        return PrayerDay(
            date = date,
            times = TrackedPrayer.entries.associateWith { tracked ->
                times.timeForPrayer(tracked.adhan).toInstant()
            },
            sunrise = times.sunrise.toInstant(),
        )
    }

    fun next(
        latitude: Double,
        longitude: Double,
        now: Instant,
        methodId: String,
        madhabId: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): NextPrayer {
        val today = now.atZone(zone).toLocalDate()
        val todayTimes = day(latitude, longitude, today, methodId, madhabId).times
        val current = todayTimes.entries
            .filter { it.value.isAfter(now) }
            .minByOrNull { it.value }
        if (current != null) {
            return NextPrayer(current.key, current.value, tomorrow = false)
        }
        val tomorrow = day(latitude, longitude, today.plusDays(1), methodId, madhabId).times
        val fajr = tomorrow.getValue(TrackedPrayer.FAJR)
        return NextPrayer(TrackedPrayer.FAJR, fajr, tomorrow = true)
    }

    fun nextFromDay(day: PrayerDay, now: Instant): NextPrayer? {
        val next = day.times.entries.filter { it.value.isAfter(now) }.minByOrNull { it.value }
            ?: return null
        return NextPrayer(next.key, next.value, tomorrow = false)
    }
}
