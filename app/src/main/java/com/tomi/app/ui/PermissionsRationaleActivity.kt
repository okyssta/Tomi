package com.tomi.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomi.app.BuildConfig
import com.tomi.app.data.settings.TomiSettings

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TomiTheme(settings = TomiSettings()) {
                PermissionRationaleScreen(
                    onDone = { finish() },
                    onOpenPrivacy = { startActivity(Intent(Intent.ACTION_VIEW, BuildConfig.PRIVACY_POLICY_URL.toUri())) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionRationaleScreen(onDone: () -> Unit, onOpenPrivacy: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Privacy & permissions") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Why Tomi asks for step access", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Tomi reads aggregated step totals from Health Connect to show your daily progress and pause movement reminders after you have walked. On Android 14+, Health Connect can count phone steps automatically. Steps written by Google Fit can also be included when Google Fit is connected to Health Connect.",
            )
            Text("What Tomi does not do", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tomi does not write or delete Health Connect records, upload health data, track your location, create an account, or use analytics. Tasks, journal entries, settings, and cached step totals stay in Tomi's local database.",
            )
            Text("Optional background access", style = MaterialTheme.typography.titleMedium)
            Text(
                "Background step access lets Tomi refresh its widget and detect movement while the app is closed. If you decline it, Tomi still works when open and movement reminders continue on schedule.",
            )
            Text("You stay in control", style = MaterialTheme.typography.titleMedium)
            Text(
                "You can revoke Health Connect, notification, exact alarm, or update-install permissions at any time in Android Settings. Export or clear your Tomi data from the app's Settings screen.",
            )
            Button(onClick = onOpenPrivacy) { Text("Read privacy policy") }
            Button(onClick = onDone) { Text("Got it") }
        }
    }
}
