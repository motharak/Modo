package com.belta.audio

import com.belta.audio.core.audio.EqualizerEngine
import com.belta.audio.core.domain.model.EqualizerPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EqualizerEngineTest {

    @Test
    fun testPhysicalGainsMappingFor5Bands() {
        val engine = EqualizerEngine()
        // Default Rock preset has 10 bands: listOf(4, 3, 2, 0, -1, -1, 1, 3, 4, 4)
        val rockPreset = EqualizerPreset.ROCK
        val physicalGains = engine.getPhysicalGainsForPreset(rockPreset, 5)

        assertEquals(5, physicalGains.size)
        // Band 0 (60Hz -> closest 62Hz / index 1): 3 dB
        assertEquals(3, physicalGains[0])
        // Band 1 (230Hz -> closest 250Hz / index 3): 0 dB
        assertEquals(0, physicalGains[1])
        // Band 2 (910Hz -> closest 1000Hz / index 5): -1 dB
        assertEquals(-1, physicalGains[2])
        // Band 3 (3600Hz -> closest 4000Hz / index 7): 3 dB
        assertEquals(3, physicalGains[3])
        // Band 4 (14000Hz -> closest 16000Hz / index 9): 4 dB
        assertEquals(4, physicalGains[4])
    }

    @Test
    fun testPhysicalGainsMappingFor10Bands() {
        val engine = EqualizerEngine()
        val rockPreset = EqualizerPreset.ROCK
        val physicalGains = engine.getPhysicalGainsForPreset(rockPreset, 10)

        assertEquals(10, physicalGains.size)
        assertEquals(rockPreset.bandGains, physicalGains)
    }

    @Test
    fun testPrePlaybackPresetCaching() {
        val engine = EqualizerEngine()
        // Adjusting sliders before player starts
        engine.setBandGain(0, 5)
        engine.setBassBoost(650)
        engine.setVirtualizer(400)

        val preset = engine.presetState.value
        assertEquals(5, preset.bandGains[0])
        assertEquals(650, preset.bassBoost)
        assertEquals(400, preset.virtualizer)
    }
}
