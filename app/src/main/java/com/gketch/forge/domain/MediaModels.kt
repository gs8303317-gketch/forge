package com.gketch.forge.domain

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class MediaKind { VIDEO, AUDIO }

@Immutable
data class ForgeMediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val kind: MediaKind,
    val dateAdded: Long,
    val albumArtUri: Uri? = null,
    val bucketId: Long = 0L,
    val bucketName: String = "",
    val relativePath: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val albumId: Long = 0L,
) {
    val isVideo: Boolean get() = kind == MediaKind.VIDEO

    fun stableKey(): String = "${kind.name}-$id-${uri}"
}

@Immutable
data class MediaFolder(
    val bucketId: Long,
    val name: String,
    val itemCount: Int,
    val thumbUri: Uri?,
    val kindHint: MediaKind,
)

@Immutable
data class AudioBrowseGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val count: Int,
    val thumbUri: Uri?,
    val tracks: List<ForgeMediaItem>,
)

data class LibraryStorageHint(
    val videoCount: Int,
    val audioCount: Int,
    val totalBytes: Long,
)

enum class AudioBrowseMode { SONGS, ALBUMS, ARTISTS, GENRES }
