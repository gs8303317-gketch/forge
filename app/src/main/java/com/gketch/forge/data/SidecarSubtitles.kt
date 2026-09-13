package com.gketch.forge.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

/**
 * Best-effort same-basename sidecar subtitles (.srt / .vtt) beside a local video.
 * Never throws — MediaStore / SAF / file path heuristics only.
 */
object SidecarSubtitles {
    private val EXTS = listOf("srt", "vtt", "SRT", "VTT")

    fun find(context: Context, item: ForgeMediaItem): Uri? {
        if (!item.isVideo) return null
        return try {
            findInternal(context, item)
        } catch (_: Throwable) {
            null
        }
    }

    fun find(context: Context, uri: Uri, titleHint: String = ""): Uri? {
        return try {
            findInternal(
                context,
                forgeItemFromUri(uri, title = titleHint.takeIf { it.isNotBlank() }, mime = "video/*"),
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun findInternal(context: Context, item: ForgeMediaItem): Uri? {
        val display = displayName(context, item.uri) ?: item.title
        val base = basename(display)
        if (base.isBlank()) return null

        // 1) file:// path sibling
        if (item.uri.scheme.equals("file", ignoreCase = true)) {
            val path = item.uri.path ?: return null
            val parent = File(path).parentFile ?: return null
            for (ext in EXTS) {
                val f = File(parent, "$base.$ext")
                if (f.isFile) return Uri.fromFile(f)
            }
        }

        // 2) MediaStore DATA column (best-effort; often null on Q+)
        runCatching {
            context.contentResolver.query(
                item.uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val data = c.getString(0)
                    if (!data.isNullOrBlank()) {
                        val parent = File(data).parentFile
                        if (parent != null) {
                            for (ext in EXTS) {
                                val f = File(parent, "$base.$ext")
                                if (f.isFile) return@findInternal Uri.fromFile(f)
                            }
                        }
                    }
                }
            }
        }

        // 3) SAF document tree sibling by display name
        findSafSibling(context, item.uri, base)?.let { return it }

        // 4) MediaStore Files query: same RELATIVE_PATH + basename.ext
        findMediaStoreSibling(context, item, base)?.let { return it }

        return null
    }

    fun basename(displayName: String): String {
        val name = displayName.substringAfterLast('/').trim()
        if (name.isBlank()) return ""
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    fun candidateNames(base: String): List<String> =
        EXTS.map { "$base.$it" }

    private fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri.lastPathSegment?.let { Uri.decode(it) }
        }
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { Uri.decode(it) }
        }
    }

    private fun findSafSibling(context: Context, uri: Uri, base: String): Uri? {
        if (!uri.scheme.equals("content", ignoreCase = true)) return null
        val raw = uri.toString()
        if (!raw.contains("/tree/")) return null
        val authority = uri.authority ?: return null
        val treeEncoded = raw.substringAfter("/tree/").substringBefore('/').takeIf { it.isNotBlank() }
            ?: return null
        val treeUri = Uri.parse("content://$authority/tree/$treeEncoded")
        val docId = try {
            DocumentsContract.getDocumentId(uri)
        } catch (_: Exception) {
            return null
        }
        val parentId = run {
            val slash = docId.lastIndexOf('/')
            if (slash > 0) {
                docId.substring(0, slash)
            } else {
                try {
                    DocumentsContract.getTreeDocumentId(treeUri)
                } catch (_: Exception) {
                    return null
                }
            }
        }
        val children = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        } catch (_: Exception) {
            return null
        }
        val wanted = candidateNames(base).map { it.lowercase() }.toSet()
        return try {
            context.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { c ->
                val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idCol < 0 || nameCol < 0) return@use null
                while (c.moveToNext()) {
                    val name = c.getString(nameCol) ?: continue
                    if (name.lowercase() in wanted) {
                        val id = c.getString(idCol) ?: continue
                        return@use DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findMediaStoreSibling(context: Context, item: ForgeMediaItem, base: String): Uri? {
        val relative = item.relativePath.trim()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        for (name in candidateNames(base)) {
            val selection: String
            val args: Array<String>
            if (relative.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selection =
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
                args = arrayOf(name, relative)
            } else {
                selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
                args = arrayOf(name)
            }
            try {
                context.contentResolver.query(
                    collection,
                    arrayOf(MediaStore.Files.FileColumns._ID),
                    selection,
                    args,
                    null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getLong(0)
                        return ContentUris.withAppendedId(collection, id)
                    }
                }
            } catch (_: Exception) {
                // try next name
            }
        }
        return null
    }
}
