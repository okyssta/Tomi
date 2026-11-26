package com.tomi.app.domain.addons

enum class Addon(
    val id: String,
    val title: String,
    val description: String,
) {
    NAMAZ(
        id = "namaz",
        title = "Namaz times & tracker",
        description = "Prayer times for your location and a daily offered/not-offered tracker.",
    ),
    ;

    companion object {
        fun fromId(id: String): Addon? = entries.firstOrNull { it.id == id }
    }
}
