package code.name.monkey.retromusic.repository

import android.content.Context
import android.database.Cursor
import android.util.Log
import code.name.monkey.retromusic.alist.network.*
import code.name.monkey.retromusic.alist.model.*
import code.name.monkey.retromusic.db.*
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AlistSongRepository(private val context: Context) : SongRepository {
    private val database = RetroDatabase.getInstance(context)
    private val alistDao: AlistDao = database.alistDao()
    private val playlistDao: PlaylistDao = database.playlistDao()

    companion object {
        private const val TAG = "AlistSongRepository"
        private const val MAX_SCAN_DEPTH = 5
        private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "ogg", "aac", "ape", "wma", "m4p", "opus", "m4b")
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
        data = data,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        composer = composer,
        albumArtist = albumArtist,
        artistIds = artistIds,
        artistNames = artistNames,
        bitrate = bitrate,
        size = size,
        format = format,
        sampleRate = sampleRate,
        coverPath = coverPath
    )

    suspend fun resolvePlaybackUrl(song: Song): String? {
        val alistSong = alistDao.getSongById(song.id) ?: return null
        val server = alistDao.getServerById(alistSong.serverId) ?: return null
        val service = AlistClient.create(server.url, server.token)
        val response = service.getFile(AlistFsGetRequest(alistSong.data), server.token)
        return response.data?.rawUrl
    }

    private fun generatePlaylistName(server: AlistServerEntity, path: String): String {
        val trimmed = path.trim('/')
        return if (trimmed.isEmpty()) {
            if (server.name.isNotBlank()) {
                server.name
            } else {
                // Sanitize URL: Remove protocol and trailing slashes
                server.url.removePrefix("https://").removePrefix("http://").trimEnd('/')
            }
        } else {
            // Get the last segment of the path (the chosen folder C from A/B/C)
            trimmed.substringAfterLast('/')
        }
    }

    private suspend fun getOrCreatePlaylistForFolder(server: AlistServerEntity, path: String): Long {
        val playlistName = generatePlaylistName(server, path)
        // Use a consistent negative ID based on the unique path to keep it stable
        val playlistId = -Math.abs((server.url + path).hashCode().toLong())
        
        val allPlaylists = playlistDao.playlists()
        val existing = allPlaylists.find { it.playListId == playlistId }
        
        if (existing == null) {
            playlistDao.createPlaylist(
                PlaylistEntity(
                    playListId = playlistId,
                    playlistName = playlistName
                )
            )
        }
        return playlistId
    }

    suspend fun scanFolder(serverId: Long, path: String) {
        val server = alistDao.getServerById(serverId) ?: return
        val service = AlistClient.create(server.url, server.token)
        val playlistId = getOrCreatePlaylistForFolder(server, path)
        scanFolderInternal(server, service, path, 0, playlistId)
    }

    suspend fun scanFolders() {
        val folders = alistDao.getAllFolders()
        for (folder in folders) {
            scanFolder(folder.serverId, folder.remotePath)
        }
    }

    suspend fun deleteSong(songId: Long) {
        withContext(Dispatchers.IO) {
            alistDao.deleteSongById(songId)
            playlistDao.deleteSongById(songId)
        }
    }

    private fun isAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return AUDIO_EXTENSIONS.contains(extension)
    }

    private suspend fun scanFolderInternal(server: AlistServerEntity, service: AlistService, path: String, depth: Int, playlistId: Long) {
        if (depth > MAX_SCAN_DEPTH) return
        val response = service.listFiles(AlistFsListRequest(path), server.token)
        val list = response.data?.content ?: return
        val songs = mutableListOf<AlistSongEntity>()
        for (file in list) {
            if (file.isDir) {
                // For subfolders, we'll keep them in the same playlist to avoid creating thousands of playlists
                scanFolderInternal(server, service, if (path == "/") "/${file.name}" else "$path/${file.name}", depth + 1, playlistId)
            } else if (isAudioFile(file.name)) {
                songs.add(fileToEntity(server, file, path))
            }
        }
        if (songs.isNotEmpty()) {
            alistDao.insertSongs(songs)
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
                    artistNames = alistSong.artistNames,
                    bitrate = alistSong.bitrate,
                    size = alistSong.size,
                    format = alistSong.format,
                    sampleRate = alistSong.sampleRate,
                    coverPath = alistSong.coverPath
                )
            }
            playlistDao.insertSongsToPlaylist(playlistSongs)
        }
    }

    private fun fileToEntity(server: AlistServerEntity, file: AlistFile, parentPath: String): AlistSongEntity {
        val fileName = file.name
        val nameWithoutExtension = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        var title = nameWithoutExtension
        var artist = server.name
        
        if (nameWithoutExtension.contains(" - ")) {
            artist = nameWithoutExtension.substringBefore(" - ").trim()
            title = nameWithoutExtension.substringAfter(" - ").trim()
        }

        val album = if (parentPath == "/" || parentPath.isEmpty()) "Alist" else parentPath.trimEnd('/').substringAfterLast('/')
        val remotePath = if (parentPath == "/") "/${file.name}" else "$parentPath/${file.name}"
        val idValue = (server.url + remotePath).hashCode().toLong()
        val negativeId = -Math.abs(idValue)
        
        val generatedArtistId = artist.hashCode().toLong()

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
            artistId = generatedArtistId,
            artistName = artist,
            composer = null,
            albumArtist = null,
            artistIds = generatedArtistId.toString(),
            artistNames = artist,
            sign = file.sign,
            bitrate = 0,
            size = file.size,
            format = file.name.substringAfterLast('.', "").uppercase(),
            sampleRate = 0,
            coverPath = null
        )
    }

    suspend fun fetchAndStoreMetadata(songId: Long, rawUrl: String) {
        withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                Log.d(TAG, "Starting metadata fetch for: $rawUrl")
                val headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                retriever.setDataSource(rawUrl, headers)

                val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artistStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val albumStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                val yearStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR)
                val trackNumberStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                val bitrateStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
                
                val durationValue = durationStr?.toLongOrNull() ?: 0L
                val bitrateValue = bitrateStr?.toIntOrNull() ?: 0
                val trackNumberValue = trackNumberStr?.substringBefore('/')?.toIntOrNull() ?: 0
                val sampleRateValue = if (android.os.Build.VERSION.SDK_INT >= 29) {
                    retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
                } else 0

                var finalCoverPath: String? = null
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    val coverDir = File(context.cacheDir, "alist_covers")
                    if (!coverDir.exists()) coverDir.mkdirs()
                    val coverFile = File(coverDir, "${songId}.jpg")
                    try {
                        FileOutputStream(coverFile).use { fos ->
                            fos.write(picture)
                        }
                        finalCoverPath = coverFile.absolutePath
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save cover: ${e.message}")
                    }
                }

                val existingSong = alistDao.getSongById(songId)
                if (existingSong != null) {
                    val finalTitle = if (title.isNullOrEmpty()) existingSong.title else title
                    val finalArtist = if (artistStr.isNullOrEmpty()) existingSong.artistName else artistStr
                    val finalAlbum = if (albumStr.isNullOrEmpty()) existingSong.albumName else albumStr
                    val finalYear = if (yearStr.isNullOrEmpty()) existingSong.year else yearStr
                    
                    val generatedArtistId = finalArtist.hashCode().toLong()

                    Log.d(TAG, "Updating metadata for $songId: $finalTitle, Artist: $finalArtist, Duration: $durationValue")
                    
                    alistDao.updateSongMetadata(songId, finalTitle, finalArtist, finalAlbum, durationValue, finalYear, trackNumberValue, bitrateValue, existingSong.size, existingSong.format, sampleRateValue, generatedArtistId, finalArtist, generatedArtistId.toString(), finalCoverPath)
                    playlistDao.updateSongMetadata(songId, finalTitle, finalArtist, finalAlbum, durationValue, finalYear, trackNumberValue, bitrateValue, existingSong.size, existingSong.format, sampleRateValue, generatedArtistId, finalArtist, generatedArtistId.toString(), finalCoverPath)
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Metadata extraction failed: ${e.message}")
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {}
            }
        }
    }
}
