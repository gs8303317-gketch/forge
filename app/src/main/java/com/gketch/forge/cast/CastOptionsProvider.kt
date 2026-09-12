package com.gketch.forge.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

/**
 * Default Cast receiver (media default). Declared in the manifest meta-data.
 * Must never throw — Cast framework may instantiate this during GMS init.
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return try {
            CastOptions.Builder()
                // Default Media Receiver — works without a custom receiver app id.
                .setReceiverApplicationId("CC1AD845")
                .setCastMediaOptions(
                    CastMediaOptions.Builder()
                        .setNotificationOptions(null)
                        .build(),
                )
                .build()
        } catch (_: Throwable) {
            CastOptions.Builder()
                .setReceiverApplicationId("CC1AD845")
                .build()
        }
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
