package com.example.urldownloader.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import com.example.urldownloader.ui.theme.AppColors
import kotlinx.coroutines.delay

// ── Card accent colour per type ───────────────────────────────────────────────
private fun typeAccent(type: MediaType): Color = when (type) {
    MediaType.IMAGE    -> Color(0xFF43A047)
    MediaType.GIF      -> Color(0xFFAB47BC)
    MediaType.VIDEO    -> Color(0xFF1E88E5)
    MediaType.AUDIO    -> Color(0xFF00ACC1)
    MediaType.PDF      -> Color(0xFFE53935)
    MediaType.DOCUMENT -> Color(0xFF1976D2)
    MediaType.ARCHIVE  -> Color(0xFFF57C00)
    MediaType.UNKNOWN  -> Color(0xFF607D8B)
}

private fun typeLabel(type: MediaType): String = when (type) {
    MediaType.IMAGE    -> "IMAGE"
    MediaType.GIF      -> "GIF"
    MediaType.VIDEO    -> "VIDEO"
    MediaType.AUDIO    -> "AUDIO"
    MediaType.PDF      -> "PDF"
    MediaType.DOCUMENT -> "DOCUMENT"
    MediaType.ARCHIVE  -> "ARCHIVE"
    MediaType.UNKNOWN  -> "FILE"
}

private fun typeIcon(type: MediaType): ImageVector = when (type) {
    MediaType.IMAGE, MediaType.GIF -> Icons.Default.Image
    MediaType.VIDEO                -> Icons.Default.PlayCircle
    MediaType.AUDIO                -> Icons.Default.MusicNote
    MediaType.PDF                  -> Icons.Default.PictureAsPdf
    MediaType.DOCUMENT             -> Icons.Default.Description
    MediaType.ARCHIVE              -> Icons.Default.FolderZip
    MediaType.UNKNOWN              -> Icons.Default.InsertDriveFile
}

