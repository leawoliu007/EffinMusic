package code.name.monkey.retromusic.repository

import android.database.Cursor
import code.name.monkey.retromusic.model.Song

class CombinedSongRepository(
    private val mediaStore: SongRepository,
    private val alist: AlistSongRepository
) : SongRepository {

    override fun songs(hideDuplicates: Boolean): List<Song> {
        val mediaStoreSongs = mediaStore.songs(hideDuplicates)
        val alistSongs = alist.songs(hideDuplicates)
        return mediaStoreSongs + alistSongs
    }

    override fun songs(cursor: Cursor?): List<Song> {
        // Cursors are usually for MediaStore
        return mediaStore.songs(cursor)
    }

    override fun sortedSongs(cursor: Cursor?): List<Song> {
        // For sorted versions, we'll sort the combined list ourselves 
        val songs = songs(hideDuplicates = false)
        // Re-use logic or sort here
        return songs.sortedBy { it.title }
    }

    override fun songs(query: String): List<Song> {
        return mediaStore.songs(query) + alist.songs(query)
    }

    override fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean): List<Song> {
        val result = mediaStore.songsByFilePath(filePath, ignoreBlacklist)
        if (result.isNotEmpty()) return result
        return alist.songsByFilePath(filePath, ignoreBlacklist)
    }

    override fun song(cursor: Cursor?): Song = mediaStore.song(cursor)

    override fun song(songId: Long): Song {
        if (songId < 0) {
            return alist.song(songId)
        }
        return mediaStore.song(songId)
    }

    override fun songs(ids: LongArray): List<Song> {
        val alistIds = ids.filter { it < 0 }.toLongArray()
        val mediaStoreIds = ids.filter { it >= 0 }.toLongArray()
        
        val results = mutableListOf<Song>()
        if (alistIds.isNotEmpty()) {
            results.addAll(alist.songs(alistIds))
        }
        if (mediaStoreIds.isNotEmpty()) {
            results.addAll(mediaStore.songs(mediaStoreIds))
        }
        return results
    }
}
