package com.belta.audio.core.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.belta.audio.core.data.database.dao.PlaylistDao
import com.belta.audio.core.data.database.dao.TrackDao
import com.belta.audio.core.data.database.entity.PlaylistEntity
import com.belta.audio.core.data.database.entity.TrackEntity
import com.belta.audio.core.domain.model.SmartPlaylistPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.belta.audio.core.data.database.dao.ScanFolderDao

data class ScanProgressInfo(
    val isScanning: Boolean = false,
    val currentTitle: String = "",
    val scannedCount: Int = 0
)

class MediaStoreScanner(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val scanFolderDao: ScanFolderDao
) {
    private val artworkUriBase = Uri.parse("content://media/external/audio/albumart")

    private val _scanProgress = MutableStateFlow(ScanProgressInfo())
    val scanProgress: StateFlow<ScanProgressInfo> = _scanProgress.asStateFlow()

    suspend fun scanLibrary(minDurationSeconds: Int = 60): Int = withContext(Dispatchers.IO) {
        _scanProgress.value = ScanProgressInfo(isScanning = true, currentTitle = "Searching audio files...", scannedCount = 0)
        val tracksToInsert = mutableListOf<TrackEntity>()
        val existingPaths = mutableListOf<String>()

        val userConfiguredFolders = scanFolderDao.getAllScanFolders().map { it.path }
        val minDurationMs = (minDurationSeconds * 1000L).coerceAtLeast(0L)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR ${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR ${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.alac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR ${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR ${MediaStore.Audio.Media.DATA} LIKE '%.wav') AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val artistId = cursor.getLong(artistIdCol)
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol)
                    val path = cursor.getString(dataCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val mimeType = cursor.getString(mimeTypeCol) ?: "audio/*"
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateModified = cursor.getLong(dateModifiedCol)
                    val year = cursor.getInt(yearCol)
                    val trackRaw = cursor.getInt(trackCol)
                    val trackNumber = if (trackRaw >= 1000) trackRaw % 1000 else trackRaw
                    val discNumber = if (trackRaw >= 1000) trackRaw / 1000 else 1

                    if (path.isEmpty() || !File(path).exists() || durationMs < minDurationMs) continue

                    // If user specified custom music folders, only include tracks inside those directories!
                    if (userConfiguredFolders.isNotEmpty()) {
                        val isInsideAllowedFolder = userConfiguredFolders.any { allowedFolder ->
                            path.startsWith(allowedFolder, ignoreCase = true)
                        }
                        if (!isInsideAllowedFolder) continue
                    }
                    existingPaths.add(path)

                    // Fetch existing record to retain user statistics (favorites, play count)
                    val existing = trackDao.getTrackByPath(path)

                    val artworkUri = ContentUris.withAppendedId(artworkUriBase, albumId).toString()

                    // Extract deep specs
                    val specs = AudioTagReader.extractSpecs(path, mimeType)

                    val entity = TrackEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        artistId = artistId,
                        album = album,
                        albumId = albumId,
                        durationMs = durationMs,
                        path = path,
                        size = size,
                        mimeType = mimeType,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        year = year,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        genre = existing?.genre ?: "",
                        bitrateKbps = specs.bitrateKbps,
                        sampleRateHz = specs.sampleRateHz,
                        bitDepth = specs.bitDepth,
                        channels = specs.channelCount,
                        isLossless = specs.isLossless,
                        isHiRes = specs.isHiRes,
                        playCount = existing?.playCount ?: 0,
                        lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                        isFavorite = existing?.isFavorite ?: false,
                        rating = existing?.rating ?: 0,
                        artworkUri = artworkUri
                    )
                    tracksToInsert.add(entity)
                    if (tracksToInsert.size % 4 == 0 || tracksToInsert.size == 1) {
                        _scanProgress.value = ScanProgressInfo(
                            isScanning = true,
                            currentTitle = title,
                            scannedCount = tracksToInsert.size
                        )
                    }
                }
            }

            if (tracksToInsert.isNotEmpty()) {
                trackDao.insertTracks(tracksToInsert)
                com.belta.audio.core.debug.DebugLogger.i(
                    com.belta.audio.core.debug.LogCategory.MEDIA_STORE,
                    "SCANNER",
                    "Indexed ${tracksToInsert.size} audio tracks into library database"
                )
            }
            if (existingPaths.isNotEmpty()) {
                trackDao.deleteOldTracks(existingPaths)
            }

            // Initialize default Smart Playlists if not present
            initializeDefaultSmartPlaylists()

            tracksToInsert.size
        } catch (e: Exception) {
            com.belta.audio.core.debug.DebugLogger.e(
                com.belta.audio.core.debug.LogCategory.MEDIA_STORE,
                "SCANNER_ERROR",
                "Scan error: ${e.localizedMessage}"
            )
            e.printStackTrace()
            0
        } finally {
            _scanProgress.value = ScanProgressInfo(
                isScanning = false,
                currentTitle = "",
                scannedCount = tracksToInsert.size
            )
        }
    }

    private suspend fun initializeDefaultSmartPlaylists() {
        val json = Json { prettyPrint = false; ignoreUnknownKeys = true }
        for (preset in SmartPlaylistPreset.entries) {
            val existing = playlistDao.getPlaylistByName(preset.title)
            if (existing == null) {
                val ruleJson = json.encodeToString(preset.definition)
                playlistDao.insertPlaylist(
                    PlaylistEntity(
                        name = preset.title,
                        description = preset.description,
                        isSmart = true,
                        smartRuleJson = ruleJson
                    )
                )
            }
        }
    }
}
