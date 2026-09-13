package com.gketch.forge.ui.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.media3.ui.PlayerView
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object FrameCapture {
    data class Result(val ok: Boolean, val message: String, val uri: Uri? = null)

    suspend fun captureToGallery(context: Context, playerView: PlayerView?, titleHint: String): Result {
        val view = playerView ?: return Result(false, "No video surface")
        val bitmap = captureBitmap(view) ?: return Result(false, "Could not capture frame")
        return try {
            val name = "Forge_${sanitize(titleHint)}_${System.currentTimeMillis()}.jpg"
            val uri = saveJpeg(context, bitmap, name)
            bitmap.recycle()
            Result(true, "Saved to Pictures/Forge", uri)
        } catch (e: Exception) {
            bitmap.recycle()
            Result(false, e.message ?: "Save failed", null)
        }
    }

    private suspend fun captureBitmap(playerView: PlayerView): Bitmap? {
        val w = playerView.width
        val h = playerView.height
        if (w <= 0 || h <= 0) return null
        val surface = findSurfaceView(playerView)
        if (surface != null) {
            return suspendCancellableCoroutine { cont ->
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                try {
                    PixelCopy.request(surface, bitmap, { result ->
                        if (result == PixelCopy.SUCCESS) {
                            cont.resume(bitmap)
                        } else {
                            bitmap.recycle()
                            cont.resume(null)
                        }
                    }, Handler(Looper.getMainLooper()))
                } catch (e: Exception) {
                    bitmap.recycle()
                    cont.resume(null)
                }
            }
        }
        val texture = findTextureView(playerView)
        if (texture != null) {
            return try {
                texture.bitmap
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private fun findSurfaceView(root: View): SurfaceView? {
        if (root is SurfaceView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findSurfaceView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun findTextureView(root: View): TextureView? {
        if (root is TextureView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findTextureView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun saveJpeg(context: Context, bitmap: Bitmap, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Forge")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                throw IOException("JPEG compress failed")
            }
        } ?: throw IOException("Could not open output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    private fun sanitize(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "frame" }
}
