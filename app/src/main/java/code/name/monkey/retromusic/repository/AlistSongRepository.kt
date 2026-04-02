package code.name.monkey.retromusic.repository

import android.content.Context
import android.database.Cursor
import android.util.Log
import code.name.monkey.retromusic.alist.network.AlistClient
import code.name.monkey.retromusic.alist.model.*
import code.name.monkey.retromusic.db.*
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.*

class AlistSongRepository(private val context: Context) : SongRepository {
    private val database = RetroDatabase.getInstance(context)
    private val alistDao: AlistDao = database.alistDao()
    private val playlistDao: PlaylistDao = database.playlistDao()

    companion object {
        private const val TAG = "AlistSongRepository"
        private const val MAX_SCAN_DEPTH = 5 // Increased depth for better discovery
    }

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

    override fun songs(ids: LongArray): List<Song> {
        val all = runBlocking { alistDao.getAllSongs() }
        return all.filter { it.id in ids.asList() }.map { it.toSong() }
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
        
        Log.d(TAG, "Scanning Alist folder: $remotePath")
        scanRecursive(client, server, remotePath, songs, 0, MAX_SCAN_DEPTH)
        
        // Always create/update playlist, even if empty
        alistDao.insertSongs(songs)
        
        // Playlist creation logic
        val cleanPath = remotePath.trimEnd('/')
        val playlistName = if (cleanPath.isEmpty() || cleanPath == "/") {
            server.name.ifEmpty { 
               server.url.removePrefix("http://").removePrefix("https://").substringBefore('/').ifEmpty { "Alist" }
            }
        } else {
            cleanPath.substringAfterLast('/')
        }
        
        Log.d(TAG, "Target playlist name: $playlistName (Found ${songs.size} songs)")
        
        withContext(Dispatchers.IO) {
            val existing = playlistDao.playlist(playlistName)
            val playlistId = if (existing.isNotEmpty()) {
                existing[0].playListId
            } else {
                playlistDao.createPlaylist(PlaylistEntity(playlistName = playlistName))
            }
            
            playlistDao.deletePlaylistSongs(playlistId)
            val playlistSongs = songs.map { alistSong ->
                SongEntity(
                    playlistCreatorId = playlistId,
                    id = alistSong.id,
                    title = alistSong.title,
                    trackNumber = alistSong.trackNumber,
                    year = alistSong.year,
                    duration = alistSong.duration,
                    data = alistSong.data,
                    dateModified = alistSong.dateModified,
                    albumId = alistSong.albumId,
                    albumName = alistSong.albumName,
                    artistId = alistSong.artistId,
                    artistName = alistSong.artistName,
                    composer = alistSong.composer,
                    albumArtist = alistSong.albumArtist,
                    artistIds = alistSong.artistIds,
                    artistNames = alistSong.artistNames
                )
            }
            playlistDao.insertSongsToPlaylist(playlistSongs)
            Log.d(TAG, "Playlist sync complete for $playlistName")
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
            Log.e(TAG, "Scan failed at $path: ${e.message}")
        }
    }

    private fun isAudioFile(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        // Expanded format support
        return listOf(
            "mp3", "flac", "m4a", "wav", "ogg", "aac", "opus", "ape", "wma", 
            "m4b", "aiff", "aif", "dsf", "dff", "mp4", "m4r", "amr", "mkv"
        ).contains(extension)
    }

    private fun fileToEntity(file: AlistFile, parentPath: String, server: AlistServerEntity): AlistSongEntity {
        val fileName = file.name.substringBeforeLast('.')
        // Parse "Singer - Title"
        var title = fileName
        var artist = server.name
        if (fileName.contains(" - ")) {
            artist = fileName.substringBefore(" - ").trim()
            title = fileName.substringAfter(" - ").trim()
        }

        val album = if (parentPath == "/" || parentPath.isEmpty()) "Alist" else parentPath.substringAfterLast('/')

        val remotePath = if (parentPath == "/") "/${file.name}" else "$parentPath/${file.name}"
        val idValue = (server.url + remotePath).hashCode().toLong()
        val negativeId = -Math.abs(idValue)
        
        return AlistSongEntity(
            id = negativeId,
            serverId = server.id,
            title = title,
            trackNumber = 0,
            year = null,
            duration = 0,
            data = remotePath,
            dateModified = System.currentTimeMillis() / 1000,
            albumId = -1,
            albumName = album,
            artistId = -1,
            artistName = artist,
            composer = null,
            albumArtist = null,
            artistIds = null,
            artistNames = null,
            sign = file.sign
        )
    }
}
