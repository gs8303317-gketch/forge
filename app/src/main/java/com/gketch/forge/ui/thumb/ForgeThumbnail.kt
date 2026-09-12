package com.gketch.forge.ui.thumb

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.gketch.forge.data.ForgeMediaItem
import com.gketch.forge.data.MediaKind
import com.gketch.forge.ui.theme.ForgeAccent
import com.gketch.forge.ui.theme.ForgeSurfaceVariant

@Composable
fun ForgeThumbnail(
    item: ForgeMediaItem,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val model = thumbnailModel(item)
    ForgeThumbnailUri(
        uri = model,
        isVideo = item.isVideo,
        modifier = modifier,
        progress = progress,
        contentScale = contentScale,
    )
}

@Composable
fun ForgeThumbnailUri(
    uri: Uri?,
    isVideo: Boolean,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    contentScale: ContentScale = ContentScale.Crop,
    folder: Boolean = false,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ForgeSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        // Grey box already acts as placeholder while loading
                    }
                    is AsyncImagePainter.State.Error,
                    is AsyncImagePainter.State.Empty -> {
                        FallbackIcon(isVideo = isVideo, folder = folder)
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }
        } else {
            FallbackIcon(isVideo = isVideo, folder = folder)
        }
        if (progress != null && progress > 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = ForgeAccent,
                trackColor = Color.Black.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun FallbackIcon(isVideo: Boolean, folder: Boolean) {
    Icon(
        imageVector = when {
            folder -> Icons.Rounded.Folder
            isVideo -> Icons.Rounded.Movie
            else -> Icons.Rounded.AudioFile
        },
        contentDescription = null,
        tint = ForgeAccent,
        modifier = Modifier.size(28.dp),
    )
}

fun thumbnailModel(item: ForgeMediaItem): Uri? {
    return when {
        item.kind == MediaKind.AUDIO -> item.albumArtUri ?: item.uri
        item.albumArtUri != null -> item.albumArtUri
        else -> item.uri
    }
}
