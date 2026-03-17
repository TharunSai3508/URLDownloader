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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
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
import okhttp3.OkHttpClient

// ── Per-type accent colour ────────────────────────────────────────────────────
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

private fun typeLabel(type: MediaType) = when (type) {
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

// Shared OkHttpClient for ExoPlayer data source — reuse connections
private val sharedOkHttp = OkHttpClient.Builder()
    .followRedirects(true)
    .addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                .build()
        )
    }
    .build()

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
            // Coloured accent strip at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(.35f))))
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

                // Content preview — all types auto-play where applicable
                when (media.type) {
                    MediaType.IMAGE    -> ImagePreview(media.url)
                    MediaType.GIF      -> GifPreview(media.url)
                    MediaType.VIDEO    -> VideoPreview(media.url)
                    MediaType.AUDIO    -> AudioPreview(media.url, accent)
                    else               -> FilePreview(typeIcon(media.type), typeLabel(media.type), accent, media.url)
                }

                Spacer(Modifier.height(10.dp))

                // URL line
                Text(
                    media.url,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // Accent-gradient download button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(.65f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick    = { Downloader.download(context, media) },
                        modifier   = Modifier.fillMaxSize(),
                        colors     = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        elevation  = ButtonDefaults.buttonElevation(0.dp, 0.dp)
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

// ── Badge ─────────────────────────────────────────────────────────────────────
@Composable
private fun TypeBadge(type: MediaType, accent: Color) {
    Row(
        modifier = Modifier
            .background(accent.copy(.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(typeIcon(type), null, tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(typeLabel(type), style = MaterialTheme.typography.labelSmall,
            color = accent, fontWeight = FontWeight.Bold)
    }
}

// ── IMAGE preview ─────────────────────────────────────────────────────────────
@Composable
private fun ImagePreview(url: String) {
    val context  = LocalContext.current
    var hasError by remember(url) { mutableStateOf(false) }
    var loading  by remember(url) { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 300.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!hasError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .listener(
                        onSuccess = { _, _ -> loading = false },
                        onError   = { _, _ -> hasError = true; loading = false }
                    )
                    .build(),
                contentDescription = null,
                modifier     = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        if (loading && !hasError) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color    = typeAccent(MediaType.IMAGE)
            )
        }
        if (hasError) {
            FilePreview(Icons.Default.BrokenImage, "Image unavailable", AppColors.Green, url)
        }
    }
}

// ── GIF preview — animated via Coil ImageDecoder / GifDecoder ────────────────
@Composable
private fun GifPreview(url: String) {
    val context  = LocalContext.current
    var hasError by remember(url) { mutableStateOf(false) }
    var loading  by remember(url) { mutableStateOf(true) }

    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else                             add(GifDecoder.Factory())
            }
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 300.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!hasError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(false)  // must be false — crossfade breaks GIF frames
                    .listener(
                        onSuccess = { _, _ -> loading = false },
                        onError   = { _, _ -> hasError = true; loading = false }
                    )
                    .build(),
                contentDescription = null,
                imageLoader  = gifLoader,
                modifier     = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        if (loading && !hasError) {
            CircularProgressIndicator(Modifier.size(32.dp), color = typeAccent(MediaType.GIF))
        }
        if (hasError) {
            FilePreview(Icons.Default.BrokenImage, "GIF unavailable", AppColors.Purple, url)
        }
    }
}

