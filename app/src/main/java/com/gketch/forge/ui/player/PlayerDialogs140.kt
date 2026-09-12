package com.gketch.forge.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gketch.forge.data.MediaBookmark
import com.gketch.forge.playback.ForgeLoudness
import com.gketch.forge.ui.library.formatDuration
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import com.gketch.forge.ui.theme.ForgeSurfaceVariant
import java.util.Locale

data class MediaInfoSnapshot(
    val title: String,
    val resolution: String,
    val durationLabel: String,
    val sizeLabel: String,
    val mime: String,
    val container: String,
    val videoTracks: List<String>,
    val audioTracks: List<String>,
    val textTracks: List<String>,
)

@Composable
fun MediaInfoDialog(
    info: MediaInfoSnapshot,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Media info", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("Title", info.title)
                InfoRow("Resolution", info.resolution)
                InfoRow("Duration", info.durationLabel)
                InfoRow("Size", info.sizeLabel)
                InfoRow("MIME", info.mime)
                InfoRow("Container", info.container)
                Spacer(Modifier.height(8.dp))
                Text("Video", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                if (info.videoTracks.isEmpty()) {
                    Text("—", color = ForgeMuted)
                } else {
                    info.videoTracks.forEach { Text(it, color = Color.White, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Audio", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                if (info.audioTracks.isEmpty()) {
                    Text("—", color = ForgeMuted)
                } else {
                    info.audioTracks.forEach { Text(it, color = Color.White, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Subtitles", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                if (info.textTracks.isEmpty()) {
                    Text("—", color = ForgeMuted)
                } else {
                    info.textTracks.forEach { Text(it, color = Color.White, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BookmarksDialog(
    bookmarks: List<MediaBookmark>,
    positionMs: Long,
    onDismiss: () -> Unit,
    onAdd: (name: String?) -> Unit,
    onRemove: (String) -> Unit,
    onJump: (Long) -> Unit,
) {
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Bookmarks", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Current · ${formatDuration(positionMs)}",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                if (!naming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { onAdd(null) },
                            label = { Text("Add at current") },
                            colors = chip140(),
                        )
                        FilterChip(
                            selected = false,
                            onClick = { naming = true; name = "" },
                            label = { Text("Named…") },
                            colors = chip140(),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text("Bookmark name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeAccent,
                            unfocusedBorderColor = ForgeSurfaceVariant,
                            focusedContainerColor = ForgeBlack,
                            unfocusedContainerColor = ForgeBlack,
                            cursorColor = ForgeAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                    Row {
                        TextButton(onClick = {
                            onAdd(name)
                            naming = false
                        }) { Text("Save", color = ForgeAccent) }
                        TextButton(onClick = { naming = false }) { Text("Cancel", color = ForgeMuted) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks for this media", color = ForgeMuted)
                } else {
                    bookmarks.forEach { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onJump(b.positionMs) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, color = Color.White)
                                Text(formatDuration(b.positionMs), color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { onRemove(b.id) }) {
                                Text("Remove", color = ForgeMuted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
fun VolumeBoostDialog(
    boostPercent: Int,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Volume boost", color = Color.White) },
        text = {
            Column {
                Text(
                    "Boost above system volume (LoudnessEnhancer). Capped at ${ForgeLoudness.MAX_BOOST_PERCENT}% / ${ForgeLoudness.MAX_GAIN_MB / 100} dB.",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text("$boostPercent%", color = ForgeAccent, style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = boostPercent.toFloat(),
                    onValueChange = { onChange(it.toInt()) },
                    valueRange = 100f..ForgeLoudness.MAX_BOOST_PERCENT.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ForgeAccent,
                        activeTrackColor = ForgeAccent,
                        inactiveTrackColor = ForgeMuted.copy(alpha = 0.3f),
                    ),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(100, 125, 150, 175, 200).forEach { p ->
                        FilterChip(
                            selected = boostPercent == p,
                            onClick = { onChange(p) },
                            label = { Text("$p%") },
                            colors = chip140(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
fun DelayDialog(
    title: String,
    delayMs: Int,
    supported: Boolean,
    note: String?,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text(title, color = Color.White) },
        text = {
            Column {
                if (!supported) {
                    Text(
                        note ?: "Not available with the current Media3 session player.",
                        color = ForgeMuted,
                    )
                } else {
                    if (note != null) {
                        Text(note, color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        formatDelay(delayMs),
                        color = ForgeAccent,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = delayMs.toFloat(),
                        onValueChange = { onChange(it.toInt()) },
                        valueRange = -5000f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = ForgeAccent,
                            activeTrackColor = ForgeAccent,
                            inactiveTrackColor = ForgeMuted.copy(alpha = 0.3f),
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-1000, -500, 0, 500, 1000).forEach { v ->
                            FilterChip(
                                selected = delayMs == v,
                                onClick = { onChange(v) },
                                label = { Text(if (v == 0) "0" else formatDelay(v)) },
                                colors = chip140(),
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = { onChange((delayMs - 100).coerceIn(-5000, 5000)) }) {
                            Text("−100 ms", color = ForgeAccent)
                        }
                        TextButton(onClick = { onChange((delayMs + 100).coerceIn(-5000, 5000)) }) {
                            Text("+100 ms", color = ForgeAccent)
                        }
                        TextButton(onClick = { onChange(0) }) {
                            Text("Reset", color = ForgeMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ForgeAccent) }
        },
    )
}

@Composable
private fun chip140() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeGraphite,
    labelColor = Color.White,
)

fun formatDelay(ms: Int): String {
    val sign = if (ms > 0) "+" else ""
    return if (kotlin.math.abs(ms) >= 1000) {
        String.format(Locale.US, "%s%.1fs", sign, ms / 1000f)
    } else {
        "$sign${ms} ms"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun guessContainer(mime: String, title: String): String {
    val t = title.lowercase()
    val m = mime.lowercase()
    return when {
        "mp4" in m || t.endsWith(".mp4") || t.endsWith(".m4v") -> "MP4"
        "webm" in m || t.endsWith(".webm") -> "WebM"
        "matroska" in m || t.endsWith(".mkv") -> "Matroska"
        "mpeg" in m || t.endsWith(".mpeg") || t.endsWith(".mpg") -> "MPEG"
        "avi" in m || t.endsWith(".avi") -> "AVI"
        "x-flv" in m || t.endsWith(".flv") -> "FLV"
        "mp2t" in m || t.endsWith(".ts") -> "MPEG-TS"
        "hls" in m || t.endsWith(".m3u8") -> "HLS"
        "dash" in m || t.endsWith(".mpd") -> "DASH"
        "ogg" in m || t.endsWith(".ogg") -> "Ogg"
        "wav" in m || t.endsWith(".wav") -> "WAV"
        "flac" in m || t.endsWith(".flac") -> "FLAC"
        "mpeg" in m && "audio" in m || t.endsWith(".mp3") -> "MP3"
        "aac" in m || t.endsWith(".aac") || t.endsWith(".m4a") -> "AAC"
        m.contains("/") -> m.substringAfter("/")
        else -> "—"
    }
}
