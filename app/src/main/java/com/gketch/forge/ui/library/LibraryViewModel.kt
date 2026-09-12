package com.gketch.forge.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.FavoritesStore
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.ForgePlaylist
import com.gketch.forge.data.LibrarySort
import com.gketch.forge.data.MediaFolder
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.data.M3uPlaylistIo
import com.gketch.forge.data.PlaylistStore
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeStore
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
enum class LibraryTab { LIBRARY, FOLDERS, PLAYLISTS, STREAMS }
enum class LibraryLayout { GRID, LIST }

data class LibraryUiState(
    val items: List<ForgeMediaItem> = emptyList(),
    val filtered: List<ForgeMediaItem> = emptyList(),
    val recent: List<ForgeMediaItem> = emptyList(),
    val favorites: List<ForgeMediaItem> = emptyList(),
    val favoriteUris: Set<String> = emptySet(),
    val folders: List<MediaFolder> = emptyList(),
    val folderItems: List<ForgeMediaItem> = emptyList(),
    val selectedFolder: MediaFolder? = null,
    val playlists: List<ForgePlaylist> = emptyList(),
    val selectedPlaylist: ForgePlaylist? = null,
    val savedStreams: List<SavedStream> = emptyList(),
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.NAME,
    val tab: LibraryTab = LibraryTab.LIBRARY,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val loading: Boolean = true,
    val error: String? = null,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MediaRepository(application)
    private val recentStore = RecentStore(application)
    private val resumeStore = ResumeStore(application)
    private val favoritesStore = FavoritesStore(application)
    private val playlistStore = PlaylistStore(application)
    private val savedStreamsStore = SavedStreamsStore(application)
    private val settingsStore = AppSettingsStore(application)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            recentStore.recent.collect { items ->
                _state.update { it.copy(recent = items) }
            }
        }
        viewModelScope.launch {
            favoritesStore.favorites.collect { items ->
                _state.update {
                    it.copy(
                        favorites = items,
                        favoriteUris = items.map { f -> f.uri.toString() }.toSet(),
                    )
                }
            }
        }
        viewModelScope.launch {
            playlistStore.playlists.collect { list ->
                _state.update { st ->
                    val selected = st.selectedPlaylist?.let { sel ->
                        list.find { it.id == sel.id }
                    }
                    st.copy(playlists = list, selectedPlaylist = selected)
                }
            }
        }
        viewModelScope.launch {
            savedStreamsStore.streams.collect { list ->
                _state.update { it.copy(savedStreams = list) }
            }
        }
        viewModelScope.launch {
            settingsStore.settings.collect { prefs ->
                _state.update {
                    it.copy(
                        sort = prefs.librarySort,
                        filtered = applyFilterAndSort(it.items, it.filter, prefs.librarySort),
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val items = repo.loadLibrary(_state.value.query)
                val folders = repo.loadFolders()
                _state.update {
                    val folderItems = it.selectedFolder?.let { folder ->
                        items.filter { m -> m.bucketId == folder.bucketId }
                            .sortedBy { m -> m.title.lowercase() }
                    }.orEmpty()
                    it.copy(
                        items = items,
                        filtered = applyFilterAndSort(items, it.filter, it.sort),
                        folders = folders,
                        folderItems = folderItems,
                        loading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Failed to load media")
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            refresh()
        }
    }

    fun setFilter(filter: LibraryFilter) {
        _state.update {
            it.copy(filter = filter, filtered = applyFilterAndSort(it.items, filter, it.sort))
        }
    }

    fun setSort(sort: LibrarySort) {
        viewModelScope.launch { settingsStore.setLibrarySort(sort) }
        _state.update {
            it.copy(sort = sort, filtered = applyFilterAndSort(it.items, it.filter, sort))
        }
    }

    fun setTab(tab: LibraryTab) {
        _state.update {
            it.copy(
                tab = tab,
                selectedFolder = if (tab != LibraryTab.FOLDERS) null else it.selectedFolder,
                selectedPlaylist = if (tab != LibraryTab.PLAYLISTS) null else it.selectedPlaylist,
                folderItems = if (tab != LibraryTab.FOLDERS) emptyList() else it.folderItems,
            )
        }
    }

    fun setLayout(layout: LibraryLayout) {
        _state.update { it.copy(layout = layout) }
    }

    fun openFolder(folder: MediaFolder) {
        viewModelScope.launch {
            val items = repo.loadFolderItems(folder.bucketId)
            _state.update {
                it.copy(selectedFolder = folder, folderItems = items, tab = LibraryTab.FOLDERS)
            }
        }
    }

    fun closeFolder() {
        _state.update { it.copy(selectedFolder = null, folderItems = emptyList()) }
    }

    fun openPlaylist(playlist: ForgePlaylist) {
        _state.update { it.copy(selectedPlaylist = playlist, tab = LibraryTab.PLAYLISTS) }
    }

    fun closePlaylist() {
        _state.update { it.copy(selectedPlaylist = null) }
    }

    fun toggleFavorite(item: ForgeMediaItem) {
        viewModelScope.launch { favoritesStore.toggle(item) }
    }

    fun isFavorite(uri: Uri): Boolean =
        _state.value.favoriteUris.contains(uri.toString())

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistStore.create(name) }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { playlistStore.rename(id, name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            playlistStore.delete(id)
            _state.update {
                if (it.selectedPlaylist?.id == id) it.copy(selectedPlaylist = null) else it
            }
        }
    }

    fun addToPlaylist(playlistId: String, item: ForgeMediaItem) {
        viewModelScope.launch { playlistStore.addItem(playlistId, item) }
    }

    fun removeFromPlaylist(playlistId: String, uri: Uri) {
        viewModelScope.launch { playlistStore.removeItem(playlistId, uri) }
    }

    fun movePlaylistItem(playlistId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { playlistStore.moveItem(playlistId, fromIndex, toIndex) }
    }

    fun clearHistory(alsoResume: Boolean) {
        viewModelScope.launch {
            recentStore.clear()
            if (alsoResume) resumeStore.clearAll()
        }
    }

    fun importM3u(uri: Uri, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val cr = getApplication<Application>().contentResolver
                val nameHint = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                    ?: "Imported"
                val parsed = cr.openInputStream(uri)?.use { M3uPlaylistIo.parse(it, nameHint) }
                    ?: run {
                        onDone("Could not read file")
                        return@launch
                    }
                if (parsed.items.isEmpty()) {
                    onDone("No entries found in M3U")
                    return@launch
                }
                val pl = playlistStore.importParsed(parsed)
                _state.update { it.copy(tab = LibraryTab.PLAYLISTS, selectedPlaylist = pl) }
                onDone("Imported “${pl.name}” (${pl.items.size} items)")
            } catch (e: Exception) {
                onDone(e.message ?: "Import failed")
            }
        }
    }

    fun exportM3u(playlistId: String, uri: Uri, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val pl = _state.value.playlists.find { it.id == playlistId }
                    ?: _state.value.selectedPlaylist
                    ?: run {
                        onDone("Playlist not found")
                        return@launch
                    }
                val cr = getApplication<Application>().contentResolver
                cr.openOutputStream(uri)?.use { M3uPlaylistIo.write(it, pl) }
                    ?: run {
                        onDone("Could not write file")
                        return@launch
                    }
                onDone("Exported “${pl.name}”")
            } catch (e: Exception) {
                onDone(e.message ?: "Export failed")
            }
        }
    }

    fun saveStream(url: String, name: String?) {
        viewModelScope.launch { savedStreamsStore.save(url, name) }
    }

    fun renameStream(id: String, name: String) {
        viewModelScope.launch { savedStreamsStore.rename(id, name) }
    }

    fun removeStream(id: String) {
        viewModelScope.launch { savedStreamsStore.remove(id) }
    }

    private fun applyFilterAndSort(
        items: List<ForgeMediaItem>,
        filter: LibraryFilter,
        sort: LibrarySort,
    ): List<ForgeMediaItem> {
        val filtered = when (filter) {
            LibraryFilter.ALL -> items
            LibraryFilter.VIDEO -> items.filter { it.kind == MediaKind.VIDEO }
            LibraryFilter.AUDIO -> items.filter { it.kind == MediaKind.AUDIO }
        }
        return when (sort) {
            LibrarySort.NAME -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.DATE -> filtered.sortedByDescending { it.dateAdded }
            LibrarySort.SIZE -> filtered.sortedByDescending { it.sizeBytes }
            LibrarySort.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }
}
