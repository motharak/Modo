package com.belta.audio

import com.belta.audio.core.domain.model.AudioSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSpecsTest {

    @Test
    fun testHiResAudioSpecsBadge() {
        val flacHiRes = AudioSpecs(
            containerFormat = "FLAC",
            codecName = "FLAC",
            bitrateKbps = 5824,
            sampleRateHz = 192000,
            bitDepth = 24,
            channelCount = 2,
            isHiRes = true,
            isLossless = true
        )

        assertEquals("192 kHz", flacHiRes.sampleRateKHzFormatted)
        assertEquals("24-bit", flacHiRes.bitDepthFormatted)
        assertEquals("5824 kbps", flacHiRes.bitrateFormatted)
        assertEquals("HI-RES AUDIO", flacHiRes.qualityBadgeText)
        assertTrue(flacHiRes.isHiRes)
    }

    @Test
    fun testCdQualitySpecs() {
        val cdTrack = AudioSpecs(
            containerFormat = "WAV",
            codecName = "PCM",
            bitrateKbps = 1411,
            sampleRateHz = 44100,
            bitDepth = 16,
            channelCount = 2,
            isHiRes = false,
            isLossless = true
        )

        assertEquals("44.1 kHz", cdTrack.sampleRateKHzFormatted)
        assertEquals("16-bit", cdTrack.bitDepthFormatted)
        assertEquals("1411 kbps", cdTrack.bitrateFormatted)
        assertEquals("LOSSLESS", cdTrack.qualityBadgeText)
    }

    @Test
    fun testMp3Specs() {
        val mp3Track = AudioSpecs(
            containerFormat = "MP3",
            codecName = "MPEG-1 Audio Layer 3",
            bitrateKbps = 320,
            sampleRateHz = 48000,
            bitDepth = 16,
            channelCount = 2,
            isHiRes = false,
            isLossless = false
        )

        assertEquals("48 kHz", mp3Track.sampleRateKHzFormatted)
        assertEquals("HIGH QUALITY", mp3Track.qualityBadgeText)
    }
}
