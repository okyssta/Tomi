package com.tomi.app.ui

import com.tomi.app.data.local.MovementType

object ExternalRoute {
    private val routes = buildSet {
        addAll(listOf("home", "tasks", "journal", "progress", "settings"))
        MovementType.entries.forEach { add("move/${it.name}") }
    }

    fun resolve(route: String?, onboardingComplete: Boolean): String? {
        if (!onboardingComplete || route == null || route.contains("..")) return null
        return route.takeIf { it in routes }
    }
}
