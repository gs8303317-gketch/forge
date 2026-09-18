package com.gketch.forge.ui.library

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.FavoritesStore
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.ForgePlaylist
import com.gketch.forge.data.HiddenFolder
import com.gketch.forge.data.HiddenFoldersStore
import com.gketch.forge.data.LibrarySort
import com.gketch.forge.data.AudioBrowseGroup
import com.gketch.forge.data.AudioBrowseMode
import com.gketch.forge.data.MediaFolder
import com.gketch.forge.data.groupAudioByAlbum
import com.gketch.forge.data.groupAudioByArtist
import com.gketch.forge.data.groupAudioByGenre
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.DeleteMediaResult
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.data.M3uPlaylistIo
import com.gketch.forge.data.PlaylistStore
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ContinueWatchItem
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.data.WatchedStore
import com.gketch.forge.data.WatchedFilter
import com.gketch.forge.data.MinClipLength
import com.gketch.forge.data.SafFolder
import com.gketch.forge.data.SafFoldersStore
import com.gketch.forge.data.SafMediaScanner
import com.gketch.forge.data.SavedStream
import com.gketch.forge.data.SavedStreamsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter { ALL, VIDEO, AUDIO }
enum class LibraryTab { VIDEO, AUDIO, PLAYLISTS, BROWSE }
enum class LibraryLayout { GRID, LIST }

