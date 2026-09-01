package com.belta.audio.core.data.sync

import android.content.Context
import com.belta.audio.core.data.database.dao.EqualizerDao
import com.belta.audio.core.data.database.dao.PlaylistDao
import com.belta.audio.core.data.database.dao.TrackDao
import com.belta.audio.core.data.database.entity.EqualizerPresetEntity
import com.belta.audio.core.data.database.entity.PlaylistEntity
import com.belta.audio.core.domain.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleDriveSyncManager(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val equalizerDao: EqualizerDao
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun createBackupPayload(): BeltaCloudBackupPayload = withContext(Dispatchers.IO) {
        val tracks = trackDao.getAllTracks()
        val userStats = tracks.filter { it.playCount > 0 || it.isFavorite || it.rating > 0 }
            .map {
                TrackUserStatBackup(
                    trackTitle = it.title,
                    artistName = it.artist,
                    playCount = it.playCount,
                    isFavorite = it.isFavorite,
                    rating = it.rating,
                    lastPlayedTimestamp = it.lastPlayedTimestamp
                )
            }

        // Get user playlists
        val playlists = mutableListOf<PlaylistBackup>()
        // User playlists and smart playlists
        // (will query playlist entities)
        val allPlaylists = mutableListOf<PlaylistEntity>()
        // We can query all playlists
        // ...

        val eqPresets = mutableListOf<com.belta.audio.core.domain.model.EqualizerPreset>()

        BeltaCloudBackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            userStats = userStats,
            playlists = playlists,
            equalizerPresets = eqPresets
        )
    }

    suspend fun exportBackupToFile(): File = withContext(Dispatchers.IO) {
        val payload = createBackupPayload()
        val jsonString = json.encodeToString(payload)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val backupFile = File(backupDir, "belta_backup_$timeStamp.json")
        backupFile.writeText(jsonString)
        backupFile
    }

    suspend fun restoreBackupFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val payload = json.decodeFromString<BeltaCloudBackupPayload>(jsonString)
            var restoredCount = 0

            // Restore user stats
            val localTracks = trackDao.getAllTracks()
            for (stat in payload.userStats) {
                val matched = localTracks.find {
                    it.title.equals(stat.trackTitle, ignoreCase = true) &&
                            it.artist.equals(stat.artistName, ignoreCase = true)
                }
                if (matched != null) {
                    val updated = matched.copy(
                        playCount = maxOf(matched.playCount, stat.playCount),
                        isFavorite = matched.isFavorite || stat.isFavorite,
                        rating = maxOf(matched.rating, stat.rating),
                        lastPlayedTimestamp = maxOf(matched.lastPlayedTimestamp, stat.lastPlayedTimestamp)
                    )
                    trackDao.updateTrack(updated)
                    restoredCount++
                }
            }

            // Restore custom equalizer presets
            for (preset in payload.equalizerPresets) {
                if (preset.isCustom) {
                    equalizerDao.insertPreset(EqualizerPresetEntity.fromDomain(preset))
                }
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
