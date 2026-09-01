package com.belta.audio.core.theme

enum class ThemeMode(
    val title: String,
    val description: String
) {
    DYNAMIC_MONET(
        title = "Dynamic Material You",
        description = "Adapts automatically to system wallpaper colors"
    ),
    DYNAMIC_ALBUM_ART(
        title = "Dynamic Album Art",
        description = "UI accents dynamically morph with current playing song artwork"
    ),
    AMOLED_BLACK(
        title = "AMOLED Pure Black",
        description = "True pitch black for maximum OLED battery savings"
    ),
    MIDNIGHT_DARK(
        title = "Midnight Dark",
        description = "Deep slate dark theme for comfortable nighttime listening"
    ),
    MINIMAL_LIGHT(
        title = "Minimal Light",
        description = "Clean modern high-contrast light theme"
    ),
    CYBERPUNK_NEON(
        title = "Cyberpunk Neon",
        description = "High-energy neon cyan and magenta accents"
    ),
    FROSTED_GLASS(
        title = "Frosted Glassmorphism",
        description = "Translucent frosted glass surfaces with soft glow"
    )
}
