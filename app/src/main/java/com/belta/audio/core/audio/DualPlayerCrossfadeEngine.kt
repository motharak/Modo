package com.belta.audio.core.audio

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import com.belta.audio.core.domain.model.RepeatMode
import com.belta.audio.core.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * DualPlayerCrossfadeEngine orchestrates two ExoPlayer instances to provide true Spotify-style
 * overlapping crossfade where Track A fades out simultaneously while Track B fades in over
 * an adjustable duration (1–12s) using a constant-energy Equal-Power curve.
 */
class DualPlayerCrossfadeEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    var playerA: ExoPlayer,
    var playerB: ExoPlayer,
    private val trackToMediaItemConverter: (Track) -> MediaItem
) {
    var primaryPlayer: ExoPlayer = playerA
        private set
    var standbyPlayer: ExoPlayer = playerB
        private set

    var crossfadeDurationSeconds: Int = 3
    var isAutomixEnabled: Boolean = true
    var fadeCurve: FadeCurve = FadeCurve.EQUAL_POWER
    var isSilenceTrimmingEnabled: Boolean = true
    var repeatMode: RepeatMode = RepeatMode.OFF

    var onActivePlayerSwapped: ((newActivePlayer: ExoPlayer, newTrack: Track) -> Unit)? = null
    var onTrackTransition: ((newTrack: Track) -> Unit)? = null
    var onAudioSessionIdChanged: ((newSessionId: Int) -> Unit)? = null
    var onAutoPlayNextRequested: (() -> Unit)? = null
    var onPlaybackEnded: (() -> Unit)? = null

    var currentQueue: List<Track> = emptyList()
        private set
    var currentIndex: Int = -1
        private set

    val currentTrack: Track?
        get() = currentQueue.getOrNull(currentIndex)

    var isCrossfading: Boolean = false
        private set

    private var crossfadeJob: Job? = null
    private var progressMonitorJob: Job? = null

    init {
        setupPlayerListeners(playerA)
        setupPlayerListeners(playerB)
        startProgressMonitor()
    }

    private fun setupPlayerListeners(p: ExoPlayer) {
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (p == primaryPlayer && state == Player.STATE_ENDED && !isCrossfading) {
                    handleTrackEnded()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (p == primaryPlayer && audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    onAudioSessionIdChanged?.invoke(audioSessionId)
                }
            }
        })
    }

    private fun startProgressMonitor() {
        progressMonitorJob?.cancel()
        progressMonitorJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                checkCrossfadeTrigger()
                delay(120L)
            }
        }
    }

    private fun checkCrossfadeTrigger() {
        if (isCrossfading || crossfadeDurationSeconds <= 0) return
        if (!primaryPlayer.isPlaying) return

        val duration = primaryPlayer.duration
        val position = primaryPlayer.currentPosition
        if (duration <= 0L || position <= 0L) return

        val remainingMs = duration - position
        val targetCrossfadeMs = (crossfadeDurationSeconds * 1000L).coerceAtLeast(1000L)

        if (remainingMs in 1L..targetCrossfadeMs) {
            val nextTrack = peekNextTrack()
            if (nextTrack != null) {
                val effectiveDuration = remainingMs.coerceAtLeast(800L)
                startOverlappingCrossfade(nextTrack, effectiveDuration)
            }
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return

        crossfadeJob?.cancel()
        isCrossfading = false

        standbyPlayer.stop()
        standbyPlayer.volume = 1f

        currentQueue = tracks
        currentIndex = startIndex.coerceIn(0, tracks.size - 1)
        val track = tracks[currentIndex]

        primaryPlayer.volume = 1f
        primaryPlayer.setMediaItem(trackToMediaItemConverter(track))
        primaryPlayer.prepare()
        primaryPlayer.play()

        onTrackTransition?.invoke(track)
        onActivePlayerSwapped?.invoke(primaryPlayer, track)
        onAudioSessionIdChanged?.invoke(primaryPlayer.audioSessionId)
    }

    fun playTrack(track: Track) {
        playQueue(listOf(track), 0)
    }

    fun startOverlappingCrossfade(nextTrack: Track, durationMs: Long) {
        if (isCrossfading) return
        isCrossfading = true
        crossfadeJob?.cancel()

        DebugLogger.i(
            LogCategory.AUDIO_ENGINE,
            "CROSSFADE",
            "Starting ${fadeCurve.name} Overlapping Crossfade ($durationMs ms) to: ${nextTrack.title}"
        )

        val nextMediaItem = trackToMediaItemConverter(nextTrack)
        standbyPlayer.setMediaItem(nextMediaItem)
        standbyPlayer.volume = 0f
        standbyPlayer.prepare()
        standbyPlayer.play()

        val startTime = System.currentTimeMillis()
        crossfadeJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                val outVolume = calculateFadeVolume(1f - fraction)
                val inVolume = calculateFadeVolume(fraction)

                primaryPlayer.volume = outVolume
                standbyPlayer.volume = inVolume

                if (fraction >= 1.0f) break
                delay(16L) // ~60fps smooth volume ramp
            }

            completeCrossfade(nextTrack)
        }
    }

    private fun completeCrossfade(nextTrack: Track) {
        primaryPlayer.pause()
        primaryPlayer.stop()
        primaryPlayer.volume = 1f

        standbyPlayer.volume = 1f

        // Swap primary and standby players
        val previousPrimary = primaryPlayer
        primaryPlayer = standbyPlayer
        standbyPlayer = previousPrimary

        currentIndex = (currentIndex + 1).coerceAtMost(currentQueue.size - 1)
        isCrossfading = false

        DebugLogger.i(
            LogCategory.AUDIO_ENGINE,
            "CROSSFADE",
            "Overlapping crossfade complete. Primary player swapped to: ${nextTrack.title}"
        )

        onTrackTransition?.invoke(nextTrack)
        onActivePlayerSwapped?.invoke(primaryPlayer, nextTrack)
        onAudioSessionIdChanged?.invoke(primaryPlayer.audioSessionId)
    }

    fun skipToNext() {
        val nextTrack = peekNextTrack()
        if (nextTrack == null) {
            onAutoPlayNextRequested?.invoke()
            return
        }

        if (crossfadeDurationSeconds > 0 && primaryPlayer.isPlaying) {
            // Quick 600ms blend on manual skip so skipping tracks never pops or clicks
            startOverlappingCrossfade(nextTrack, 600L)
        } else {
            crossfadeJob?.cancel()
            isCrossfading = false
            standbyPlayer.stop()
            standbyPlayer.volume = 1f

            primaryPlayer.volume = 1f
            primaryPlayer.setMediaItem(trackToMediaItemConverter(nextTrack))
            primaryPlayer.prepare()
            primaryPlayer.play()

            currentIndex++
            onTrackTransition?.invoke(nextTrack)
        }
    }

    fun skipToPrevious() {
        crossfadeJob?.cancel()
        isCrossfading = false
        standbyPlayer.stop()
        standbyPlayer.volume = 1f

        if (primaryPlayer.currentPosition > 3000L) {
            primaryPlayer.seekTo(0L)
            return
        }

        if (currentIndex > 0) {
            currentIndex--
            val prevTrack = currentQueue[currentIndex]
            primaryPlayer.volume = 1f
            primaryPlayer.setMediaItem(trackToMediaItemConverter(prevTrack))
            primaryPlayer.prepare()
            primaryPlayer.play()
            onTrackTransition?.invoke(prevTrack)
        } else {
            primaryPlayer.seekTo(0L)
        }
    }

    private fun handleTrackEnded() {
        val next = peekNextTrack()
        if (next != null) {
            currentIndex++
            primaryPlayer.setMediaItem(trackToMediaItemConverter(next))
            primaryPlayer.prepare()
            primaryPlayer.play()
            onTrackTransition?.invoke(next)
        } else {
            onAutoPlayNextRequested?.invoke() ?: onPlaybackEnded?.invoke()
        }
    }

    private fun peekNextTrack(): Track? {
        if (currentQueue.isEmpty()) return null
        return when (repeatMode) {
            RepeatMode.ONE -> currentTrack
            RepeatMode.ALL -> {
                val nextIdx = (currentIndex + 1) % currentQueue.size
                currentQueue.getOrNull(nextIdx)
            }
            RepeatMode.OFF -> {
                if (currentIndex in 0 until currentQueue.size - 1) {
                    currentQueue[currentIndex + 1]
                } else null
            }
        }
    }

    fun togglePlayPause() {
        if (primaryPlayer.isPlaying) {
            primaryPlayer.pause()
            if (isCrossfading) standbyPlayer.pause()
        } else {
            primaryPlayer.play()
            if (isCrossfading) standbyPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false
            standbyPlayer.stop()
            standbyPlayer.volume = 1f
            primaryPlayer.volume = 1f
        }
        primaryPlayer.seekTo(positionMs)
    }

    fun setSpeed(speed: Float, pitch: Float = 1.0f) {
        val params = androidx.media3.common.PlaybackParameters(speed, pitch)
        playerA.playbackParameters = params
        playerB.playbackParameters = params
    }

    fun calculateFadeVolume(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        return when (fadeCurve) {
            FadeCurve.EQUAL_POWER -> {
                // Constant energy equal-power curve: sin(pi/2 * t)
                sin(Math.PI * 0.5 * t).toFloat().coerceIn(0f, 1f)
            }
            FadeCurve.EXPONENTIAL -> {
                (t * t).coerceIn(0f, 1f)
            }
            FadeCurve.LINEAR -> t
        }
    }

    fun release() {
        progressMonitorJob?.cancel()
        crossfadeJob?.cancel()
        playerA.release()
        playerB.release()
    }
}
