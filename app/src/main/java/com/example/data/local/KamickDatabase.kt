package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        MangaEntity::class,
        ChapterEntity::class,
        CategoryEntity::class,
        TrackerEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KamickDatabase : RoomDatabase() {
    abstract fun kamickDao(): KamickDao

    companion object {
        @Volatile
        private var INSTANCE: KamickDatabase? = null

        fun getDatabase(context: Context): KamickDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KamickDatabase::class.java,
                    "kamick_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
