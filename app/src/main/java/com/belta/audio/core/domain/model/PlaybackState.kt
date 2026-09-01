package com.belta.audio.core.domain.model

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class ShuffleMode {
    OFF,
    ON
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val currentSpecs: AudioSpecs = AudioSpecs(),
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val sleepTimerRemainingSeconds: Long = 0L,
    val isSleepTimerActive: Boolean = false,
    val crossfadeDurationSeconds: Int = 3,
    val isGaplessEnabled: Boolean = true,
    val isReplayGainEnabled: Boolean = true
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
