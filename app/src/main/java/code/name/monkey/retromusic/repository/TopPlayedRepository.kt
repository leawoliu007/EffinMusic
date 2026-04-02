/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.repository

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import code.name.monkey.retromusic.Constants.NUMBER_OF_TOP_TRACKS
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.providers.HistoryStore
import code.name.monkey.retromusic.providers.SongPlayCountStore
import code.name.monkey.retromusic.util.PreferenceUtil


/**
 * Created by hemanths on 16/08/17.
 */

interface TopPlayedRepository {
    fun recentlyPlayedTracks(): List<Song>

    fun topTracks(): List<Song>

    fun notRecentlyPlayedTracks(): List<Song>

    fun topAlbums(): List<Album>

    fun topArtists(): List<Artist>
}

class RealTopPlayedRepository(
    private val context: Context,
    private val songRepository: SongRepository,
    private val albumRepository: RealAlbumRepository,
    private val artistRepository: RealArtistRepository
) : TopPlayedRepository {

    override fun recentlyPlayedTracks(): List<Song> {
        val ids = HistoryStore.getInstance(context).queryRecentIds(PreferenceUtil.getRecentlyPlayedCutoffTimeMillis().toLong()).use { cursor ->
            val result = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(cursor.getColumnIndex(HistoryStore.RecentStoreColumns.ID)))
            }
            result.toLongArray()
        }
        return songRepository.songs(ids)
    }

    override fun topTracks(): List<Song> {
        val ids = SongPlayCountStore.getInstance(context).getTopPlayedResults(NUMBER_OF_TOP_TRACKS).use { cursor ->
            val result = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(cursor.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)))
            }
            result.toLongArray()
        }
        return songRepository.songs(ids)
    }

    override fun notRecentlyPlayedTracks(): List<Song> {
        val allSongs = songRepository.songs(PreferenceUtil.hideDuplicateSongs)
        val playedIds = SongPlayCountStore.getInstance(context).getTopPlayedResults(1000).use { cursor ->
            val result = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(cursor.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)))
            }
            result.toSet()
        }
        return allSongs.filter { it.id !in playedIds }
    }

    override fun topAlbums(): List<Album> {
        return albumRepository.splitIntoAlbums(topTracks(), sorted = false)
    }

    override fun topArtists(): List<Artist> {
        return artistRepository.splitIntoArtists(topAlbums())
    }
}
