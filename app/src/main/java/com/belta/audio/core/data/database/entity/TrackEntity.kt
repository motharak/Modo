package com.belta.audio.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.belta.audio.core.domain.model.Track

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["genre"]),
        Index(value = ["isFavorite"]),
        Index(value = ["playCount"])
    ]
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val path: String,
    val size: Long,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val year: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val genre: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val channels: Int,
    val isLossless: Boolean,
    val isHiRes: Boolean,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val isFavorite: Boolean,
    val rating: Int,
    val artworkUri: String?
) {
    fun toDomain(): Track = Track(
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
        genre = genre,
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        channels = channels,
        isLossless = isLossless,
        isHiRes = isHiRes,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayedTimestamp,
        isFavorite = isFavorite,
        rating = rating,
        artworkUri = artworkUri
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            artistId = track.artistId,
            album = track.album,
            albumId = track.albumId,
            durationMs = track.durationMs,
            path = track.path,
            size = track.size,
            mimeType = track.mimeType,
            dateAdded = track.dateAdded,
            dateModified = track.dateModified,
            year = track.year,
            trackNumber = track.trackNumber,
            discNumber = track.discNumber,
            genre = track.genre,
            bitrateKbps = track.bitrateKbps,
            sampleRateHz = track.sampleRateHz,
            bitDepth = track.bitDepth,
            channels = track.channels,
            isLossless = track.isLossless,
            isHiRes = track.isHiRes,
            playCount = track.playCount,
            lastPlayedTimestamp = track.lastPlayedTimestamp,
            isFavorite = track.isFavorite,
            rating = track.rating,
            artworkUri = track.artworkUri
        )
    }
}
