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
package code.name.monkey.retromusic.glide.audiocover

import android.media.MediaMetadataRetriever
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.*

class AudioFileCoverFetcher(private val model: AudioFileCover) : DataFetcher<InputStream> {
    private var stream: InputStream? = null
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        
        // 1. Check if the path is already a direct image file (Alist Cache)
        if (model.filePath.endsWith(".jpg", true) || model.filePath.endsWith(".png", true)) {
            try {
                val file = File(model.filePath)
                if (file.exists()) {
                    stream = FileInputStream(file)
                    callback.onDataReady(stream)
                    return
                }
            } catch (e: Exception) {
                // Fallback to retriever
            }
        }

        // 2. Original MediaMetadataRetriever logic for local files
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(model.filePath)
            val picture = retriever.embeddedPicture
            stream = if (picture != null) {
                ByteArrayInputStream(picture)
            } else {
                AudioFileCoverUtils.fallback(model.filePath)
            }
            callback.onDataReady(stream)
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    override fun cleanup() {
        if (stream != null) {
            try {
                stream?.close()
            } catch (ignore: IOException) {
            }
        }
    }

    override fun cancel() {
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}