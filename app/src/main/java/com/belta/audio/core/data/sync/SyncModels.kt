package com.belta.audio.core.data.sync

import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.Playlist
import kotlinx.serialization.Serializable

@Serializable
data class TrackUserStatBackup(
    val trackTitle: String,
    val artistName: String,
    val playCount: Int,
    val isFavorite: Boolean,
    val rating: Int,
    val lastPlayedTimestamp: Long
)

@Serializable
data class PlaylistBackup(
    val name: String,
    val description: String,
    val isSmart: Boolean,
    val smartRuleJson: String?,
    val trackKeys: List<String> = emptyList() // "artist|title"
)

@Serializable
data class BeltaCloudBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val userStats: List<TrackUserStatBackup> = emptyList(),
    val playlists: List<PlaylistBackup> = emptyList(),
    val equalizerPresets: List<EqualizerPreset> = emptyList()
)
