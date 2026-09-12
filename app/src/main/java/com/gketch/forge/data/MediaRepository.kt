package com.gketch.forge.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Audio.Media.RELATIVE_PATH
        }
        val selectionParts = mutableListOf("${MediaStore.Audio.Media.IS_MUSIC} != 0")
        val args = mutableListOf<String>()
        if (query.isNotBlank()) {
            selectionParts += "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
            args += "%$query%"
        }
        val items = mutableListOf<ForgeMediaItem>()
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selectionParts.joinToString(" AND "),
            args.toTypedArray().ifEmpty { null },
            "${MediaStore.Audio.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val albumId = cursor.getLong(albumCol)
                val art = if (albumId > 0) {
                    Uri.parse("content://media/external/audio/albumart/$albumId")
                } else null
                items += ForgeMediaItem(
                    id = id,
                    uri = uri,
                    title = cursor.getString(nameCol) ?: "Audio",
                    durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                    sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                    mimeType = cursor.getString(mimeCol) ?: "audio/*",
                    kind = MediaKind.AUDIO,
                    dateAdded = cursor.getLong(dateCol),
                    albumArtUri = art,
                    bucketId = cursor.getLong(bucketIdCol),
                    bucketName = cursor.getString(bucketNameCol) ?: "Music",
                    relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else "",
                )
            }
        }
        return items
    }
}
