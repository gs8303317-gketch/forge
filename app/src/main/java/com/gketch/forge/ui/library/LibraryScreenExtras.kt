package com.gketch.forge.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gketch.forge.data.ContinueWatchItem
import com.gketch.forge.data.AudioBrowseMode
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.ForgePlaylist
import com.gketch.forge.data.LibrarySort
import com.gketch.forge.data.MediaFolder
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.SavedStream
import com.gketch.forge.R
import com.gketch.forge.MainActivity
import com.gketch.forge.data.WatchedFilter
import com.gketch.forge.data.WatchedStore
import com.gketch.forge.data.forgeItemFromUri
import com.gketch.forge.data.isPlayableStreamUrl
import com.gketch.forge.ui.permissions.hasMediaPermission
import com.gketch.forge.ui.thumb.ForgeThumbnail
import com.gketch.forge.ui.thumb.ForgeThumbnailUri
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import com.gketch.forge.ui.theme.ForgeSurfaceVariant
import java.util.Locale
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Bodies, cards, sections, dialogs extracted from LibraryScreen

@Composable
internal fun LibraryBody(
    state: LibraryUiState,
    permitted: Boolean,
    padding: PaddingValues,
    listState: LazyListState,
    gridState: LazyGridState,
    onPlay: (List<ForgeMediaItem>, Int) -> Unit,
    onToggleFavorite: (ForgeMediaItem) -> Unit,
    onAddToPlaylist: (ForgeMediaItem) -> Unit,
    onToggleSelect: (ForgeMediaItem) -> Unit,
    onBeginSelect: (ForgeMediaItem) -> Unit,
    onRemoveRecent: (ForgeMediaItem) -> Unit,
    onRemoveContinue: (ForgeMediaItem) -> Unit,
    onDeleteMedia: (ForgeMediaItem) -> Unit = {},
    onToggleWatched: (ForgeMediaItem) -> Unit = {},
) {
    val kindFavs = state.favorites.filter { if (state.tab == LibraryTab.AUDIO) !it.isVideo else it.isVideo }
    val kindRecent = state.recent.filter { if (state.tab == LibraryTab.AUDIO) !it.isVideo else it.isVideo }
    val continueWatching = if (state.tab == LibraryTab.VIDEO) state.continueWatching else emptyList()
    val recentlyAdded = if (state.tab == LibraryTab.VIDEO) state.recentlyAdded else emptyList()
    val sectionTitle = if (state.tab == LibraryTab.AUDIO) "Audio" else "Videos"
    when {
        state.loading && permitted && state.filtered.isEmpty() && kindRecent.isEmpty() && kindFavs.isEmpty() && continueWatching.isEmpty() && recentlyAdded.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForgeAccent)
            }
        }
        state.error != null && permitted && state.filtered.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }
        state.filtered.isEmpty() && kindRecent.isEmpty() && kindFavs.isEmpty() && continueWatching.isEmpty() && recentlyAdded.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        "No media found",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Open a stream from ⋮ or grant media access",
                        color = ForgeMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        state.layout == LibraryLayout.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                state = gridState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (kindFavs.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "fav-header") {
                        FavoritesSection(items = kindFavs, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                if (continueWatching.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "continue-header") {
                        ContinueWatchingSection(
                            items = continueWatching,
                            onPlay = { onPlay(listOf(it), 0) },
                            onRemove = onRemoveContinue,
                        )
                    }
                }
                if (recentlyAdded.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "added-header") {
                        RecentlyAddedSection(items = recentlyAdded, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                if (kindRecent.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "recent-header") {
                        RecentSection(items = kindRecent, onPlay = { onPlay(listOf(it), 0) }, onRemove = onRemoveRecent)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "lib-header") {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = ForgeMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                itemsIndexed(state.filtered, key = { _, item -> item.stableKey() }) { index, item ->
                    MediaGridCard(
                        item = item,
                        favorite = state.favoriteUris.contains(item.uri.toString()),
                        watched = WatchedStore.keyFor(item.uri.toString()) in state.watchedKeys,
                        selected = item.stableKey() in state.selectedKeys,
                        selecting = state.selecting,
                        onClick = {
                            if (state.selecting) onToggleSelect(item)
                            else onPlay(state.filtered, index)
                        },
                        onLongClick = { onBeginSelect(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                        onToggleWatched = { onToggleWatched(item) },
                        onDelete = { onDeleteMedia(item) },
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = listState,
                contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (kindFavs.isNotEmpty() && state.query.isBlank()) {
                    item(key = "fav-header") {
                        FavoritesSection(items = kindFavs, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                if (continueWatching.isNotEmpty() && state.query.isBlank()) {
                    item(key = "continue-header") {
                        ContinueWatchingSection(
                            items = continueWatching,
                            onPlay = { onPlay(listOf(it), 0) },
                            onRemove = onRemoveContinue,
                        )
                    }
                }
                if (recentlyAdded.isNotEmpty() && state.query.isBlank()) {
                    item(key = "added-header") {
                        RecentlyAddedSection(items = recentlyAdded, onPlay = { onPlay(listOf(it), 0) })
                    }
                }
                if (kindRecent.isNotEmpty() && state.query.isBlank()) {
                    item(key = "recent-header") {
                        RecentSection(items = kindRecent, onPlay = { onPlay(listOf(it), 0) }, onRemove = onRemoveRecent)
                    }
                }
                item(key = "lib-header") {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = ForgeMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                itemsIndexed(state.filtered, key = { _, item -> item.stableKey() }) { index, item ->
                    MediaRow(
                        item = item,
                        favorite = state.favoriteUris.contains(item.uri.toString()),
                        watched = WatchedStore.keyFor(item.uri.toString()) in state.watchedKeys,
                        selected = item.stableKey() in state.selectedKeys,
                        selecting = state.selecting,
                        onClick = {
                            if (state.selecting) onToggleSelect(item)
                            else onPlay(state.filtered, index)
                        },
                        onLongClick = { onBeginSelect(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                        onToggleWatched = { onToggleWatched(item) },
                        onDelete = { onDeleteMedia(item) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun FoldersBody(
    state: LibraryUiState,
    padding: PaddingValues,
    browseListState: LazyListState,
    folderListState: LazyListState,
    folderGridState: LazyGridState,
    onOpenFolder: (MediaFolder) -> Unit,
    onCloseFolder: () -> Unit,
    onFolderSubPath: (String) -> Unit,
    onOpenSubfolder: (String) -> Unit,
    onPlay: (List<ForgeMediaItem>, Int) -> Unit,
    onToggleFavorite: (ForgeMediaItem) -> Unit,
    onAddToPlaylist: (ForgeMediaItem) -> Unit,
    onHideFolder: (MediaFolder) -> Unit,
    onAddSafFolder: () -> Unit,
    onAddStream: () -> Unit,
    onRenameStream: (SavedStream) -> Unit,
    onDeleteStream: (String) -> Unit,
    onToggleSelect: (ForgeMediaItem) -> Unit,
    onBeginSelect: (ForgeMediaItem) -> Unit,
    onDeleteMedia: (ForgeMediaItem) -> Unit = {},
    onToggleWatched: (ForgeMediaItem) -> Unit = {},
) {
    val folder = state.selectedFolder
    if (folder == null) {
        val q = state.query.trim().lowercase()
        val streams = if (q.isEmpty()) state.savedStreams
        else state.savedStreams.filter {
            it.name.lowercase().contains(q) || it.url.lowercase().contains(q)
        }
        val folders = if (q.isEmpty()) state.folders
        else state.folders.filter { it.name.lowercase().contains(q) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = browseListState,
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "browse-actions") {
                Row(modifier = Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onAddSafFolder) {
                        Icon(Icons.Rounded.CreateNewFolder, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_folder), color = ForgeAccent)
                    }
                    TextButton(onClick = onAddStream) {
                        Icon(Icons.Rounded.Link, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_stream), color = ForgeAccent)
                    }
                }
            }
            if (streams.isNotEmpty()) {
                item(key = "streams-label") {
                    Text(
                        stringResource(R.string.streams),
                        color = ForgeMuted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                items(streams, key = { it.id }) { stream ->
                    SavedStreamRow(
                        stream = stream,
                        onPlay = { onPlay(listOf(stream.toMediaItem()), 0) },
                        onRename = { onRenameStream(stream) },
                        onDelete = { onDeleteStream(stream.id) },
                    )
                }
            }
            item(key = "folders-label") {
                Text(stringResource(R.string.folders), color = ForgeMuted, style = MaterialTheme.typography.titleMedium)
            }
            if (folders.isEmpty()) {
                item(key = "folders-empty") {
                    Text(
                        "No folders found. Add a folder from storage.",
                        color = ForgeMuted,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(folders, key = { it.bucketId }) { f ->
                FolderRow(
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
                IconButton(onClick = {
                    if (state.folderSubPath.isNotEmpty()) {
                        val parts = state.folderSubPath.trim('/').split('/').filter { it.isNotEmpty() }
                        val parent = parts.dropLast(1).joinToString("/")
                        onFolderSubPath(parent)
                    } else {
                        onCloseFolder()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    FolderBreadcrumbs(
                        folderName = folder.name,
                        subPath = state.folderSubPath,
                        onBrowse = onCloseFolder,
                        onFolderRoot = { onFolderSubPath("") },
                        onSubPath = onFolderSubPath,
                    )
                    Text(
                        if (state.query.isBlank()) {
                            val n = state.folderItems.size + state.folderSubfolders.size
                            "$n items"
                        } else {
                            "${state.folderItems.size} of ${state.folderItemsAll.size} items"
                        },
                        color = ForgeMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = {
                        val queue = if (state.query.isBlank() && state.folderSubPath.isEmpty()) {
                            state.folderItemsAll
                        } else {
                            state.folderItems
                        }
                        if (queue.isNotEmpty()) onPlay(queue.shuffled(), 0)
                    },
                    enabled = state.folderItemsAll.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.random_play_folder), tint = ForgeAccent)
                }
                IconButton(
                    onClick = { if (state.folderItems.isNotEmpty()) onPlay(state.folderItems, 0) },
                    enabled = state.folderItems.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play_folder), tint = ForgeAccent)
                }
            }
            if (state.layout == LibraryLayout.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = folderListState,
                    contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(state.folderSubfolders, key = { "sub-$it" }) { name ->
                        SubfolderRow(name = name, onClick = { onOpenSubfolder(name) })
                    }
                    itemsIndexed(state.folderItems, key = { _, item -> item.stableKey() }) { index, item ->
                        MediaRow(
                            item = item,
                            favorite = state.favoriteUris.contains(item.uri.toString()),
                            watched = WatchedStore.keyFor(item.uri.toString()) in state.watchedKeys,
                            selected = item.stableKey() in state.selectedKeys,
                            selecting = state.selecting,
                            onClick = {
                                if (state.selecting) onToggleSelect(item)
                                else onPlay(state.folderItems, index)
                            },
                            onLongClick = { onBeginSelect(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                            onToggleWatched = { onToggleWatched(item) },
                            onDelete = { onDeleteMedia(item) },
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = folderGridState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.folderSubfolders, key = { "sub-$it" }) { name ->
                        SubfolderRow(name = name, onClick = { onOpenSubfolder(name) })
                    }
                    itemsIndexed(state.folderItems, key = { _, item -> item.stableKey() }) { index, item ->
                        MediaGridCard(
                            item = item,
                            favorite = state.favoriteUris.contains(item.uri.toString()),
                            watched = WatchedStore.keyFor(item.uri.toString()) in state.watchedKeys,
                            selected = item.stableKey() in state.selectedKeys,
                            selecting = state.selecting,
                            onClick = {
                                if (state.selecting) onToggleSelect(item)
                                else onPlay(state.folderItems, index)
                            },
                            onLongClick = { onBeginSelect(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                            onToggleWatched = { onToggleWatched(item) },
                            onDelete = { onDeleteMedia(item) },
                        )
                    }
                }
            }
        }
    }
}


@Composable
internal fun FolderBreadcrumbs(
    folderName: String,
    subPath: String,
    onBrowse: () -> Unit,
    onFolderRoot: () -> Unit,
    onSubPath: (String) -> Unit,
) {
    val parts = subPath.trim('/').split('/').filter { it.isNotEmpty() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = false,
            onClick = onBrowse,
            label = { Text(stringResource(R.string.browse_root)) },
            colors = filterColors(),
        )
        Text("›", color = ForgeMuted)
        FilterChip(
            selected = parts.isEmpty(),
            onClick = onFolderRoot,
            label = { Text(folderName, maxLines = 1) },
            colors = filterColors(),
        )
        parts.forEachIndexed { index, part ->
            Text("›", color = ForgeMuted)
            val path = parts.take(index + 1).joinToString("/")
            FilterChip(
                selected = index == parts.lastIndex,
                onClick = { onSubPath(path) },
                label = { Text(part, maxLines = 1) },
                colors = filterColors(),
            )
        }
    }
}

@Composable
internal fun SubfolderRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Folder, contentDescription = null, tint = ForgeAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun PlaylistsBody(
    state: LibraryUiState,
    padding: PaddingValues,
    playlistsListState: LazyListState,
    playlistItemsState: LazyListState,
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
            state = playlistsListState,
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
                state = playlistItemsState,
                contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContinueWatchingSection(
    items: List<ContinueWatchItem>,
    onPlay: (ForgeMediaItem) -> Unit,
    onRemove: (ForgeMediaItem) -> Unit = {},
) {
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PlayCircle, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Continue watching", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Long-press to remove", style = MaterialTheme.typography.labelSmall, color = ForgeMuted)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(items.take(16), key = { it.item.uri.toString() }) { cw ->
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForgeGraphite)
                        .combinedClickable(
                            onClick = { onPlay(cw.item) },
                            onLongClick = { onRemove(cw.item) },
                        )
                        .padding(6.dp),
                ) {
                        ThumbBox(
                            item = cw.item,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            progress = cw.progress,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cw.item.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            minLines = 2,
                        )
                    }
            }
        }
    }
}

@Composable
internal fun FavoritesSection(
    items: List<ForgeMediaItem>,
    onPlay: (ForgeMediaItem) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Favorites", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(items.take(20), key = { it.uri.toString() }) { item ->
                RecentCard(item = item, onClick = { onPlay(item) })
            }
        }
    }
}

@Composable
internal fun RecentlyAddedSection(
    items: List<ForgeMediaItem>,
    onPlay: (ForgeMediaItem) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NewReleases, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.recently_added), style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(items.take(20), key = { it.uri.toString() }) { item ->
                RecentCard(item = item, onClick = { onPlay(item) })
            }
        }
    }
}

@Composable
internal fun RecentSection(
    items: List<ForgeMediaItem>,
    onPlay: (ForgeMediaItem) -> Unit,
    onRemove: (ForgeMediaItem) -> Unit = {},
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Recently played", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Long-press to remove", style = MaterialTheme.typography.labelSmall, color = ForgeMuted)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(items.take(12), key = { it.uri.toString() }) { item ->
                RecentCard(
                    item = item,
                    onClick = { onPlay(item) },
                    onRemove = { onRemove(item) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecentCard(
    item: ForgeMediaItem,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ForgeGraphite)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onRemove?.invoke() },
            )
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
internal fun MediaGridCard(
    item: ForgeMediaItem,
    favorite: Boolean,
    watched: Boolean = false,
    selected: Boolean = false,
    selecting: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleWatched: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ForgeSurfaceVariant else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    if (!selecting) menu = true
                },
            )
            .padding(4.dp),
    ) {
        Box {
            ThumbBox(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (item.isVideo) 16f / 9f else 1f),
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = ForgeAccent,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(22.dp),
                )
            }
            if (watched) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = ForgeAccent,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(18.dp),
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
                if (onToggleWatched != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (watched) stringResource(R.string.mark_unwatched)
                                else stringResource(R.string.mark_watched),
                                color = Color.White,
                            )
                        },
                        onClick = { menu = false; onToggleWatched() },
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Delete from device", color = Color.White) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDuration(item.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = ForgeMuted,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaRow(
    item: ForgeMediaItem,
    favorite: Boolean,
    watched: Boolean = false,
    selected: Boolean = false,
    selecting: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleWatched: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) ForgeSurfaceVariant else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    if (!selecting) menu = true
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ThumbBox(item = item, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)))
            if (watched) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = ForgeAccent,
                    modifier = Modifier.align(Alignment.BottomStart).padding(2.dp).size(12.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = buildString {
                    if (!item.isVideo && item.artist.isNotBlank()) {
                        append(item.artist)
                        append(" · ")
                    } else {
                        append(if (item.isVideo) "Video" else "Audio")
                        append(" · ")
                    }
                    append(formatDuration(item.durationMs))
                },
                style = MaterialTheme.typography.bodySmall,
                color = ForgeMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "Favorite",
                tint = if (favorite) ForgeAccent else ForgeMuted,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = ForgeGraphite) {
            DropdownMenuItem(
                text = { Text("Add to playlist", color = Color.White) },
                onClick = { menu = false; onAddToPlaylist() },
            )
            if (onToggleWatched != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (watched) stringResource(R.string.mark_unwatched)
                            else stringResource(R.string.mark_watched),
                            color = Color.White,
                        )
                    },
                    onClick = { menu = false; onToggleWatched() },
                )
            }
            if (onRemove != null) {
                DropdownMenuItem(
                    text = { Text("Remove from playlist", color = Color.White) },
                    onClick = { menu = false; onRemove() },
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Delete from device", color = Color.White) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderCard(folder: MediaFolder, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
    ) {
        ForgeThumbnailUri(
            uri = folder.thumbUri,
            isVideo = folder.kindHint == MediaKind.VIDEO,
            folder = true,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(folder.name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${folder.itemCount} items", color = ForgeMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderRow(folder: MediaFolder, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ForgeThumbnailUri(
            uri = folder.thumbUri,
            isVideo = folder.kindHint == MediaKind.VIDEO,
            folder = true,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${folder.itemCount} items", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun PlaylistRow(
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
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.QueueMusic, null, tint = ForgeAccent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${playlist.items.size} items", color = ForgeMuted, style = MaterialTheme.typography.bodySmall)
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
internal fun ThumbBox(item: ForgeMediaItem, modifier: Modifier = Modifier, progress: Float? = null) {
    ForgeThumbnail(item = item, modifier = modifier, progress = progress)
}

@Composable
internal fun StreamDialog(
    onDismiss: () -> Unit,
    onOpen: (url: String, name: String?, save: Boolean) -> Unit,
    onSaveOnly: (url: String, name: String?) -> Unit,
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var saveToo by remember { mutableStateOf(true) }
    val valid = isPlayableStreamUrl(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ForgeGraphite,
        title = { Text(stringResource(R.string.network_stream), color = MaterialTheme.colorScheme.onBackground) },
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
                TextButton(
                    onClick = {
                        val cm = context.getSystemService(ClipboardManager::class.java)
                        val raw = cm?.primaryClip?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        val found = MainActivity.findPlayableUrl(raw)
                        if (found != null) url = found
                        else if (raw.isNotBlank()) url = raw.trim()
                    },
                ) {
                    Icon(Icons.Rounded.ContentPaste, null, tint = ForgeAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.paste_clipboard), color = ForgeAccent)
                }
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
internal fun StreamsBody(
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
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
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
internal fun SavedStreamsSection(
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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(streams.take(12), key = { it.id }) { stream ->
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
internal fun SavedStreamRow(
    stream: SavedStream,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ForgeSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Link, null, tint = ForgeAccent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stream.name, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
internal fun NameDialog(
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
internal fun AddToPlaylistDialog(
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
internal fun filterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ForgeAccent,
    selectedLabelColor = ForgeBlack,
    containerColor = ForgeGraphite,
    labelColor = ForgeMuted,
)

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
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

internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
