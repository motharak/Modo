package com.belta.audio.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val isSmart: Boolean = false,
    val smartRuleJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val coverArtworkUri: String? = null
)
