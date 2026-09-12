package com.gketch.forge.ui.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

data class ScrollAnchor(val index: Int = 0, val offset: Int = 0)

@Composable
fun rememberKeyedLazyListState(
    key: String,
    viewModel: LibraryViewModel,
): LazyListState {
    val state = remember(key) {
        val saved = viewModel.listAnchor(key)
        LazyListState(saved.index, saved.offset)
    }
    DisposableEffect(key, state) {
        onDispose {
            viewModel.saveListAnchor(
                key,
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset,
            )
        }
    }
    LaunchedEffect(key, state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> viewModel.saveListAnchor(key, index, offset) }
    }
    return state
}

@Composable
fun rememberKeyedLazyGridState(
    key: String,
    viewModel: LibraryViewModel,
): LazyGridState {
    val state = remember(key) {
        val saved = viewModel.gridAnchor(key)
        LazyGridState(saved.index, saved.offset)
    }
    DisposableEffect(key, state) {
        onDispose {
            viewModel.saveGridAnchor(
                key,
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset,
            )
        }
    }
    LaunchedEffect(key, state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> viewModel.saveGridAnchor(key, index, offset) }
    }
    return state
}
