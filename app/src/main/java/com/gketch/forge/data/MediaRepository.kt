package com.gketch.forge.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class DeleteMediaResult {
    data object Deleted : DeleteMediaResult()
    data class NeedUserConfirm(val intentSender: IntentSender) : DeleteMediaResult()
    data class Failed(val message: String) : DeleteMediaResult()
}

class MediaRepository(private val context: Context) {

    suspend fun loadLibrary(
        query: String = "",
        hiddenBucketIds: Set<Long> = emptySet(),
        extra: List<ForgeMediaItem> = emptyList(),
    ): List<ForgeMediaItem> = withContext(Dispatchers.IO) {
        val videos = try {
            queryVideos(query)
        } catch (_: SecurityException) {
            emptyList()
        }
        val audio = try {
            queryAudio(query)
        } catch (_: SecurityException) {
            emptyList()
        }
        val extras = if (query.isBlank()) extra else extra.filter {
            it.title.contains(query, ignoreCase = true)
        }
        (videos + audio + extras)
            .filter { it.bucketId !in hiddenBucketIds }
            .distinctBy { it.uri.toString() }
            .sortedByDescending { it.dateAdded }
    }

    suspend fun loadFolders(
        hiddenBucketIds: Set<Long> = emptySet(),
        extra: List<ForgeMediaItem> = emptyList(),
    ): List<MediaFolder> = withContext(Dispatchers.IO) {
        val items = try {
            loadLibrary(hiddenBucketIds = hiddenBucketIds, extra = extra)
        } catch (_: Exception) {
            emptyList()
        }
        items
            .groupBy { it.bucketId to (it.bucketName.ifBlank { "Unknown" }) }
            .map { (key, group) ->
                val (bucketId, name) = key
                MediaFolder(
                    bucketId = bucketId,
                    name = name,
                    itemCount = group.size,
                    thumbUri = group.firstOrNull { it.albumArtUri != null }?.albumArtUri
                        ?: group.firstOrNull()?.uri,
                    kindHint = if (group.any { it.isVideo }) MediaKind.VIDEO else MediaKind.AUDIO,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun loadFolderItems(bucketId: Long): List<ForgeMediaItem> = withContext(Dispatchers.IO) {
        loadLibrary().filter { it.bucketId == bucketId }.sortedBy { it.title.lowercase() }
    }

    /**
     * Delete a MediaStore or SAF document. On API 30+ MediaStore items may require a
     * system confirmation IntentSender ([DeleteMediaResult.NeedUserConfirm]).
     */
    suspend fun deleteMedia(item: ForgeMediaItem): DeleteMediaResult = withContext(Dispatchers.IO) {
        val uri = item.uri
        val scheme = uri.scheme?.lowercase().orEmpty()
        try {
            when {
                scheme == "content" && uri.authority?.contains("media", ignoreCase = true) == true -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                        DeleteMediaResult.NeedUserConfirm(pi.intentSender)
                    } else {
                        val rows = context.contentResolver.delete(uri, null, null)
                        if (rows > 0) DeleteMediaResult.Deleted else DeleteMediaResult.Failed("Unable to delete")
                    }
                }
                scheme == "content" -> {
                    val ok = try {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                    } catch (_: SecurityException) {
                        false
                    } catch (_: Exception) {
                        false
                    }
                    if (ok) DeleteMediaResult.Deleted else DeleteMediaResult.Failed("No permission to delete this file")
                }
                scheme == "file" -> {
                    val path = uri.path
                    if (path.isNullOrBlank()) {
                        DeleteMediaResult.Failed("Invalid file path")
                    } else {
                        val file = File(path)
                        if (file.exists() && file.delete()) DeleteMediaResult.Deleted
                        else DeleteMediaResult.Failed("Could not delete file")
                    }
                }
                else -> DeleteMediaResult.Failed("Cannot delete this location")
            }
        } catch (e: SecurityException) {
            DeleteMediaResult.Failed(e.message ?: "Permission denied")
        } catch (e: Exception) {
            DeleteMediaResult.Failed(e.message ?: "Delete failed")
        }
    }


    suspend fun storageHint(
        hiddenBucketIds: Set<Long> = emptySet(),
        extra: List<ForgeMediaItem> = emptyList(),
    ): LibraryStorageHint = withContext(Dispatchers.IO) {
        val items = try {
            loadLibrary(hiddenBucketIds = hiddenBucketIds, extra = extra)
        } catch (_: Exception) {
            emptyList()
        }
        LibraryStorageHint(
            videoCount = items.count { it.isVideo },
            audioCount = items.count { !it.isVideo },
            totalBytes = items.sumOf { it.sizeBytes.coerceAtLeast(0L) },
        )
    }

    private fun queryVideos(query: String): List<ForgeMediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Video.Media.RELATIVE_PATH
        }
        val selection: String?
        val args: Array<String>?
        if (query.isBlank()) {
            selection = null
            args = null
        } else {
            selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            args = arrayOf("%$query%")
        }
        val items = mutableListOf<ForgeMediaItem>()
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            args,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            } else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += ForgeMediaItem(
                    id = id,
                    uri = uri,
                    title = cursor.getString(nameCol) ?: "Video",
                    durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                    sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                    mimeType = cursor.getString(mimeCol) ?: "video/*",
                    kind = MediaKind.VIDEO,
                    dateAdded = cursor.getLong(dateCol),
                    albumArtUri = uri,
                    bucketId = cursor.getLong(bucketIdCol),
                    bucketName = cursor.getString(bucketNameCol) ?: "Videos",
                    relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else "",
                )
            }
        }
        return items
    }

    private fun queryAudio(query: String): List<ForgeMediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val baseProjection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            baseProjection += MediaStore.Audio.Media.RELATIVE_PATH
        }
        val withGenre = baseProjection.toMutableList().also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it += MediaStore.Audio.Media.GENRE
            }
        }
        val selectionParts = mutableListOf("${MediaStore.Audio.Media.IS_MUSIC} != 0")
        val args = mutableListOf<String>()
        if (query.isNotBlank()) {
            selectionParts += "(${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?)"
            args += "%$query%"
            args += "%$query%"
            args += "%$query%"
            args += "%$query%"
        }
        fun read(projection: List<String>, includeGenre: Boolean): List<ForgeMediaItem>? {
            val items = mutableListOf<ForgeMediaItem>()
            return try {
                context.contentResolver.query(
                    collection,
                    projection.toTypedArray(),
                    selectionParts.joinToString(" AND "),
                    args.toTypedArray().ifEmpty { null },
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC",
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val genreCol = if (includeGenre) cursor.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
                    val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
                    val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                    val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    } else -1
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(collection, id)
                        val albumId = cursor.getLong(albumIdCol)
                        val art = if (albumId > 0) {
                            Uri.parse("content://media/external/audio/albumart/$albumId")
                        } else null
                        val display = cursor.getString(nameCol)
                        val titled = if (titleCol >= 0) cursor.getString(titleCol) else null
                        items += ForgeMediaItem(
                            id = id,
                            uri = uri,
                            title = titled?.takeIf { it.isNotBlank() } ?: display ?: "Audio",
                            durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                            sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                            mimeType = cursor.getString(mimeCol) ?: "audio/*",
                            kind = MediaKind.AUDIO,
                            dateAdded = cursor.getLong(dateCol),
                            albumArtUri = art,
                            bucketId = cursor.getLong(bucketIdCol),
                            bucketName = cursor.getString(bucketNameCol) ?: "Music",
                            relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else "",
                            artist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty() else "",
                            album = if (albumCol >= 0) cursor.getString(albumCol).orEmpty() else "",
                            genre = if (genreCol >= 0) cursor.getString(genreCol).orEmpty() else "",
                            albumId = albumId,
                        )
                    }
                    items
                }
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: SecurityException) {
                emptyList()
            }
        }
        return read(withGenre, includeGenre = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ?: read(baseProjection, includeGenre = false)
            ?: emptyList()
    }
}
