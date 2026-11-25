package com.tomi.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.tomi.app.BuildConfig

class MainActivity : ComponentActivity() {
    private val viewModel: TomiViewModel by viewModels()
    private var pendingRoute by mutableStateOf<String?>(null)
    private var healthDisclosureMode by mutableStateOf<Boolean?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshHealth() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val ready by viewModel.ready.collectAsStateWithLifecycle()
            TomiTheme(settings = settings) {
                Surface {
                    healthDisclosureMode?.let { includeBackground ->
                        AlertDialog(
                            onDismissRequest = { healthDisclosureMode = null },
                            title = { Text("Allow step access?") },
                            text = {
                                Column {
                                    Text(
                                        if (includeBackground) {
                                            "Tomi can read aggregate steps while it is closed to reset your movement reminder after walking. You can revoke this optional access at any time in Health Connect."
                                        } else {
                                            "Tomi reads aggregate step totals to show daily and weekly progress. It does not write or upload health records. Background access is requested separately."
                                        },
                                    )
                                    TextButton(
                                        onClick = {
                                            startActivity(Intent(Intent.ACTION_VIEW, BuildConfig.PRIVACY_POLICY_URL.toUri()))
                                        },
                                    ) { Text("Privacy policy") }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    healthDisclosureMode = null
                                    performHealthPermissionRequest(includeBackground)
                                }) { Text("Continue") }
                            },
                            dismissButton = {
                                TextButton(onClick = { healthDisclosureMode = null }) { Text("Not now") }
                            },
                        )
                    }
                    if (ready) {
                        TomiNavHost(
                            viewModel = viewModel,
                            startDestination = if (settings.onboardingComplete) "home" else "onboarding",
                            initialRoute = pendingRoute,
                            onRouteHandled = { pendingRoute = null },
                            onRequestNotifications = ::requestNotifications,
                            onRequestHealthPermissions = { requestHealthPermissions(includeBackground = false) },
                            onRequestBackgroundHealthPermissions = { requestHealthPermissions(includeBackground = true) },
                        )
                    } else {
                        LoadingScreen()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestHealthPermissions(includeBackground: Boolean) {
        healthDisclosureMode = includeBackground
    }

    private fun performHealthPermissionRequest(includeBackground: Boolean) {
        when (HealthConnectClient.getSdkStatus(this)) {
            HealthConnectClient.SDK_UNAVAILABLE -> openHealthConnectStore()
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> openHealthConnectSettings()
            else -> {
                val permissions = mutableSetOf(HealthPermission.getReadPermission(StepsRecord::class))
                if (includeBackground) {
                    permissions += HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
                }
                healthPermissionLauncher.launch(permissions)
            }
        }
    }

    private fun openHealthConnectSettings() {
        try {
            startActivity(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"))
        } catch (_: ActivityNotFoundException) {
            openHealthConnectStore()
        }
    }

    private fun openHealthConnectStore() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=com.google.android.apps.healthdata".toUri(),
                ),
            )
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri(),
                ),
            )
        }
    }

    companion object {
        const val EXTRA_ROUTE = "com.tomi.app.extra.ROUTE"
    }
}
