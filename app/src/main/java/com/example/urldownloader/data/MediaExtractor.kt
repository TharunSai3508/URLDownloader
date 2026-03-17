package com.example.urldownloader.data

import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object MediaExtractor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val VIDEO_EXTS = setOf("mp4", "webm", "m3u8", "mkv", "avi", "mov", "flv", "ts", "3gp", "wmv")
    private val AUDIO_EXTS = setOf("mp3", "ogg", "wav", "flac", "aac", "m4a", "opus", "wma", "m4b")
    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "bmp", "ico", "tiff", "tif", "avif", "heic")
    private val PDF_EXTS = setOf("pdf")
    private val DOC_EXTS = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "xml", "json", "rtf", "odt", "ods")
    private val ARCHIVE_EXTS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst")

    private val allMediaExts = VIDEO_EXTS + AUDIO_EXTS + IMAGE_EXTS + PDF_EXTS + DOC_EXTS + ARCHIVE_EXTS

    private val MEDIA_URL_REGEX = Regex(
        """https?://[^\s"'<>{}|\\\^\[\]]+\.(${allMediaExts.joinToString("|")})(\?[^\s"'<>]*)?""",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun extract(url: String): List<MediaItem> {
        val trimmed = url.trim()

        // If URL itself is a direct media file, return it immediately
        val directType = detectTypeByExtension(trimmed)
        if (directType != MediaType.UNKNOWN) {
            val title = trimmed.substringAfterLast('/').substringBefore('?')
            return listOf(MediaItem(trimmed, directType, title = title))
        }

        val request = Request.Builder()
            .url(trimmed)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = client.newCall(request).execute()
        val contentType = response.header("Content-Type") ?: ""
        val body = response.body?.string() ?: ""

        // Server returned a media file directly
        val ctMediaType = detectTypeByContentType(contentType)
        if (ctMediaType != MediaType.UNKNOWN) {
            val title = trimmed.substringAfterLast('/').substringBefore('?')
            val mime = contentType.substringBefore(';').trim()
            return listOf(MediaItem(trimmed, ctMediaType, title = title, mimeType = mime))
        }

        val results = mutableListOf<MediaItem>()
        val doc = Jsoup.parse(body, trimmed)

        // Open Graph meta tags (highest quality)
        doc.select("meta[property=og:image], meta[name=og:image]").forEach { el ->
            val src = el.attr("content").trim()
            if (src.isNotEmpty()) results += MediaItem(src, MediaType.IMAGE, title = "Preview Image")
        }
        doc.select("meta[property=og:video], meta[property=og:video:url]").forEach { el ->
            val src = el.attr("content").trim()
            if (src.isNotEmpty()) results += MediaItem(src, MediaType.VIDEO, title = "Preview Video")
        }
        doc.select("meta[property=og:audio]").forEach { el ->
            val src = el.attr("content").trim()
            if (src.isNotEmpty()) results += MediaItem(src, MediaType.AUDIO, title = "Preview Audio")
        }

        // <img> tags – including lazy-loaded variants
        doc.select("img").forEach { el ->
            val src = (el.absUrl("src").takeIf { it.isNotEmpty() }
                ?: el.absUrl("data-src").takeIf { it.isNotEmpty() }
                ?: el.absUrl("data-lazy-src").takeIf { it.isNotEmpty() }
                ?: el.absUrl("data-original"))
            if (src.isNotEmpty()) {
                val type = detectTypeByExtension(src).let { if (it == MediaType.UNKNOWN) MediaType.IMAGE else it }
                val title = el.attr("alt").takeIf { it.isNotEmpty() } ?: src.substringAfterLast('/')
                results += MediaItem(src, type, title = title)
            }
        }

        // <video> tags
        doc.select("video[src]").forEach { el ->
            val src = el.absUrl("src")
            if (src.isNotEmpty()) results += MediaItem(src, MediaType.VIDEO,
                title = el.attr("title").takeIf { it.isNotEmpty() } ?: "Video")
        }
        doc.select("video source[src]").forEach { el ->
            val src = el.absUrl("src")
            if (src.isNotEmpty()) results += MediaItem(src, detectTypeByExtension(src)
                .let { if (it == MediaType.UNKNOWN) MediaType.VIDEO else it }, title = "Video")
        }

        // <audio> tags
        doc.select("audio[src]").forEach { el ->
            val src = el.absUrl("src")
            if (src.isNotEmpty()) results += MediaItem(src, MediaType.AUDIO, title = "Audio")
        }
        doc.select("audio source[src]").forEach { el ->
            val src = el.absUrl("src")
            if (src.isNotEmpty()) results += MediaItem(src, detectTypeByExtension(src)
                .let { if (it == MediaType.UNKNOWN) MediaType.AUDIO else it }, title = "Audio")
        }

        // <a href> download links (non-image files)
        doc.select("a[href]").forEach { el ->
            val href = el.absUrl("href")
            if (href.isNotEmpty()) {
                val type = detectTypeByExtension(href)
                if (type != MediaType.UNKNOWN && type != MediaType.IMAGE && type != MediaType.GIF) {
                    val label = el.text().takeIf { it.isNotEmpty() } ?: href.substringAfterLast('/')
                    results += MediaItem(href, type, title = label)
                }
            }
        }

        // Regex scan for raw media URLs embedded in HTML/JS/JSON
        MEDIA_URL_REGEX.findAll(body).forEach { match ->
            val raw = match.value.trimEnd(',', ';', '"', '\'', ')', ']', '}')
            val type = detectTypeByExtension(raw)
            val title = raw.substringAfterLast('/').substringBefore('?')
            results += MediaItem(raw, type, title = title)
        }

        return results.distinctBy { it.url }
    }

    fun detectTypeByExtension(url: String): MediaType {
        val ext = url.substringAfterLast('.').substringBefore('?').substringBefore('#').lowercase().trim()
        return when {
            ext == "gif" -> MediaType.GIF
            ext in IMAGE_EXTS -> MediaType.IMAGE
            ext in VIDEO_EXTS -> MediaType.VIDEO
            ext in AUDIO_EXTS -> MediaType.AUDIO
            ext in PDF_EXTS -> MediaType.PDF
            ext in DOC_EXTS -> MediaType.DOCUMENT
            ext in ARCHIVE_EXTS -> MediaType.ARCHIVE
            else -> MediaType.UNKNOWN
        }
    }

    private fun detectTypeByContentType(contentType: String): MediaType {
        val ct = contentType.lowercase()
        return when {
            ct.startsWith("image/gif") -> MediaType.GIF
            ct.startsWith("image/") -> MediaType.IMAGE
            ct.startsWith("video/") -> MediaType.VIDEO
            ct.startsWith("audio/") -> MediaType.AUDIO
            ct.contains("pdf") -> MediaType.PDF
            ct.contains("zip") || ct.contains("archive") || ct.contains("compressed") ||
                    ct.contains("x-tar") || ct.contains("x-rar") -> MediaType.ARCHIVE
            ct.contains("msword") || ct.contains("officedocument") ||
                    ct.startsWith("text/plain") || ct.startsWith("text/csv") -> MediaType.DOCUMENT
            else -> MediaType.UNKNOWN
        }
    }
}
