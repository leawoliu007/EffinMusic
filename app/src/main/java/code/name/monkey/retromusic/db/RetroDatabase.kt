package code.name.monkey.retromusic.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        SongEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        SongMetadataEntity::class,
        AlistServerEntity::class,
        AlistFolderEntity::class,
        AlistSongEntity::class
    ],
    version = 32,
    exportSchema = false
)
abstract class RetroDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun songMetadataDao(): SongMetadataDao
    abstract fun alistDao(): AlistDao

    companion object {
        @Volatile
        private var INSTANCE: RetroDatabase? = null

        fun getInstance(context: Context): RetroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RetroDatabase::class.java,
                    "playlist.db" // Match MainModule.kt
                ).fallbackToDestructiveMigration()
                 .addMigrations(*allMigrations)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
