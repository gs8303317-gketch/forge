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
    val bucketId: Long = 0L,
    val bucketName: String = "",
    val relativePath: String = "",
) {
    val isVideo: Boolean get() = kind == MediaKind.VIDEO

    fun stableKey(): String = "${kind.name}-$id-${uri}"
}

data class MediaFolder(
    val bucketId: Long,
    val name: String,
    val itemCount: Int,
    val thumbUri: Uri?,
    val kindHint: MediaKind,
)

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma", "aiff",
)

fun inferMediaKind(uri: Uri, mime: String?): MediaKind {
    val m = mime.orEmpty().lowercase()
    if (m.startsWith("audio")) return MediaKind.AUDIO
    if (m.startsWith("video")) return MediaKind.VIDEO
    val path = (uri.lastPathSegment ?: uri.path ?: uri.toString()).lowercase()
    val ext = path.substringAfterLast('.', missingDelimiterValue = "")
    if (ext in AUDIO_EXTENSIONS) return MediaKind.AUDIO
    return MediaKind.VIDEO
}

fun isPlayableStreamUrl(raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return false
    val scheme = Uri.parse(trimmed).scheme?.lowercase() ?: return false
    return scheme in setOf("http", "https", "rtsp", "rtsps")
}

fun forgeItemFromUri(
    uri: Uri,
    title: String? = null,
    mime: String? = null,
): ForgeMediaItem {
    val kind = inferMediaKind(uri, mime)
    val fallback = if (kind == MediaKind.AUDIO) "audio/*" else "video/*"
    val name = title
        ?: uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.let { Uri.decode(it) }
            ?.takeIf { it.isNotBlank() }
        ?: "Stream"
    return ForgeMediaItem(
        id = uri.toString().hashCode().toLong(),
        uri = uri,
        title = name,
        durationMs = 0L,
        sizeBytes = 0L,
        mimeType = mime?.takeIf { it.isNotBlank() } ?: fallback,
        kind = kind,
        dateAdded = System.currentTimeMillis() / 1000,
    )
}
