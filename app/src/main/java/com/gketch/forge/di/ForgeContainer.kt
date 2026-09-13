package com.gketch.forge.di

import android.content.Context
import com.gketch.forge.data.AppSettingsStore
import com.gketch.forge.data.BookmarkStore
import com.gketch.forge.data.BrightnessStore
import com.gketch.forge.data.FavoritesStore
import com.gketch.forge.data.HiddenFoldersStore
import com.gketch.forge.data.MediaRepository
import com.gketch.forge.data.PinLockStore
import com.gketch.forge.data.PlaylistStore
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.data.SafFoldersStore
import com.gketch.forge.data.SavedStreamsStore
import com.gketch.forge.data.TrackPrefsStore
import com.gketch.forge.data.WatchedStore
import com.gketch.forge.player.ForgePlayerPrefsStore

/**
 * Thin service locator for stores/repos. Existing call sites may keep
 * constructing stores directly; this is the tidy singleton entry for DI.
 */
class ForgeContainer(context: Context) {
    private val app = context.applicationContext

    val appSettings by lazy { AppSettingsStore(app) }
    val mediaRepository by lazy { MediaRepository(app) }
    val resume by lazy { ResumeStore(app) }
    val recent by lazy { RecentStore(app) }
    val favorites by lazy { FavoritesStore(app) }
    val playlists by lazy { PlaylistStore(app) }
    val bookmarks by lazy { BookmarkStore(app) }
    val brightness by lazy { BrightnessStore(app) }
    val hiddenFolders by lazy { HiddenFoldersStore(app) }
    val safFolders by lazy { SafFoldersStore(app) }
    val savedStreams by lazy { SavedStreamsStore(app) }
    val pinLock by lazy { PinLockStore(app) }
    val trackPrefs by lazy { TrackPrefsStore(app) }
    val watched by lazy { WatchedStore(app) }
    val playerPrefs by lazy { ForgePlayerPrefsStore(app) }

    companion object {
        @Volatile private var instance: ForgeContainer? = null

        fun get(context: Context): ForgeContainer {
            return instance ?: synchronized(this) {
                instance ?: ForgeContainer(context).also { instance = it }
            }
        }
    }
}
