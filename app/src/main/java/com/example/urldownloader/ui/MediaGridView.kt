package com.example.urldownloader.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.urldownloader.download.Downloader
import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType
import com.example.urldownloader.ui.theme.AppColors

// ── Grid container ────────────────────────────────────────────────────────────
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    selectedUrls: Set<String>,
    onToggleSelect: (String) -> Unit
) {
    LazyVerticalGrid(
        columns              = GridCells.Adaptive(minSize = 152.dp),
        verticalArrangement  = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding       = PaddingValues(bottom = 96.dp)  // space for FAB
    ) {
        items(items, key = { it.url }) { media ->
            GridCard(
                media         = media,
                isSelected    = media.url in selectedUrls,
                onToggleSelect = { onToggleSelect(media.url) }
            )
        }
    }
}

// ── Individual grid card ──────────────────────────────────────────────────────
@Composable
private fun GridCard(
    media: MediaItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    val context = LocalContext.current
    val accent  = gridAccent(media.type)

    val borderColor by animateColorAsState(
        if (isSelected) AppColors.Purple else Color.Transparent,
        tween(150), label = "border"
    )
    val overlayAlpha by animateColorAsState(
        if (isSelected) AppColors.Purple.copy(.18f) else Color.Transparent,
        tween(150), label = "overlay"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggleSelect() }
    ) {
        // ── Thumbnail (image / GIF / thumbnailUrl for video) ──────────────────
        val thumbUrl: String? = media.thumbnailUrl
            ?: when (media.type) {
                MediaType.IMAGE, MediaType.GIF -> media.url
                else -> null
            }

        if (thumbUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier     = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Non-visual content — coloured icon background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(.22f), accent.copy(.07f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    gridIcon(media.type), null,
                    tint     = accent,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // Selection tint overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayAlpha)
        )

        // Bottom gradient + title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.75f)))
                )
        )
        if (media.title.isNotEmpty()) {
            Text(
                text     = media.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 7.dp, end = 28.dp, bottom = 6.dp),
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Type badge — top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .background(accent.copy(.88f), RoundedCornerShape(5.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                gridTypeLabel(media.type),
                style      = MaterialTheme.typography.labelSmall,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 9.sp
            )
        }

        // Platform badge if present — below type badge
        if (!media.platform.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 26.dp, end = 5.dp)
                    .background(Color.Black.copy(.65f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    platformEmoji(media.platform),
                    style  = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color  = Color.White
                )
            }
        }

        // Selection circle — top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(24.dp)
                .background(
                    if (isSelected) AppColors.Purple else Color.Black.copy(.40f),
                    CircleShape
                )
                .border(1.5.dp, if (isSelected) AppColors.Purple else Color.White.copy(.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, null,
                    tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // Download button — bottom right (visible on non-selected state)
        if (!isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(28.dp)
                    .background(accent.copy(.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick  = { Downloader.download(context, media) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.Download, null,
                        tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun gridAccent(type: MediaType): Color = when (type) {
    MediaType.IMAGE    -> Color(0xFF43A047)
    MediaType.GIF      -> Color(0xFFAB47BC)
    MediaType.VIDEO    -> Color(0xFF1E88E5)
    MediaType.AUDIO    -> Color(0xFF00ACC1)
    MediaType.PDF      -> Color(0xFFE53935)
    MediaType.DOCUMENT -> Color(0xFF1976D2)
    MediaType.ARCHIVE  -> Color(0xFFF57C00)
    MediaType.SUBTITLE -> Color(0xFF00897B)
    MediaType.EBOOK    -> Color(0xFF6D4C41)
    MediaType.UNKNOWN  -> Color(0xFF607D8B)
}

private fun gridIcon(type: MediaType): ImageVector = when (type) {
    MediaType.IMAGE, MediaType.GIF -> Icons.Default.Image
    MediaType.VIDEO                -> Icons.Default.PlayCircle
    MediaType.AUDIO                -> Icons.Default.MusicNote
    MediaType.PDF                  -> Icons.Default.PictureAsPdf
    MediaType.DOCUMENT             -> Icons.Default.Description
    MediaType.ARCHIVE              -> Icons.Default.FolderZip
    MediaType.SUBTITLE             -> Icons.Default.ClosedCaption
    MediaType.EBOOK                -> Icons.Default.MenuBook
    MediaType.UNKNOWN              -> Icons.Default.InsertDriveFile
}

private fun gridTypeLabel(type: MediaType): String = when (type) {
    MediaType.IMAGE    -> "IMG"
    MediaType.GIF      -> "GIF"
    MediaType.VIDEO    -> "VID"
    MediaType.AUDIO    -> "AUD"
    MediaType.PDF      -> "PDF"
    MediaType.DOCUMENT -> "DOC"
    MediaType.ARCHIVE  -> "ZIP"
    MediaType.SUBTITLE -> "SUB"
    MediaType.EBOOK    -> "EPUB"
    MediaType.UNKNOWN  -> "FILE"
}

private fun platformEmoji(platform: String?): String = when (platform) {
    "youtube"     -> "▶ YouTube"
    "reddit"      -> "Reddit"
    "googledrive" -> "Drive"
    "imgur"       -> "Imgur"
    "giphy"       -> "Giphy"
    "vimeo"       -> "Vimeo"
    "twitter"     -> "Twitter"
    "instagram"   -> "IG"
    "facebook"    -> "FB"
    "tiktok"      -> "TikTok"
    else          -> platform ?: ""
}
