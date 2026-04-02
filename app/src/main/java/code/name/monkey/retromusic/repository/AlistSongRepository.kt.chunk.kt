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

    suspend fun fetchAndStoreMetadata(songId: Long, rawUrl: String) = withContext(Dispatchers.IO) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(rawUrl, HashMap())
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artistStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val albumStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val yearStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR)
            val trackNumberStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)

            val durationValue = durationStr?.toLongOrNull() ?: 0L
            val trackNumberValue = trackNumberStr?.substringBefore('/')?.toIntOrNull() ?: 0

            if (!title.isNullOrEmpty()) {
                Log.d(TAG, "Fetched metadata for Alist song $songId: $title - $artistStr")
                alistDao.updateSongMetadata(songId, title, artistStr ?: "Unknown Artist", albumStr ?: "Unknown Album", durationValue, yearStr, trackNumberValue)
                playlistDao.updateSongMetadata(songId, title, artistStr ?: "Unknown Artist", albumStr ?: "Unknown Album", durationValue, yearStr, trackNumberValue)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch metadata for $rawUrl", e)
        } finally {
            retriever.release()
        }
    }
}
