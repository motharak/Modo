package com.belta.audio.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.belta.audio.core.domain.model.EqualizerPreset

@Entity(
    tableName = "play_history",
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["playedAt"])
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val durationPlayedMs: Long = 0,
    val completed: Boolean = false
)

@Entity(
    tableName = "equalizer_presets"
)
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandGainsJson: String, // Comma separated integers
    val bassBoost: Int,
    val virtualizer: Int,
    val reverbStage: String = "NONE",
    val isCustom: Boolean = true
) {
    fun toDomain(): EqualizerPreset {
        val gains = bandGainsJson.split(",").mapNotNull { it.trim().toIntOrNull() }
        val stage = try {
            com.belta.audio.core.domain.model.ReverbSoundStage.valueOf(reverbStage)
        } catch (_: Exception) {
            com.belta.audio.core.domain.model.ReverbSoundStage.NONE
        }
        return EqualizerPreset(
            id = id,
            name = name,
            bandGains = if (gains.size == 10) gains else List(10) { 0 },
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            reverbStage = stage,
            isCustom = isCustom
        )
    }

    companion object {
        fun fromDomain(preset: EqualizerPreset): EqualizerPresetEntity = EqualizerPresetEntity(
            id = preset.id,
            name = preset.name,
            bandGainsJson = preset.bandGains.joinToString(","),
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            reverbStage = preset.reverbStage.name,
            isCustom = preset.isCustom
        )
    }
}
