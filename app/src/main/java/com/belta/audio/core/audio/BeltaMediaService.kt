package com.belta.audio.core.audio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.belta.audio.core.data.database.BeltaDatabase
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import com.belta.audio.core.domain.model.Track
import com.belta.audio.ui.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class BeltaMediaService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    lateinit var equalizerEngine: EqualizerEngine
        private set
    lateinit var crossfadeManager: CrossfadeManager
        private set
    lateinit var sleepTimerManager: SleepTimerManager
        private set

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        equalizerEngine = EqualizerEngine()
        equalizerEngineInstance = equalizerEngine
        crossfadeManager = CrossfadeManager()
        sleepTimerManager = SleepTimerManager(serviceScope) {
            player.pause()
        }

        // Connect auxiliary reverb callback to player
        equalizerEngine.onAuxReverbChanged = { auxId, level ->
            try {
                if (auxId != null && level > 0f) {
                    player.setAuxEffectInfo(androidx.media3.common.AuxEffectInfo(auxId, level))
                } else {
                    player.clearAuxEffectInfo()
                }
            } catch (e: Exception) {
                DebugLogger.e(LogCategory.DSP_EQUALIZER, "REVERB", "Failed to update player aux reverb: ${e.message}")
            }
        }

        // Configure audiophile ExoPlayer with universal codec support for ALAC, AAC, FLAC via native FFmpeg
        val renderersFactory = object : DefaultRenderersFactory(this@BeltaMediaService) {
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: androidx.media3.exoplayer.audio.AudioSink,
                eventHandler: android.os.Handler,
                eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                if (androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable()) {
                    DebugLogger.i(
                        LogCategory.AUDIO_ENGINE,
                        "FFMPEG",
                        "FFmpeg native audio decoder loaded: version ${androidx.media3.decoder.ffmpeg.FfmpegLibrary.getVersion()}"
                    )
                    out.add(androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer(eventHandler, eventListener, audioSink))
                }

                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    true, // enable fallback to alternate decoders
                    audioSink,
                    eventHandler,
                    eventListener,
                    out
                )
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableAudioTrackPlaybackParams(true)
            setEnableDecoderFallback(true)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp4ExtractorFlags(androidx.media3.extractor.mp4.Mp4Extractor.FLAG_READ_SEF_DATA)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this, extractorsFactory)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true) // automatic audio focus handling
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Eagerly initialize audio effects if session ID is already allocated by ExoPlayer
        val initialSessionId = player.audioSessionId
        if (initialSessionId != C.AUDIO_SESSION_ID_UNSET && initialSessionId != 0) {
            equalizerEngine.initAudioEffects(initialSessionId)
        }

        crossfadeManager.attachPlayer(player)

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    equalizerEngine.initAudioEffects(audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0 && !equalizerEngine.isInitialized) {
                        equalizerEngine.initAudioEffects(sessionId)
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                DebugLogger.e(
                    LogCategory.AUDIO_ENGINE,
                    "EXOPLAYER",
                    "Playback error [${error.errorCodeName}]: ${error.message} (cause: ${error.cause?.message})"
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let { trackId ->
                    serviceScope.launch(Dispatchers.IO) {
                        val db = BeltaDatabase.getInstance(applicationContext)
                        db.trackDao().incrementPlayCount(trackId)
                    }
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(MediaSessionCallback())
            .build()

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, filter)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(CUSTOM_COMMAND_SET_SPEED, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_SET_SPEED -> {
                    val speed = args.getFloat("speed", 1.0f)
                    val pitch = args.getFloat("pitch", 1.0f)
                    player.playbackParameters = PlaybackParameters(speed, pitch)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onDestroy() {
        unregisterReceiver(becomingNoisyReceiver)
        equalizerEngine.releaseAudioEffects()
        equalizerEngineInstance = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        var equalizerEngineInstance: EqualizerEngine? = null
            private set

        const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "com.belta.audio.TOGGLE_FAVORITE"
        const val CUSTOM_COMMAND_SET_SPEED = "com.belta.audio.SET_SPEED"

        fun trackToMediaItem(track: Track): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
                .build()

            val mediaUri = if (track.path.startsWith("content://") || track.path.startsWith("file://")) {
                Uri.parse(track.path)
            } else {
                Uri.fromFile(java.io.File(track.path))
            }

            return MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(mediaUri)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
