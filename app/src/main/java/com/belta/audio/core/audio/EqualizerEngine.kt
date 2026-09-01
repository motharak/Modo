package com.belta.audio.core.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.ReverbSoundStage

class EqualizerEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    private var isEnabled = true
    private var currentPreset: EqualizerPreset = EqualizerPreset.FLAT
    private var isReplayGainEnabled = true
    private var currentReverbStage: ReverbSoundStage = ReverbSoundStage.NONE

    fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        releaseAudioEffects()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(currentPreset.bassBoost.toShort())
                }
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(currentPreset.virtualizer.toShort())
                }
            }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = isReplayGainEnabled
                setTargetGain(150)
            }
            presetReverb = PresetReverb(0, audioSessionId).apply {
                preset = currentReverbStage.presetId
                enabled = isEnabled && currentReverbStage != ReverbSoundStage.NONE
            }

            applyPreset(currentPreset)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        currentPreset = preset
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minBandLevel = eq.bandLevelRange[0]
            val maxBandLevel = eq.bandLevelRange[1]

            for (i in 0 until numBands.coerceAtMost(preset.bandGains.size)) {
                val userGain = preset.bandGains[i] // -10 to +10 dB
                val calculatedMilliBels = (userGain * 100).toShort().coerceIn(minBandLevel, maxBandLevel)
                eq.setBandLevel(i.toShort(), calculatedMilliBels)
            }

            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(preset.bassBoost.toShort().coerceIn(0, 1000))
                }
            }

            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(preset.virtualizer.toShort().coerceIn(0, 1000))
                }
            }

            setReverbSoundStage(preset.reverbStage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setReverbSoundStage(stage: ReverbSoundStage) {
        currentReverbStage = stage
        try {
            presetReverb?.let {
                if (stage == ReverbSoundStage.NONE) {
                    it.enabled = false
                } else {
                    it.preset = stage.presetId
                    it.enabled = isEnabled
                }
            }
            currentPreset = currentPreset.copy(reverbStage = stage, isCustom = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Int) {
        val eq = equalizer ?: return
        try {
            if (bandIndex in 0 until eq.numberOfBands) {
                val minBandLevel = eq.bandLevelRange[0]
                val maxBandLevel = eq.bandLevelRange[1]
                val milliBels = (gainDb * 100).toShort().coerceIn(minBandLevel, maxBandLevel)
                eq.setBandLevel(bandIndex.toShort(), milliBels)

                val updatedGains = currentPreset.bandGains.toMutableList()
                if (bandIndex < updatedGains.size) {
                    updatedGains[bandIndex] = gainDb
                }
                currentPreset = currentPreset.copy(bandGains = updatedGains, isCustom = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.toShort().coerceIn(0, 1000))
                }
            }
            currentPreset = currentPreset.copy(bassBoost = strength, isCustom = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizer(strength: Int) {
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.toShort().coerceIn(0, 1000))
                }
            }
            currentPreset = currentPreset.copy(virtualizer = strength, isCustom = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        isReplayGainEnabled = enabled
        try {
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEffectsEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            presetReverb?.enabled = enabled && currentReverbStage != ReverbSoundStage.NONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun releaseAudioEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            presetReverb = null
            loudnessEnhancer = null
        }
    }
}
