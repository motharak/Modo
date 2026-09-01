package com.belta.audio.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.belta.audio.core.data.database.entity.PlaylistEntity
import com.belta.audio.core.data.database.entity.PlaylistItemEntity
import com.belta.audio.core.data.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY isSmart DESC, name COLLATE NOCASE ASC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isSmart = 1 ORDER BY name ASC")
    fun getSmartPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isSmart = 0 ORDER BY name ASC")
    fun getUserPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_items pi ON t.id = pi.trackId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.orderIndex ASC
    """)
    fun getTracksForPlaylistFlow(playlistId: Long): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_items pi ON t.id = pi.trackId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.orderIndex ASC
    """)
    suspend fun getTracksForPlaylist(playlistId: Long): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    fun getTrackCountForPlaylistFlow(playlistId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Transaction
    suspend fun setPlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        clearPlaylistItems(playlistId)
        val items = trackIds.mapIndexed { index, trackId ->
            PlaylistItemEntity(
                playlistId = playlistId,
                trackId = trackId,
                orderIndex = index
            )
        }
        insertPlaylistItems(items)
    }
}
