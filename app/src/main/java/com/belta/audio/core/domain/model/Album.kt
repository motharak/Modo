package com.belta.audio.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0L,
    val songCount: Int = 0,
    val year: Int = 0,
    val artworkUri: String? = null
)

@Serializable
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val albumCount: Int = 0
)

@Serializable
data class Folder(
    val path: String,
    val name: String,
    val songCount: Int
)
