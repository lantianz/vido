package com.vido.local.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "private_media")
data class PrivateMediaEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val modifiedMs: Long,
    val folder: String,
    val scannedAtMs: Long,
)

@Entity(tableName = "private_directories")
data class PrivateDirectoryEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAtMs: Long,
)

@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val uri: String,
    val positionMs: Long,
    val updatedAtMs: Long,
)

@Dao
interface PrivateMediaDao {
    @Query("SELECT * FROM private_media ORDER BY scannedAtMs DESC, title COLLATE NOCASE")
    fun observeAll(): Flow<List<PrivateMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<PrivateMediaEntity>)

    @Query("DELETE FROM private_media")
    suspend fun clear()
}

@Dao
interface PrivateDirectoryDao {
    @Query("SELECT * FROM private_directories ORDER BY addedAtMs DESC")
    fun observeAll(): Flow<List<PrivateDirectoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(directory: PrivateDirectoryEntity)

    @Query("DELETE FROM private_directories WHERE treeUri = :uri")
    suspend fun delete(uri: String)
}

@Dao
interface PlaybackDao {
    @Query("SELECT positionMs FROM playback_positions WHERE uri = :uri LIMIT 1")
    suspend fun positionFor(uri: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(position: PlaybackPositionEntity)
}

@Database(
    entities = [PrivateMediaEntity::class, PrivateDirectoryEntity::class, PlaybackPositionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun privateMediaDao(): PrivateMediaDao
    abstract fun directoryDao(): PrivateDirectoryDao
    abstract fun playbackDao(): PlaybackDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "vido.db",
        ).build()
    }
}
