package com.gketch.forge.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gketch.forge.R
import com.gketch.forge.data.AudioBrowseGroup
import com.gketch.forge.data.AudioBrowseMode
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeGraphite
import com.gketch.forge.ui.theme.ForgeMuted
import com.gketch.forge.ui.theme.ForgeSurfaceVariant
import com.gketch.forge.ui.thumb.ForgeThumbnailUri

@Composable
fun AudioBrowseModeChips(
    mode: AudioBrowseMode,
    onMode: (AudioBrowseMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AudioBrowseMode.entries.forEach { m ->
            FilterChip(
                selected = mode == m,
                onClick = { onMode(m) },
                label = {
                    Text(
                        when (m) {
                            AudioBrowseMode.SONGS -> stringResource(R.string.audio_songs)
                            AudioBrowseMode.ALBUMS -> stringResource(R.string.audio_albums)
                            AudioBrowseMode.ARTISTS -> stringResource(R.string.audio_artists)
                            AudioBrowseMode.GENRES -> stringResource(R.string.audio_genres)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        when (m) {
                            AudioBrowseMode.SONGS -> Icons.Rounded.AudioFile
                            AudioBrowseMode.ALBUMS -> Icons.Rounded.Album
                            AudioBrowseMode.ARTISTS -> Icons.Rounded.Person
                            AudioBrowseMode.GENRES -> Icons.Rounded.Category
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForgeSurfaceVariant,
                    selectedLabelColor = ForgeAccent,
                    selectedLeadingIconColor = ForgeAccent,
                    containerColor = ForgeGraphite,
                    labelColor = ForgeMuted,
                    iconColor = ForgeMuted,
                ),
            )
        }
    }
}

@Composable
fun AudioGroupsList(
    groups: List<AudioBrowseGroup>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onOpen: (AudioBrowseGroup) -> Unit,
) {
    if (groups.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.no_media), color = ForgeMuted)
        }
        return
    }
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(groups, key = { it.key }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(group) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ForgeThumbnailUri(
                    uri = group.thumbUri ?: group.tracks.firstOrNull()?.albumArtUri,
                    isVideo = false,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${group.subtitle} · ${group.count}",
                        color = ForgeMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun AudioGroupTracksHeader(
    group: AudioBrowseGroup,
    mode: AudioBrowseMode,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForgeGraphite)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = false,
                onClick = onBack,
                label = {
                    Text(
                        when (mode) {
                            AudioBrowseMode.SONGS -> stringResource(R.string.audio_songs)
                            AudioBrowseMode.ALBUMS -> stringResource(R.string.audio_albums)
                            AudioBrowseMode.ARTISTS -> stringResource(R.string.audio_artists)
                            AudioBrowseMode.GENRES -> stringResource(R.string.audio_genres)
                        },
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForgeSurfaceVariant,
                    selectedLabelColor = ForgeAccent,
                    containerColor = ForgeGraphite,
                    labelColor = ForgeMuted,
                ),
            )
            Text("›", color = ForgeMuted)
            Text(
                group.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
            Text(
                group.subtitle,
                color = ForgeMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onShuffleAll != null) {
                androidx.compose.material3.TextButton(onClick = onShuffleAll) {
                    Text(stringResource(R.string.shuffle_all), color = ForgeAccent)
                }
            }
            androidx.compose.material3.TextButton(onClick = onPlayAll) {
                Text(stringResource(R.string.action_play), color = ForgeAccent)
            }
        }
    }
}
