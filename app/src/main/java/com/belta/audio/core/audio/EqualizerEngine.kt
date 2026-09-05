package com.belta.audio.core.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.ReverbSoundStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs

class EqualizerEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null
    private var environmentalReverb: EnvironmentalReverb? = null

    var isInitialized = false
        private set

    private var currentAudioSessionId: Int = 0
    private var isEnabled = true
    private var currentPreset: EqualizerPreset = EqualizerPreset.FLAT
    private var isReplayGainEnabled = true
    private var currentReverbStage: ReverbSoundStage = ReverbSoundStage.NONE

    // Reference 10-band center frequencies (Hz) used by standard presets
    val standardFrequenciesHz = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    // Observable physical band frequencies dynamically queried from hardware (e.g. 5-band or 10-band)
    private val _bandFrequencies = MutableStateFlow(listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"))
    val bandFrequencies: StateFlow<List<String>> = _bandFrequencies.asStateFlow()

    // Observable active preset state
    private val _presetState = MutableStateFlow(EqualizerPreset.FLAT)
    val presetState: StateFlow<EqualizerPreset> = _presetState.asStateFlow()

    // Observable master DSP toggle state
    private val _enabledState = MutableStateFlow(true)
    val enabledState: StateFlow<Boolean> = _enabledState.asStateFlow()

    // Callback to attach/clear auxiliary reverb on player
    var onAuxReverbChanged: ((auxId: Int?, sendLevel: Float) -> Unit)? = null

    val isReverbActive: Boolean
        get() = isEnabled && currentReverbStage != ReverbSoundStage.NONE

    fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentAudioSessionId && isInitialized) return

        currentAudioSessionId = audioSessionId
        releaseAudioEffects()

        DebugLogger.i(LogCategory.DSP_EQUALIZER, "INIT", "Initializing audio DSP effects for audioSessionId=$audioSessionId")

        // 1. Hardware Graphic Equalizer
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEnabled
            }
            updateHardwareBands()
            DebugLogger.i(LogCategory.DSP_EQUALIZER, "EQUALIZER", "Hardware Equalizer active on session $audioSessionId (${equalizer?.numberOfBands} bands)")
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "EQUALIZER", "Failed to init Equalizer: ${e.message}")
        }

        // 2. Bass Boost
        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = isEnabled && currentPreset.bassBoost > 0
                if (strengthSupported) {
                    setStrength(currentPreset.bassBoost.toShort().coerceIn(0, 1000))
                }
            }
            DebugLogger.i(LogCategory.DSP_EQUALIZER, "BASS_BOOST", "Bass Boost active on session $audioSessionId (strengthSupported=${bassBoost?.strengthSupported})")
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "BASS_BOOST", "Failed to init BassBoost: ${e.message}")
        }

        // 3. Virtualizer (3D Spatial Stage)
        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = isEnabled && currentPreset.virtualizer > 0
                if (strengthSupported) {
                    setStrength(currentPreset.virtualizer.toShort().coerceIn(0, 1000))
                }
            }
            DebugLogger.i(LogCategory.DSP_EQUALIZER, "VIRTUALIZER", "Virtualizer active on session $audioSessionId (strengthSupported=${virtualizer?.strengthSupported})")
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "VIRTUALIZER", "Failed to init Virtualizer: ${e.message}")
        }

        // 4. Loudness Enhancer (ReplayGain normalization)
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = isEnabled && isReplayGainEnabled
                setTargetGain(150)
            }
            DebugLogger.i(LogCategory.DSP_EQUALIZER, "LOUDNESS", "LoudnessEnhancer active on session $audioSessionId")
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "LOUDNESS", "Failed to init LoudnessEnhancer: ${e.message}")
        }

        // 5. Environmental Reverb (Direct audioSessionId insert effect for tangible acoustic stage change)
        try {
            environmentalReverb = EnvironmentalReverb(0, audioSessionId).apply {
                enabled = isEnabled && currentReverbStage != ReverbSoundStage.NONE
            }
            applyEnvironmentalReverbSettings(currentReverbStage)
            DebugLogger.i(LogCategory.DSP_EQUALIZER, "REVERB", "EnvironmentalReverb direct session active on audioSessionId=$audioSessionId")
        } catch (e: Exception) {
            DebugLogger.w(LogCategory.DSP_EQUALIZER, "REVERB", "EnvironmentalReverb direct insert failed, trying PresetReverb: ${e.message}")
            try {
                // Direct session insert fallback
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    preset = currentReverbStage.presetId
                    enabled = isEnabled && currentReverbStage != ReverbSoundStage.NONE
                }
                DebugLogger.i(LogCategory.DSP_EQUALIZER, "REVERB", "PresetReverb active on session $audioSessionId")
            } catch (e2: Exception) {
                // Secondary fallback: session 0 aux
                try {
                    presetReverb = PresetReverb(0, 0).apply {
                        preset = currentReverbStage.presetId
                        val active = isEnabled && currentReverbStage != ReverbSoundStage.NONE
                        enabled = active
                        if (active) onAuxReverbChanged?.invoke(id, 1.0f)
                    }
                    DebugLogger.i(LogCategory.DSP_EQUALIZER, "REVERB", "PresetReverb active on session 0 aux")
                } catch (e3: Exception) {
                    DebugLogger.e(LogCategory.DSP_EQUALIZER, "REVERB", "All reverb initialization failed: ${e3.message}")
                }
            }
        }

        isInitialized = true
        applyPreset(currentPreset)
    }

    private fun updateHardwareBands() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val freqs = mutableListOf<String>()
            for (i in 0 until numBands) {
                val freqMilliHz = eq.getCenterFreq(i.toShort())
                val freqHz = freqMilliHz / 1000
                val label = if (freqHz >= 1000) {
                    if (freqHz % 1000 == 0) "${freqHz / 1000}kHz" else String.format(Locale.US, "%.1fkHz", freqHz / 1000.0)
                } else {
                    "${freqHz}Hz"
                }
                freqs.add(label)
            }
            if (freqs.isNotEmpty()) {
                _bandFrequencies.value = freqs
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "BANDS", "Failed to read hardware bands: ${e.message}")
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        val eq = equalizer
        val numBands = eq?.numberOfBands?.toInt() ?: _bandFrequencies.value.size
        val physicalGains = getPhysicalGainsForPreset(preset, numBands)

        currentPreset = preset.copy(bandGains = physicalGains)
        currentReverbStage = preset.reverbStage
        _presetState.value = currentPreset

        if (eq != null) {
            try {
                val minBandLevel = eq.bandLevelRange[0]
                val maxBandLevel = eq.bandLevelRange[1]
                for (i in 0 until numBands) {
                    val userGain = physicalGains.getOrElse(i) { 0 }
                    val milliBels = (userGain * 100).toShort().coerceIn(minBandLevel, maxBandLevel)
                    eq.setBandLevel(i.toShort(), milliBels)
                }
            } catch (e: Exception) {
                DebugLogger.e(LogCategory.DSP_EQUALIZER, "EQUALIZER", "Error applying band levels: ${e.message}")
            }
        }

        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(preset.bassBoost.toShort().coerceIn(0, 1000))
                }
                it.enabled = isEnabled && preset.bassBoost > 0
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "BASS_BOOST", "Error applying bass boost: ${e.message}")
        }

        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(preset.virtualizer.toShort().coerceIn(0, 1000))
                }
                it.enabled = isEnabled && preset.virtualizer > 0
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "VIRTUALIZER", "Error applying virtualizer: ${e.message}")
        }

        setReverbSoundStage(preset.reverbStage)
    }

    fun getPhysicalGainsForPreset(preset: EqualizerPreset, numBands: Int = _bandFrequencies.value.size): List<Int> {
        if (preset.bandGains.size == numBands) {
            return preset.bandGains
        }
        val eq = equalizer
        return (0 until numBands).map { i ->
            val freqHz = try {
                eq?.getCenterFreq(i.toShort())?.let { it / 1000 }
            } catch (e: Exception) {
                null
            } ?: fallbackFreqForBand(i, numBands)
            getClosestGainForFrequency(freqHz, preset.bandGains)
        }
    }

    private fun fallbackFreqForBand(bandIndex: Int, totalBands: Int): Int {
        if (totalBands == 5) {
            return when (bandIndex) {
                0 -> 60
                1 -> 230
                2 -> 910
                3 -> 3600
                else -> 14000
            }
        }
        val minFreq = 31.0
        val maxFreq = 16000.0
        val ratio = Math.pow(maxFreq / minFreq, 1.0 / (totalBands - 1).coerceAtLeast(1))
        return (minFreq * Math.pow(ratio, bandIndex.toDouble())).toInt()
    }

    private fun getClosestGainForFrequency(targetFreqHz: Int, sourceGains: List<Int>): Int {
        if (sourceGains.isEmpty()) return 0
        if (sourceGains.size == 1) return sourceGains[0]

        var closestIdx = 0
        var minDiff = Int.MAX_VALUE
        for (i in standardFrequenciesHz.indices) {
            if (i < sourceGains.size) {
                val diff = abs(standardFrequenciesHz[i] - targetFreqHz)
                if (diff < minDiff) {
                    minDiff = diff
                    closestIdx = i
                }
            }
        }
        return sourceGains.getOrElse(closestIdx) { 0 }
    }

    fun setBandGain(bandIndex: Int, gainDb: Int) {
        val updatedGains = currentPreset.bandGains.toMutableList()
        while (updatedGains.size <= bandIndex) {
            updatedGains.add(0)
        }
        updatedGains[bandIndex] = gainDb
        currentPreset = currentPreset.copy(bandGains = updatedGains, isCustom = true)
        _presetState.value = currentPreset

        val eq = equalizer ?: return
        try {
            if (bandIndex in 0 until eq.numberOfBands) {
                val minBandLevel = eq.bandLevelRange[0]
                val maxBandLevel = eq.bandLevelRange[1]
                val milliBels = (gainDb * 100).toShort().coerceIn(minBandLevel, maxBandLevel)
                eq.setBandLevel(bandIndex.toShort(), milliBels)
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "EQUALIZER", "Error setting band gain: ${e.message}")
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        currentPreset = currentPreset.copy(bassBoost = clamped, isCustom = true)
        _presetState.value = currentPreset
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
                it.enabled = isEnabled && clamped > 0
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "BASS_BOOST", "Error setting bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        currentPreset = currentPreset.copy(virtualizer = clamped, isCustom = true)
        _presetState.value = currentPreset
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
                it.enabled = isEnabled && clamped > 0
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "VIRTUALIZER", "Error setting virtualizer: ${e.message}")
        }
    }

    fun setReverbSoundStage(stage: ReverbSoundStage) {
        currentReverbStage = stage
        currentPreset = currentPreset.copy(reverbStage = stage)
        _presetState.value = currentPreset
        applyEnvironmentalReverbSettings(stage)
    }

    private fun applyEnvironmentalReverbSettings(stage: ReverbSoundStage) {
        val env = environmentalReverb
        if (env != null) {
            try {
                if (stage == ReverbSoundStage.NONE) {
                    env.enabled = false
                    return
                }

                env.enabled = isEnabled
                when (stage) {
                    ReverbSoundStage.CONCERT_HALL -> {
                        env.roomLevel = 0.toShort()
                        env.roomHFLevel = (-600).toShort()
                        env.decayTime = 3900 // 3.9 seconds vast cathedral/concert hall decay
                        env.decayHFRatio = 700.toShort()
                        env.reflectionsLevel = (-1000).toShort()
                        env.reflectionsDelay = 20
                        env.reverbLevel = 200.toShort()
                        env.reverbDelay = 30
                        env.diffusion = 1000.toShort()
                        env.density = 1000.toShort()
                    }
                    ReverbSoundStage.LARGE_AUDITORIUM -> {
                        env.roomLevel = (-500).toShort()
                        env.roomHFLevel = (-800).toShort()
                        env.decayTime = 4500 // 4.5 seconds wide amphitheater
                        env.decayHFRatio = 600.toShort()
                        env.reflectionsLevel = (-800).toShort()
                        env.reflectionsDelay = 30
                        env.reverbLevel = 300.toShort()
                        env.reverbDelay = 40
                        env.diffusion = 900.toShort()
                        env.density = 900.toShort()
                    }
                    ReverbSoundStage.ACOUSTIC_CHAMBER -> {
                        env.roomLevel = (-600).toShort()
                        env.roomHFLevel = (-500).toShort()
                        env.decayTime = 2400 // 2.4 seconds warm chamber
                        env.decayHFRatio = 800.toShort()
                        env.reflectionsLevel = (-1100).toShort()
                        env.reflectionsDelay = 15
                        env.reverbLevel = 0.toShort()
                        env.reverbDelay = 25
                        env.diffusion = 800.toShort()
                        env.density = 850.toShort()
                    }
                    ReverbSoundStage.STUDIO_ROOM -> {
                        env.roomLevel = (-800).toShort()
                        env.roomHFLevel = (-400).toShort()
                        env.decayTime = 1100 // 1.1 seconds tight studio
                        env.decayHFRatio = 850.toShort()
                        env.reflectionsLevel = (-1300).toShort()
                        env.reflectionsDelay = 10
                        env.reverbLevel = (-200).toShort()
                        env.reverbDelay = 15
                        env.diffusion = 700.toShort()
                        env.density = 800.toShort()
                    }
                    ReverbSoundStage.VINTAGE_PLATE -> {
                        env.roomLevel = (-400).toShort()
                        env.roomHFLevel = 0.toShort() // Bright highs for analog plate
                        env.decayTime = 1600 // 1.6 seconds shimmer plate
                        env.decayHFRatio = 950.toShort()
                        env.reflectionsLevel = 0.toShort()
                        env.reflectionsDelay = 5
                        env.reverbLevel = 450.toShort()
                        env.reverbDelay = 10
                        env.diffusion = 1000.toShort()
                        env.density = 1000.toShort()
                    }
                    else -> {
                        env.enabled = false
                    }
                }
                DebugLogger.i(LogCategory.DSP_EQUALIZER, "REVERB", "Applied environmental reverb stage: ${stage.name} (decay=${env.decayTime}ms)")
            } catch (e: Exception) {
                DebugLogger.e(LogCategory.DSP_EQUALIZER, "REVERB", "Error setting environmental reverb stage: ${e.message}")
            }
        }

        // Fallback for presetReverb if active
        presetReverb?.let {
            try {
                if (stage == ReverbSoundStage.NONE) {
                    it.enabled = false
                    onAuxReverbChanged?.invoke(null, 0f)
                } else {
                    it.preset = stage.presetId
                    it.enabled = isEnabled
                    if (isEnabled) {
                        onAuxReverbChanged?.invoke(it.id, 1.0f)
                    } else {
                        onAuxReverbChanged?.invoke(null, 0f)
                    }
                }
            } catch (e: Exception) {
                DebugLogger.e(LogCategory.DSP_EQUALIZER, "REVERB", "Error setting preset reverb fallback: ${e.message}")
            }
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        isReplayGainEnabled = enabled
        try {
            loudnessEnhancer?.let {
                it.enabled = isEnabled && enabled
                if (enabled) {
                    it.setTargetGain(150)
                }
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "LOUDNESS", "Error setting ReplayGain: ${e.message}")
        }
    }

    fun setEffectsEnabled(enabled: Boolean) {
        isEnabled = enabled
        _enabledState.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && (currentPreset.bassBoost > 0)
            virtualizer?.enabled = enabled && (currentPreset.virtualizer > 0)
            environmentalReverb?.let {
                it.enabled = enabled && currentReverbStage != ReverbSoundStage.NONE
            }
            presetReverb?.let {
                val reverbActive = enabled && currentReverbStage != ReverbSoundStage.NONE
                it.enabled = reverbActive
                if (reverbActive) {
                    onAuxReverbChanged?.invoke(it.id, 1.0f)
                } else {
                    onAuxReverbChanged?.invoke(null, 0f)
                }
            }
            loudnessEnhancer?.enabled = enabled && isReplayGainEnabled
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.DSP_EQUALIZER, "DSP_CHAIN", "Error toggling effects: ${e.message}")
        }
    }

    fun isEffectsEnabled(): Boolean = isEnabled

    fun releaseAudioEffects() {
        try {
            equalizer?.release()
        } catch (e: Exception) { /* ignore */ }
        try {
            bassBoost?.release()
        } catch (e: Exception) { /* ignore */ }
        try {
            virtualizer?.release()
        } catch (e: Exception) { /* ignore */ }
        try {
            environmentalReverb?.release()
        } catch (e: Exception) { /* ignore */ }
        try {
            presetReverb?.release()
        } catch (e: Exception) { /* ignore */ }
        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) { /* ignore */ }

        equalizer = null
        bassBoost = null
        virtualizer = null
        environmentalReverb = null
        presetReverb = null
        loudnessEnhancer = null
        isInitialized = false
        onAuxReverbChanged?.invoke(null, 0f)
    }
}
