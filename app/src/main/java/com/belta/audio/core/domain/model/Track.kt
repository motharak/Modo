package com.belta.audio.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a music track in the library.
 */
@Serializable
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0L,
    val album: String,
    val albumId: Long = 0L,
    val durationMs: Long,
    val path: String,
    val size: Long = 0L,
    val mimeType: String = "audio/*",
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val genre: String = "",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val channels: Int = 2,
    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    val artworkUri: String? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val audioQualitySummary: String
        get() {
            val codec = when {
                mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                mimeType.contains("alac", ignoreCase = true) || (path.endsWith(".m4a", ignoreCase = true) && isLossless) -> "ALAC"
                mimeType.contains("aac", ignoreCase = true) || mimeType.contains("mp4a", ignoreCase = true) -> "AAC"
                mimeType.contains("mp3", ignoreCase = true) -> "MP3"
                mimeType.contains("opus", ignoreCase = true) -> "Opus"
                else -> mimeType.substringAfterLast("/").uppercase()
            }
            val khz = String.format("%.1f kHz", sampleRateHz / 1000.0)
            return "$codec | $bitDepth-bit/$khz | $bitrateKbps kbps"
        }
}
