package com.belta.audio.core.audio

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.belta.audio.core.data.scanner.AudioTagReader
import com.belta.audio.core.domain.model.AudioSpecs
import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.PlaybackState
import com.belta.audio.core.domain.model.RepeatMode
import com.belta.audio.core.domain.model.ShuffleMode
import com.belta.audio.core.domain.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class AudioEngineController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    private var currentQueue: List<Track> = emptyList()
    private var progressJob: Job? = null

    init {
        initMediaController()
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, BeltaMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListeners()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListeners() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTicker() else stopProgressTicker()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _playbackState.update {
                        it.copy(
                            durationMs = controller.duration.coerceAtLeast(0L),
                            currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                        )
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                val currentTrack = currentQueue.find { it.id == mediaId }
                val queueIndex = if (currentTrack != null) currentQueue.indexOf(currentTrack) else -1

                var specs = AudioSpecs()
                if (currentTrack != null) {
                    specs = AudioTagReader.extractSpecs(currentTrack.path, currentTrack.mimeType)
                    com.belta.audio.core.debug.DebugLogger.i(
                        com.belta.audio.core.debug.LogCategory.AUDIO_ENGINE,
                        "PLAYBACK",
                        "Now playing: ${currentTrack.title} - ${currentTrack.artist}",
                        "Specs: ${specs.containerFormat} | ${specs.bitDepthFormatted}/${specs.sampleRateKHzFormatted} | ${specs.bitrateFormatted}"
                    )
                }

                _playbackState.update {
                    it.copy(
                        currentTrack = currentTrack,
                        currentQueueIndex = queueIndex,
                        currentSpecs = specs,
                        durationMs = controller.duration.coerceAtLeast(0L),
                        currentPositionMs = 0L
                    )
                }
            }

            override fun onRepeatModeChanged(repeatModeInt: Int) {
                val mode = when (repeatModeInt) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _playbackState.update { it.copy(repeatMode = mode) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                val mode = if (shuffleModeEnabled) ShuffleMode.ON else ShuffleMode.OFF
                _playbackState.update { it.copy(shuffleMode = mode) }
            }
        })
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val controller = mediaController ?: return

        currentQueue = tracks
        val mediaItems = tracks.map { BeltaMediaService.trackToMediaItem(it) }

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()

        _playbackState.update {
            it.copy(
                queue = tracks,
                currentQueueIndex = startIndex,
                currentTrack = tracks.getOrNull(startIndex)
            )
        }
    }

    fun playTrack(track: Track) {
        playQueue(listOf(track), 0)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val media3Mode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        controller.repeatMode = media3Mode
        _playbackState.update { it.copy(repeatMode = next) }
    }

    fun toggleShuffleMode() {
        val controller = mediaController ?: return
        val enabled = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = enabled
        _playbackState.update {
            it.copy(shuffleMode = if (enabled) ShuffleMode.ON else ShuffleMode.OFF)
        }
    }

    fun setPlaybackSpeed(speed: Float, pitch: Float = 1.0f) {
        mediaController?.playbackParameters = PlaybackParameters(speed, pitch)
        _playbackState.update { it.copy(playbackSpeed = speed, playbackPitch = pitch) }
    }

    fun setCrossfadeDuration(seconds: Int) {
        _playbackState.update { it.copy(crossfadeDurationSeconds = seconds) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        _playbackState.update { it.copy(isReplayGainEnabled = enabled) }
    }

    fun release() {
        stopProgressTicker()
        MediaController.releaseFuture(controllerFuture ?: return)
        mediaController = null
    }
}
