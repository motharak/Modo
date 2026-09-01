package com.belta.audio.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.belta.audio.core.data.database.entity.EqualizerPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EqualizerDao {

    @Query("SELECT * FROM equalizer_presets ORDER BY isCustom ASC, name ASC")
    fun getAllPresetsFlow(): Flow<List<EqualizerPresetEntity>>

    @Query("SELECT * FROM equalizer_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): EqualizerPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultPresets(presets: List<EqualizerPresetEntity>)

    @Update
    suspend fun updatePreset(preset: EqualizerPresetEntity)

    @Query("DELETE FROM equalizer_presets WHERE id = :id AND isCustom = 1")
    suspend fun deletePresetById(id: Long)
}
