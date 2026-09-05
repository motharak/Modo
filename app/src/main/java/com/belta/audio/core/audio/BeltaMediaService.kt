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
import androidx.media3.common.ForwardingPlayer
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
import kotlinx.coroutines.withContext

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
    var crossfadeEngine: DualPlayerCrossfadeEngine? = null
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

        val playerB = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        crossfadeEngine = DualPlayerCrossfadeEngine(
            context = this,
            scope = serviceScope,
            playerA = player,
            playerB = playerB,
            trackToMediaItemConverter = { trackToMediaItem(it) }
        ).apply {
            onActivePlayerSwapped = { newActivePlayer, _ ->
                equalizerEngine.initAudioEffects(newActivePlayer.audioSessionId)
            }
            onAudioSessionIdChanged = { newSessionId ->
                equalizerEngine.initAudioEffects(newSessionId)
            }
            onAutoPlayNextRequested = {
                autoPlayNextTrackInService()
            }
        }

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
                } else if (state == Player.STATE_ENDED) {
                    autoPlayNextTrackInService()
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

        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                    true
                } else {
                    super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() {
                if (super.hasNextMediaItem()) {
                    super.seekToNext()
                } else {
                    autoPlayNextTrackInService()
                }
            }

            override fun seekToNextMediaItem() {
                if (super.hasNextMediaItem()) {
                    super.seekToNextMediaItem()
                } else {
                    autoPlayNextTrackInService()
                }
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
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
                .add(SessionCommand(CUSTOM_COMMAND_SET_CROSSFADE, Bundle.EMPTY))
                .build()

            // Keep COMMAND_SEEK_TO_NEXT active so phone lock screen and Bluetooth always show an enabled Next button
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            if (playerCommand == Player.COMMAND_SEEK_TO_NEXT || playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                if (!player.hasNextMediaItem()) {
                    autoPlayNextTrackInService()
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
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
                CUSTOM_COMMAND_SET_CROSSFADE -> {
                    val duration = args.getInt("crossfade_duration", 3)
                    val automix = args.getBoolean("automix_enabled", true)
                    val curveName = args.getString("fade_curve", "EQUAL_POWER")
                    val curve = try { FadeCurve.valueOf(curveName) } catch (_: Exception) { FadeCurve.EQUAL_POWER }
                    crossfadeManager.crossfadeDurationSeconds = duration
                    crossfadeManager.isSilenceTrimmingEnabled = automix
                    crossfadeManager.fadeCurve = curve
                    crossfadeEngine?.let { engine ->
                        engine.crossfadeDurationSeconds = duration
                        engine.isAutomixEnabled = automix
                        engine.fadeCurve = curve
                    }
                    DebugLogger.i(
                        LogCategory.AUDIO_ENGINE,
                        "CROSSFADE",
                        "Crossfade configured: ${duration}s, automix=$automix, curve=$curveName"
                    )
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun autoPlayNextTrackInService() {
        try {
            val prefs = getSharedPreferences("belta_prefs", Context.MODE_PRIVATE)
            val isAutoPlayEnabled = prefs.getBoolean("auto_play_radio_enabled", true)
            if (!isAutoPlayEnabled) return

            // Safely read currentMediaId on the Main Thread (where ExoPlayer lives)
            val currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull()

            serviceScope.launch(Dispatchers.IO) {
                try {
                    val db = BeltaDatabase.getInstance(applicationContext)
                    val allEntities = db.trackDao().getAllTracks()
                    if (allEntities.isEmpty()) return@launch

                    val currentEntity = allEntities.find { it.id == currentMediaId }
                    val candidates = allEntities.filter { it.id != currentMediaId }
                    if (candidates.isEmpty()) return@launch

                    val similar = if (currentEntity != null && !currentEntity.genre.isNullOrBlank()) {
                        candidates.filter { it.genre.equals(currentEntity.genre, ignoreCase = true) || it.artistId == currentEntity.artistId }
                    } else emptyList()

                    val chosenEntity = if (similar.isNotEmpty()) similar.random() else candidates.random()
                    val chosenTrack = chosenEntity.toDomain()
                    val mediaItem = trackToMediaItem(chosenTrack)

                    withContext(Dispatchers.Main) {
                        try {
                            player.addMediaItem(mediaItem)
                            player.seekToDefaultPosition(player.mediaItemCount - 1)
                            player.prepare()
                            player.play()
                            DebugLogger.i(
                                LogCategory.AUDIO_ENGINE,
                                "AUTO_PLAY",
                                "Service auto-playing random track on lock screen: ${chosenTrack.title} by ${chosenTrack.artist}"
                            )
                        } catch (e: Exception) {
                            DebugLogger.e(LogCategory.AUDIO_ENGINE, "AUTO_PLAY", "Error starting playback on main: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    DebugLogger.e(LogCategory.AUDIO_ENGINE, "AUTO_PLAY", "Error loading auto-play tracks: ${e.message}")
                }
            }
        } catch (e: Exception) {
            DebugLogger.e(LogCategory.AUDIO_ENGINE, "AUTO_PLAY", "Error in autoPlayNextTrackInService: ${e.message}")
        }
    }

    override fun onDestroy() {
        unregisterReceiver(becomingNoisyReceiver)
        equalizerEngine.releaseAudioEffects()
        equalizerEngineInstance = null
        crossfadeEngine?.release()
        crossfadeEngine = null
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
        const val CUSTOM_COMMAND_SET_CROSSFADE = "com.belta.audio.SET_CROSSFADE"

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
