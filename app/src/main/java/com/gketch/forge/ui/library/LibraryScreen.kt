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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlay: (items: List<ForgeMediaItem>, index: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onRequestPermission: () -> Unit,
    onExpandPlayer: () -> Unit = {},
    pinLockEnabled: Boolean = false,
    onGatePlaylists: () -> Boolean = { false },
    openPlaylistsTick: Int = 0,
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
    var deleteCandidate by remember { mutableStateOf<ForgeMediaItem?>(null) }
    var awaitingSystemDelete by remember { mutableStateOf<ForgeMediaItem?>(null) }
    var exportPlaylist by remember { mutableStateOf<ForgePlaylist?>(null) }
    var m3uMessage by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    val permitted = hasMediaPermission(context)
    val activity = remember(context) { context.findActivity() }
    var showExitDialog by remember { mutableStateOf(false) }

    val videoListState = rememberKeyedLazyListState("lib:VIDEO:list", viewModel)
    val videoGridState = rememberKeyedLazyGridState("lib:VIDEO:grid", viewModel)
    val audioListState = rememberKeyedLazyListState("lib:AUDIO:list", viewModel)
    val audioGridState = rememberKeyedLazyGridState("lib:AUDIO:grid", viewModel)
    val audioGroupsListState = rememberKeyedLazyListState("lib:AUDIO:groups", viewModel)
    val audioGroupTracksState = rememberKeyedLazyListState(
        "lib:AUDIO:group:${state.selectedAudioGroup?.key ?: "none"}",
        viewModel,
    )
    val mediaListState = if (state.tab == LibraryTab.AUDIO) audioListState else videoListState
    val mediaGridState = if (state.tab == LibraryTab.AUDIO) audioGridState else videoGridState
    val browseListState = rememberKeyedLazyListState("browse", viewModel)
    val folderKey = state.selectedFolder?.bucketId?.toString() ?: "none"
    val folderListState = rememberKeyedLazyListState("folder:$folderKey:list", viewModel)
    val folderGridState = rememberKeyedLazyGridState("folder:$folderKey:grid", viewModel)
    val playlistsListState = rememberKeyedLazyListState("playlists", viewModel)
    val playlistKey = state.selectedPlaylist?.id ?: "none"
    val playlistItemsState = rememberKeyedLazyListState("playlist:$playlistKey", viewModel)

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
    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val item = awaitingSystemDelete
        awaitingSystemDelete = null
        if (result.resultCode == Activity.RESULT_OK && item != null) {
            viewModel.onDeleteConfirmed(item)
            snackScopeMsg = "Deleted “${item.title}”"
        }
    }

    LaunchedEffect(permitted) {
        // Only scan when permission becomes available; avoid full rescan on back/recompose.
        if (permitted) viewModel.refreshIfNeeded()
    }
    LaunchedEffect(openPlaylistsTick) {
        if (openPlaylistsTick > 0) viewModel.setTab(LibraryTab.PLAYLISTS)
    }

    LaunchedEffect(state.query) {
        if (state.query.isNotEmpty()) searchExpanded = true
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
    fun handleLibraryBack() {
        if (showExitDialog) {
            showExitDialog = false
            return
        }
        if (!viewModel.consumeBack()) {
            showExitDialog = true
        }
    }

    BackHandler(enabled = true) {
        handleLibraryBack()
    }

    Scaffold(
        containerColor = ForgeBlack,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                MiniPlayerBar(onExpand = onExpandPlayer)
                NavigationBar(containerColor = ForgeGraphite) {
                    val items = listOf(
                        Triple(LibraryTab.VIDEO, Icons.Rounded.Movie, stringResource(R.string.nav_video)),
                        Triple(LibraryTab.AUDIO, Icons.Rounded.AudioFile, stringResource(R.string.nav_audio)),
                        Triple(LibraryTab.PLAYLISTS, Icons.Rounded.QueueMusic, stringResource(R.string.nav_playlists)),
                        Triple(LibraryTab.BROWSE, Icons.Rounded.Folder, stringResource(R.string.nav_browse)),
                    )
                    items.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = state.tab == tab,
                            onClick = {
                                if (tab == LibraryTab.PLAYLISTS && pinLockEnabled && onGatePlaylists()) {
                                    // Parent shows PIN gate
                                } else {
                                    viewModel.setTab(tab)
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = {
                                Text(
                                    label,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ForgeAccent,
                                selectedTextColor = ForgeAccent,
                                indicatorColor = ForgeSurfaceVariant,
                                unselectedIconColor = ForgeMuted,
                                unselectedTextColor = ForgeMuted,
                            ),
                        )
                    }
                }
            }
        },
        topBar = {
            val sectionTitle = when {
                state.selecting -> "${state.selectedKeys.size} selected"
                state.tab == LibraryTab.VIDEO -> stringResource(R.string.nav_video)
                state.tab == LibraryTab.AUDIO -> stringResource(R.string.nav_audio)
                state.tab == LibraryTab.PLAYLISTS -> stringResource(R.string.nav_playlists)
                state.tab == LibraryTab.BROWSE -> stringResource(R.string.nav_browse)
                else -> stringResource(R.string.app_name)
            }
            val showMediaChrome = state.tab == LibraryTab.VIDEO ||
                state.tab == LibraryTab.AUDIO ||
                (state.tab == LibraryTab.BROWSE && state.selectedFolder != null)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForgeBlack),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (state.selecting) Color.White else ForgeAccent,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(min = 72.dp),
                        )
                    },
                    navigationIcon = {
                        if (state.selecting) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear selection", tint = Color.White)
                            }
                        }
                    },
                    actions = {
                        if (state.selecting) {
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
                            IconButton(onClick = {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded && state.query.isNotEmpty()) {
                                    viewModel.onQueryChange("")
                                }
                            }) {
                                Icon(
                                    if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                                    contentDescription = if (searchExpanded) "Close search" else "Search",
                                    tint = ForgeMuted,
                                )
                            }
                            if (showMediaChrome) {
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
                                Box {
                                    IconButton(onClick = { sortMenu = true }) {
                                        Icon(Icons.Rounded.Sort, contentDescription = "Sort", tint = ForgeMuted)
                                    }
                                    DropdownMenu(
                                        expanded = sortMenu,
                                        onDismissRequest = { sortMenu = false },
                                        containerColor = ForgeGraphite,
                                    ) {
                                        LibrarySort.entries.forEach { sort ->
                                            DropdownMenuItem(
                                                text = { Text(sort.label, color = Color.White) },
                                                onClick = {
                                                    viewModel.setSort(sort)
                                                    sortMenu = false
                                                },
                                                trailingIcon = {
                                                    if (state.sort == sort) {
                                                        Icon(Icons.Rounded.Check, null, tint = ForgeAccent)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Box {
                                IconButton(onClick = { overflowMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = ForgeMuted)
                                }
                                DropdownMenu(
                                    expanded = overflowMenu,
                                    onDismissRequest = { overflowMenu = false },
                                    containerColor = ForgeGraphite,
                                ) {
                                    if ((state.tab == LibraryTab.VIDEO ||
                                            (state.tab == LibraryTab.AUDIO &&
                                                state.audioBrowseMode == AudioBrowseMode.SONGS &&
                                                state.selectedAudioGroup == null)) &&
                                        state.filtered.isNotEmpty()
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.shuffle_all), color = Color.White) },
                                            onClick = {
                                                overflowMenu = false
                                                onPlay(state.filtered.shuffled(), 0)
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Shuffle, null, tint = ForgeAccent) },
                                        )
                                    }
                                    if (state.tab == LibraryTab.AUDIO &&
                                        state.selectedAudioGroup != null &&
                                        state.selectedAudioGroup!!.tracks.isNotEmpty()
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.shuffle_all), color = Color.White) },
                                            onClick = {
                                                overflowMenu = false
                                                onPlay(state.selectedAudioGroup!!.tracks.shuffled(), 0)
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Shuffle, null, tint = ForgeAccent) },
                                        )
                                    }
                                    if (state.tab == LibraryTab.PLAYLISTS) {
                                        DropdownMenuItem(
                                            text = { Text("New playlist", color = Color.White) },
                                            onClick = {
                                                overflowMenu = false
                                                showCreatePlaylist = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = ForgeAccent)
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Import M3U", color = Color.White) },
                                            onClick = {
                                                overflowMenu = false
                                                importM3uLauncher.launch(
                                                    arrayOf(
                                                        "audio/x-mpegurl",
                                                        "application/vnd.apple.mpegurl",
                                                        "text/plain",
                                                        "*/*",
                                                    ),
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.FileUpload, null, tint = ForgeAccent) },
                                        )
                                    }
                                    if (state.tab == LibraryTab.BROWSE && state.selectedFolder == null) {
                                        DropdownMenuItem(
                                            text = { Text("Add folder", color = Color.White) },
                                            onClick = {
                                                overflowMenu = false
                                                safTreeLauncher.launch(null)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.CreateNewFolder, null, tint = ForgeAccent)
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.library_rescan), color = Color.White) },
                                        onClick = {
                                            overflowMenu = false
                                            viewModel.forceRescan()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Refresh, null, tint = ForgeAccent) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open stream", color = Color.White) },
                                        onClick = {
                                            overflowMenu = false
                                            showStreamDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Link, null, tint = ForgeAccent) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Playback history", color = Color.White) },
                                        onClick = {
                                            overflowMenu = false
                                            onOpenHistory()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.History, null, tint = ForgeAccent) },
                                    )
                                    if (state.recent.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Clear history", color = Color.White) },
                                            onClick = {
                                                overflowMenu = false
                                                showClearHistory = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = ForgeAccent) },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.settings), color = Color.White) },
                                        onClick = {
                                            overflowMenu = false
                                            onOpenSettings()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Settings, null, tint = ForgeAccent) },
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ForgeBlack,
                        titleContentColor = ForgeAccent,
                        actionIconContentColor = ForgeMuted,
                        navigationIconContentColor = Color.White,
                    ),
                )
                if (searchExpanded || state.query.isNotEmpty()) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (state.selectedFolder != null) stringResource(R.string.search_folder_hint)
                                else stringResource(R.string.search_hint),
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = ForgeMuted)
                        },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = ForgeMuted)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                    )
                }
                if (state.tab == LibraryTab.VIDEO ||
                    (state.tab == LibraryTab.AUDIO && state.audioBrowseMode == AudioBrowseMode.SONGS) ||
                    (state.tab == LibraryTab.BROWSE && state.selectedFolder != null)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WatchedFilter.entries.forEach { wf ->
                            FilterChip(
                                selected = state.watchedFilter == wf,
                                onClick = { viewModel.setWatchedFilter(wf) },
                                label = {
                                    Text(
                                        when (wf) {
                                            WatchedFilter.ALL -> stringResource(R.string.all)
                                            WatchedFilter.WATCHED -> stringResource(R.string.watched)
                                            WatchedFilter.UNWATCHED -> stringResource(R.string.unwatched)
                                        },
                                        maxLines = 1,
                                    )
                                },
                                colors = filterColors(),
                            )
                        }
                    }
                }
                if (state.tab == LibraryTab.AUDIO && state.selectedAudioGroup == null) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        AudioBrowseModeChips(
                            mode = state.audioBrowseMode,
                            onMode = viewModel::setAudioBrowseMode,
                        )
                    }
                }
                if (!permitted) {
                    TextButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text("Grant media access to scan this device", color = ForgeAccent)
                    }
                }
            }
        },
    ) { padding ->
        val ptrState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.forceRescan() },
            state = ptrState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
        val innerPadding = PaddingValues(0.dp)
        when (state.tab) {
            LibraryTab.VIDEO -> LibraryBody(
                state = state,
                permitted = permitted,
                padding = innerPadding,
                listState = mediaListState,
                gridState = mediaGridState,
                onPlay = onPlay,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { addToPlaylistItem = it },
                onToggleSelect = viewModel::toggleSelected,
                onBeginSelect = viewModel::beginSelection,
                onRemoveRecent = viewModel::removeRecent,
                onRemoveContinue = viewModel::removeContinueWatching,
                onDeleteMedia = { deleteCandidate = it },
                onToggleWatched = viewModel::toggleWatched,
            )
            LibraryTab.AUDIO -> {
                val group = state.selectedAudioGroup
                when {
                    group != null -> {
                        Column(Modifier.fillMaxSize().padding(innerPadding)) {
                            AudioGroupTracksHeader(
                                group = group,
                                mode = state.audioBrowseMode,
                                onBack = viewModel::closeAudioGroup,
                                onPlayAll = { onPlay(group.tracks, 0) },
                                onShuffleAll = { onPlay(group.tracks.shuffled(), 0) },
                            )
                            LazyColumn(
                                state = audioGroupTracksState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                            ) {
                                items(group.tracks, key = { it.stableKey() }) { item ->
                                    val idx = group.tracks.indexOf(item)
                                    MediaRow(
                                        item = item,
                                        favorite = state.favoriteUris.contains(item.uri.toString()),
                                        watched = false,
                                        selected = false,
                                        selecting = false,
                                        onClick = { onPlay(group.tracks, idx) },
                                        onLongClick = {},
                                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                                        onAddToPlaylist = { addToPlaylistItem = item },
                                        onToggleWatched = {},
                                        onDelete = { deleteCandidate = item },
                                    )
                                }
                            }
                        }
                    }
                    state.audioBrowseMode != AudioBrowseMode.SONGS -> AudioGroupsList(
                        groups = state.audioGroups,
                        listState = audioGroupsListState,
                        contentPadding = innerPadding,
                        onOpen = viewModel::openAudioGroup,
                    )
                    else -> LibraryBody(
                        state = state,
                        permitted = permitted,
                        padding = innerPadding,
                        listState = mediaListState,
                        gridState = mediaGridState,
                        onPlay = onPlay,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToPlaylist = { addToPlaylistItem = it },
                        onToggleSelect = viewModel::toggleSelected,
                        onBeginSelect = viewModel::beginSelection,
                        onRemoveRecent = viewModel::removeRecent,
                        onRemoveContinue = viewModel::removeContinueWatching,
                        onDeleteMedia = { deleteCandidate = it },
                        onToggleWatched = viewModel::toggleWatched,
                    )
                }
            }
            LibraryTab.BROWSE -> FoldersBody(
                state = state,
                padding = innerPadding,
                browseListState = browseListState,
                folderListState = folderListState,
                folderGridState = folderGridState,
                onOpenFolder = viewModel::openFolder,
                onCloseFolder = viewModel::closeFolder,
                onFolderSubPath = viewModel::setFolderSubPath,
                onOpenSubfolder = viewModel::openFolderSubfolder,
                onPlay = onPlay,
                onToggleFavorite = viewModel::toggleFavorite,
                onAddToPlaylist = { addToPlaylistItem = it },
                onHideFolder = { hideFolder = it },
                onAddSafFolder = { safTreeLauncher.launch(null) },
                onAddStream = { showStreamDialog = true },
                onRenameStream = { renameStream = it },
                onDeleteStream = viewModel::removeStream,
                onToggleSelect = viewModel::toggleSelected,
                onBeginSelect = viewModel::beginSelection,
                onDeleteMedia = { deleteCandidate = it },
                onToggleWatched = viewModel::toggleWatched,
            )
            LibraryTab.PLAYLISTS -> PlaylistsBody(
                state = state,
                padding = innerPadding,
                playlistsListState = playlistsListState,
                playlistItemsState = playlistItemsState,
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
        }
        } // PullToRefreshBox
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
                viewModel.setTab(LibraryTab.BROWSE)
            },
        )
    }
    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            containerColor = ForgeGraphite,
            title = { Text("Clear history", color = Color.White) },
            text = {
                Column {
                    Text(
                        "Remove recently played items. You can also clear saved resume positions.",
                        color = ForgeMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.clearHistory(alsoResume = true)
                            showClearHistory = false
                        },
                    ) { Text("Clear recent + resume", color = Color.White) }
                }
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
                TextButton(onClick = { showClearHistory = false }) {
                    Text("Cancel", color = ForgeMuted)
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
    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = ForgeGraphite,
            title = { Text("Delete media", color = Color.White) },
            text = {
                Text(
                    "Delete “${item.title}” from this device? This cannot be undone.",
                    color = ForgeMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (val result = viewModel.deleteMedia(item)) {
                                is com.gketch.forge.data.DeleteMediaResult.Deleted -> {
                                    deleteCandidate = null
                                    snackScopeMsg = "Deleted “${item.title}”"
                                }
                                is com.gketch.forge.data.DeleteMediaResult.NeedUserConfirm -> {
                                    awaitingSystemDelete = item
                                    deleteCandidate = null
                                    deleteConfirmLauncher.launch(
                                        IntentSenderRequest.Builder(result.intentSender).build(),
                                    )
                                }
                                is com.gketch.forge.data.DeleteMediaResult.Failed -> {
                                    deleteCandidate = null
                                    snackScopeMsg = result.message
                                }
                            }
                        }
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel", color = ForgeMuted)
                }
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

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = ForgeGraphite,
            title = { Text("Exit Forge?", color = Color.White) },
            text = { Text("Close the app?", color = ForgeMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    },
                ) { Text("Exit", color = ForgeAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = ForgeMuted)
                }
            },
        )
    }
}

