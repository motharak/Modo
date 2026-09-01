package com.belta.audio.core.data.repository

import com.belta.audio.core.domain.model.Album
import com.belta.audio.core.domain.model.Artist
import com.belta.audio.core.domain.model.Folder
import com.belta.audio.core.domain.model.Lyrics
import com.belta.audio.core.domain.model.Track
import com.belta.audio.core.data.scanner.ScanProgressInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioRepository {
    val scanProgress: StateFlow<ScanProgressInfo>
    fun getAllTracksFlow(): Flow<List<Track>>
    fun getFavoriteTracksFlow(): Flow<List<Track>>
    fun getAlbumsFlow(): Flow<List<Album>>
    fun getArtistsFlow(): Flow<List<Artist>>
    fun getFoldersFlow(): Flow<List<Folder>>
    fun getTracksByAlbumFlow(albumId: Long): Flow<List<Track>>
    fun getTracksByArtistFlow(artistId: Long): Flow<List<Track>>
    fun getTracksByFolderFlow(folderPath: String): Flow<List<Track>>
    fun getHiResTracksFlow(): Flow<List<Track>>
    fun getMostPlayedTracksFlow(limit: Int = 50): Flow<List<Track>>
    fun getRecentlyAddedTracksFlow(limit: Int = 50): Flow<List<Track>>
    fun getRecentlyPlayedTracksFlow(limit: Int = 50): Flow<List<Track>>
    fun searchTracksFlow(query: String): Flow<List<Track>>
    suspend fun getTrackById(id: Long): Track?
    suspend fun toggleFavorite(trackId: Long, isFavorite: Boolean)
    suspend fun recordTrackPlayed(trackId: Long)
    suspend fun scanLibrary(minDurationSeconds: Int = 60): Int
    suspend fun getLyricsForTrack(track: Track): Lyrics
    suspend fun updateTrackMetadata(track: Track)
}
