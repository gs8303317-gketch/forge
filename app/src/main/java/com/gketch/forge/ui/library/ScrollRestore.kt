package com.gketch.forge.ui.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

data class ScrollAnchor(val index: Int = 0, val offset: Int = 0)

@Composable
fun rememberKeyedLazyListState(
    key: String,
    viewModel: LibraryViewModel,
): LazyListState {
    val pending = remember(key) { viewModel.listAnchor(key) }
    val state = remember(key) { LazyListState(0, 0) }
    var restored by remember(key) { mutableStateOf(pending.index == 0 && pending.offset == 0) }

    LaunchedEffect(key, state) {
        snapshotFlow { state.layoutInfo.totalItemsCount }
            .collect { count ->
                if (!restored && count > pending.index) {
                    runCatching {
                        state.scrollToItem(
                            pending.index.coerceIn(0, count - 1),
                            pending.offset.coerceAtLeast(0),
                        )
                    }
                    restored = true
                } else if (!restored && count > 0 && pending.index == 0) {
                    restored = true
                }
            }
    }
    LaunchedEffect(key, state) {
        snapshotFlow {
            Triple(
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset,
                state.layoutInfo.totalItemsCount,
            )
        }.collect { (index, offset, count) ->
            if (!restored || count <= 0) return@collect
            viewModel.saveListAnchor(key, index, offset)
        }
    }
    DisposableEffect(key, state) {
        onDispose {
            if (restored || state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0) {
                viewModel.saveListAnchor(
                    key,
                    state.firstVisibleItemIndex,
                    state.firstVisibleItemScrollOffset,
                )
            }
        }
    }
    return state
}

@Composable
fun rememberKeyedLazyGridState(
    key: String,
    viewModel: LibraryViewModel,
): LazyGridState {
    val pending = remember(key) { viewModel.gridAnchor(key) }
    val state = remember(key) { LazyGridState(0, 0) }
    var restored by remember(key) { mutableStateOf(pending.index == 0 && pending.offset == 0) }

    LaunchedEffect(key, state) {
        snapshotFlow { state.layoutInfo.totalItemsCount }
            .collect { count ->
                if (!restored && count > pending.index) {
                    runCatching {
                        state.scrollToItem(
                            pending.index.coerceIn(0, count - 1),
                            pending.offset.coerceAtLeast(0),
                        )
                    }
                    restored = true
                } else if (!restored && count > 0 && pending.index == 0) {
                    restored = true
                }
            }
    }
    LaunchedEffect(key, state) {
        snapshotFlow {
            Triple(
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset,
                state.layoutInfo.totalItemsCount,
            )
        }.collect { (index, offset, count) ->
            if (!restored || count <= 0) return@collect
            viewModel.saveGridAnchor(key, index, offset)
        }
    }
    DisposableEffect(key, state) {
        onDispose {
            if (restored || state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0) {
                viewModel.saveGridAnchor(
                    key,
                    state.firstVisibleItemIndex,
                    state.firstVisibleItemScrollOffset,
                )
            }
        }
    }
    return state
}
