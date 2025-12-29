package com.rrrainielll.teledrop.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SyncFolderEntity::class, UploadedMediaEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncFolderDao(): SyncFolderDao
    abstract fun uploadedMediaDao(): UploadedMediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teledrop_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migration from version 1 to 2: Add index on checksum column
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create index on checksum column for faster duplicate detection
                database.execSQL("CREATE INDEX IF NOT EXISTS index_uploaded_media_checksum ON uploaded_media(checksum)")
            }
        }
    }
}
