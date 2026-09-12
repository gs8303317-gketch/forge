package com.gketch.forge.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsOverlay(
    progress: PlayerProgressState,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val res = when {
        progress.statsWidth > 0 && progress.statsHeight > 0 ->
            "${progress.statsWidth}×${progress.statsHeight}"
        else -> "—"
    }
    val fps = if (progress.statsFps > 0.5f) {
        String.format(Locale.US, "%.1f fps", progress.statsFps)
    } else {
        "fps —"
    }
    val br = if (progress.statsBitrateKbps > 0) {
        "${progress.statsBitrateKbps} kbps"
    } else {
        "bitrate —"
    }
    val buf = "buf ${progress.statsBufferedPct}%"
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text("STATS", color = ForgeAccent, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "$res  ·  $fps",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "$br  ·  $buf",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "${progress.statsStateLabel}  ·  ${formatPowerSpeed(speed)}",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun SpeedFinePanel(
    selected: Float,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Speed · ${formatPowerSpeed(selected)}", color = Color.White)
            TextButton(onClick = onDone) {
                Text("Done", color = ForgeAccent)
            }
        }
        Slider(
            value = selected.coerceIn(0.25f, 3f),
            onValueChange = { raw ->
                val stepped = ((raw * 20f).roundToInt() / 20f).coerceIn(0.25f, 3f)
                onChange(stepped)
            },
            valueRange = 0.25f..3f,
            steps = ((3f - 0.25f) / 0.05f).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = ForgeAccent,
                activeTrackColor = ForgeAccent,
                inactiveTrackColor = ForgeMuted.copy(alpha = 0.3f),
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onChange((selected - 0.05f).coerceIn(0.25f, 3f)) }) {
                Text("−0.05", color = ForgeAccent)
            }
            TextButton(onClick = { onChange((selected + 0.05f).coerceIn(0.25f, 3f)) }) {
                Text("+0.05", color = ForgeAccent)
            }
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f).forEach { preset ->
                FilterChip(
                    selected = kotlin.math.abs(preset - selected) < 0.001f,
                    onClick = { onChange(preset) },
                    label = { Text(formatPowerSpeed(preset)) },
                    colors = powerChipColors(),
                )
            }
        }
    }
}

@Composable
fun TransformPanel(
    mirrorH: Boolean,
    mirrorV: Boolean,
    rotationDeg: Int,
    zoomLabel: String? = null,
    onMirrorH: () -> Unit,
    onMirrorV: () -> Unit,
    onRotate: (Int) -> Unit,
    onResetZoom: () -> Unit = {},
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildString {
                    append("Transform · ${rotationDeg}°")
                    if (zoomLabel != null) append(" · $zoomLabel")
                },
                color = Color.White,
            )
            TextButton(onClick = onDone) { Text("Done", color = ForgeAccent) }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = mirrorH,
                onClick = onMirrorH,
                label = { Text("Mirror H") },
                colors = powerChipColors(),
            )
            FilterChip(
                selected = mirrorV,
                onClick = onMirrorV,
                label = { Text("Mirror V") },
                colors = powerChipColors(),
            )
            FilterChip(
                selected = false,
                onClick = { onRotate(-90) },
                label = { Text("⟲ 90°") },
                colors = powerChipColors(),
            )
            FilterChip(
                selected = false,
                onClick = { onRotate(90) },
                label = { Text("⟳ 90°") },
                colors = powerChipColors(),
            )
            if (zoomLabel != null) {
                FilterChip(
                    selected = true,
                    onClick = onResetZoom,
                    label = { Text("Reset zoom") },
                    colors = powerChipColors(),
                )
            }
            FilterChip(
                selected = false,
                onClick = onReset,
                label = { Text("Reset") },
                colors = powerChipColors(),
            )
        }
    }
}

@Composable
fun QuickDelayBar(
    title: String,
    delayMs: Int,
    onAdjust: (Int) -> Unit,
    onOpenFull: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$title ${formatPowerDelay(delayMs)}", color = Color.White, modifier = Modifier.padding(end = 4.dp))
        listOf(-500, -100, 100, 500).forEach { delta ->
            FilterChip(
                selected = false,
                onClick = { onAdjust(delta) },
                label = { Text(if (delta > 0) "+${delta}" else "$delta") },
                colors = powerChipColors(),
            )
        }
        FilterChip(
            selected = delayMs == 0,
            onClick = { onAdjust(-delayMs) },
            label = { Text("0") },
            colors = powerChipColors(),
        )
        TextButton(onClick = onOpenFull) { Text("More", color = ForgeAccent) }
        TextButton(onClick = onDone) { Text("Done", color = ForgeMuted) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    items: List<ForgeMediaItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForgeGraphite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QueueMusic, null, tint = ForgeAccent)
                Spacer(Modifier.size(8.dp))
                Text("Queue · ${items.size}", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (items.isEmpty()) {
                    Text("Queue is empty", color = ForgeMuted)
                } else {
                    items.forEachIndexed { i, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPlayIndex(i) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = if (i == currentIndex) ForgeAccent else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatPowerDuration(item.durationMs),
                                    color = ForgeMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            IconButton(onClick = { onMove(i, i - 1) }, enabled = i > 0) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = if (i > 0) Color.White else ForgeMuted,
                                )
                            }
                            IconButton(onClick = { onMove(i, i + 1) }, enabled = i < items.lastIndex) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = if (i < items.lastIndex) Color.White else ForgeMuted,
                                )
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Done", color = ForgeAccent)
            }
        }
    }
}

@Composable
fun BufferedProgressTrack(
    progress: Float,
    buffered: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(3.dp).fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(ForgeMuted.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
        )
        Box(
            Modifier
                .fillMaxWidth(buffered.coerceIn(0f, 1f))
                .height(3.dp)
                .background(ForgeAccent.copy(alpha = 0.35f), RoundedCornerShape(2.dp)),
        )
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(ForgeAccent, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun powerChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = Color.Black,
    containerColor = ForgeGraphite,
    labelColor = Color.White,
)

fun formatPowerSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}×"
    } else {
        val s = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
        "${s}×"
    }
}

fun formatPowerDelay(ms: Int): String {
    val sign = if (ms > 0) "+" else ""
    return if (kotlin.math.abs(ms) >= 1000) {
        String.format(Locale.US, "%s%.1fs", sign, ms / 1000f)
    } else {
        "$sign${ms} ms"
    }
}

fun formatPowerDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
