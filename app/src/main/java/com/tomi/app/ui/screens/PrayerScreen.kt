package com.tomi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomi.app.domain.addons.Addon
import com.tomi.app.domain.prayer.PrayerDay
import com.tomi.app.domain.prayer.PrayerSchedule
import com.tomi.app.domain.prayer.TrackedPrayer
import com.tomi.app.ui.TomiViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerScreen(viewModel: TomiViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prayerLogs by viewModel.prayerLogs.collectAsStateWithLifecycle()
    val dayContext by viewModel.dayContext.collectAsStateWithLifecycle()
    var locationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(30_000L)
        }
    }
    val enabled = Addon.NAMAZ.id in settings.addonsEnabled
    val latitude = settings.prayerLatitude
    val longitude = settings.prayerLongitude
    val prayerDay = remember(latitude, longitude, dayContext.date, settings.prayerMethod, settings.prayerMadhab) {
        if (latitude == null || longitude == null) {
            null
        } else {
            runCatching {
                PrayerSchedule.day(
                    latitude = latitude,
                    longitude = longitude,
                    date = dayContext.date,
                    methodId = settings.prayerMethod,
                    madhabId = settings.prayerMadhab,
                )
            }.getOrNull()
        }
    }
    val next = remember(prayerDay, now) {
        if (prayerDay == null) {
            null
        } else {
            PrayerSchedule.nextFromDay(prayerDay, now) ?: runCatching {
                PrayerSchedule.next(
                    latitude = requireNotNull(latitude),
                    longitude = requireNotNull(longitude),
                    now = now,
                    methodId = settings.prayerMethod,
                    madhabId = settings.prayerMadhab,
                )
            }.getOrNull()
        }
    }
    val offered = prayerLogs.associate { it.prayer to it.offered }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            locationMessage = null
            viewModel.refreshPrayerLocation()
        } else {
            locationMessage = "Location permission is needed to calculate prayer times."
        }
    }

    fun requestLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            locationMessage = null
            viewModel.refreshPrayerLocation()
        } else {
            locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Namaz") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!enabled) {
                item {
                    Card {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Namaz addon is off", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Enable this addon to calculate prayer times and keep a daily offered/not-offered tracker.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = { viewModel.setAddonEnabled(Addon.NAMAZ, true) }) {
                                Text("Enable Namaz")
                            }
                        }
                    }
                }
            } else {
                item {
                    Card {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.AccessTime, contentDescription = null)
                                Text("Next prayer", style = MaterialTheme.typography.titleMedium)
                            }
                            if (prayerDay == null) {
                                Text("Set your location to calculate prayer times.", style = MaterialTheme.typography.bodyLarge)
                                Button(onClick = ::requestLocation) { Text("Use current location") }
                            } else if (next != null) {
                                Text(next.prayer.displayName, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    "${next.at.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}" +
                                        if (next.tomorrow) " · tomorrow" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            } else {
                                Text("Prayer times are unavailable for this location.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                item {
                    Card {
                        ListItem(
                            headlineContent = { Text(settings.prayerLocationName ?: "Current location") },
                            supportingContent = {
                                Text(
                                    if (prayerDay == null) {
                                        "Location is needed for accurate times"
                                    } else {
                                        "Times are calculated on-device for today"
                                    },
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                            trailingContent = {
                                TextButton(onClick = ::requestLocation) { Text("Update") }
                            },
                        )
                        locationMessage?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                if (prayerDay != null) {
                    item {
                        PrayerTimesCard(
                            day = prayerDay,
                            offered = offered,
                            onToggle = viewModel::setPrayerOffered,
                        )
                    }
                    item {
                        CalculationCard(
                            methodId = settings.prayerMethod,
                            madhabId = settings.prayerMadhab,
                            onMethod = viewModel::setPrayerMethod,
                            onMadhab = viewModel::setPrayerMadhab,
                        )
                    }
                    item {
                        Text(
                            "Prayer times are calculated on your device using your approximate location. " +
                                "The location is stored only in Tomi and is never uploaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesCard(
    day: PrayerDay,
    offered: Map<String, Boolean>,
    onToggle: (TrackedPrayer, Boolean) -> Unit,
) {
    Card {
        Column {
            ListItem(
                headlineContent = { Text("Today's prayers") },
                supportingContent = { Text("Mark the prayers you have offered") },
                leadingContent = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
            )
            TrackedPrayer.entries.forEach { prayer ->
                val at = day.times.getValue(prayer)
                val isOffered = offered[prayer.name] == true
                ListItem(
                    headlineContent = { Text(prayer.displayName) },
                    supportingContent = {
                        Text(
                            if (isOffered) "Offered" else "Not offered yet",
                            color = if (isOffered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Checkbox(
                            checked = isOffered,
                            onCheckedChange = { onToggle(prayer, it) },
                        )
                    },
                    trailingContent = {
                        Text(
                            at.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                )
            }
            ListItem(
                headlineContent = { Text("Sunrise") },
                supportingContent = { Text("Not tracked") },
                trailingContent = {
                    Text(
                        day.sunrise.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculationCard(
    methodId: String,
    madhabId: String,
    onMethod: (String) -> Unit,
    onMadhab: (String) -> Unit,
) {
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Calculation", style = MaterialTheme.typography.titleMedium)
            Text("Method", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerSchedule.methods.forEach { method ->
                    FilterChip(
                        selected = methodId == method.id,
                        onClick = { onMethod(method.id) },
                        label = { Text(method.label) },
                    )
                }
            }
            Text("Asr", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerSchedule.madhabs.forEach { madhab ->
                    FilterChip(
                        selected = madhabId == madhab.id,
                        onClick = { onMadhab(madhab.id) },
                        label = { Text(madhab.label) },
                    )
                }
            }
        }
    }
}
