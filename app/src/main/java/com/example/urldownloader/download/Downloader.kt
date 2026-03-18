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

    fun download(context: Context, media: MediaItem) {
        val fileName = buildFileName(media)
        runCatching {
            val request = DownloadManager.Request(Uri.parse(media.url))
                .setTitle(fileName)
                .setDescription("Downloading via URL Downloader")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .addRequestHeader("User-Agent", UA)
                .addRequestHeader("Accept", "*/*")
                .addRequestHeader("Referer", "https://www.google.com/")
                .setDestinationInExternalPublicDir(getDirectory(media.type), fileName)
                .apply { media.mimeType?.let { setMimeType(it) } }

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
        val raw  = Uri.parse(media.url).lastPathSegment ?: "file_${System.currentTimeMillis()}"
        val name = raw.substringBefore('?').substringBefore('#')
        return if ('.' in name) name else "$name${defaultExtension(media.type)}"
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
