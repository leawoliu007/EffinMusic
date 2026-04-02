/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */

package code.name.monkey.retromusic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class Song(
    open val id: Long,
    open val title: String,
    open val trackNumber: Int,
    open val year: String?,
    open val duration: Long,
    open val data: String,
    open val dateModified: Long,
    open val albumId: Long,
    open val albumName: String,
    open val artistId: Long,
    open val artistName: String,
    open val composer: String?,
    open val albumArtist: String?,
    open val artistIds: String? = null,
    open val artistNames: String? = null,
    open val bitrate: Int = 0,
    open val size: Long = 0,
    open val format: String? = null,
    open val sampleRate: Int = 0,
    open val coverPath: String? = null
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Song) return false

        if (id != other.id) return false
        if (title != other.title) return false
        if (trackNumber != other.trackNumber) return false
        if (year != other.year) return false
        if (duration != other.duration) return false
        if (data != other.data) return false
        if (dateModified != other.dateModified) return false
        if (albumId != other.albumId) return false
        if (albumName != other.albumName) return false
        if (artistId != other.artistId) return false
        if (artistName != other.artistName) return false
        if (composer != other.composer) return false
        if (albumArtist != other.albumArtist) return false
        if (artistIds != other.artistIds) return false
        if (artistNames != other.artistNames) return false
        if (bitrate != other.bitrate) return false
        if (size != other.size) return false
        if (format != other.format) return false
        if (sampleRate != other.sampleRate) return false
        if (coverPath != other.coverPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + trackNumber
        result = 31 * result + (year?.hashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + albumId.hashCode()
        result = 31 * result + albumName.hashCode()
        result = 31 * result + artistId.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + (composer?.hashCode() ?: 0)
        result = 31 * result + (albumArtist?.hashCode() ?: 0)
        result = 31 * result + (artistIds?.hashCode() ?: 0)
        result = 31 * result + (artistNames?.hashCode() ?: 0)
        result = 31 * result + bitrate
        result = 31 * result + size.hashCode()
        result = 31 * result + (format?.hashCode() ?: 0)
        result = 31 * result + sampleRate
        result = 31 * result + (coverPath?.hashCode() ?: 0)
        return result
    }

    fun copy(
        id: Long = this.id,
        title: String = this.title,
        trackNumber: Int = this.trackNumber,
        year: String? = this.year,
        duration: Long = this.duration,
        data: String = this.data,
        dateModified: Long = this.dateModified,
        albumId: Long = this.albumId,
        albumName: String = this.albumName,
        artistId: Long = this.artistId,
        artistName: String = this.artistName,
        composer: String? = this.composer,
        albumArtist: String? = this.albumArtist,
        artistIds: String? = this.artistIds,
        artistNames: String? = this.artistNames,
        bitrate: Int = this.bitrate,
        size: Long = this.size,
        format: String? = this.format,
        sampleRate: Int = this.sampleRate,
        coverPath: String? = this.coverPath
    ): Song {
        return Song(
            id, title, trackNumber, year, duration, data, dateModified, albumId, albumName, artistId,
            artistName, composer, albumArtist, artistIds, artistNames, bitrate, size, format, sampleRate, coverPath
        )
    }

    companion object {
        val emptySong = Song(
            -1L, "", -1, "", -1L, "", -1L, -1L, "", -1L, "", "", "", "", "", 0, 0L, null, 0, null
        )
    }
}
