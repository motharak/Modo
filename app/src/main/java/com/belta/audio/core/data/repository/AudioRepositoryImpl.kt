package com.belta.audio.core.data.repository

import com.belta.audio.core.data.database.dao.PlayHistoryDao
import com.belta.audio.core.data.database.dao.TrackDao
import com.belta.audio.core.data.database.entity.PlayHistoryEntity
import com.belta.audio.core.data.database.entity.TrackEntity
import com.belta.audio.core.data.scanner.AudioTagReader
import com.belta.audio.core.data.scanner.MediaStoreScanner
import com.belta.audio.core.domain.model.Album
import com.belta.audio.core.domain.model.Artist
import com.belta.audio.core.domain.model.Folder
import com.belta.audio.core.domain.model.Lyrics
import com.belta.audio.core.domain.model.Track
import com.belta.audio.core.data.scanner.ScanProgressInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class AudioRepositoryImpl(
    private val trackDao: TrackDao,
    private val playHistoryDao: PlayHistoryDao,
    private val mediaStoreScanner: MediaStoreScanner
) : AudioRepository {

    override val scanProgress: StateFlow<ScanProgressInfo> = mediaStoreScanner.scanProgress

    override fun getAllTracksFlow(): Flow<List<Track>> {
        return trackDao.getAllTracksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteTracksFlow(): Flow<List<Track>> {
        return trackDao.getFavoriteTracksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getAlbumsFlow(): Flow<List<Album>> {
        return trackDao.getAllTracksFlow().map { trackEntities ->
            trackEntities.groupBy { it.albumId }.map { (albumId, tracks) ->
                val first = tracks.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = first.artist,
                    artistId = first.artistId,
                    songCount = tracks.size,
                    year = tracks.maxOfOrNull { it.year } ?: 0,
                    artworkUri = first.artworkUri
                )
            }.sortedBy { it.title.lowercase() }
        }
    }

    override fun getArtistsFlow(): Flow<List<Artist>> {
        return trackDao.getAllTracksFlow().map { trackEntities ->
            trackEntities.groupBy { it.artistId }.map { (artistId, tracks) ->
                val first = tracks.first()
                val albumCount = tracks.map { it.albumId }.distinct().size
                Artist(
                    id = artistId,
                    name = first.artist,
                    songCount = tracks.size,
                    albumCount = albumCount
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return trackDao.getAllTracksFlow().map { trackEntities ->
            trackEntities.groupBy { File(it.path).parent ?: "" }
                .filter { it.key.isNotEmpty() }
                .map { (folderPath, tracks) ->
                    Folder(
                        path = folderPath,
                        name = File(folderPath).name,
                        songCount = tracks.size
                    )
                }.sortedBy { it.name.lowercase() }
        }
    }

    override fun getTracksByAlbumFlow(albumId: Long): Flow<List<Track>> {
        return trackDao.getTracksByAlbumFlow(albumId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTracksByArtistFlow(artistId: Long): Flow<List<Track>> {
        return trackDao.getTracksByArtistFlow(artistId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTracksByFolderFlow(folderPath: String): Flow<List<Track>> {
        return trackDao.getAllTracksFlow().map { list ->
            list.filter { File(it.path).parent == folderPath }.map { it.toDomain() }
        }
    }

    override fun getHiResTracksFlow(): Flow<List<Track>> {
        return trackDao.getHiResTracksFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getMostPlayedTracksFlow(limit: Int): Flow<List<Track>> {
        return trackDao.getMostPlayedTracksFlow(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentlyAddedTracksFlow(limit: Int): Flow<List<Track>> {
        return trackDao.getRecentlyAddedTracksFlow(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentlyPlayedTracksFlow(limit: Int): Flow<List<Track>> {
        return trackDao.getRecentlyPlayedTracksFlow(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun searchTracksFlow(query: String): Flow<List<Track>> {
        return trackDao.searchTracksFlow(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTrackById(id: Long): Track? {
        return trackDao.getTrackById(id)?.toDomain()
    }

    override suspend fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        trackDao.updateFavorite(trackId, isFavorite)
    }

    override suspend fun recordTrackPlayed(trackId: Long) {
        trackDao.incrementPlayCount(trackId)
        playHistoryDao.insertHistory(
            PlayHistoryEntity(
                trackId = trackId,
                completed = true
            )
        )
    }

    override suspend fun scanLibrary(minDurationSeconds: Int): Int {
        return mediaStoreScanner.scanLibrary(minDurationSeconds)
    }

    override suspend fun getLyricsForTrack(track: Track): Lyrics {
        return AudioTagReader.parseLyrics(track.path, track.id)
    }

    override suspend fun updateTrackMetadata(track: Track) {
        trackDao.updateTrack(TrackEntity.fromDomain(track))
    }
}
