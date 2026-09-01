package com.belta.audio.core.domain.model

import android.media.audiofx.PresetReverb
import kotlinx.serialization.Serializable

@Serializable
enum class ReverbSoundStage(val displayName: String, val description: String, val presetId: Short) {
    NONE("Direct Stereo", "Pure direct sound without room reverb", PresetReverb.PRESET_NONE),
    CONCERT_HALL("Big Concert Hall", "Vast cathedral hall with deep acoustic reflections", PresetReverb.PRESET_LARGEHALL),
    LARGE_AUDITORIUM("Large Auditorium", "Expansive amphitheater soundstage", PresetReverb.PRESET_LARGEROOM),
    ACOUSTIC_CHAMBER("Acoustic Chamber", "Warm medium hall reverberation", PresetReverb.PRESET_MEDIUMHALL),
    STUDIO_ROOM("Studio Room", "Subtle tight room ambience", PresetReverb.PRESET_SMALLROOM),
    VINTAGE_PLATE("Plate Reverb", "Classic bright analog shimmer reverberation", PresetReverb.PRESET_PLATE)
}

@Serializable
data class EqualizerPreset(
    val id: Long = 0L,
    val name: String,
    val bandGains: List<Int>, // Gain in dB for 10 bands (-10 to +10 dB)
    val bassBoost: Int = 0, // 0 to 1000
    val virtualizer: Int = 0, // 0 to 1000
    val reverbStage: ReverbSoundStage = ReverbSoundStage.NONE,
    val isCustom: Boolean = false
) {
    companion object {
        val FLAT = EqualizerPreset(
            id = 1,
            name = "Flat / Neutral",
            bandGains = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            bassBoost = 0,
            virtualizer = 0,
            reverbStage = ReverbSoundStage.NONE
        )
        val CONCERT_HALL = EqualizerPreset(
            id = 2,
            name = "Big Concert Hall",
            bandGains = listOf(3, 2, 1, 0, -1, 1, 2, 3, 4, 3),
            bassBoost = 350,
            virtualizer = 600,
            reverbStage = ReverbSoundStage.CONCERT_HALL
        )
        val BASS_BOOST = EqualizerPreset(
            id = 3,
            name = "Deep Sub-Bass",
            bandGains = listOf(7, 6, 4, 2, 0, 0, 0, 1, 2, 2),
            bassBoost = 700,
            virtualizer = 100,
            reverbStage = ReverbSoundStage.NONE
        )
        val VOCAL_ENHANCER = EqualizerPreset(
            id = 4,
            name = "Vocal Clarity",
            bandGains = listOf(-2, -1, 0, 3, 5, 5, 3, 1, 0, -1),
            bassBoost = 0,
            virtualizer = 150,
            reverbStage = ReverbSoundStage.STUDIO_ROOM
        )
        val ELECTRONIC = EqualizerPreset(
            id = 5,
            name = "Electronic & EDM",
            bandGains = listOf(5, 4, 2, 0, -2, 2, 1, 3, 4, 5),
            bassBoost = 500,
            virtualizer = 300,
            reverbStage = ReverbSoundStage.LARGE_AUDITORIUM
        )
        val ROCK = EqualizerPreset(
            id = 6,
            name = "Rock & Metal",
            bandGains = listOf(4, 3, 2, 0, -1, -1, 1, 3, 4, 4),
            bassBoost = 300,
            virtualizer = 150,
            reverbStage = ReverbSoundStage.ACOUSTIC_CHAMBER
        )
        val JAZZ = EqualizerPreset(
            id = 7,
            name = "Jazz Club",
            bandGains = listOf(3, 2, 1, 2, -1, -1, 0, 1, 2, 3),
            bassBoost = 150,
            virtualizer = 100,
            reverbStage = ReverbSoundStage.STUDIO_ROOM
        )
        val CLASSICAL = EqualizerPreset(
            id = 8,
            name = "Symphonic Orchestra",
            bandGains = listOf(4, 3, 2, 2, -1, -1, 0, 2, 3, 3),
            bassBoost = 200,
            virtualizer = 400,
            reverbStage = ReverbSoundStage.CONCERT_HALL
        )
        val SPATIAL_IMMERSIVE = EqualizerPreset(
            id = 9,
            name = "3D Spatial Stage",
            bandGains = listOf(2, 1, 0, 0, 1, 2, 2, 3, 3, 4),
            bassBoost = 400,
            virtualizer = 850,
            reverbStage = ReverbSoundStage.LARGE_AUDITORIUM
        )
        val HIP_HOP = EqualizerPreset(
            id = 10,
            name = "Hip-Hop & Trap",
            bandGains = listOf(6, 5, 3, 1, -1, 1, 2, 3, 4, 3),
            bassBoost = 750,
            virtualizer = 200,
            reverbStage = ReverbSoundStage.STUDIO_ROOM
        )
        val STUDIO_MONITOR = EqualizerPreset(
            id = 11,
            name = "Studio Monitor Hi-Fi",
            bandGains = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            bassBoost = 0,
            virtualizer = 0,
            reverbStage = ReverbSoundStage.NONE
        )
        val WARM_TUBE = EqualizerPreset(
            id = 12,
            name = "Warm Tube Amp",
            bandGains = listOf(4, 3, 2, 1, 0, 0, 1, 1, -1, -2),
            bassBoost = 300,
            virtualizer = 150,
            reverbStage = ReverbSoundStage.ACOUSTIC_CHAMBER
        )
        val BRIGHT_AIR = EqualizerPreset(
            id = 13,
            name = "Bright Air & Treble",
            bandGains = listOf(-2, -1, 0, 0, 1, 2, 4, 5, 6, 7),
            bassBoost = 0,
            virtualizer = 200,
            reverbStage = ReverbSoundStage.VINTAGE_PLATE
        )

        val DEFAULT_PRESETS = listOf(
            FLAT,
            CONCERT_HALL,
            BASS_BOOST,
            VOCAL_ENHANCER,
            ELECTRONIC,
            ROCK,
            JAZZ,
            CLASSICAL,
            SPATIAL_IMMERSIVE,
            HIP_HOP,
            STUDIO_MONITOR,
            WARM_TUBE,
            BRIGHT_AIR
        )
    }
}
