package com.belta.audio.core.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteExtractor {

    suspend fun extractColorSchemeFromArtwork(
        context: Context,
        artworkUri: String?
    ): ColorScheme? = withContext(Dispatchers.IO) {
        if (artworkUri == null) return@withContext null
        try {
            val uri = Uri.parse(artworkUri)
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(4) // Downsample for fast palette extraction
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            val palette = Palette.from(bitmap).generate()
            val dominantRgb = palette.getDominantColor(0xFF6750A4.toInt())
            val vibrantRgb = palette.getVibrantColor(dominantRgb)
            val darkVibrantRgb = palette.getDarkVibrantColor(0xFF1C1B1F.toInt())
            val lightVibrantRgb = palette.getLightVibrantColor(0xFFEADDFF.toInt())

            val primaryColor = Color(vibrantRgb)
            val containerColor = Color(darkVibrantRgb)
            val onContainerColor = Color(lightVibrantRgb)

            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.Black,
                primaryContainer = containerColor,
                onPrimaryContainer = onContainerColor,
                secondary = Color(palette.getMutedColor(vibrantRgb)),
                surface = Color(0xFF141218),
                background = Color(0xFF0F0D13)
            )
        } catch (_: Exception) {
            null
        }
    }
}
