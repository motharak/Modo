package com.belta.audio.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@Serializable
data class Lyrics(
    val trackId: Long,
    val isSynced: Boolean,
    val lines: List<LyricLine> = emptyList(),
    val rawPlainLyrics: String = ""
)
