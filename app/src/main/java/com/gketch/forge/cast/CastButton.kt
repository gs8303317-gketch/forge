package com.gketch.forge.cast

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.mediarouter.app.MediaRouteButton
import com.gketch.forge.ui.theme.ForgeAccent
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun ForgeCastButton(modifier: Modifier = Modifier) {
    if (!ForgeCast.available) return
    val tint = ForgeAccent.toArgb()
    AndroidView(
        modifier = modifier.size(40.dp),
        factory = { context: Context ->
            MediaRouteButton(context).apply {
                try {
                    CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
                } catch (_: Throwable) {
                }
                try {
                    val field = MediaRouteButton::class.java.getDeclaredField("mRemoteIndicator")
                    field.isAccessible = true
                    val d = field.get(this) as? Drawable
                    if (d != null) {
                        val wrapped = DrawableCompat.wrap(d.mutate())
                        DrawableCompat.setTint(wrapped, tint)
                        setRemoteIndicatorDrawable(wrapped)
                    }
                } catch (_: Throwable) {
                }
            }
        },
    )
}
