package com.rrrainielll.teledrop.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SyncFolderEntity::class, UploadedMediaEntity::class],
    version = 1,
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
                    // Add migrations here as schema evolves
                    // Example: .addMigrations(MIGRATION_1_2)
                    
                    // Fallback to destructive migration for development
                    // Remove this in production if you want to prevent data loss
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Example migration from version 1 to 2 (for future use)
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         // Example: Add a new column
        //         // database.execSQL("ALTER TABLE sync_folders ADD COLUMN last_sync_time INTEGER DEFAULT 0 NOT NULL")
        //     }
        // }
    }
}