data class LibraryUiState(
    val items: List<ForgeMediaItem> = emptyList(),
    val filtered: List<ForgeMediaItem> = emptyList(),
    val recent: List<ForgeMediaItem> = emptyList(),
    val favorites: List<ForgeMediaItem> = emptyList(),
    val continueWatching: List<ContinueWatchItem> = emptyList(),
    val recentlyAdded: List<ForgeMediaItem> = emptyList(),
    val favoriteUris: Set<String> = emptySet(),
    val folders: List<MediaFolder> = emptyList(),
    val folderItemsAll: List<ForgeMediaItem> = emptyList(),
    val folderItems: List<ForgeMediaItem> = emptyList(),
    val selectedFolder: MediaFolder? = null,
    val folderSubPath: String = "",
    val folderSubfolders: List<String> = emptyList(),
    val playlists: List<ForgePlaylist> = emptyList(),
    val selectedPlaylist: ForgePlaylist? = null,
    val savedStreams: List<SavedStream> = emptyList(),
    val hiddenFolders: List<HiddenFolder> = emptyList(),
    val hiddenBucketIds: Set<Long> = emptySet(),
    val safFolders: List<SafFolder> = emptyList(),
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.VIDEO,
    val sort: LibrarySort = LibrarySort.NAME,
    val tab: LibraryTab = LibraryTab.VIDEO,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val selecting: Boolean = false,
    val selectedKeys: Set<String> = emptySet(),
    val watchedKeys: Set<String> = emptySet(),
    val watchedFilter: WatchedFilter = WatchedFilter.ALL,
    val minClipSeconds: Int = 0,
    val audioBrowseMode: AudioBrowseMode = AudioBrowseMode.SONGS,
    val audioGroups: List<AudioBrowseGroup> = emptyList(),
    val selectedAudioGroup: AudioBrowseGroup? = null,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MediaRepository(application)
    private val recentStore = RecentStore(application)
    private val resumeStore = ResumeStore(application)
    private val favoritesStore = FavoritesStore(application)
    private val playlistStore = PlaylistStore(application)
    private val savedStreamsStore = SavedStreamsStore(application)
    private val settingsStore = AppSettingsStore(application)
    private val hiddenStore = HiddenFoldersStore(application)
    private val safStore = SafFoldersStore(application)
    private val safScanner = SafMediaScanner(application)
    private val watchedStore = WatchedStore(application)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private val listAnchors = mutableMapOf<String, ScrollAnchor>()
    private val gridAnchors = mutableMapOf<String, ScrollAnchor>()

    private fun filterFolderItems(
        all: List<ForgeMediaItem>,
        query: String,
        watchedKeys: Set<String> = _state.value.watchedKeys,
        watchedFilter: WatchedFilter = _state.value.watchedFilter,
        minClipSeconds: Int = _state.value.minClipSeconds,
    ): List<ForgeMediaItem> {
        val q = query.trim()
        var list = all
        if (minClipSeconds > 0) {
            val minMs = minClipSeconds * 1000L
            list = list.filter { !it.isVideo || it.durationMs <= 0L || it.durationMs >= minMs }
        }
        list = when (watchedFilter) {
            WatchedFilter.ALL -> list
            WatchedFilter.WATCHED -> list.filter { watchedStore.isWatched(watchedKeys, it.uri.toString()) }
            WatchedFilter.UNWATCHED -> list.filter { !watchedStore.isWatched(watchedKeys, it.uri.toString()) }
        }
        if (q.isEmpty()) return list
        return list.filter { it.title.contains(q, ignoreCase = true) }
    }

    fun listAnchor(key: String): ScrollAnchor = listAnchors[key] ?: ScrollAnchor()
    fun saveListAnchor(key: String, index: Int, offset: Int) { listAnchors[key] = ScrollAnchor(index, offset) }
    fun gridAnchor(key: String): ScrollAnchor = gridAnchors[key] ?: ScrollAnchor()
    fun saveGridAnchor(key: String, index: Int, offset: Int) { gridAnchors[key] = ScrollAnchor(index, offset) }

    fun consumeBack(): Boolean {
        val st = _state.value
        if (st.selecting) { clearSelection(); return true }
        if (st.tab == LibraryTab.AUDIO && st.selectedAudioGroup != null) { closeAudioGroup(); return true }
        if (st.tab == LibraryTab.BROWSE && st.selectedFolder != null) { closeFolder(); return true }
        if (st.tab == LibraryTab.PLAYLISTS && st.selectedPlaylist != null) { closePlaylist(); return true }
        return false
    }

    init {
        viewModelScope.launch {
            recentStore.recent.collect { _ ->
                _state.update { it.copy(recent = emptyList()) }
            }
        }
        viewModelScope.launch {
            favoritesStore.favorites.collect { items ->
                _state.update {
                    it.copy(favorites = items, favoriteUris = items.map { f -> f.uri.toString() }.toSet())
                }
            }
        }
        viewModelScope.launch {
            playlistStore.playlists.collect { list ->
                _state.update { st ->
                    val selected = st.selectedPlaylist?.let { sel -> list.find { it.id == sel.id } }
                    st.copy(playlists = list, selectedPlaylist = selected)
                }
            }
        }
        viewModelScope.launch {
            savedStreamsStore.streams.collect { list -> _state.update { it.copy(savedStreams = list) } }
        }
        viewModelScope.launch {
            var appliedTab = false
            settingsStore.settings.collect { prefs ->
                _state.update {
                    val tab = if (!appliedTab) {
                        appliedTab = true
                        runCatching { LibraryTab.valueOf(prefs.lastLibraryTab) }.getOrDefault(LibraryTab.VIDEO)
                    } else it.tab
                    val filter = when (tab) {
                        LibraryTab.VIDEO -> LibraryFilter.VIDEO
                        LibraryTab.AUDIO -> LibraryFilter.AUDIO
                        else -> it.filter
                    }
                    it.copy(
                        sort = prefs.librarySort,
                        tab = tab,
                        filter = filter,
                        minClipSeconds = prefs.minClipLength.seconds,
                        filtered = applyFilterAndSort(it.items, filter, prefs.librarySort, it.watchedKeys, it.watchedFilter, prefs.minClipLength.seconds),
                        folderItems = filterFolderItems(it.folderItemsAll, it.query, it.watchedKeys, it.watchedFilter, prefs.minClipLength.seconds),
                    )
                }
            }
        }
        viewModelScope.launch {
            hiddenStore.hidden.collect { list ->
                _state.update { it.copy(hiddenFolders = list, hiddenBucketIds = list.map { f -> f.bucketId }.toSet()) }
                refresh()
            }
        }
        viewModelScope.launch {
            safStore.folders.collect { list ->
                _state.update { it.copy(safFolders = list) }
                refresh()
            }
        }
        viewModelScope.launch {
            watchedStore.watchedKeys.collect { keys ->
                _state.update { st ->
                    st.copy(
                        watchedKeys = keys,
                        filtered = applyFilterAndSort(st.items, st.filter, st.sort, keys, st.watchedFilter, st.minClipSeconds),
                        folderItems = filterFolderItems(st.folderItemsAll, st.query, keys, st.watchedFilter, st.minClipSeconds),
                    )
                }
            }
        }
    }

    fun refreshIfNeeded() {
        if (_state.value.items.isNotEmpty() && !_state.value.loading) return
        refresh()
    }

    fun forceRescan() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            runCatching {
                val cr = getApplication<Application>().contentResolver
                cr.notifyChange(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null)
                cr.notifyChange(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            }
            delay(150)
            try {
                applyCatalog(refreshing = false)
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.message ?: "Failed to load media") }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val keepVisible = _state.value.items.isNotEmpty()
            _state.update { it.copy(loading = !keepVisible, error = null) }
            try {
                applyCatalog()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to load media") }
            }
        }
    }

    private suspend fun applyCatalog(refreshing: Boolean = false) {
        val hidden = _state.value.hiddenBucketIds
        val safItems = try { safScanner.scan(_state.value.safFolders) } catch (_: Exception) { emptyList() }
        val inFolder = _state.value.selectedFolder != null
        val libraryQuery = if (inFolder) "" else _state.value.query
        val items = repo.loadLibrary(libraryQuery, hidden, safItems)
        val folders = repo.loadFolders(hidden, safItems)
        val selected = _state.value.selectedFolder
        val folderAll = if (selected != null) {
            try {
                repo.loadLibrary("", hidden, safItems).filter { m -> m.bucketId == selected.bucketId }.sortedBy { m -> m.title.lowercase() }
            } catch (_: Exception) {
                items.filter { m -> m.bucketId == selected.bucketId }.sortedBy { m -> m.title.lowercase() }
            }
        } else emptyList()
        _state.update {
            val snap = resumeStore.positionSnapshot()
            val continuing = resumeStore.continueWatching(items, snap, videosOnly = true)
            val groups = recomputeAudioGroups(items, it.audioBrowseMode, it.query)
            val sel = it.selectedAudioGroup?.let { g -> groups.find { x -> x.key == g.key } }
            it.copy(
                items = items,
                filtered = applyFilterAndSort(items, it.filter, it.sort),
                folders = folders,
                folderItemsAll = folderAll,
                folderItems = filterFolderItems(folderAll, it.query),
                continueWatching = continuing,
                recentlyAdded = emptyList(),
                loading = false,
                refreshing = refreshing,
                audioGroups = groups,
                selectedAudioGroup = sel,
            )
        }
    }

    fun onQueryChange(query: String) {
        _state.update { st ->
            if (st.selectedFolder != null) st.copy(query = query, folderItems = filterFolderItems(st.folderItemsAll, query))
            else st.copy(query = query)
        }
        if (_state.value.selectedFolder != null) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(250); refresh() }
    }

    fun setFilter(filter: LibraryFilter) {
        _state.update { it.copy(filter = filter, filtered = applyFilterAndSort(it.items, filter, it.sort)) }
    }

    fun setSort(sort: LibrarySort) {
        viewModelScope.launch { settingsStore.setLibrarySort(sort) }
        _state.update { it.copy(sort = sort, filtered = applyFilterAndSort(it.items, it.filter, sort)) }
    }

    private fun recomputeAudioGroups(items: List<ForgeMediaItem>, mode: AudioBrowseMode, query: String): List<AudioBrowseGroup> {
        val audio = items.filter { !it.isVideo }
        val q = query.trim()
        val filtered = if (q.isEmpty()) audio else audio.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) || it.album.contains(q, ignoreCase = true) || it.genre.contains(q, ignoreCase = true)
        }
        return when (mode) {
            AudioBrowseMode.SONGS -> emptyList()
            AudioBrowseMode.ALBUMS -> groupAudioByAlbum(filtered)
            AudioBrowseMode.ARTISTS -> groupAudioByArtist(filtered)
            AudioBrowseMode.GENRES -> groupAudioByGenre(filtered)
        }
    }

    fun setTab(tab: LibraryTab) {
        viewModelScope.launch { settingsStore.setLastLibraryTab(tab.name) }
        _state.update {
            val filter = when (tab) {
                LibraryTab.VIDEO -> LibraryFilter.VIDEO
                LibraryTab.AUDIO -> LibraryFilter.AUDIO
                else -> it.filter
            }
            val mode = if (tab == LibraryTab.AUDIO) it.audioBrowseMode else AudioBrowseMode.SONGS
            it.copy(
                tab = tab,
                filter = filter,
                filtered = applyFilterAndSort(it.items, filter, it.sort, it.watchedKeys, it.watchedFilter, it.minClipSeconds),
                selecting = false,
                selectedKeys = emptySet(),
                selectedAudioGroup = if (tab == LibraryTab.AUDIO) it.selectedAudioGroup else null,
                audioGroups = if (tab == LibraryTab.AUDIO) recomputeAudioGroups(it.items, mode, it.query) else emptyList(),
            )
        }
    }

    fun setAudioBrowseMode(mode: AudioBrowseMode) {
        _state.update { it.copy(audioBrowseMode = mode, selectedAudioGroup = null, audioGroups = recomputeAudioGroups(it.items, mode, it.query)) }
    }
    fun openAudioGroup(group: AudioBrowseGroup) { _state.update { it.copy(selectedAudioGroup = group) } }
    fun closeAudioGroup() { _state.update { it.copy(selectedAudioGroup = null) } }
    fun removeRecent(item: ForgeMediaItem) { viewModelScope.launch { recentStore.remove(item.uri) } }
    fun removeContinueWatching(item: ForgeMediaItem) {
        viewModelScope.launch {
            resumeStore.clear(item.uri.toString())
            val snap = resumeStore.positionSnapshot()
            _state.update { it.copy(continueWatching = resumeStore.continueWatching(it.items, snap, videosOnly = true)) }
        }
    }
    fun setLayout(layout: LibraryLayout) { _state.update { it.copy(layout = layout) } }

    fun openFolder(folder: MediaFolder) {
        viewModelScope.launch {
            val hidden = _state.value.hiddenBucketIds
            val all = try { repo.loadFolderItems(folder.bucketId) } catch (_: Exception) {
                _state.value.items.filter { it.bucketId == folder.bucketId }
            }.filter { it.bucketId !in hidden }.sortedBy { it.title.lowercase() }
            val base = folderBreadcrumbBase(folder, all)
            val level = itemsAtLevel(all, base, "")
            val display = if (level.isNotEmpty() || subfoldersAt(all, base, "").isNotEmpty()) level else all
            _state.update {
                it.copy(selectedFolder = folder, folderItemsAll = all, folderSubPath = "", folderSubfolders = subfoldersAt(all, base, ""), folderItems = filterFolderItems(display, it.query), tab = LibraryTab.BROWSE)
            }
        }
    }

    fun closeFolder() {
        _state.update {
            it.copy(selectedFolder = null, folderItemsAll = emptyList(), folderItems = emptyList(), folderSubPath = "", folderSubfolders = emptyList(), query = if (it.tab == LibraryTab.BROWSE) "" else it.query)
        }
    }

    fun setFolderSubPath(subPath: String) {
        val st = _state.value
        val folder = st.selectedFolder ?: return
        val base = folderBreadcrumbBase(folder, st.folderItemsAll)
        val clean = subPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        val levelItems = itemsAtLevel(st.folderItemsAll, base, clean)
        val subs = subfoldersAt(st.folderItemsAll, base, clean)
        val display = if (levelItems.isNotEmpty() || subs.isNotEmpty()) levelItems else {
            val current = base + pathSegments(clean)
            st.folderItemsAll.filter { pathSegments(it.relativePath).take(current.size) == current }
        }
        _state.update { it.copy(folderSubPath = clean, folderSubfolders = subs, folderItems = filterFolderItems(display, it.query)) }
    }

    fun openFolderSubfolder(name: String) {
        val prefix = _state.value.folderSubPath.trim('/')
        setFolderSubPath(listOf(prefix, name).filter { it.isNotEmpty() }.joinToString("/"))
    }

    private fun pathSegments(relativePath: String): List<String> =
        relativePath.trim('/').split('/').filter { it.isNotBlank() }

    private fun folderBreadcrumbBase(folder: MediaFolder, items: List<ForgeMediaItem>): List<String> {
        val paths = items.map { pathSegments(it.relativePath) }.filter { it.isNotEmpty() }
        if (paths.isEmpty()) return listOf(folder.name)
        val sample = paths.minByOrNull { it.size } ?: return listOf(folder.name)
        val idx = sample.indexOfLast { it.equals(folder.name, ignoreCase = true) }
        return if (idx >= 0) sample.take(idx + 1) else listOf(folder.name)
    }

    private fun subfoldersAt(items: List<ForgeMediaItem>, baseSegments: List<String>, subPath: String): List<String> {
        val current = baseSegments + pathSegments(subPath)
        val names = linkedSetOf<String>()
        for (item in items) {
            val segs = pathSegments(item.relativePath)
            if (segs.size > current.size && segs.take(current.size) == current) names += segs[current.size]
        }
        return names.sortedBy { it.lowercase() }
    }

    private fun itemsAtLevel(items: List<ForgeMediaItem>, baseSegments: List<String>, subPath: String): List<ForgeMediaItem> {
        val current = baseSegments + pathSegments(subPath)
        return items.filter { pathSegments(it.relativePath) == current }
    }

    fun removeDeletedFromLists(uri: android.net.Uri) {
        val key = uri.toString()
        _state.update { st ->
            val items = st.items.filterNot { it.uri.toString() == key }
            val folderAll = st.folderItemsAll.filterNot { it.uri.toString() == key }
            st.copy(
                items = items,
                filtered = applyFilterAndSort(items, st.filter, st.sort),
                folderItemsAll = folderAll,
                folderItems = filterFolderItems(folderAll, st.query),
                recent = st.recent.filterNot { it.uri.toString() == key },
                favorites = st.favorites.filterNot { it.uri.toString() == key },
                continueWatching = st.continueWatching.filterNot { it.item.uri.toString() == key },
                selectedKeys = st.selectedKeys - listOfNotNull(
                    st.items.find { it.uri.toString() == key }?.stableKey(),
                    st.folderItemsAll.find { it.uri.toString() == key }?.stableKey(),
                ).toSet(),
            )
        }
        viewModelScope.launch { recentStore.remove(uri); resumeStore.clear(key); favoritesStore.remove(uri) }
    }

    suspend fun deleteMedia(item: ForgeMediaItem): DeleteMediaResult {
        val result = repo.deleteMedia(item)
        if (result is DeleteMediaResult.Deleted) removeDeletedFromLists(item.uri)
        return result
    }

    fun onDeleteConfirmed(item: ForgeMediaItem) { removeDeletedFromLists(item.uri); refresh() }
    fun openPlaylist(playlist: ForgePlaylist) { _state.update { it.copy(selectedPlaylist = playlist, tab = LibraryTab.PLAYLISTS) } }
    fun closePlaylist() { _state.update { it.copy(selectedPlaylist = null) } }
    fun toggleFavorite(item: ForgeMediaItem) { viewModelScope.launch { favoritesStore.toggle(item) } }
    fun isFavorite(uri: Uri): Boolean = _state.value.favoriteUris.contains(uri.toString())
    fun createPlaylist(name: String) { viewModelScope.launch { playlistStore.create(name) } }
    fun renamePlaylist(id: String, name: String) { viewModelScope.launch { playlistStore.rename(id, name) } }
    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            playlistStore.delete(id)
            _state.update { if (it.selectedPlaylist?.id == id) it.copy(selectedPlaylist = null) else it }
        }
    }
    fun addToPlaylist(playlistId: String, item: ForgeMediaItem) { viewModelScope.launch { playlistStore.addItem(playlistId, item) } }
    fun addItemsToPlaylist(playlistId: String, items: List<ForgeMediaItem>) { viewModelScope.launch { playlistStore.addItems(playlistId, items) } }
    fun removeFromPlaylist(playlistId: String, uri: Uri) { viewModelScope.launch { playlistStore.removeItem(playlistId, uri) } }
    fun movePlaylistItem(playlistId: String, fromIndex: Int, toIndex: Int) { viewModelScope.launch { playlistStore.moveItem(playlistId, fromIndex, toIndex) } }
    fun clearHistory(alsoResume: Boolean) {
        viewModelScope.launch { recentStore.clear(); if (alsoResume) resumeStore.clearAll() }
    }

    fun importM3u(uri: Uri, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val cr = getApplication<Application>().contentResolver
                val nameHint = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported"
                val parsed = cr.openInputStream(uri)?.use { M3uPlaylistIo.parse(it, nameHint) } ?: run { onDone("Could not read file"); return@launch }
                if (parsed.items.isEmpty()) { onDone("No entries found in M3U"); return@launch }
                val pl = playlistStore.importParsed(parsed)
                _state.update { it.copy(tab = LibraryTab.PLAYLISTS, selectedPlaylist = pl) }
                onDone("Imported \u201c${pl.name}\u201d (${pl.items.size} items)")
            } catch (e: Exception) { onDone(e.message ?: "Import failed") }
        }
    }

    fun exportM3u(playlistId: String, uri: Uri, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val pl = _state.value.playlists.find { it.id == playlistId } ?: _state.value.selectedPlaylist ?: run { onDone("Playlist not found"); return@launch }
                val cr = getApplication<Application>().contentResolver
                cr.openOutputStream(uri)?.use { M3uPlaylistIo.write(it, pl) } ?: run { onDone("Could not write file"); return@launch }
                onDone("Exported \u201c${pl.name}\u201d")
            } catch (e: Exception) { onDone(e.message ?: "Export failed") }
        }
    }

    fun saveStream(url: String, name: String?) { viewModelScope.launch { savedStreamsStore.save(url, name) } }
    fun renameStream(id: String, name: String) { viewModelScope.launch { savedStreamsStore.rename(id, name) } }
    fun removeStream(id: String) { viewModelScope.launch { savedStreamsStore.remove(id) } }
    fun beginSelection(item: ForgeMediaItem) { _state.update { it.copy(selecting = true, selectedKeys = setOf(item.stableKey())) } }
    fun toggleSelected(item: ForgeMediaItem) {
        _state.update { st ->
            if (!st.selecting) st.copy(selecting = true, selectedKeys = setOf(item.stableKey()))
            else {
                val next = st.selectedKeys.toMutableSet()
                val key = item.stableKey()
                if (!next.add(key)) next.remove(key)
                st.copy(selecting = next.isNotEmpty(), selectedKeys = next)
            }
        }
    }
    fun clearSelection() { _state.update { it.copy(selecting = false, selectedKeys = emptySet()) } }
    fun selectedItems(): List<ForgeMediaItem> {
        val st = _state.value
        val pool = if (st.tab == LibraryTab.BROWSE && st.selectedFolder != null) st.folderItems else st.filtered
        return pool.filter { it.stableKey() in st.selectedKeys }
    }
    fun favoriteSelected() {
        val items = selectedItems()
        viewModelScope.launch { favoritesStore.addAll(items) }
        clearSelection()
    }
    fun addSelectedToPlaylist(playlistId: String) {
        val items = selectedItems()
        viewModelScope.launch { playlistStore.addItems(playlistId, items) }
        clearSelection()
    }
    fun hideFolder(folder: MediaFolder) {
        viewModelScope.launch {
            hiddenStore.hide(folder)
            _state.update {
                if (it.selectedFolder?.bucketId == folder.bucketId) it.copy(selectedFolder = null, folderItems = emptyList()) else it
            }
        }
    }
    fun unhideFolder(bucketId: Long) { viewModelScope.launch { hiddenStore.unhide(bucketId) } }
    fun addSafFolder(uri: Uri, name: String?) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            val label = name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Folder"
            safStore.add(uri, label)
        }
    }
    fun removeSafFolder(uri: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try { app.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            safStore.remove(uri)
        }
    }
    fun setWatchedFilter(filter: WatchedFilter) {
        _state.update { st ->
            st.copy(
                watchedFilter = filter,
                filtered = applyFilterAndSort(st.items, st.filter, st.sort, st.watchedKeys, filter, st.minClipSeconds),
                folderItems = filterFolderItems(st.folderItemsAll, st.query, st.watchedKeys, filter, st.minClipSeconds),
            )
        }
    }
    fun toggleWatched(item: ForgeMediaItem) { viewModelScope.launch { watchedStore.toggle(item.uri.toString()) } }
    fun markWatched(uri: String) { viewModelScope.launch { watchedStore.markWatched(uri) } }

    private fun applyFilterAndSort(
        items: List<ForgeMediaItem>,
        filter: LibraryFilter,
        sort: LibrarySort,
        watchedKeys: Set<String> = _state.value.watchedKeys,
        watchedFilter: WatchedFilter = _state.value.watchedFilter,
        minClipSeconds: Int = _state.value.minClipSeconds,
    ): List<ForgeMediaItem> {
        var filtered = when (filter) {
            LibraryFilter.ALL -> items
            LibraryFilter.VIDEO -> items.filter { it.kind == MediaKind.VIDEO }
            LibraryFilter.AUDIO -> items.filter { it.kind == MediaKind.AUDIO }
        }
        if (minClipSeconds > 0) {
            val minMs = minClipSeconds * 1000L
            filtered = filtered.filter { !it.isVideo || it.durationMs <= 0L || it.durationMs >= minMs }
        }
        filtered = when (watchedFilter) {
            WatchedFilter.ALL -> filtered
            WatchedFilter.WATCHED -> filtered.filter { watchedStore.isWatched(watchedKeys, it.uri.toString()) }
            WatchedFilter.UNWATCHED -> filtered.filter { !watchedStore.isWatched(watchedKeys, it.uri.toString()) }
        }
        return when (sort) {
            LibrarySort.NAME -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.DATE -> filtered.sortedByDescending { it.dateAdded }
            LibrarySort.SIZE -> filtered.sortedByDescending { it.sizeBytes }
            LibrarySort.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }
}
