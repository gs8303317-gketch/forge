package com.gketch.forge.data

import android.net.Uri

typealias MediaKind = com.gketch.forge.domain.MediaKind
typealias ForgeMediaItem = com.gketch.forge.domain.ForgeMediaItem
typealias MediaFolder = com.gketch.forge.domain.MediaFolder
typealias AudioBrowseGroup = com.gketch.forge.domain.AudioBrowseGroup
typealias LibraryStorageHint = com.gketch.forge.domain.LibraryStorageHint
typealias AudioBrowseMode = com.gketch.forge.domain.AudioBrowseMode
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

fun groupAudioByAlbum(items: List<ForgeMediaItem>): List<AudioBrowseGroup> =
    items.filter { !it.isVideo }
        .groupBy { it.albumId.takeIf { id -> id > 0 }?.toString() ?: it.album.ifBlank { "Unknown album" } }
        .map { (key, tracks) ->
            val title = tracks.firstOrNull()?.album?.takeIf { it.isNotBlank() } ?: "Unknown album"
            val artist = tracks.map { it.artist }.firstOrNull { it.isNotBlank() } ?: "Unknown artist"
            AudioBrowseGroup(
                key = "album:$key",
                title = title,
                subtitle = artist,
                count = tracks.size,
                thumbUri = tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                tracks = tracks.sortedBy { it.title.lowercase() },
            )
        }
        .sortedBy { it.title.lowercase() }

fun groupAudioByArtist(items: List<ForgeMediaItem>): List<AudioBrowseGroup> =
    items.filter { !it.isVideo }
        .groupBy { it.artist.ifBlank { "Unknown artist" } }
        .map { (artist, tracks) ->
            AudioBrowseGroup(
                key = "artist:$artist",
                title = artist,
                subtitle = "${tracks.size} songs",
                count = tracks.size,
                thumbUri = tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                tracks = tracks.sortedBy { it.title.lowercase() },
            )
        }
        .sortedBy { it.title.lowercase() }

fun groupAudioByGenre(items: List<ForgeMediaItem>): List<AudioBrowseGroup> =
    items.filter { !it.isVideo }
        .groupBy { it.genre.ifBlank { "Unknown genre" } }
        .map { (genre, tracks) ->
            AudioBrowseGroup(
                key = "genre:$genre",
                title = genre,
                subtitle = "${tracks.size} songs",
                count = tracks.size,
                thumbUri = tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                tracks = tracks.sortedBy { it.title.lowercase() },
            )
        }
        .sortedBy { it.title.lowercase() }
