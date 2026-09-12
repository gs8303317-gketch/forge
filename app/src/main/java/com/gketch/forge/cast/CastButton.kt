package com.gketch.forge.cast

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.R as AppCompatR
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Cast MediaRoute entry. Uses an AppCompat-themed context so MediaRouteButton
 * never crashes the platform Material activity theme. Any failure hides the button.
 */
@Composable
fun ForgeCastButton(modifier: Modifier = Modifier) {
    // Snapshot once; if setup fails we flip local state so this composition drops the view.
    var enabled by remember { mutableStateOf(ForgeCast.available) }
    if (!enabled || !ForgeCast.available) return

    AndroidView(
        modifier = modifier.size(40.dp),
        factory = { context: Context ->
            try {
                val themed = ContextThemeWrapper(context, AppCompatR.style.Theme_AppCompat_NoActionBar)
                val button = MediaRouteButton(themed)
                button.contentDescription = "Cast"
                try {
                    CastButtonFactory.setUpMediaRouteButton(context.applicationContext, button)
                } catch (t: Throwable) {
                    ForgeCast.markUnavailable("setUpMediaRouteButton: ${t.javaClass.simpleName}")
                    button.visibility = View.GONE
                    // Defer compose state update to next frame via view post
                    button.post { enabled = false }
                }
                button
            } catch (t: Throwable) {
                ForgeCast.markUnavailable("MediaRouteButton: ${t.javaClass.simpleName}")
                View(context).also { v -> v.post { enabled = false } }
            }
        },
    )
}
