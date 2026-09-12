package com.gketch.forge.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaKind
import com.gketch.forge.data.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter { ALL, VIDEO, AUDIO }

data class LibraryUiState(
    val items: List<ForgeMediaItem> = emptyList(),
    val filtered: List<ForgeMediaItem> = emptyList(),
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val loading: Boolean = true,
    val error: String? = null,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MediaRepository(application)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val items = repo.loadLibrary(_state.value.query)
                _state.update {
                    it.copy(
                        items = items,
                        filtered = applyFilter(items, it.filter),
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
            it.copy(filter = filter, filtered = applyFilter(it.items, filter))
        }
    }

    private fun applyFilter(items: List<ForgeMediaItem>, filter: LibraryFilter): List<ForgeMediaItem> {
        return when (filter) {
            LibraryFilter.ALL -> items
            LibraryFilter.VIDEO -> items.filter { it.kind == MediaKind.VIDEO }
            LibraryFilter.AUDIO -> items.filter { it.kind == MediaKind.AUDIO }
        }
    }
}
