package com.gketch.forge.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.RecentStore
import com.gketch.forge.data.ResumeStore
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeBlack
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import com.gketch.forge.ui.thumb.ForgeThumbnail
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onPlay: (ForgeMediaItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recentStore = remember { RecentStore(context) }
    val resumeStore = remember { ResumeStore(context) }
    val recent by recentStore.recent.collectAsState(initial = emptyList())
    var clearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBlack),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 0.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "History",
                style = MaterialTheme.typography.titleLarge,
                color = ForgeAccent,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (recent.isNotEmpty()) {
                TextButton(onClick = { clearDialog = true }) {
                    Text("Clear", color = ForgeAccent)
                }
            }
        }
        if (recent.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No playback history yet",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Played media will show up here",
                    color = ForgeMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(recent, key = { it.uri.toString() }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ForgeThumbnail(
                            item = item,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                if (item.isVideo) "Video" else "Audio",
                                color = ForgeMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(
                            onClick = { scope.launch { recentStore.remove(item.uri) } },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = ForgeMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Clear history", color = Color.White) },
            text = {
                Column {
                    Text(
                        "Remove all recently played items? Resume positions can be cleared too.",
                        color = ForgeMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                recentStore.clear()
                                resumeStore.clearAll()
                                clearDialog = false
                            }
                        },
                    ) { Text("Clear recent + resume", color = Color.White) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            recentStore.clear()
                            clearDialog = false
                        }
                    },
                ) { Text("Clear recent", color = ForgeAccent) }
            },
            dismissButton = {
                TextButton(onClick = { clearDialog = false }) {
                    Text("Cancel", color = ForgeMuted)
                }
            },
            containerColor = ForgeGraphite,
        )
    }
}