// ── VIDEO preview — ExoPlayer with auto-play (muted), HLS + DASH + MP4 ───────
@Composable
private fun VideoPreview(url: String) {
    val context   = LocalContext.current
    var hasError  by remember(url) { mutableStateOf(false) }
    var isMuted   by remember(url) { mutableStateOf(true) }

    // Build ExoPlayer with OkHttp data source for better HTTP/HTTPS compat
    val exoPlayer = remember(url) {
        val dsFactory = OkHttpDataSource.Factory(sharedOkHttp)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory))
            .build()
            .apply {
                setMediaItem(ExoMediaItem.fromUri(url))
                // Audio focus: don't request focus when muted auto-playing
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */ false
                )
                volume        = 0f   // start muted (social-media style auto-play)
                repeatMode    = Player.REPEAT_MODE_ONE
                playWhenReady = true // ← AUTO-PLAY
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { hasError = true }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
    }

    if (hasError) {
        FilePreview(Icons.Default.PlayCircle, "Video (preview unavailable)", AppColors.Blue, url)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player          = exoPlayer
                    useController   = true
                    resizeMode      = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Mute / unmute floating button (top-right corner)
        IconButton(
            onClick = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .background(Color.Black.copy(.55f), CircleShape)
        ) {
            Icon(
                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint     = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

// ── AUDIO preview — pure Compose player, auto-plays, no AndroidView ──────────
@Composable
private fun AudioPreview(url: String, accent: Color) {
    val context    = LocalContext.current

    var isPlaying  by remember(url) { mutableStateOf(false) }
    var progress   by remember(url) { mutableStateOf(0f) }
    var durationMs by remember(url) { mutableStateOf(0L) }
    var hasError   by remember(url) { mutableStateOf(false) }
    var isReady    by remember(url) { mutableStateOf(false) }

    val exoPlayer = remember(url) {
        val dsFactory = OkHttpDataSource.Factory(sharedOkHttp)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory))
            .build()
            .apply {
                setMediaItem(ExoMediaItem.fromUri(url))
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */ true
                )
                playWhenReady = true // ← AUTO-PLAY
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean)  { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) { hasError = true }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isReady    = true
                    durationMs = maxOf(exoPlayer.duration, 1L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
    }

    // Poll position every 200 ms while playing
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

    // ── Audio player card ─────────────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(accent.copy(.18f), accent.copy(.06f))),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause button
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    Brush.radialGradient(listOf(accent, accent.copy(.6f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isReady) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(26.dp),
                    color       = Color.White,
                    strokeWidth = 2.5.dp,
                    trackColor  = Color.Transparent
                )
            } else {
                IconButton(
                    onClick  = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            // Animated waveform when playing, seeker when paused
            if (isPlaying) {
                AnimatedWaveform(accent)
            } else {
                Slider(
                    value    = progress,
                    onValueChange = { p ->
                        progress = p
                        exoPlayer.seekTo((p * durationMs).toLong())
                    },
                    colors   = SliderDefaults.colors(
                        thumbColor       = accent,
                        activeTrackColor = accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }
            // Time labels
            Row(Modifier.fillMaxWidth()) {
                Text(fmtMs((progress * durationMs).toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.weight(1f))
                Text(fmtMs(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// Animated equalizer bars — pure Compose, no AndroidView
@Composable
private fun AnimatedWaveform(accent: Color) {
    val inf = rememberInfiniteTransition(label = "waveform")

    @Composable
    fun bar(ms: Int, delay: Int, lo: Float = 0.15f, hi: Float = 1f) =
        inf.animateFloat(lo, hi,
            infiniteRepeatable(tween(ms, delay, LinearEasing), RepeatMode.Reverse),
            "bar_${ms}_$delay").value

    val bars = listOf(
        bar(380, 0),   bar(260, 50),  bar(440, 110), bar(300, 80),
        bar(500, 20),  bar(240, 95),  bar(420, 40),  bar(340, 160),
        bar(460, 200), bar(290, 55),  bar(410, 130), bar(350, 85),
        bar(470, 10),  bar(280, 70)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        bars.forEach { frac ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(listOf(accent, accent.copy(.3f))))
            )
        }
    }
}

private fun fmtMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

// ── Generic file / error card ─────────────────────────────────────────────────
@Composable
private fun FilePreview(icon: ImageVector, label: String, iconColor: Color, url: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(iconColor.copy(.13f), iconColor.copy(.04f))),
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
                url.substringAfterLast('/').substringBefore('?').take(44),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
