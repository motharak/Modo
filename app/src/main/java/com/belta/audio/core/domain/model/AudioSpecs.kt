package com.belta.audio.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Detailed technical audio specifications.
 */
@Serializable
data class AudioSpecs(
    val containerFormat: String = "UNKNOWN",
    val codecName: String = "UNKNOWN",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,
    val channelCount: Int = 2,
    val isHiRes: Boolean = false,
    val isLossless: Boolean = false,
    val channelLayout: String = "Stereo"
) {
    val sampleRateKHzFormatted: String
        get() {
            val khz = sampleRateHz / 1000.0
            return if (khz % 1.0 == 0.0) {
                String.format("%.0f kHz", khz)
            } else {
                String.format("%.1f kHz", khz)
            }
        }

    val bitDepthFormatted: String
        get() = "$bitDepth-bit"

    val bitrateFormatted: String
        get() = if (bitrateKbps > 0) "$bitrateKbps kbps" else "Variable"

    val qualityBadgeText: String
        get() {
            return when {
                isHiRes -> "HI-RES AUDIO"
                isLossless -> "LOSSLESS"
                bitrateKbps >= 320 -> "HIGH QUALITY"
                else -> "STANDARD"
            }
        }

    val shortSummary: String
        get() = "$containerFormat | $bitDepthFormatted / $sampleRateKHzFormatted | $bitrateFormatted"
}
