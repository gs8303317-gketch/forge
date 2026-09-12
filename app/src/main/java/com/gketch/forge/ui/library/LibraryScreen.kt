package com.gketch.forge.ui.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.forgeItemFromUri
import com.gketch.forge.data.isPlayableStreamUrl
import com.gketch.forge.ui.permissions.hasMediaPermission
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
    onRequestPermission: () -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showStreamDialog by remember { mutableStateOf(false) }
    val permitted = hasMediaPermission(context)

    LaunchedEffect(permitted) {
        if (permitted) viewModel.refresh()
    }

    Scaffold(
        containerColor = ForgeBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showStreamDialog = true },
                containerColor = ForgeAccent,
                contentColor = Color.Black,
            ) {
                Icon(Icons.Rounded.Link, contentDescription = "Open stream")
            }
        },
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
                    IconButton(onClick = { showStreamDialog = true }) {
                        Icon(Icons.Rounded.Link, contentDescription = "Open stream", tint = ForgeMuted)
                    }
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
                if (!permitted) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRequestPermission) {
                        Text("Grant media access to scan this device", color = ForgeAccent)
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading && permitted && state.filtered.isEmpty() && state.recent.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ForgeAccent)
                }
            }
            state.error != null && permitted && state.filtered.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            state.filtered.isEmpty() && state.recent.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No media found", color = ForgeMuted)
                        Spacer(Modifier.height(8.dp))
                        Text("Open a network stream with the link button", color = ForgeMuted)
                    }
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
                    if (state.recent.isNotEmpty() && state.query.isBlank()) {
                        item(key = "recent-header") {
                            RecentSection(
                                items = state.recent,
                                onPlay = { item -> onPlay(listOf(item), 0) },
                            )
                        }
                        item(key = "library-header") {
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.titleMedium,
                                color = ForgeMuted,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                    }
                    if (state.filtered.isEmpty() && state.recent.isNotEmpty()) {
                        item(key = "empty-library") {
                            Text(
                                text = if (permitted) "No matching library items" else "Grant access to browse the library",
                                color = ForgeMuted,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                    itemsIndexed(state.filtered, key = { _, item -> "${item.kind}-${item.id}" }) { index, item ->
                        MediaRow(item = item) {
                            onPlay(state.filtered, index)
                        }
                    }
                }
            }
        }
    }

    if (showStreamDialog) {
        StreamDialog(
            onDismiss = { showStreamDialog = false },
            onOpen = { url ->
                val uri = Uri.parse(url.trim())
                val item = forgeItemFromUri(uri)
                showStreamDialog = false
                onPlay(listOf(item), 0)
            },
        )
    }
}

@Composable
private fun RecentSection(
    items: List<ForgeMediaItem>,
    onPlay: (ForgeMediaItem) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = ForgeAccent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Recently played",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.take(12).forEach { item ->
                RecentCard(item = item, onClick = { onPlay(item) })
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RecentCard(item: ForgeMediaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ForgeGraphite)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (item.albumArtUri != null) {
                AsyncImage(
                    model = item.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
        )
    }
}

@Composable
private fun StreamDialog(
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    val valid = isPlayableStreamUrl(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Open network stream", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                Text(
                    "Paste an http(s) or rtsp URL.",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://…  or  rtsp://…") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { if (valid) onOpen(url) },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeAccent,
                        unfocusedBorderColor = ForgeSurfaceVariant,
                        cursorColor = ForgeAccent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onOpen(url) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = ForgeAccent, contentColor = Color.Black),
            ) {
                Text("Play")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ForgeMuted)
            }
        },
    )
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
