package com.belta.audio.core.data.repository

import com.belta.audio.core.data.database.dao.EqualizerDao
import com.belta.audio.core.data.database.dao.PlaylistDao
import com.belta.audio.core.data.database.entity.EqualizerPresetEntity
import com.belta.audio.core.data.database.entity.PlaylistEntity
import com.belta.audio.core.data.database.entity.PlaylistItemEntity
import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.Playlist
import com.belta.audio.core.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao,
    private val equalizerDao: EqualizerDao
) : PlaylistRepository {

    override fun getAllPlaylistsFlow(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSmartPlaylistsFlow(): Flow<List<Playlist>> {
        return playlistDao.getSmartPlaylistsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUserPlaylistsFlow(): Flow<List<Playlist>> {
        return playlistDao.getUserPlaylistsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTracksForPlaylistFlow(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylistFlow(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistDao.getPlaylistById(id)?.toDomain()
    }

    override suspend fun createPlaylist(
        name: String,
        description: String,
        isSmart: Boolean,
        smartRuleJson: String?
    ): Long {
        val entity = PlaylistEntity(
            name = name,
            description = description,
            isSmart = isSmart,
            smartRuleJson = smartRuleJson
        )
        return playlistDao.insertPlaylist(entity)
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(PlaylistEntity.fromDomain(playlist))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        val currentTracks = playlistDao.getTracksForPlaylist(playlistId)
        var startIndex = currentTracks.size
        val items = trackIds.map { trackId ->
            PlaylistItemEntity(
                playlistId = playlistId,
                trackId = trackId,
                orderIndex = startIndex++
            )
        }
        playlistDao.insertPlaylistItems(items)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    override suspend fun setPlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        playlistDao.setPlaylistTracks(playlistId, trackIds)
    }

    override fun getEqualizerPresetsFlow(): Flow<List<EqualizerPreset>> {
        return equalizerDao.getAllPresetsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveEqualizerPreset(preset: EqualizerPreset): Long {
        return equalizerDao.insertPreset(EqualizerPresetEntity.fromDomain(preset))
    }

    override suspend fun deleteEqualizerPreset(id: Long) {
        equalizerDao.deletePresetById(id)
    }
}
