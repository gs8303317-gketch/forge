package com.gketch.forge.ui.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gketch.forge.R
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.PinLockState
import com.gketch.forge.data.PinLockStore
import com.gketch.forge.data.forgeItemFromUri
import com.gketch.forge.ui.library.HistoryScreen
import com.gketch.forge.ui.library.LibraryScreen
import com.gketch.forge.ui.lock.PinLockGate
import com.gketch.forge.ui.permissions.PermissionScreen
import com.gketch.forge.ui.permissions.hasMediaPermission
import com.gketch.forge.ui.player.PlayerScreen
import com.gketch.forge.ui.settings.SettingsScreen

object Routes {
    const val PERMISSION = "permission"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val PLAYER = "player"
}

data class PlaybackSession(
    val queue: List<ForgeMediaItem>,
    val startIndex: Int,
)

@Composable
fun ForgeNav(
    externalUri: Uri? = null,
    externalMime: String? = null,
    onExternalConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var session by remember { mutableStateOf<PlaybackSession?>(null) }
    val start = if (hasMediaPermission(context)) Routes.LIBRARY else Routes.PERMISSION

    val pinStore = remember { PinLockStore(context) }
    val pinState by pinStore.state.collectAsState(initial = PinLockState())
    var sessionUnlocked by remember { mutableStateOf(false) }
    var pendingSettings by remember { mutableStateOf(false) }
    var pendingPlaylists by remember { mutableStateOf(false) }
    var openPlaylistsTick by remember { mutableStateOf(0) }

    val blocking = pinState.enabled && !sessionUnlocked

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val canPopNav = navBackStackEntry != null && navController.previousBackStackEntry != null
    BackHandler(enabled = canPopNav && !blocking) {
        navController.popBackStack()
    }

    fun openQueue(items: List<ForgeMediaItem>, index: Int) {
        session = PlaybackSession(items, index)
        navController.navigate(Routes.PLAYER) {
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(externalUri, blocking) {
        val uri = externalUri ?: return@LaunchedEffect
        if (blocking) return@LaunchedEffect
        val mime = externalMime ?: context.contentResolver.getType(uri)
        openQueue(listOf(forgeItemFromUri(uri, mime = mime)), 0)
        onExternalConsumed()
    }

    // Cold start: full-screen lock before any library UI
    if (blocking && !pendingSettings && !pendingPlaylists) {
        PinLockGate(
            store = pinStore,
            biometricEnabled = pinState.biometricEnabled,
            onUnlocked = { sessionUnlocked = true },
        )
        return
    }

    // Soft lock for settings / playlists when session not yet unlocked
    // (also covers case where we somehow land here without cold-start unlock)
    if (blocking && (pendingSettings || pendingPlaylists)) {
        PinLockGate(
            store = pinStore,
            biometricEnabled = pinState.biometricEnabled,
            onUnlocked = {
                val wantSettings = pendingSettings
                val wantPlaylists = pendingPlaylists
                pendingSettings = false
                pendingPlaylists = false
                sessionUnlocked = true
                if (wantSettings) {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                }
                if (wantPlaylists) {
                    openPlaylistsTick += 1
                }
            },
            title = stringResource(R.string.pin_unlock_protected),
        )
        return
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.PERMISSION) {
            PermissionScreen(
                onGranted = {
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onPlay = { items, index -> openQueue(items, index) },
                onOpenSettings = {
                    if (pinState.enabled && !sessionUnlocked) {
                        pendingSettings = true
                    } else {
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                    }
                },
                onOpenHistory = { navController.navigate(Routes.HISTORY) { launchSingleTop = true } },
                onRequestPermission = { navController.navigate(Routes.PERMISSION) },
                onExpandPlayer = {
                    if (session != null) {
                        navController.navigate(Routes.PLAYER) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                pinLockEnabled = pinState.enabled,
                onGatePlaylists = {
                    if (pinState.enabled && !sessionUnlocked) {
                        pendingPlaylists = true
                        true
                    } else false
                },
                openPlaylistsTick = openPlaylistsTick,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onPlay = { item -> openQueue(listOf(item), 0) },
            )
        }
        composable(Routes.PLAYER) {
            val current = session
            if (current == null || current.queue.isEmpty()) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                PlayerScreen(
                    queue = current.queue,
                    startIndex = current.startIndex,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
