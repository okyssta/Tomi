package com.tomi.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tomi.app.data.local.MovementType
import com.tomi.app.ui.screens.HomeScreen
import com.tomi.app.ui.screens.JournalScreen
import com.tomi.app.ui.screens.MoveScreen
import com.tomi.app.ui.screens.OnboardingScreen
import com.tomi.app.ui.screens.ProgressScreen
import com.tomi.app.ui.screens.SettingsScreen
import com.tomi.app.ui.screens.TasksScreen

@Composable
fun LoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator() }
}

@Composable
fun TomiNavHost(
    viewModel: TomiViewModel,
    startDestination: String,
    initialRoute: String?,
    onRouteHandled: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = setOf("home", "tasks", "journal", "progress")

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            navController.navigate(initialRoute) { launchSingleTop = true }
            onRouteHandled()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                NavigationBar {
                    BottomDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    initialInterval = 40,
                    onComplete = { interval ->
                        viewModel.completeOnboarding(interval)
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRequestNotifications = onRequestNotifications,
                )
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onStartMovement = { type -> navController.navigate("move/${type.name}") },
                    onOpenTasks = { navController.navigate("tasks") { launchSingleTop = true } },
                    onOpenSettings = { navController.navigate("settings") },
                    onConnectHealth = onRequestHealthPermissions,
                )
            }
            composable("tasks") { TasksScreen(viewModel) }
            composable("journal") { JournalScreen(viewModel) }
            composable("progress") { ProgressScreen(viewModel) }
            composable("settings") {
                SettingsScreen(viewModel = viewModel, onConnectHealth = onRequestHealthPermissions)
            }
            composable("move/{type}") { entry ->
                val type = runCatching { MovementType.valueOf(entry.arguments?.getString("type").orEmpty()) }
                    .getOrDefault(MovementType.WALK)
                MoveScreen(initialType = type, viewModel = viewModel, onDone = { navController.popBackStack() })
            }
        }
    }
}

private enum class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HOME("home", "Home", Icons.Outlined.Home),
    TASKS("tasks", "Tasks", Icons.Outlined.Checklist),
    JOURNAL("journal", "Journal", Icons.Outlined.Book),
    PROGRESS("progress", "Progress", Icons.Outlined.Insights),
}
