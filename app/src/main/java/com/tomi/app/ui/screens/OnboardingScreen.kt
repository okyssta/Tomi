package com.tomi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    initialInterval: Int,
    onComplete: (Int) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            title = "Move a little, often",
            body = "Tomi gently reminds you to stand, stretch, or walk during long desk sessions.",
        ),
        OnboardingPage(
            icon = Icons.Outlined.HealthAndSafety,
            title = "A calmer workday",
            body = "Keep tasks, notes, mood check-ins, water, and movement in one simple place.",
        ),
        OnboardingPage(
            icon = Icons.Outlined.Lock,
            title = "Your data stays yours",
            body = "Everything is stored on this device. Step data is read through Google Health Connect and is never uploaded by Tomi.",
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var interval by remember { mutableIntStateOf(initialInterval) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = pages[page].icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = pages[page].title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    Text(
                        text = pages[page].body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (page == 0) {
                        Text(
                            text = "Remind me every $interval minutes",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 32.dp),
                        )
                        Slider(
                            value = interval.toFloat(),
                            onValueChange = { interval = it.toInt() },
                            valueRange = 30f..45f,
                            steps = 14,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pages.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            .background(
                                if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = pagerState.currentPage > 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                ) { Text("Back") }
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onRequestNotifications()
                            onComplete(interval)
                        }
                    },
                ) { Text(if (pagerState.currentPage < pages.lastIndex) "Next" else "Get started") }
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
)
