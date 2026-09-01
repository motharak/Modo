package com.belta.audio.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Default Brand Colors
val BrandPrimary = Color(0xFF6750A4)
val BrandSecondary = Color(0xFF625B71)
val BrandTertiary = Color(0xFF7D5260)

// AMOLED Black Theme
val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF231E3D),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color(0xFF000000),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

// Midnight Dark Theme
val MidnightColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF0F0D13),
    onBackground = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

// Minimal Light Theme
val MinimalLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F)
)

// Cyberpunk Neon Theme
val NeonColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF), // Neon Cyan
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003840),
    onPrimaryContainer = Color(0xFF70FAFF),
    secondary = Color(0xFFFF0055), // Neon Magenta
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A12),
    onSurface = Color(0xFFE6F0FF),
    background = Color(0xFF05050A),
    onBackground = Color(0xFFE6F0FF),
    surfaceVariant = Color(0xFF141424),
    onSurfaceVariant = Color(0xFFA0A5C0)
)

// Frosted Glass Theme Scheme
val FrostedGlassColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF041E42),
    primaryContainer = Color(0xFF1F3B66),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF81C995),
    onSecondary = Color(0xFF043318),
    surface = Color(0xCC1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    background = Color(0xFF111315),
    onBackground = Color(0xFFE3E2E6),
    surfaceVariant = Color(0x992B3038),
    onSurfaceVariant = Color(0xFFC3C7D0)
)
