package com.gketch.forge.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VIDEO_EXTS = setOf(
    "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "m2ts", "wmv", "flv", "mpg", "mpeg",
)
private val AUDIO_EXTS = setOf(
    "mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma", "aiff", "alac",
)

class SafMediaScanner(private val context: Context) {

    suspend fun scan(folders: List<SafFolder>): List<ForgeMediaItem> = withContext(Dispatchers.IO) {
        folders.flatMap { scanFolder(it) }
    }

    suspend fun scanFolder(folder: SafFolder): List<ForgeMediaItem> = withContext(Dispatchers.IO) {
        val tree = folder.treeUri()
        val children = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree),
            )
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        val bucketId = folder.uri.hashCode().toLong()
        walk(children, tree, folder.name, bucketId, pathPrefix = "saf/${folder.name}/", depth = 0)
    }

    private fun walk(
        childrenUri: Uri,
        tree: Uri,
        bucketName: String,
        bucketId: Long,
        pathPrefix: String,
        depth: Int,
    ): List<ForgeMediaItem> {
        if (depth > 8) return emptyList()
        val resolver = context.contentResolver
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val items = mutableListOf<ForgeMediaItem>()
        val cursor = try {
            resolver.query(childrenUri, projection, null, null, null)
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        cursor.use { c ->
            val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val dateCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            if (idCol < 0 || nameCol < 0) return emptyList()
            while (c.moveToNext()) {
                val docId = c.getString(idCol) ?: continue
                val name = c.getString(nameCol) ?: continue
                val mime = if (mimeCol >= 0) c.getString(mimeCol).orEmpty() else ""
                val size = if (sizeCol >= 0) c.getLong(sizeCol).coerceAtLeast(0L) else 0L
                val modified = if (dateCol >= 0) c.getLong(dateCol) else 0L
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR || mime == "vnd.android.document/directory") {
                    val child = try {
                        DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
                    } catch (_: Exception) {
                        null
                    }
                    if (child != null) {
                        items += walk(
                            child,
                            tree,
                            bucketName,
                            bucketId,
                            pathPrefix = "$pathPrefix$name/",
                            depth = depth + 1,
                        )
                    }
                    continue
                }
                val kind = kindFor(mime, name) ?: continue
                val uri = try {
                    DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                } catch (_: Exception) {
                    continue
                }
                items += ForgeMediaItem(
                    id = uri.toString().hashCode().toLong(),
                    uri = uri,
                    title = name,
                    durationMs = 0L,
                    sizeBytes = size,
                    mimeType = mime.ifBlank { if (kind == MediaKind.AUDIO) "audio/*" else "video/*" },
                    kind = kind,
                    dateAdded = if (modified > 0) modified / 1000 else System.currentTimeMillis() / 1000,
                    albumArtUri = if (kind == MediaKind.VIDEO) uri else null,
                    bucketId = bucketId,
                    bucketName = bucketName,
                    relativePath = pathPrefix,
                )
            }
        }
        return items
    }

    companion object {
        fun kindFor(mime: String, name: String): MediaKind? {
            val m = mime.lowercase()
            if (m.startsWith("audio/")) return MediaKind.AUDIO
            if (m.startsWith("video/")) return MediaKind.VIDEO
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext in AUDIO_EXTS) return MediaKind.AUDIO
            if (ext in VIDEO_EXTS) return MediaKind.VIDEO
            return null
        }
    }
}
