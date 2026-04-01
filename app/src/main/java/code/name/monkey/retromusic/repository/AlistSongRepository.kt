package code.name.monkey.retromusic.repository

import android.content.Context
import android.database.Cursor
import code.name.monkey.retromusic.alist.network.AlistClient
import code.name.monkey.retromusic.alist.model.*
import code.name.monkey.retromusic.db.AlistDao
import code.name.monkey.retromusic.db.AlistServerEntity
import code.name.monkey.retromusic.db.AlistSongEntity
import code.name.monkey.retromusic.db.RetroDatabase
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.*

class AlistSongRepository(private val context: Context) : SongRepository {
    private val alistDao: AlistDao = RetroDatabase.getInstance(context).alistDao()

    override fun songs(hideDuplicates: Boolean): List<Song> {
        return runBlocking {
            alistDao.getAllSongs().map { it.toSong() }
        }
    }

    override fun songs(cursor: Cursor?): List<Song> = emptyList()
    override fun sortedSongs(cursor: Cursor?): List<Song> = songs()
    override fun songs(query: String): List<Song> {
        return songs().filter { it.title.contains(query, true) || it.artistName.contains(query, true) }
    }

    override fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean): List<Song> {
        return songs().filter { it.data == filePath }
    }

    override fun song(cursor: Cursor?): Song = Song.emptySong
    override fun song(songId: Long): Song {
        return songs().firstOrNull { it.id == songId } ?: Song.emptySong
    }

    private fun AlistSongEntity.toSong() = Song(
        id = id,
        title = title,
        trackNumber = trackNumber,
        year = year,
        duration = duration,
        data = rawUrl ?: data,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        composer = composer,
        albumArtist = albumArtist,
        artistIds = artistIds,
        artistNames = artistNames
    )

    suspend fun resolvePlaybackUrl(song: Song): String? {
        val songEntity = alistDao.getAllSongs().find { it.id == song.id } ?: return null
        val server = alistDao.getServerById(songEntity.serverId) ?: return null
        
        if (songEntity.rawUrl != null && songEntity.expires > System.currentTimeMillis()) {
            return songEntity.rawUrl
        }

        val client = AlistClient.create(server.url)
        return try {
            val response = client.getFile(AlistFsGetRequest(path = songEntity.data), server.token)
            if (response.code == 200 && response.data?.rawUrl != null) {
                val updated = songEntity.copy(
                    rawUrl = response.data.rawUrl,
                    expires = System.currentTimeMillis() + 3600000
                )
                alistDao.insertSongs(listOf(updated))
                response.data.rawUrl
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun scanFolder(serverId: Long, remotePath: String) {
        val server = alistDao.getServerById(serverId) ?: return
        val client = AlistClient.create(server.url)
        val songs = mutableListOf<AlistSongEntity>()
        
        // Recursive scan with limited depth (default 4)
        scanRecursive(client, server, remotePath, songs, currentDepth = 0, maxDepth = 4)
        
        if (songs.isNotEmpty()) {
            alistDao.insertSongs(songs)
        }
    }

    private suspend fun scanRecursive(
        client: code.name.monkey.retromusic.alist.network.AlistService,
        server: AlistServerEntity,
        path: String,
        results: MutableList<AlistSongEntity>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        
        try {
            val response = client.listFiles(AlistFsListRequest(path = path), server.token)
            if (response.code == 200 && response.data?.content != null) {
                for (file in response.data.content) {
                    val fullPath = if (path == "/") "/${file.name}" else "$path/${file.name}"
                    if (file.isDir) {
                        scanRecursive(client, server, fullPath, results, currentDepth + 1, maxDepth)
                    } else if (isAudioFile(file.name)) {
                        results.add(fileToEntity(file, path, server))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isAudioFile(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return listOf("mp3", "flac", "m4a", "wav", "ogg", "aac", "opus", "ape", "wma", "m4b", "aiff", "aif", "dsf", "dff").contains(extension)
    }

    private fun fileToEntity(file: AlistFile, parentPath: String, server: AlistServerEntity): AlistSongEntity {
        val remotePath = if (parentPath == "/") "/${file.name}" else "$parentPath/${file.name}"
        val id = (server.url + remotePath).hashCode().toLong()
        val negativeId = -Math.abs(id)
        
        return AlistSongEntity(
            id = negativeId,
            serverId = server.id,
            title = file.name.substringBeforeLast('.'),
            trackNumber = 0,
            year = null,
            duration = 0,
            data = remotePath,
            dateModified = 0,
            albumId = -1,
            albumName = "Alist",
            artistId = -1,
            artistName = server.name,
            composer = null,
            albumArtist = null,
            artistIds = null,
            artistNames = null,
            sign = file.sign
        )
    }
}
