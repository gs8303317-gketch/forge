package com.gketch.forge

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.gketch.forge.cast.ForgeCast
import com.gketch.forge.ui.thumb.MediaThumbnailFetcher
import com.gketch.forge.widget.PlaybackWidgetUpdater

class ForgeApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Never crash the process from Application.onCreate — all init is best-effort.
        try {
            ForgeCast.init(this)
        } catch (_: Throwable) {
        }
        try {
            PlaybackWidgetUpdater.init(this)
        } catch (_: Throwable) {
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                // Prefer MediaStore/SAF thumbnail fetcher over Coil's default content decoder
                add(MediaThumbnailFetcher.Factory())
            }
            .crossfade(true)
            .build()
}
