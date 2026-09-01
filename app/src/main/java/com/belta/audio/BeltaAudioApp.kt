package com.belta.audio

import android.app.Application
import com.belta.audio.core.audio.AudioEngineController
import com.belta.audio.core.debug.DebugHttpServer
import com.belta.audio.core.data.database.BeltaDatabase
import com.belta.audio.core.data.repository.AudioRepository
import com.belta.audio.core.data.repository.AudioRepositoryImpl
import com.belta.audio.core.data.repository.PlaylistRepository
import com.belta.audio.core.data.repository.PlaylistRepositoryImpl
import com.belta.audio.core.data.scanner.MediaStoreScanner
import com.belta.audio.core.data.sync.GoogleDriveSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import com.belta.audio.core.data.lyrics.LyricsRepository
import com.belta.audio.core.data.lyrics.LyricsRepositoryImpl

class BeltaAudioApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: BeltaDatabase
        private set
    lateinit var mediaStoreScanner: MediaStoreScanner
        private set
    lateinit var audioRepository: AudioRepository
        private set
    lateinit var lyricsRepository: LyricsRepository
        private set
    lateinit var playlistRepository: PlaylistRepository
        private set
    lateinit var syncManager: GoogleDriveSyncManager
        private set
    lateinit var audioEngineController: AudioEngineController
        private set
    lateinit var debugHttpServer: DebugHttpServer
        private set

    override fun onCreate() {
        super.onCreate()

        database = BeltaDatabase.getInstance(this)
        mediaStoreScanner = MediaStoreScanner(this, database.trackDao(), database.playlistDao(), database.scanFolderDao())
        audioRepository = AudioRepositoryImpl(database.trackDao(), database.playHistoryDao(), mediaStoreScanner)
        lyricsRepository = LyricsRepositoryImpl(this)
        playlistRepository = PlaylistRepositoryImpl(database.playlistDao(), database.equalizerDao())
        syncManager = GoogleDriveSyncManager(this, database.trackDao(), database.playlistDao(), database.equalizerDao())
        audioEngineController = AudioEngineController(this, appScope)

        debugHttpServer = DebugHttpServer(this, appScope, 8080)
        debugHttpServer.start()
    }

    override fun onTerminate() {
        debugHttpServer.stop()
        audioEngineController.release()
        super.onTerminate()
    }
}
