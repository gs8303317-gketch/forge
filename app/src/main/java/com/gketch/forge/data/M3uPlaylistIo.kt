package com.gketch.forge.data

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * Simple M3U / M3U8 playlist import & export for local files.
 * Supports #EXTINF duration/title lines and bare path/URL entries.
 */
object M3uPlaylistIo {

    fun parse(input: InputStream, defaultName: String = "Imported"): ParsedM3u {
        val reader = BufferedReader(input.reader(StandardCharsets.UTF_8))
        var playlistName = defaultName
        var pendingTitle: String? = null
        var pendingDurationSec: Long = -1
        val items = mutableListOf<ForgeMediaItem>()
        reader.useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEach
                when {
                    line.equals("#EXTM3U", ignoreCase = true) -> Unit
                    line.startsWith("#PLAYLIST:", ignoreCase = true) -> {
                        playlistName = line.substringAfter(':').trim().ifBlank { playlistName }
                    }
                    line.startsWith("#EXTINF:", ignoreCase = true) -> {
                        val body = line.substringAfter(':')
                        val durationPart = body.substringBefore(',').trim()
                        pendingDurationSec = durationPart.toLongOrNull() ?: -1L
                        pendingTitle = body.substringAfter(',', missingDelimiterValue = "")
                            .trim()
                            .ifBlank { null }
                    }
                    line.startsWith("#") -> Unit
                    else -> {
                        val uri = coerceUri(line) ?: return@forEach
                        val title = pendingTitle
                            ?: uri.lastPathSegment?.let { Uri.decode(it) }?.substringAfterLast('/')
                            ?: "Track ${items.size + 1}"
                        val durationMs = if (pendingDurationSec > 0) pendingDurationSec * 1000L else 0L
                        items += forgeItemFromUri(uri, title = title).copy(durationMs = durationMs)
                        pendingTitle = null
                        pendingDurationSec = -1
                    }
                }
            }
        }
        return ParsedM3u(name = playlistName, items = items)
    }

    fun write(output: OutputStream, playlist: ForgePlaylist) {
        OutputStreamWriter(output, StandardCharsets.UTF_8).use { w ->
            w.appendLine("#EXTM3U")
            w.appendLine("#PLAYLIST:${playlist.name}")
            playlist.items.forEach { item ->
                val durSec = if (item.durationMs > 0) (item.durationMs / 1000L) else -1L
                w.appendLine("#EXTINF:$durSec,${item.title}")
                w.appendLine(item.uri.toString())
            }
            w.flush()
        }
    }

    private fun coerceUri(line: String): Uri? {
        val trimmed = line.trim().trim('"')
        if (trimmed.isEmpty()) return null
        val parsed = Uri.parse(trimmed)
        return when {
            parsed.scheme != null -> parsed
            trimmed.startsWith("/") -> Uri.parse("file://$trimmed")
            else -> runCatching { Uri.parse(trimmed) }.getOrNull()
        }
    }
}

data class ParsedM3u(
    val name: String,
    val items: List<ForgeMediaItem>,
)
