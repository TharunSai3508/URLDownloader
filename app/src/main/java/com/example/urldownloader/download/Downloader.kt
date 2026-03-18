package com.example.urldownloader.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType

object Downloader {

    private const val UA = "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"

    // Platforms where we only have the page URL — direct download not possible
    private val NO_DIRECT_DOWNLOAD = setOf(
        "youtube", "tiktok", "instagram", "facebook",
        "twitter", "dailymotion", "twitch", "pinterest", "vimeo"
    )

    fun download(context: Context, media: MediaItem) {
        // Block page-URL-only platform videos with a clear message
        if (media.platform in NO_DIRECT_DOWNLOAD && media.type == MediaType.VIDEO
            && media.downloadUrl == null) {
            val name = media.platform?.replaceFirstChar { it.uppercaseChar() } ?: "This platform"
            Toast.makeText(
                context,
                "$name videos cannot be downloaded directly. Use a dedicated downloader app.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Use explicit downloadUrl (e.g. Reddit MP4) or fall back to the media URL
        val targetUrl = media.downloadUrl ?: media.url
        val fileName  = buildFileName(media)
        // Prefer explicit mimeType; otherwise derive from MediaType to ensure correct file handling
        val mime      = media.mimeType ?: mimeFromType(media.type)

        runCatching {
            val request = DownloadManager.Request(Uri.parse(targetUrl))
                .setTitle(fileName)
                .setDescription("Downloading via URL Downloader")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", UA)
                .addRequestHeader("Accept", "*/*")
                .addRequestHeader("Referer", "https://www.google.com/")
                .setDestinationInExternalPublicDir(getDirectory(media.type), fileName)
                .apply { mime?.let { setMimeType(it) } }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, "Downloading: $fileName", Toast.LENGTH_SHORT).show()
        }.onFailure { e ->
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun downloadAll(context: Context, items: List<MediaItem>) {
        items.forEach { download(context, it) }
        if (items.isNotEmpty()) {
            Toast.makeText(context, "Queued ${items.size} download(s)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildFileName(media: MediaItem): String {
        val source = media.downloadUrl ?: media.url
        val titleCandidate = media.title
            .replace(Regex("""[^A-Za-z0-9._ -]"""), "_")
            .trim()
            .takeIf { it.isNotBlank() }
        val raw = Uri.parse(source).lastPathSegment
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?.takeIf { it.isNotBlank() }
            ?: titleCandidate
            ?: "file_${System.currentTimeMillis()}"
        val name = raw.take(80).trim().ifBlank { "file_${System.currentTimeMillis()}" }
        return if ('.' in name) name else "$name${defaultExtension(media.type)}"
    }

    private fun mimeFromType(type: MediaType): String? = when (type) {
        MediaType.IMAGE    -> "image/jpeg"
        MediaType.GIF      -> "image/gif"
        MediaType.VIDEO    -> "video/mp4"
        MediaType.AUDIO    -> "audio/mpeg"
        MediaType.PDF      -> "application/pdf"
        MediaType.DOCUMENT -> "application/octet-stream"
        MediaType.ARCHIVE  -> "application/zip"
        MediaType.SUBTITLE -> "text/plain"
        MediaType.EBOOK    -> "application/epub+zip"
        MediaType.UNKNOWN  -> null
    }

    private fun defaultExtension(type: MediaType) = when (type) {
        MediaType.IMAGE    -> ".jpg"
        MediaType.GIF      -> ".gif"
        MediaType.VIDEO    -> ".mp4"
        MediaType.AUDIO    -> ".mp3"
        MediaType.PDF      -> ".pdf"
        MediaType.DOCUMENT -> ".txt"
        MediaType.ARCHIVE  -> ".zip"
        MediaType.SUBTITLE -> ".srt"
        MediaType.EBOOK    -> ".epub"
        MediaType.UNKNOWN  -> ""
    }

    private fun getDirectory(type: MediaType) = when (type) {
        MediaType.IMAGE, MediaType.GIF -> Environment.DIRECTORY_PICTURES
        MediaType.VIDEO                -> Environment.DIRECTORY_MOVIES
        MediaType.AUDIO                -> Environment.DIRECTORY_MUSIC
        else                           -> Environment.DIRECTORY_DOWNLOADS
    }
}
