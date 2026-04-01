package code.name.monkey.retromusic.db

import androidx.room.*

@Entity(tableName = "alist_server")
data class AlistServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val token: String? = null
)

@Entity(tableName = "alist_folder")
data class AlistFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long,
    val remotePath: String,
    val name: String
)

@Entity(tableName = "alist_song")
data class AlistSongEntity(
    @PrimaryKey val id: Long, 
    val serverId: Long,
    val title: String,
    val trackNumber: Int,
    val year: String?,
    val duration: Long,
    val data: String,
    val dateModified: Long,
    val albumId: Long,
    val albumName: String,
    val artistId: Long,
    val artistName: String,
    val composer: String?,
    val albumArtist: String?,
    val artistIds: String?,
    val artistNames: String?,
    val rawUrl: String? = null,
    val sign: String? = null,
    val expires: Long = 0
)

@Dao
interface AlistDao {
    @Query("SELECT * FROM alist_server")
    suspend fun getAllServers(): List<AlistServerEntity>

    @Query("SELECT * FROM alist_server WHERE id = :serverId")
    suspend fun getServerById(serverId: Long): AlistServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: AlistServerEntity): Long

    @Delete
    suspend fun deleteServer(server: AlistServerEntity)

    @Query("SELECT * FROM alist_folder")
    suspend fun getAllFolders(): List<AlistFolderEntity>

    @Query("SELECT * FROM alist_folder WHERE serverId = :serverId")
    suspend fun getFoldersForServer(serverId: Long): List<AlistFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: AlistFolderEntity): Long

    @Delete
    suspend fun deleteFolder(folder: AlistFolderEntity)

    @Query("SELECT * FROM alist_song")
    suspend fun getAllSongs(): List<AlistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<AlistSongEntity>)

    @Query("DELETE FROM alist_song WHERE serverId = :serverId")
    suspend fun deleteSongsByServer(serverId: Long)
    
    @Query("DELETE FROM alist_song WHERE serverId = :serverId AND data LIKE :path || '%'")
    suspend fun deleteSongsByPath(serverId: Long, path: String)
}