// ── Root card ─────────────────────────────────────────────────────────────────
@Composable
fun MediaItemView(media: MediaItem) {
    val context = LocalContext.current
    val accent  = typeAccent(media.type)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Coloured top strip + badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent, accent.copy(alpha = 0.4f))
                        )
                    )
            )

            Column(Modifier.padding(12.dp)) {

                // Badge row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(media.type, accent)
                    if (media.title.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            media.title,
                            style    = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Content preview
                when (media.type) {
                    MediaType.IMAGE                -> ImagePreview(media.url)
                    MediaType.GIF                  -> GifPreview(media.url)
                    MediaType.VIDEO                -> VideoPreview(media.url)
                    MediaType.AUDIO                -> AudioPreview(media.url, accent)
                    MediaType.PDF, MediaType.DOCUMENT,
                    MediaType.ARCHIVE, MediaType.UNKNOWN ->
                        FilePreview(typeIcon(media.type), typeLabel(media.type), accent, media.url)
                }

                Spacer(Modifier.height(10.dp))

                // URL chip
                Text(
                    media.url,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // Download button with accent gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent, accent.copy(alpha = 0.70f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { Downloader.download(context, media) },
                        modifier = Modifier.fillMaxSize(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Type badge ────────────────────────────────────────────────────────────────
@Composable
private fun TypeBadge(type: MediaType, accent: Color) {
    Row(
        modifier = Modifier
            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(typeIcon(type), null, tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            typeLabel(type),
            style       = MaterialTheme.typography.labelSmall,
            color       = accent,
            fontWeight  = FontWeight.Bold
        )
    }
}

// ── Image preview ─────────────────────────────────────────────────────────────
@Composable
private fun ImagePreview(url: String) {
    val context = LocalContext.current
    var hasError by remember(url) { mutableStateOf(false) }

    if (hasError) {
        FilePreview(Icons.Default.BrokenImage, "Image unavailable", AppColors.Green, url)
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .listener(onError = { _, _ -> hasError = true })
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 300.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.FillWidth
    )
}

// ── GIF preview (Coil GIF decoder — API 28+ uses ImageDecoder, older uses Movie) ──
@Composable
private fun GifPreview(url: String) {
    val context     = LocalContext.current
    var hasError by remember(url) { mutableStateOf(false) }

    // Build a Coil ImageLoader that can decode animated GIFs
    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else                             add(GifDecoder.Factory())
            }
            .build()
    }

    if (hasError) {
        FilePreview(Icons.Default.BrokenImage, "GIF unavailable", AppColors.Purple, url)
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(false)          // keep false for GIF so frames don't fade
            .listener(onError = { _, _ -> hasError = true })
            .build(),
        contentDescription = null,
        imageLoader = gifLoader,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 300.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.FillWidth
    )
}

// ── Video preview (ExoPlayer via AndroidView — HLS/MP4/WebM/MKV all supported) ──
@Composable
private fun VideoPreview(url: String) {
    val context      = LocalContext.current
    var hasError by remember(url) { mutableStateOf(false) }

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { hasError = true }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    if (hasError) {
        FilePreview(Icons.Default.PlayCircle, "Video (preview unavailable)", AppColors.Blue, url)
        return
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player        = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

// ── Audio preview — pure Compose player, no AndroidView ──────────────────────
@Composable
private fun AudioPreview(url: String, accent: Color) {
    val context = LocalContext.current

    var isPlaying by remember(url) { mutableStateOf(false) }
    var progress  by remember(url) { mutableStateOf(0f) }
    var durationMs by remember(url) { mutableStateOf(0L) }
    var hasError  by remember(url) { mutableStateOf(false) }
    var isReady   by remember(url) { mutableStateOf(false) }

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) { hasError = true }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isReady    = true
                    durationMs = maxOf(exoPlayer.duration, 1L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll position while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
                progress   = exoPlayer.currentPosition.toFloat() / exoPlayer.duration
            }
            delay(200)
        }
    }

    if (hasError) {
        FilePreview(Icons.Default.MusicOff, "Audio (preview unavailable)", accent, url)
        return
    }

    // ── Audio card UI ─────────────────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause button
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Brush.radialGradient(listOf(accent, accent.copy(.7f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!isReady) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color    = Color.White,
                    strokeWidth = 2.dp,
                    trackColor  = Color.Transparent
                )
            } else {
                IconButton(
                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint   = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            // Waveform (animated when playing) or slider (when paused)
            if (isPlaying) {
                AnimatedWaveform(accent = accent)
            } else {
                Slider(
                    value    = progress,
                    onValueChange = { p ->
                        progress = p
                        exoPlayer.seekTo((p * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor       = accent,
                        activeTrackColor = accent
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
            }

            // Time labels
            Row(Modifier.fillMaxWidth()) {
                Text(
                    formatMs((progress * durationMs).toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatMs(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// Animated waveform bars — pure Compose, shown while audio is playing
@Composable
private fun AnimatedWaveform(accent: Color) {
    val inf = rememberInfiniteTransition(label = "wave")

    @Composable
    fun bar(period: Int, delay: Int, minH: Float = 0.2f, maxH: Float = 1f) =
        inf.animateFloat(
            minH, maxH,
            infiniteRepeatable(tween(period, delay, LinearEasing), RepeatMode.Reverse),
            label = "bar${delay}"
        ).value

    val h = listOf(
        bar(380, 0),  bar(280, 60), bar(440, 120), bar(320, 80),
        bar(500, 20), bar(260, 100), bar(400, 40), bar(340, 160),
        bar(460, 200), bar(300, 50), bar(420, 130), bar(360, 90)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        h.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(listOf(accent, accent.copy(.4f)))
                    )
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

// ── Generic file-type card ────────────────────────────────────────────────────
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
                Brush.horizontalGradient(
                    listOf(iconColor.copy(.12f), iconColor.copy(.04f))
                ),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                url.substringAfterLast('/').substringBefore('?').take(42),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
