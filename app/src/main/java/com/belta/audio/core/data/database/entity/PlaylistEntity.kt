package com.belta.audio.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.belta.audio.core.domain.model.Playlist

@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["name"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isSmart: Boolean = false,
    val smartRuleJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverArtworkUri: String? = null,
    val crossfadeSeconds: Int? = null,
    val isAutomixEnabled: Boolean = true,
    val fadeCurve: String = "EQUAL_POWER"
) {
    fun toDomain(songCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        isSmart = isSmart,
        smartRuleJson = smartRuleJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
        songCount = songCount,
        coverArtworkUri = coverArtworkUri,
        crossfadeSeconds = crossfadeSeconds,
        isAutomixEnabled = isAutomixEnabled,
        fadeCurve = fadeCurve
    )

    companion object {
        fun fromDomain(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            isSmart = playlist.isSmart,
            smartRuleJson = playlist.smartRuleJson,
            createdAt = playlist.createdAt,
            updatedAt = playlist.updatedAt,
            coverArtworkUri = playlist.coverArtworkUri,
            crossfadeSeconds = playlist.crossfadeSeconds,
            isAutomixEnabled = playlist.isAutomixEnabled,
            fadeCurve = playlist.fadeCurve
        )
    }
}

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackId"])
    ]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val trackId: Long,
    val orderIndex: Int,
    val addedAt: Long = System.currentTimeMillis()
)
