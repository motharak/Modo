package com.belta.audio.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.belta.audio.core.data.database.entity.ScanFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanFolderDao {

    @Query("SELECT * FROM scan_folders ORDER BY name ASC")
    fun getAllScanFoldersFlow(): Flow<List<ScanFolderEntity>>

    @Query("SELECT * FROM scan_folders ORDER BY name ASC")
    suspend fun getAllScanFolders(): List<ScanFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ScanFolderEntity)

    @Query("DELETE FROM scan_folders WHERE path = :path")
    suspend fun deleteFolder(path: String)

    @Query("DELETE FROM scan_folders")
    suspend fun clearAllFolders()
}
