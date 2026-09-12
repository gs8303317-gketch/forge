package com.gketch.forge.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gketch.forge.MainActivity
import com.gketch.forge.R

class PlaybackWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val title = PlaybackWidgetState.title(context)
        val playing = PlaybackWidgetState.isPlaying(context)
        provideContent {
            GlanceTheme {
                WidgetContent(title = title, playing = playing)
            }
        }
    }
}

@Composable
private fun WidgetContent(title: String, playing: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Forge",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            )
            Text(
                text = title.ifBlank { "Nothing playing" },
                style = TextStyle(fontSize = 14.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Image(
            provider = ImageProvider(
                if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            ),
            contentDescription = if (playing) "Pause" else "Play",
            modifier = GlanceModifier.clickable(actionRunCallback<TogglePlayAction>()),
        )
    }
}

class TogglePlayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlaybackWidgetUpdater.toggle(context)
        PlaybackWidget().update(context, glanceId)
    }
}

class PlaybackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlaybackWidget()
}
