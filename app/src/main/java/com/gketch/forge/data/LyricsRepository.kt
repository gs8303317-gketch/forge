package com.gketch.forge.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class LyricsResult(
    val text: String,
    val source: String,
)

object LyricsRepository {
    suspend fun load(context: Context, item: ForgeMediaItem?): LyricsResult? = withContext(Dispatchers.IO) {
        if (item == null) return@withContext null
        embedded(context, item.uri)?.let { return@withContext LyricsResult(it, "embedded") }
        sidecarLrc(context, item)?.let { return@withContext LyricsResult(it, ".lrc") }
        null
    }

    private fun embedded(context: Context, uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val keys = mutableListOf<Int>()
            // METADATA_KEY_LYRICS = 29 on API 31+ (hidden/compat); try common extras
            if (Build.VERSION.SDK_INT >= 31) {
                keys += 29 // MediaMetadataRetriever.METADATA_KEY_LYRICS
            }
            for (key in keys) {
                val value = runCatching { retriever.extractMetadata(key) }.getOrNull()
                if (!value.isNullOrBlank()) return value.trim()
            }
            // Some OEMs stash lyrics in DESCRIPTION / COMPILATION rarely — skip.
            null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun sidecarLrc(context: Context, item: ForgeMediaItem): String? {
        val candidates = mutableListOf<Uri>()
        // Same folder sibling via display name: title.lrc / file.lrc
        val display = displayName(context, item.uri) ?: item.title
        val base = display.substringBeforeLast('.').ifBlank { display }
        // MediaStore relative path — try common Music paths via content query is hard;
        // for file:// resolve sibling; for content try DocumentContract sibling by path string.
        val scheme = item.uri.scheme?.lowercase()
        if (scheme == "file") {
            val path = item.uri.path ?: return null
            val parent = java.io.File(path).parentFile ?: return null
            listOf("$base.lrc", "$base.LRC", "${item.title}.lrc").forEach { name ->
                val f = java.io.File(parent, name)
                if (f.isFile) return readTextFile(f)
            }
        }
        // Best-effort: open relative path under same MediaStore tree is unreliable without DATA column.
        // Try DATA column on older APIs.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.query(
                    item.uri,
                    arrayOf(android.provider.MediaStore.MediaColumns.DATA),
                    null,
                    null,
                    null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val data = c.getString(0) ?: return@use
                        val parent = java.io.File(data).parentFile ?: return@use
                        val f = java.io.File(parent, "$base.lrc")
                        if (f.isFile) return readTextFile(f)
                    }
                }
            }
        }
        // Strip timed tags for display if we ever load lrc via other means
        candidates.clear()
        return null
    }

    private fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun readTextFile(file: java.io.File): String? {
        return try {
            file.bufferedReader().use { stripLrc(it.readText()) }.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun stripLrc(raw: String): String {
        val lines = raw.lineSequence().map { line ->
            line.replace(Regex("""\[\d{1,2}:\d{2}([.:]\d{1,3})?]"""), "")
                .replace(Regex("""\[[a-zA-Z]+:[^\]]*\]"""), "")
                .trim()
        }.filter { it.isNotEmpty() }
        return lines.joinToString("\n").trim()
    }

    fun fromMedia3Description(description: CharSequence?): LyricsResult? {
        val text = description?.toString()?.trim().orEmpty()
        if (text.length < 8) return null
        // Heuristic: multi-line or long text in description may be lyrics
        if ('\n' in text || text.length > 80) {
            return LyricsResult(text, "metadata")
        }
        return null
    }
}
