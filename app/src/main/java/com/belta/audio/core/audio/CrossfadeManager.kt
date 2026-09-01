package com.belta.audio.core.audio

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.media3.exoplayer.ExoPlayer
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import kotlin.math.cos
import kotlin.math.sin

enum class FadeCurve {
    EQUAL_POWER, // Spotify standard: preserves constant audio energy (3dB midpoint compensation)
    EXPONENTIAL, // Smooth logarithmic decay
    LINEAR
}

class CrossfadeManager(
    private var player: ExoPlayer? = null
) {
    var crossfadeDurationSeconds: Int = 5
    var fadeCurve: FadeCurve = FadeCurve.EQUAL_POWER
    var isSilenceTrimmingEnabled: Boolean = true

    private var currentFadeAnimator: ValueAnimator? = null

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
    }

    /**
     * Applies smooth Spotify-style equal-power fade-out on the outgoing track.
     */
    fun startFadeOut(durationMs: Long = (crossfadeDurationSeconds * 1000L).coerceAtLeast(500L), onComplete: () -> Unit = {}) {
        currentFadeAnimator?.cancel()

        DebugLogger.d(
            LogCategory.AUDIO_ENGINE,
            "CROSSFADE",
            "Starting ${fadeCurve.name} Fade-Out ($durationMs ms)"
        )

        currentFadeAnimator = ValueAnimator.ofFloat(1.0f, 0.0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val volume = calculateFadeVolume(fraction, isFadeIn = false)
                player?.volume = volume
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    player?.volume = 0f
                    onComplete()
                }
            })
            start()
        }
    }

    /**
     * Applies smooth Spotify-style equal-power fade-in on the incoming track.
     */
    fun startFadeIn(durationMs: Long = (crossfadeDurationSeconds * 1000L).coerceAtLeast(500L)) {
        currentFadeAnimator?.cancel()

        DebugLogger.d(
            LogCategory.AUDIO_ENGINE,
            "CROSSFADE",
            "Starting ${fadeCurve.name} Fade-In ($durationMs ms)"
        )

        player?.volume = 0f
        currentFadeAnimator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val volume = calculateFadeVolume(fraction, isFadeIn = true)
                player?.volume = volume
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    player?.volume = 1f
                }
            })
            start()
        }
    }

    private fun calculateFadeVolume(fraction: Float, isFadeIn: Boolean): Float {
        val t = fraction.coerceIn(0f, 1f)
        return when (fadeCurve) {
            FadeCurve.EQUAL_POWER -> {
                // Constant energy equal-power curve: cos(pi/2 * t) for out, sin(pi/2 * t) for in
                if (isFadeIn) {
                    sin(Math.PI * 0.5 * t).toFloat()
                } else {
                    cos(Math.PI * 0.5 * (1.0 - t)).toFloat()
                }
            }
            FadeCurve.EXPONENTIAL -> {
                if (isFadeIn) {
                    (t * t)
                } else {
                    (t * t)
                }
            }
            FadeCurve.LINEAR -> t
        }
    }

    fun cancelFades() {
        currentFadeAnimator?.cancel()
        currentFadeAnimator = null
        player?.volume = 1f
    }
}
