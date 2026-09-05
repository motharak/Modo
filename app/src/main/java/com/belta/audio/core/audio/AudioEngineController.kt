package com.belta.audio.core.audio

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
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
import kotlinx.coroutines.withContext

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

        // Eagerly synchronize currently playing track on controller connect
        val initialItem = controller.currentMediaItem
        if (initialItem != null) {
            val mediaId = initialItem.mediaId.toLongOrNull()
            var initialTrack = currentQueue.find { it.id == mediaId }
            if (initialTrack == null) {
                val meta = initialItem.mediaMetadata
                initialTrack = Track(
                    id = mediaId ?: 0L,
                    title = meta.title?.toString() ?: "Unknown Title",
                    artist = meta.artist?.toString() ?: "Unknown Artist",
                    album = meta.albumTitle?.toString() ?: "Unknown Album",
                    durationMs = controller.duration.coerceAtLeast(0L),
                    path = initialItem.requestMetadata.mediaUri?.path ?: "",
                    artworkUri = meta.artworkUri?.toString()
                )
                currentQueue = currentQueue + initialTrack
            }
            _playbackState.update {
                it.copy(
                    isPlaying = controller.isPlaying,
                    currentTrack = initialTrack,
                    queue = currentQueue,
                    currentQueueIndex = currentQueue.indexOf(initialTrack),
                    durationMs = controller.duration.coerceAtLeast(0L),
                    currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
                )
            }
        }

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
                } else if (state == Player.STATE_ENDED) {
                    handlePlaybackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                var currentTrack = currentQueue.find { it.id == mediaId }

                if (currentTrack == null && mediaItem != null) {
                    // Reconstruct immediately from metadata so UI doesn't lose currentTrack
                    val candidate = autoPlayCandidatesProvider?.invoke()?.find { it.id == mediaId }
                    if (candidate != null) {
                        currentTrack = candidate
                        currentQueue = currentQueue + candidate
                    } else {
                        val meta = mediaItem.mediaMetadata
                        val fallbackTrack = Track(
                            id = mediaId ?: 0L,
                            title = meta.title?.toString() ?: "Unknown Title",
                            artist = meta.artist?.toString() ?: "Unknown Artist",
                            album = meta.albumTitle?.toString() ?: "Unknown Album",
                            durationMs = controller.duration.coerceAtLeast(0L),
                            path = mediaItem.requestMetadata.mediaUri?.path ?: "",
                            artworkUri = meta.artworkUri?.toString()
                        )
                        currentTrack = fallbackTrack
                        currentQueue = currentQueue + fallbackTrack

                        // Fetch complete DB entity asynchronously
                        scope.launch(Dispatchers.IO) {
                            val dbTrack = com.belta.audio.core.data.database.BeltaDatabase.getInstance(context)
                                .trackDao().getTrackById(mediaId ?: -1L)?.toDomain()
                            if (dbTrack != null) {
                                withContext(Dispatchers.Main) {
                                    currentQueue = currentQueue.map { if (it.id == dbTrack.id) dbTrack else it }
                                    _playbackState.update { it.copy(currentTrack = dbTrack, queue = currentQueue) }
                                }
                            }
                        }
                    }
                }

                val queueIndex = if (currentTrack != null) currentQueue.indexOf(currentTrack) else -1

                var specs = AudioSpecs()
                if (currentTrack != null && currentTrack.path.isNotBlank()) {
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
                        queue = currentQueue,
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

    var isAutoPlayRadioEnabled: Boolean = true
    var autoPlayCandidatesProvider: (() -> List<Track>)? = null

    private fun handlePlaybackEnded() {
        if (isAutoPlayRadioEnabled && _playbackState.value.repeatMode == RepeatMode.OFF) {
            playNextAutoPlayTrack()
        }
    }

    fun playNextAutoPlayTrack() {
        val controller = mediaController ?: return
        val current = _playbackState.value.currentTrack
        val allCandidates = autoPlayCandidatesProvider?.invoke() ?: emptyList()
        val candidates = allCandidates.filter { it.id != current?.id }
        if (candidates.isEmpty()) return

        // Prioritize same genre or artist if available, else random from library
        val similar = if (current != null && !current.genre.isNullOrBlank()) {
            candidates.filter { it.genre.equals(current.genre, ignoreCase = true) || it.artistId == current.artistId }
        } else emptyList()

        val nextTrack = if (similar.isNotEmpty()) similar.random() else candidates.random()
        val mediaItem = BeltaMediaService.trackToMediaItem(nextTrack)

        currentQueue = currentQueue + nextTrack
        controller.addMediaItem(mediaItem)
        controller.seekToDefaultPosition(controller.mediaItemCount - 1)
        controller.prepare()
        controller.play()

        _playbackState.update {
            it.copy(
                queue = currentQueue,
                currentTrack = nextTrack,
                currentQueueIndex = currentQueue.size - 1
            )
        }
        com.belta.audio.core.debug.DebugLogger.i(
            com.belta.audio.core.debug.LogCategory.AUDIO_ENGINE,
            "AUTO_PLAY",
            "Album queue completed. Auto-playing random track: ${nextTrack.title} by ${nextTrack.artist}"
        )
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (isAutoPlayRadioEnabled) {
            playNextAutoPlayTrack()
        }
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
        sendCrossfadeConfig(seconds, isAutomix = true, curve = "EQUAL_POWER")
    }

    fun setPlaylistFlowConfig(crossfadeSeconds: Int?, isAutomixEnabled: Boolean, fadeCurve: String) {
        val duration = crossfadeSeconds ?: _playbackState.value.crossfadeDurationSeconds
        sendCrossfadeConfig(duration, isAutomixEnabled, fadeCurve)
    }

    private fun sendCrossfadeConfig(duration: Int, isAutomix: Boolean, curve: String) {
        val bundle = Bundle().apply {
            putInt("crossfade_duration", duration)
            putBoolean("automix_enabled", isAutomix)
            putString("fade_curve", curve)
        }
        val command = SessionCommand(BeltaMediaService.CUSTOM_COMMAND_SET_CROSSFADE, Bundle.EMPTY)
        mediaController?.sendCustomCommand(command, bundle)
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
