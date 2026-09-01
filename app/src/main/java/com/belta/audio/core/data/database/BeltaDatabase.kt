package com.belta.audio.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.belta.audio.core.data.database.dao.EqualizerDao
import com.belta.audio.core.data.database.dao.PlayHistoryDao
import com.belta.audio.core.data.database.dao.PlaylistDao
import com.belta.audio.core.data.database.dao.ScanFolderDao
import com.belta.audio.core.data.database.dao.TrackDao
import com.belta.audio.core.data.database.entity.EqualizerPresetEntity
import com.belta.audio.core.data.database.entity.PlayHistoryEntity
import com.belta.audio.core.data.database.entity.PlaylistEntity
import com.belta.audio.core.data.database.entity.PlaylistItemEntity
import com.belta.audio.core.data.database.entity.ScanFolderEntity
import com.belta.audio.core.data.database.entity.TrackEntity
import com.belta.audio.core.domain.model.EqualizerPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlayHistoryEntity::class,
        EqualizerPresetEntity::class,
        ScanFolderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BeltaDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun equalizerDao(): EqualizerDao
    abstract fun scanFolderDao(): ScanFolderDao

    companion object {
        @Volatile
        private var INSTANCE: BeltaDatabase? = null

        fun getInstance(context: Context): BeltaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeltaDatabase::class.java,
                    "belta_audio.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default equalizer presets
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).equalizerDao()
                                val defaultEntities = EqualizerPreset.DEFAULT_PRESETS.map {
                                    EqualizerPresetEntity.fromDomain(it)
                                }
                                dao.insertDefaultPresets(defaultEntities)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
