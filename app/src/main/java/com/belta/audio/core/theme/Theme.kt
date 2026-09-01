package com.belta.audio.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun BeltaAudioTheme(
    themeMode: ThemeMode = ThemeMode.DYNAMIC_MONET,
    albumArtColorScheme: ColorScheme? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (themeMode) {
        ThemeMode.DYNAMIC_MONET -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) MidnightColorScheme else MinimalLightColorScheme
            }
        }
        ThemeMode.DYNAMIC_ALBUM_ART -> {
            albumArtColorScheme ?: if (darkTheme) MidnightColorScheme else MinimalLightColorScheme
        }
        ThemeMode.AMOLED_BLACK -> AmoledColorScheme
        ThemeMode.MIDNIGHT_DARK -> MidnightColorScheme
        ThemeMode.MINIMAL_LIGHT -> MinimalLightColorScheme
        ThemeMode.CYBERPUNK_NEON -> NeonColorScheme
        ThemeMode.FROSTED_GLASS -> FrostedGlassColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
