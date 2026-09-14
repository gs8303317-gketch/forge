package com.gketch.forge.ui.player

import com.gketch.forge.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.WindowManager
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.data.ResumeBehavior
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.player.ForgeLoudness
import com.gketch.forge.player.SeriesEpisode

internal suspend fun applyResumePolicy(
    player: Player,
    uri: String,
    behavior: ResumeBehavior,
    resumeStore: ResumeStore,
    onPrompt: (Long) -> Unit,
) {
    val saved = try {
        resumeStore.getPosition(uri)
    } catch (_: Exception) {
        0L
    }
    when (behavior) {
        ResumeBehavior.ALWAYS_START_OVER -> {
            player.seekTo(0L)
            player.play()
        }
        ResumeBehavior.ALWAYS_CONTINUE -> {
            if (saved >= ResumeStore.MIN_SAVE_MS) player.seekTo(saved)
            player.play()
        }
        ResumeBehavior.ASK -> {
            when {
                saved >= ResumeStore.RESUME_PROMPT_MS -> onPrompt(saved)
                saved >= ResumeStore.MIN_SAVE_MS -> {
                    player.seekTo(saved)
                    player.play()
                }
                else -> player.play()
            }
        }
    }
}

internal fun shareCurrentMedia(context: android.content.Context, item: ForgeMediaItem?) {
    if (item == null) {
        Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = item.uri
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType.takeIf { it.isNotBlank() && '*' !in it }
            ?: if (item.isVideo) "video/*" else "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, item.title)
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Share failed", Toast.LENGTH_SHORT).show()
    }
}

internal fun parseChapters(metadata: Metadata): List<MediaChapter> {
    val out = mutableListOf<MediaChapter>()
    for (i in 0 until metadata.length()) {
        when (val entry = metadata.get(i)) {
            is ChapterFrame -> {
                var title = entry.chapterId.ifBlank { "Chapter" }
                for (j in 0 until entry.getSubFrameCount()) {
                    val frame = entry.getSubFrame(j) as? TextInformationFrame
                    val text = frame?.values?.firstOrNull()?.takeIf { it.isNotBlank() }
                    if (text != null) {
                        title = text
                        break
                    }
                }
                out += MediaChapter(title, entry.startTimeMs.toLong().coerceAtLeast(0L))
            }
        }
    }
    return out
}

internal fun applyForcedAspect(playerView: PlayerView, aspect: AspectMode) {
    try {
        val frame = playerView.findViewById<AspectRatioFrameLayout>(
            androidx.media3.ui.R.id.exo_content_frame,
        ) ?: return
        if (frame.resizeMode != aspect.resizeMode) {
            frame.resizeMode = aspect.resizeMode
        }
        val forced = aspect.forcedRatio
        val nextRatio = when {
            forced != null && forced > 0f -> forced
            else -> {
                val vs = playerView.player?.videoSize
                if (vs != null && vs.width > 0 && vs.height > 0) {
                    vs.width * (if (vs.pixelWidthHeightRatio > 0f) vs.pixelWidthHeightRatio else 1f) / vs.height
                } else {
                    null
                }
            }
        }
        if (nextRatio != null) {
            val tagKey = 0x46A50101
            val prev = frame.getTag(tagKey) as? Float
            if (prev == null || kotlin.math.abs(prev - nextRatio) > 0.0001f) {
                frame.setAspectRatio(nextRatio)
                frame.setTag(tagKey, nextRatio)
            }
        }
    } catch (_: Throwable) {
    }
}

internal fun estimateFrameStepMs(player: Player): Long {
    return try {
        val groups = player.currentTracks.groups
        var fps = 0f
        for (g in groups) {
            if (g.type != C.TRACK_TYPE_VIDEO) continue
            for (i in 0 until g.length) {
                val rate = g.getTrackFormat(i).frameRate
                if (rate > 1f && rate < 240f) {
                    fps = rate
                    break
                }
            }
            if (fps > 0f) break
        }
        when {
            fps > 1f -> (1000f / fps).toLong().coerceIn(16L, 100L)
            else -> 33L
        }
    } catch (_: Exception) {
        33L
    }
}

internal fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}×"
    } else {
        "${speed}×"
    }
}

internal fun formatSleep(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

internal fun musicVolumeFraction(context: Context): Float {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
}

internal fun setMusicVolume(context: Context, fraction: Float) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val value = (fraction.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
}

internal fun combinedVolumeLevel(context: Context): Float {
    val boost = ForgeLoudness.boostPercent
    return if (boost > 100) {
        1f + ((boost - 100).toFloat() / (ForgeLoudness.MAX_BOOST_PERCENT - 100).toFloat())
            .coerceIn(0f, 1f)
    } else {
        musicVolumeFraction(context).coerceIn(0f, 1f)
    }
}

internal fun setCombinedVolumeLevel(context: Context, level: Float, onBoostPercent: (Int) -> Unit) {
    val v = level.coerceIn(0f, 2f)
    runCatching {
        if (v <= 1f) {
            setMusicVolume(context, v)
            ForgeLoudness.setBoostPercent(100)
            onBoostPercent(100)
        } else {
            setMusicVolume(context, 1f)
            val pct = (
                100f + (v - 1f) * (ForgeLoudness.MAX_BOOST_PERCENT - 100).toFloat()
                ).toInt().coerceIn(100, ForgeLoudness.MAX_BOOST_PERCENT)
            ForgeLoudness.setBoostPercent(pct)
            onBoostPercent(pct)
        }
    }
}

internal fun shareSnapshotUri(context: Context, uri: android.net.Uri) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_snapshot)))
    }
}

internal suspend fun maybeSeriesAutoNext(
    current: ForgeMediaItem,
    mediaRepository: MediaRepository,
    onOffer: (ForgeMediaItem) -> Unit,
) {
    if (current.bucketId == 0L) return
    val folder = mediaRepository.loadFolderItems(current.bucketId)
    val next = SeriesEpisode.findNext(current, folder) ?: return
    onOffer(next)
}

internal fun clearWindowBrightness(activity: Activity?) {
    ForgeWindowBrightness.clear(activity)
}
