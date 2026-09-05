package com.belta.audio.core.data.repository

import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.Playlist
import com.belta.audio.core.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>
    fun getSmartPlaylistsFlow(): Flow<List<Playlist>>
    fun getUserPlaylistsFlow(): Flow<List<Playlist>>
    fun getTracksForPlaylistFlow(playlistId: Long): Flow<List<Track>>
    suspend fun getTracksForPlaylist(playlistId: Long): List<Track>
    suspend fun getPlaylistById(id: Long): Playlist?
    suspend fun createPlaylist(name: String, description: String = "", isSmart: Boolean = false, smartRuleJson: String? = null): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    suspend fun setPlaylistTracks(playlistId: Long, trackIds: List<Long>)

    // Equalizer presets
    fun getEqualizerPresetsFlow(): Flow<List<EqualizerPreset>>
    suspend fun saveEqualizerPreset(preset: EqualizerPreset): Long
    suspend fun deleteEqualizerPreset(id: Long)
}
