package com.gketch.forge.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.gketch.forge.data.ForgePlaylist
import com.gketch.forge.data.LibrarySort
import com.gketch.forge.data.MediaFolder
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.SavedStream
import com.gketch.forge.data.forgeItemFromUri
import com.gketch.forge.data.isPlayableStreamUrl
import com.gketch.forge.ui.permissions.hasMediaPermission
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import com.gketch.forge.ui.theme.ForgeSurfaceVariant
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun LibraryScreen(
    onPlay: (items: List<ForgeMediaItem>, index: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onExpandPlayer: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showStreamDialog by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var renamePlaylist by remember { mutableStateOf<ForgePlaylist?>(null) }
    var renameStream by remember { mutableStateOf<SavedStream?>(null) }
    var addToPlaylistItem by remember { mutableStateOf<ForgeMediaItem?>(null) }
    var addSelectedToPlaylist by remember { mutableStateOf(false) }
    var hideFolder by remember { mutableStateOf<MediaFolder?>(null) }
    var exportPlaylist by remember { mutableStateOf<ForgePlaylist?>(null) }
    var m3uMessage by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permitted = hasMediaPermission(context)

    val importM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            viewModel.importM3u(uri) { msg -> m3uMessage = msg }
        }
    }
    var snackScopeMsg by remember { mutableStateOf<String?>(null) }
    val safTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            viewModel.addSafFolder(uri, null)
            snackScopeMsg = "Folder added"
        }
    }
    val exportM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri ->
        val pl = exportPlaylist
        exportPlaylist = null
        if (uri != null && pl != null) {
            viewModel.exportM3u(pl.id, uri) { msg -> m3uMessage = msg }
        }
    }

    LaunchedEffect(permitted) {
        if (permitted) viewModel.refresh()
    }

    LaunchedEffect(m3uMessage) {
        val msg = m3uMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        m3uMessage = null
    }
    LaunchedEffect(snackScopeMsg) {
        val msg = snackScopeMsg ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        snackScopeMsg = null
    }
    BackHandler(enabled = state.selecting) {
        viewModel.clearSelection()
    }

    Scaffold(
        containerColor = ForgeBlack,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            when (state.tab) {
                LibraryTab.PLAYLISTS -> FloatingActionButton(
                    onClick = { showCreatePlaylist = true },
                    containerColor = ForgeAccent,
                    contentColor = Color.Black,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "New playlist")
                }
                LibraryTab.STREAMS -> FloatingActionButton(
                    onClick = { showStreamDialog = true },
                    containerColor = ForgeAccent,
                    contentColor = Color.Black,
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = "Add stream")
                }
                else -> FloatingActionButton(
                    onClick = { showStreamDialog = true },
                    containerColor = ForgeAccent,
                    contentColor = Color.Black,
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = "Open stream")
                }
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
                    if (state.selecting) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear selection", tint = Color.White)
                        }
                        Text(
                            text = "${state.selectedKeys.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { viewModel.favoriteSelected() },
                            enabled = state.selectedKeys.isNotEmpty(),
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = "Add to favorites", tint = ForgeAccent)
                        }
                        IconButton(
                            onClick = { addSelectedToPlaylist = true },
                            enabled = state.selectedKeys.isNotEmpty(),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to playlist", tint = ForgeAccent)
                        }
                    } else {
                    Text(
                        text = "Forge",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ForgeAccent,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.tab == LibraryTab.LIBRARY) {
                        IconButton(onClick = {
                            viewModel.setLayout(
                                if (state.layout == LibraryLayout.GRID) LibraryLayout.LIST
                                else LibraryLayout.GRID,
                            )
                        }) {
                            Icon(
                                if (state.layout == LibraryLayout.GRID) Icons.Rounded.ViewList
                                else Icons.Rounded.GridView,
                                contentDescription = "Toggle layout",
                                tint = ForgeMuted,
                            )
                        }
                    }
                    if (state.tab == LibraryTab.PLAYLISTS) {
                        IconButton(onClick = {
                            importM3uLauncher.launch(
                                arrayOf(
                                    "audio/x-mpegurl",
                                    "application/vnd.apple.mpegurl",
                                    "text/plain",
                                    "*/*",
                                ),
                            )
                        }) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = "Import M3U", tint = ForgeMuted)
                        }
                    }
                    IconButton(onClick = { showStreamDialog = true }) {
                        Icon(Icons.Rounded.Link, contentDescription = "Open stream", tint = ForgeMuted)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = ForgeMuted)
                    }
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
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.tab == LibraryTab.LIBRARY && state.filter == LibraryFilter.ALL,
                        onClick = {
                            viewModel.setTab(LibraryTab.LIBRARY)
                            viewModel.setFilter(LibraryFilter.ALL)
                        },
                        label = { Text("All") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.tab == LibraryTab.LIBRARY && state.filter == LibraryFilter.VIDEO,
                        onClick = {
                            viewModel.setTab(LibraryTab.LIBRARY)
                            viewModel.setFilter(LibraryFilter.VIDEO)
                        },
                        label = { Text("Videos") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.tab == LibraryTab.LIBRARY && state.filter == LibraryFilter.AUDIO,
                        onClick = {
                            viewModel.setTab(LibraryTab.LIBRARY)
                            viewModel.setFilter(LibraryFilter.AUDIO)
                        },
                        label = { Text("Audio") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.tab == LibraryTab.FOLDERS,
                        onClick = { viewModel.setTab(LibraryTab.FOLDERS) },
                        label = { Text("Folders") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.tab == LibraryTab.PLAYLISTS,
                        onClick = { viewModel.setTab(LibraryTab.PLAYLISTS) },
                        label = { Text("Playlists") },
                        colors = filterColors(),
                    )
                    FilterChip(
                        selected = state.tab == LibraryTab.STREAMS,
                        onClick = { viewModel.setTab(LibraryTab.STREAMS) },
                        label = { Text("Streams") },
                        colors = filterColors(),
                    )
                }
                if (state.tab == LibraryTab.LIBRARY) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Sort, null, tint = ForgeMuted, modifier = Modifier.size(18.dp))
                        LibrarySort.entries.forEach { sort ->
                            FilterChip(
                                selected = state.sort == sort,
                                onClick = { viewModel.setSort(sort) },
                                label = { Text(sort.label) },
                                colors = filterColors(),
                            )
                        }
                        if (state.recent.isNotEmpty()) {
                            TextButton(onClick = { showClearHistory = true }) {
                                Text("Clear history", color = ForgeAccent)
                            }
                        }
                    }
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
        when (state.tab) {
            LibraryTab.LIBRARY -> LibraryBody(
                state = state,
                permitted = permitted,
                padding = padding,
                onPlay = onPlay,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { addToPlaylistItem = it },
                onToggleSelect = viewModel::toggleSelected,
                onBeginSelect = viewModel::beginSelection,
            )
            LibraryTab.FOLDERS -> FoldersBody(
                state = state,
                padding = padding,
                onOpenFolder = viewModel::openFolder,
                onCloseFolder = viewModel::closeFolder,
                onPlay = onPlay,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { addToPlaylistItem = it },
                onHideFolder = { hideFolder = it },
                onAddSafFolder = { safTreeLauncher.launch(null) },
                onToggleSelect = viewModel::toggleSelected,
                onBeginSelect = viewModel::beginSelection,
            )
            LibraryTab.PLAYLISTS -> PlaylistsBody(
                state = state,
                padding = padding,
                onOpen = viewModel::openPlaylist,
                onClose = viewModel::closePlaylist,
                onPlay = onPlay,
                onRename = { renamePlaylist = it },
                onDelete = viewModel::deletePlaylist,
                onRemoveItem = viewModel::removeFromPlaylist,
                onMoveItem = viewModel::movePlaylistItem,
                onToggleFavorite = viewModel::toggleFavorite,
                onImportM3u = {
                    importM3uLauncher.launch(
                        arrayOf(
                            "audio/x-mpegurl",
                            "application/vnd.apple.mpegurl",
                            "text/plain",
                            "*/*",
                        ),
                    )
                },
                onExportM3u = { pl ->
                    exportPlaylist = pl
                    val safe = pl.name.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "playlist" }
                    exportM3uLauncher.launch("$safe.m3u")
                },
            )
            LibraryTab.STREAMS -> StreamsBody(
                state = state,
                padding = padding,
                onPlay = onPlay,
                onRename = { renameStream = it },
                onDelete = viewModel::removeStream,
            )
        }
    }

    if (showStreamDialog) {
        StreamDialog(
            onDismiss = { showStreamDialog = false },
            onOpen = { url, name, save ->
                val trimmed = url.trim()
                val uri = Uri.parse(trimmed)
                val item = forgeItemFromUri(uri, title = name?.takeIf { it.isNotBlank() })
                if (save) viewModel.saveStream(trimmed, name)
                showStreamDialog = false
                onPlay(listOf(item), 0)
            },
            onSaveOnly = { url, name ->
                viewModel.saveStream(url, name)
                showStreamDialog = false
                viewModel.setTab(LibraryTab.STREAMS)
            },
        )
    }
    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            containerColor = ForgeGraphite,
            title = { Text("Clear history", color = Color.White) },
            text = {
                Text(
                    "Remove recently played items. Optionally also clear saved resume positions.",
                    color = ForgeMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory(alsoResume = false)
                        showClearHistory = false
                    },
                ) { Text("Clear recent", color = ForgeAccent) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.clearHistory(alsoResume = true)
                            showClearHistory = false
                        },
                    ) { Text("Recent + resume", color = Color.White) }
                    TextButton(onClick = { showClearHistory = false }) {
                        Text("Cancel", color = ForgeMuted)
                    }
                }
            },
        )
    }

    if (showCreatePlaylist) {

        NameDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onDismiss = { showCreatePlaylist = false },
            onConfirm = {
                viewModel.createPlaylist(it)
                showCreatePlaylist = false
            },
        )
    }
    renamePlaylist?.let { pl ->
        NameDialog(
            title = "Rename playlist",
            initial = pl.name,
            confirmLabel = "Save",
            onDismiss = { renamePlaylist = null },
            onConfirm = {
                viewModel.renamePlaylist(pl.id, it)
                renamePlaylist = null
            },
        )
    }
    renameStream?.let { stream ->
        NameDialog(
            title = "Rename stream",
            initial = stream.name,
            confirmLabel = "Save",
            onDismiss = { renameStream = null },
            onConfirm = {
                viewModel.renameStream(stream.id, it)
                renameStream = null
            },
        )
    }
    addToPlaylistItem?.let { item ->
        AddToPlaylistDialog(
            playlists = state.playlists,
            onDismiss = { addToPlaylistItem = null },
            onPick = { id ->
                viewModel.addToPlaylist(id, item)
                addToPlaylistItem = null
            },
            onCreate = {
                showCreatePlaylist = true
                addToPlaylistItem = null
            },
        )
    }
    if (addSelectedToPlaylist) {
        AddToPlaylistDialog(
            playlists = state.playlists,
            onDismiss = { addSelectedToPlaylist = false },
            onPick = { id ->
                viewModel.addSelectedToPlaylist(id)
                addSelectedToPlaylist = false
            },
            onCreate = {
                showCreatePlaylist = true
                addSelectedToPlaylist = false
            },
        )
    }
    hideFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { hideFolder = null },
            containerColor = ForgeGraphite,
            title = { Text("Hide folder", color = Color.White) },
            text = {
                Text(
                    "Hide “${folder.name}” from the library? You can unhide it in Settings.",
                    color = ForgeMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hideFolder(folder)
                        hideFolder = null
                    },
                ) { Text("Hide", color = ForgeAccent) }
            },
            dismissButton = {
                TextButton(onClick = { hideFolder = null }) { Text("Cancel", color = ForgeMuted) }
            },
        )
    }
}

