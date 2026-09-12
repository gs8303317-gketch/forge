package com.gketch.forge.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaKind
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import com.gketch.forge.ui.theme.ForgeSurfaceVariant
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun LibraryScreen(
    onPlay: (items: List<ForgeMediaItem>, index: Int) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = ForgeBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForgeBlack)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Forge",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ForgeAccent,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = ForgeMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search media…") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = ForgeMuted)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeAccent,
                        unfocusedBorderColor = ForgeSurfaceVariant,
                        focusedContainerColor = ForgeGraphite,
                        unfocusedContainerColor = ForgeGraphite,
                        cursorColor = ForgeAccent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filter == LibraryFilter.ALL,
                        onClick = { viewModel.setFilter(LibraryFilter.ALL) },
                        label = { Text("All") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.filter == LibraryFilter.VIDEO,
                        onClick = { viewModel.setFilter(LibraryFilter.VIDEO) },
                        label = { Text("Videos") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.filter == LibraryFilter.AUDIO,
                        onClick = { viewModel.setFilter(LibraryFilter.AUDIO) },
                        label = { Text("Audio") },
                        colors = filterColors(),
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ForgeAccent)
                }
            }
            state.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            state.filtered.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No media found", color = ForgeMuted)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(state.filtered, key = { _, item -> "${item.kind}-${item.id}" }) { index, item ->
                        MediaRow(item = item) {
                            onPlay(state.filtered, index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun filterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = ForgeBlack,
    containerColor = ForgeGraphite,
    labelColor = ForgeMuted,
)

@Composable
private fun MediaRow(item: ForgeMediaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ForgeGraphite)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (item.albumArtUri != null) {
                AsyncImage(
                    model = item.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = if (item.kind == MediaKind.VIDEO) Icons.Rounded.Movie else Icons.Rounded.AudioFile,
                    contentDescription = null,
                    tint = ForgeAccent,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${if (item.isVideo) "Video" else "Audio"} · ${formatDuration(item.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = ForgeMuted,
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
