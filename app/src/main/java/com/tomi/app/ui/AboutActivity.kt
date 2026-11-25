package com.tomi.app.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tomi.app.BuildConfig

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TomiTheme(settings = com.tomi.app.data.settings.TomiSettings()) {
                AboutScreen(
                    versionName = BuildConfig.VERSION_NAME,
                    onOpenRepository = { openUrl("https://github.com/okyssta/Tomi") },
                    onOpenPrivacy = { openUrl(BuildConfig.PRIVACY_POLICY_URL) },
                )
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onOpenRepository: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    val notices = remember {
        context.assets.open("licenses.txt").bufferedReader().use { it.readText() }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("About & licenses") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Tomi $versionName", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Offline-first movement, tasks, journal, and Health Connect steps for desk workers.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenRepository, modifier = Modifier.fillMaxWidth()) { Text("Source repository") }
                    OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) { Text("Privacy policy") }
                }
            }
            item { Text("Open-source licenses", style = MaterialTheme.typography.titleLarge) }
            item {
                SelectionContainer {
                    Text(
                        text = notices,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