@Composable
private fun LibraryBody(
    state: LibraryUiState,
    permitted: Boolean,
    padding: PaddingValues,
    onPlay: (List<ForgeMediaItem>, Int) -> Unit,
    onToggleFavorite: (ForgeMediaItem) -> Unit,
    onAddToPlaylist: (ForgeMediaItem) -> Unit,
    onToggleSelect: (ForgeMediaItem) -> Unit,
    onBeginSelect: (ForgeMediaItem) -> Unit,
) {
    when {
        state.loading && permitted && state.filtered.isEmpty() && state.recent.isEmpty() && state.favorites.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForgeAccent)
            }
        }
        state.error != null && permitted && state.filtered.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }
        state.filtered.isEmpty() && state.recent.isEmpty() && state.favorites.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No media found", color = ForgeMuted)
                    Spacer(Modifier.height(8.dp))
                    Text("Open or save a network stream with the link button", color = ForgeMuted)
                }
            }
        }
        state.layout == LibraryLayout.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.favorites.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "fav-header") {
                        FavoritesSection(items = state.favorites, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                if (state.savedStreams.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "streams-header") {
                        SavedStreamsSection(
                            streams = state.savedStreams,
                            onPlay = { onPlay(listOf(it.toMediaItem()), 0) },
                        )
                    }
                }
                if (state.recent.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "recent-header") {
                        RecentSection(items = state.recent, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "lib-header") {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForgeMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                itemsIndexed(state.filtered, key = { _, item -> item.stableKey() }) { index, item ->
                    MediaGridCard(
                        item = item,
                        favorite = state.favoriteUris.contains(item.uri.toString()),
                        selected = item.stableKey() in state.selectedKeys,
                        selecting = state.selecting,
                        onClick = {
                            if (state.selecting) onToggleSelect(item)
                            else onPlay(state.filtered, index)
                        },
                        onLongClick = { onBeginSelect(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.favorites.isNotEmpty() && state.query.isBlank()) {
                    item(key = "fav-header") {
                        FavoritesSection(items = state.favorites, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                if (state.savedStreams.isNotEmpty() && state.query.isBlank()) {
                    item(key = "streams-header") {
                        SavedStreamsSection(
                            streams = state.savedStreams,
                            onPlay = { onPlay(listOf(it.toMediaItem()), 0) },
                        )
                    }
                }
                if (state.recent.isNotEmpty() && state.query.isBlank()) {
                    item(key = "recent-header") {
                        RecentSection(items = state.recent, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                item(key = "lib-header") {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForgeMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                itemsIndexed(state.filtered, key = { _, item -> item.stableKey() }) { index, item ->
                    MediaRow(
                        item = item,
                        favorite = state.favoriteUris.contains(item.uri.toString()),
                        selected = item.stableKey() in state.selectedKeys,
                        selecting = state.selecting,
                        onClick = {
                            if (state.selecting) onToggleSelect(item)
                            else onPlay(state.filtered, index)
                        },
                        onLongClick = { onBeginSelect(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersBody(
    state: LibraryUiState,
    padding: PaddingValues,
    onOpenFolder: (MediaFolder) -> Unit,
    onCloseFolder: () -> Unit,
    onPlay: (List<ForgeMediaItem>, Int) -> Unit,
    onToggleFavorite: (ForgeMediaItem) -> Unit,
    onAddToPlaylist: (ForgeMediaItem) -> Unit,
    onHideFolder: (MediaFolder) -> Unit,
    onAddSafFolder: () -> Unit,
    onToggleSelect: (ForgeMediaItem) -> Unit,
    onBeginSelect: (ForgeMediaItem) -> Unit,
) {
    val folder = state.selectedFolder
    if (folder == null) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = onAddSafFolder) {
                    Icon(Icons.Rounded.CreateNewFolder, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add folder", color = ForgeAccent)
                }
            }
            if (state.folders.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("No folders found. Add a folder from storage.", color = ForgeMuted, modifier = Modifier.padding(24.dp))
                }
            }
            items(
                count = state.folders.size,
                key = { state.folders[it].bucketId },
            ) { i ->
                val f = state.folders[i]
                FolderCard(
                    folder = f,
                    onClick = { onOpenFolder(f) },
                    onLongClick = { onHideFolder(f) },
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onCloseFolder) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(folder.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("${folder.itemCount} items", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(
                    onClick = { if (state.folderItems.isNotEmpty()) onPlay(state.folderItems, 0) },
                    enabled = state.folderItems.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play folder", tint = ForgeAccent)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.folderItems, key = { _, item -> item.stableKey() }) { index, item ->
                    MediaGridCard(
                        item = item,
                        favorite = state.favoriteUris.contains(item.uri.toString()),
                        selected = item.stableKey() in state.selectedKeys,
                        selecting = state.selecting,
                        onClick = {
                            if (state.selecting) onToggleSelect(item)
                            else onPlay(state.folderItems, index)
                        },
                        onLongClick = { onBeginSelect(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsBody(
    state: LibraryUiState,
    padding: PaddingValues,
    onOpen: (ForgePlaylist) -> Unit,
    onClose: () -> Unit,
    onPlay: (List<ForgeMediaItem>, Int) -> Unit,
    onRename: (ForgePlaylist) -> Unit,
    onDelete: (String) -> Unit,
    onRemoveItem: (String, Uri) -> Unit,
    onMoveItem: (String, Int, Int) -> Unit,
    onToggleFavorite: (ForgeMediaItem) -> Unit,
    onImportM3u: () -> Unit,
    onExportM3u: (ForgePlaylist) -> Unit,
) {
    val selected = state.selectedPlaylist
    if (selected == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onImportM3u) {
                        Icon(Icons.Rounded.FileUpload, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import M3U", color = ForgeAccent)
                    }
                }
            }
            if (state.playlists.isEmpty()) {
                item {
                    Text(
                        "No playlists yet. Tap + to create one, or import an M3U file.",
                        color = ForgeMuted,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            items(state.playlists, key = { it.id }) { pl ->
                PlaylistRow(
                    playlist = pl,
                    onOpen = { onOpen(pl) },
                    onPlay = { if (pl.items.isNotEmpty()) onPlay(pl.items, 0) },
                    onRename = { onRename(pl) },
                    onDelete = { onDelete(pl.id) },
                    onExport = { onExportM3u(pl) },
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(selected.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("${selected.items.size} items", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(
                    onClick = { onExportM3u(selected) },
                    enabled = selected.items.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "Export M3U", tint = ForgeMuted)
                }
                IconButton(
                    onClick = { if (selected.items.isNotEmpty()) onPlay(selected.items, 0) },
                    enabled = selected.items.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play playlist", tint = ForgeAccent)
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (selected.items.isEmpty()) {
                    item { Text("Playlist is empty. Star or add items from the library.", color = ForgeMuted) }
                }
                itemsIndexed(selected.items, key = { _, item -> item.stableKey() }) { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            MediaRow(
                                item = item,
                                favorite = state.favoriteUris.contains(item.uri.toString()),
                                onClick = { onPlay(selected.items, index) },
                                onToggleFavorite = { onToggleFavorite(item) },
                                onAddToPlaylist = {},
                                onRemove = { onRemoveItem(selected.id, item.uri) },
                            )
                        }
                        IconButton(
                            onClick = { onMoveItem(selected.id, index, index - 1) },
                            enabled = index > 0,
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Move up",
                                tint = if (index > 0) Color.White else ForgeMuted,
                            )
                        }
                        IconButton(
                            onClick = { onMoveItem(selected.id, index, index + 1) },
                            enabled = index < selected.items.lastIndex,
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Move down",
                                tint = if (index < selected.items.lastIndex) Color.White else ForgeMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    items: List<ForgeMediaItem>,
    onPlay: (ForgeMediaItem) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Favorites", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.take(20).forEach { item ->
                RecentCard(item = item, onClick = { onPlay(item) })
            }
        }
    }
}

@Composable
private fun RecentSection(
    items: List<ForgeMediaItem>,
    onPlay: (ForgeMediaItem) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Recently played", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
        ThumbBox(item = item, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridCard(
    item: ForgeMediaItem,
    favorite: Boolean,
    selected: Boolean = false,
    selecting: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ForgeSurfaceVariant else ForgeGraphite)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(8.dp),
    ) {
        Box {
            ThumbBox(item = item, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = ForgeAccent,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(22.dp),
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
            ) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (favorite) ForgeAccent else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = ForgeGraphite) {
                DropdownMenuItem(
                    text = { Text(if (favorite) "Remove favorite" else "Add favorite", color = Color.White) },
                    onClick = { menu = false; onToggleFavorite() },
                )
                DropdownMenuItem(
                    text = { Text("Add to playlist", color = Color.White) },
                    onClick = { menu = false; onAddToPlaylist() },
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
        Text(
            text = formatDuration(item.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = ForgeMuted,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaRow(
    item: ForgeMediaItem,
    favorite: Boolean,
    selected: Boolean = false,
    selecting: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ForgeSurfaceVariant else ForgeGraphite)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    if (!selecting) menu = true
                },
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbBox(item = item, modifier = Modifier.size(64.dp))
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
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "Favorite",
                tint = if (favorite) ForgeAccent else ForgeMuted,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = ForgeGraphite) {
            DropdownMenuItem(
                text = { Text("Add to playlist", color = Color.White) },
                onClick = { menu = false; onAddToPlaylist() },
            )
            if (onRemove != null) {
                DropdownMenuItem(
                    text = { Text("Remove from playlist", color = Color.White) },
                    onClick = { menu = false; onRemove() },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(folder: MediaFolder, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ForgeGraphite)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (folder.thumbUri != null) {
                AsyncImage(
                    model = folder.thumbUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Rounded.Folder, null, tint = ForgeAccent, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(folder.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${folder.itemCount} items", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PlaylistRow(
    playlist: ForgePlaylist,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ForgeGraphite)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.QueueMusic, null, tint = ForgeAccent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("${playlist.items.size} items", color = ForgeMuted, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onPlay, enabled = playlist.items.isNotEmpty()) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = ForgeAccent)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = ForgeMuted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = ForgeGraphite) {
                DropdownMenuItem(text = { Text("Rename", color = Color.White) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(
                    text = { Text("Export M3U", color = Color.White) },
                    onClick = { menu = false; onExport() },
                    leadingIcon = { Icon(Icons.Rounded.FileDownload, null, tint = ForgeAccent) },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.White) },
                    onClick = { menu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = ForgeAccent) },
                )
            }
        }
    }
}

@Composable
private fun ThumbBox(item: ForgeMediaItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ForgeSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val model = item.albumArtUri ?: item.uri.takeIf { item.isVideo }
        if (model != null) {
            AsyncImage(
                model = model,
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
}

@Composable
private fun StreamDialog(
    onDismiss: () -> Unit,
    onOpen: (url: String, name: String?, save: Boolean) -> Unit,
    onSaveOnly: (url: String, name: String?) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var saveToo by remember { mutableStateOf(true) }
    val valid = isPlayableStreamUrl(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Network stream", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                Text("Paste an http(s) or rtsp URL. Optionally save it for quick access.", color = ForgeMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://…  or  rtsp://…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Name (optional)") },
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { saveToo = !saveToo }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Save to Streams", color = Color.White)
                    Text(if (saveToo) "Yes" else "No", color = ForgeAccent)
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { onSaveOnly(url, name.ifBlank { null }) },
                    enabled = valid,
                ) { Text("Save", color = ForgeMuted) }
                Button(
                    onClick = { onOpen(url, name.ifBlank { null }, saveToo) },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAccent, contentColor = Color.Black),
                ) { Text("Play") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ForgeMuted) }
        },
    )
}

@Composable
private fun StreamsBody(
    state: LibraryUiState,
    padding: PaddingValues,
    onPlay: (List<ForgeMediaItem>, Int) -> Unit,
    onRename: (SavedStream) -> Unit,
    onDelete: (String) -> Unit,
) {
    val q = state.query.trim().lowercase()
    val streams = if (q.isEmpty()) state.savedStreams
    else state.savedStreams.filter {
        it.name.lowercase().contains(q) || it.url.lowercase().contains(q)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (streams.isEmpty()) {
            item {
                Text(
                    "No saved streams yet. Tap + to add a network URL.",
                    color = ForgeMuted,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(streams, key = { it.id }) { stream ->
            SavedStreamRow(
                stream = stream,
                onPlay = { onPlay(listOf(stream.toMediaItem()), 0) },
                onRename = { onRename(stream) },
                onDelete = { onDelete(stream.id) },
            )
        }
    }
}

@Composable
private fun SavedStreamsSection(
    streams: List<SavedStream>,
    onPlay: (SavedStream) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Link, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Saved streams", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            streams.take(12).forEach { stream ->
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ForgeGraphite)
                        .clickable { onPlay(stream) }
                        .padding(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ForgeSurfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Link, null, tint = ForgeAccent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stream.name,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        minLines = 2,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedStreamRow(
    stream: SavedStream,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ForgeGraphite)
            .clickable(onClick = onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Link, null, tint = ForgeAccent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stream.name, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stream.url, color = ForgeMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = ForgeAccent)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = ForgeMuted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = ForgeGraphite) {
                DropdownMenuItem(text = { Text("Rename", color = Color.White) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.White) },
                    onClick = { menu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = ForgeAccent) },
                )
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text(title, color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForgeAccent, contentColor = Color.Black),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ForgeMuted) }
        },
    )
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<ForgePlaylist>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onCreate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text("Add to playlist", color = Color.White) },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text("No playlists yet.", color = ForgeMuted)
                } else {
                    playlists.forEach { pl ->
                        Text(
                            text = pl.name,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(pl.id) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate) { Text("New playlist", color = ForgeAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ForgeMuted) }
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
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ForgeAccent,
    unfocusedBorderColor = ForgeSurfaceVariant,
    focusedContainerColor = ForgeGraphite,
    unfocusedContainerColor = ForgeGraphite,
    cursorColor = ForgeAccent,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
)

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
