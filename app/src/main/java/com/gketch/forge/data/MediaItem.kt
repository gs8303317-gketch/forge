package com.gketch.forge.data

import android.net.Uri

enum class MediaKind { VIDEO, AUDIO }

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
) {
    val isVideo: Boolean get() = kind == MediaKind.VIDEO
}
