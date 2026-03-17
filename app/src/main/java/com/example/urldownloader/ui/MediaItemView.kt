package com.example.urldownloader.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.urldownloader.download.Downloader
import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType

@Composable
fun MediaItemView(media: MediaItem, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {

            // Type badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(media.type)
                if (media.title.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Content preview
            when (media.type) {
                MediaType.IMAGE -> ImagePreview(url = media.url, context = context)
                MediaType.GIF -> GifPreview(url = media.url, context = context)
                MediaType.VIDEO -> VideoPreview(url = media.url, context = context)
                MediaType.AUDIO -> AudioPreview(url = media.url, context = context)
                MediaType.PDF -> FilePreview(
                    icon = Icons.Default.PictureAsPdf,
                    label = "PDF Document",
                    iconColor = Color(0xFFE53935),
                    url = media.url
                )
                MediaType.DOCUMENT -> FilePreview(
                    icon = Icons.Default.Description,
                    label = "Document",
                    iconColor = Color(0xFF1976D2),
                    url = media.url
                )
                MediaType.ARCHIVE -> FilePreview(
                    icon = Icons.Default.FolderZip,
                    label = "Archive",
                    iconColor = Color(0xFFF57F17),
                    url = media.url
                )
                MediaType.UNKNOWN -> FilePreview(
                    icon = Icons.Default.InsertDriveFile,
                    label = "File",
                    iconColor = MaterialTheme.colorScheme.outline,
                    url = media.url
                )
            }

            Spacer(Modifier.height(10.dp))

            // URL preview (truncated)
            Text(
                text = media.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Download button
            Button(
                onClick = { Downloader.download(context, media) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun TypeBadge(type: MediaType) {
    val (label, color) = when (type) {
        MediaType.IMAGE -> "IMAGE" to Color(0xFF2E7D32)
        MediaType.GIF -> "GIF" to Color(0xFF6A1B9A)
        MediaType.VIDEO -> "VIDEO" to Color(0xFF1565C0)
        MediaType.AUDIO -> "AUDIO" to Color(0xFF00695C)
        MediaType.PDF -> "PDF" to Color(0xFFC62828)
        MediaType.DOCUMENT -> "DOC" to Color(0xFF1976D2)
        MediaType.ARCHIVE -> "ARCHIVE" to Color(0xFFE65100)
        MediaType.UNKNOWN -> "FILE" to Color(0xFF546E7A)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ImagePreview(url: String, context: Context) {
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 280.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun GifPreview(url: String, context: Context) {
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(false)
            .build(),
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 280.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun VideoPreview(url: String, context: Context) {
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
private fun AudioPreview(url: String, context: Context) {
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                controllerShowTimeoutMs = 0
                controllerHideOnTouch = false
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}

@Composable
private fun FilePreview(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    url: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = url.substringAfterLast('/').substringBefore('?').take(40),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
