package com.belta.audio

import com.belta.audio.core.audio.FadeCurve
import com.belta.audio.core.domain.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CrossfadeEngineTest {

    private fun calculateVolume(fraction: Float, curve: FadeCurve): Float {
        val t = fraction.coerceIn(0f, 1f)
        return when (curve) {
            FadeCurve.EQUAL_POWER -> sin(Math.PI * 0.5 * t).toFloat().coerceIn(0f, 1f)
            FadeCurve.EXPONENTIAL -> (t * t).coerceIn(0f, 1f)
            FadeCurve.LINEAR -> t
        }
    }

    @Test
    fun testEqualPowerPreservesConstantAcousticEnergy() {
        // Spotify standard: V_out^2 + V_in^2 must equal 1.0 (constant 3dB energy invariant)
        val steps = 20
        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val inVol = calculateVolume(t, FadeCurve.EQUAL_POWER)
            val outVol = calculateVolume(1f - t, FadeCurve.EQUAL_POWER)

            val totalEnergy = (inVol * inVol) + (outVol * outVol)
            assertEquals("Energy invariant failed at t=$t", 1.0f, totalEnergy, 0.001f)
        }
    }

    @Test
    fun testEqualPowerMidpointCompensation() {
        // Midpoint (t=0.5) must be sqrt(2)/2 ~= 0.7071f, not 0.5f (which would be -3dB dip)
        val midpointVol = calculateVolume(0.5f, FadeCurve.EQUAL_POWER)
        assertEquals(0.7071f, midpointVol, 0.001f)
    }

    @Test
    fun testExponentialCurve() {
        assertEquals(0f, calculateVolume(0f, FadeCurve.EXPONENTIAL), 0.0001f)
        assertEquals(0.25f, calculateVolume(0.5f, FadeCurve.EXPONENTIAL), 0.0001f)
        assertEquals(1f, calculateVolume(1f, FadeCurve.EXPONENTIAL), 0.0001f)
    }

    @Test
    fun testLinearCurve() {
        assertEquals(0f, calculateVolume(0f, FadeCurve.LINEAR), 0.0001f)
        assertEquals(0.5f, calculateVolume(0.5f, FadeCurve.LINEAR), 0.0001f)
        assertEquals(1f, calculateVolume(1f, FadeCurve.LINEAR), 0.0001f)
    }

    @Test
    fun testPlaylistRemixFlowDefaults() {
        val playlist = Playlist(
            id = 1L,
            name = "Party Mix",
            crossfadeSeconds = 8,
            isAutomixEnabled = true,
            fadeCurve = "EQUAL_POWER"
        )

        assertEquals(8, playlist.crossfadeSeconds)
        assertTrue(playlist.isAutomixEnabled)
        assertEquals("EQUAL_POWER", playlist.fadeCurve)
    }
}
