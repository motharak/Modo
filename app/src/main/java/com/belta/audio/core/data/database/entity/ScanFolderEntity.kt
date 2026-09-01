package com.belta.audio.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_folders")
data class ScanFolderEntity(
    @PrimaryKey val path: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)
