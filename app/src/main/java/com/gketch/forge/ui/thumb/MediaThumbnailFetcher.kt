package com.gketch.forge.ui.thumb

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads MediaStore / SAF / file thumbnails that Coil cannot decode as plain images
 * (video content URIs, audio album art, document providers).
 */
class MediaThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val target = pixelSize(options)
        val bitmap = loadBitmap(context, data, target) ?: return@withContext null
        DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme?.lowercase() ?: return null
            if (scheme != ContentResolver.SCHEME_CONTENT &&
                scheme != ContentResolver.SCHEME_FILE
            ) {
                return null
            }
            return MediaThumbnailFetcher(options.context, data, options)
        }
    }

    companion object {
        fun pixelSize(options: Options): Size {
            fun dim(d: Dimension, fallback: Int): Int = when (d) {
                is Dimension.Pixels -> d.px.coerceIn(64, 1024)
                else -> fallback
            }
            return Size(
                dim(options.size.width, 256),
                dim(options.size.height, 256),
            )
        }

        fun loadBitmap(context: Context, uri: Uri, size: Size): Bitmap? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    return context.contentResolver.loadThumbnail(uri, size, CancellationSignal())
                } catch (_: Exception) {
                    // fall through
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                mediaStoreVideoThumb(context, uri, size)?.let { return it }
            }

            decodeStream(context, uri, size)?.let { return it }
            return retrieverBitmap(context, uri, size)
        }

        private fun mediaStoreVideoThumb(context: Context, uri: Uri, size: Size): Bitmap? {
            val id = uri.lastPathSegment?.toLongOrNull() ?: return null
            val kind = if (size.width <= 96 && size.height <= 96) {
                MediaStore.Video.Thumbnails.MICRO_KIND
            } else {
                MediaStore.Video.Thumbnails.MINI_KIND
            }
            return try {
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    kind,
                    BitmapFactory.Options(),
                )
            } catch (_: Exception) {
                try {
                    val thumbUri = ContentUris.withAppendedId(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    decodeStream(context, thumbUri, size)
                } catch (_: Exception) {
                    null
                }
            }
        }

        private fun decodeStream(context: Context, uri: Uri, size: Size): Bitmap? {
            return try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, size)
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun calculateInSampleSize(boundsWidth: Int, boundsHeight: Int, size: Size): Int {
            var inSampleSize = 1
            if (boundsHeight > size.height || boundsWidth > size.width) {
                val halfH = boundsHeight / 2
                val halfW = boundsWidth / 2
                while (halfH / inSampleSize >= size.height && halfW / inSampleSize >= size.width) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize.coerceAtLeast(1)
        }

        private fun retrieverBitmap(context: Context, uri: Uri, size: Size): Bitmap? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture?.let { bytes ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, size)
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.let { return it }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        -1,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        size.width,
                        size.height,
                    )
                } else {
                    retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
    }
}
