package com.example.urldownloader.data

import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

object MediaExtractor {

    internal val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ── Extension sets (10+ media types) ─────────────────────────────────────

    private val VIDEO_EXTS    = setOf("mp4","webm","m3u8","mkv","avi","mov","flv","ts","3gp","wmv","mpd","ogv")
    private val AUDIO_EXTS    = setOf("mp3","ogg","wav","flac","aac","m4a","opus","wma","m4b","oga","aiff")
    private val IMAGE_EXTS    = setOf("jpg","jpeg","png","webp","bmp","tiff","tif","avif","heic","heif","jxl")
    private val PDF_EXTS      = setOf("pdf")
    private val DOC_EXTS      = setOf("doc","docx","xls","xlsx","ppt","pptx","txt","csv","xml","json","rtf","odt","ods","odp")
    private val ARCHIVE_EXTS  = setOf("zip","rar","7z","tar","gz","bz2","xz","zst","cab","dmg","iso")
    private val SUBTITLE_EXTS = setOf("srt","vtt","ass","ssa","sub","sbv")
    private val EBOOK_EXTS    = setOf("epub","mobi","azw","azw3","fb2","lit")

    private val allMediaExts = VIDEO_EXTS + AUDIO_EXTS + IMAGE_EXTS + setOf("gif") +
            PDF_EXTS + DOC_EXTS + ARCHIVE_EXTS + SUBTITLE_EXTS + EBOOK_EXTS

    private val MEDIA_URL_REGEX = Regex(
        """https?://[^\s"'<>{}|\\\^\[\]]+\.(${allMediaExts.joinToString("|")})(\?[^\s"'<>]*)?""",
        setOf(RegexOption.IGNORE_CASE)
    )

    // ── Junk-image filter ─────────────────────────────────────────────────────

    private val SKIP_FRAGMENTS = setOf(
        "favicon", "icon/", "/icons/", "/logo", "logo/", "sprite",
        "pixel", "beacon", "tracking", "analytics", "googletagmanager",
        "doubleclick", "facebook.com/tr", "bat.bing", "placeholder",
        "spacer", "transparent", "blank.gif", "loading", "spinner",
        "1x1", "2x2", "avatar/default", "default_avatar"
    )

    private fun shouldSkipUrl(url: String): Boolean {
        if (url.isBlank() || url.startsWith("data:")) return true
        val lo = url.lowercase()
        val ext = lo.substringAfterLast(".").substringBefore("?").substringBefore("#").take(6)
        if (ext == "svg" || ext == "ico") return true
        return SKIP_FRAGMENTS.any { lo.contains(it) }
    }

    private fun shouldSkipImg(el: Element): Boolean {
        val src = el.absUrl("src").takeIf { it.isNotEmpty() }
            ?: el.absUrl("data-src").takeIf { it.isNotEmpty() }
            ?: return true
        if (shouldSkipUrl(src)) return true
        val w = el.attr("width").replace("px", "").toIntOrNull() ?: 0
        val h = el.attr("height").replace("px", "").toIntOrNull() ?: 0
        if ((w in 1..79) || (h in 1..79)) return true
        return false
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun extract(url: String): List<MediaItem> {
        val trimmed = url.trim()

        // 1. Direct media file URL
        val directType = detectTypeByExtension(trimmed)
        if (directType != MediaType.UNKNOWN) {
            val title = trimmed.substringAfterLast('/').substringBefore('?')
            val thumb = if (directType == MediaType.IMAGE || directType == MediaType.GIF) trimmed else null
            return listOf(MediaItem(trimmed, directType, title, thumbnailUrl = thumb))
        }

        // 2. Known platform → specialised extraction
        PlatformExtractor.extract(trimmed, client)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        // 3. Generic HTML scrape with smart filtering
        return scrapeGenericPage(trimmed)
    }

    // ── Generic page scraper ──────────────────────────────────────────────────

    private fun scrapeGenericPage(url: String): List<MediaItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent",
                "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
            .header("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.google.com/")
            .build()

        val response    = client.newCall(request).execute()
        val contentType = response.header("Content-Type") ?: ""
        val body        = response.body?.string() ?: ""

        // Server returned a media file directly (e.g. CDN link with no extension in path)
        val ctType = detectTypeByContentType(contentType)
        if (ctType != MediaType.UNKNOWN) {
            val title = url.substringAfterLast('/').substringBefore('?')
            val mime  = contentType.substringBefore(';').trim()
            val thumb = if (ctType == MediaType.IMAGE || ctType == MediaType.GIF) url else null
            return listOf(MediaItem(url, ctType, title, mimeType = mime, thumbnailUrl = thumb))
        }

        val results = mutableListOf<MediaItem>()
        val doc     = Jsoup.parse(body, url)

        // ── Tier 1: OG / Twitter card meta tags ───────────────────────────────
        fun firstMeta(vararg selectors: String): String? =
            selectors.firstNotNullOfOrNull { sel ->
                doc.select(sel).firstOrNull()?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
            }

        firstMeta("meta[property=og:image]", "meta[name=og:image]",
                  "meta[name=twitter:image]", "meta[name=twitter:image:src]")
            ?.takeIf { !shouldSkipUrl(it) }
            ?.let { results += MediaItem(it, MediaType.IMAGE, "Preview Image", thumbnailUrl = it) }

        firstMeta("meta[property=og:video]", "meta[property=og:video:url]",
                  "meta[name=twitter:player:stream]")
            ?.takeIf { !shouldSkipUrl(it) }
            ?.let {
                val t = detectTypeByExtension(it).let { t -> if (t == MediaType.UNKNOWN) MediaType.VIDEO else t }
                results += MediaItem(it, t, "Preview Video")
            }

        firstMeta("meta[property=og:audio]")
            ?.takeIf { !shouldSkipUrl(it) }
            ?.let { results += MediaItem(it, MediaType.AUDIO, "Preview Audio") }

        // ── Tier 2: <video> and <audio> tags ──────────────────────────────────
        doc.select("video[src], video source[src]").forEach { el ->
            val src = el.absUrl("src").takeIf { it.isNotEmpty() } ?: return@forEach
            if (!shouldSkipUrl(src)) {
                results += MediaItem(src,
                    detectTypeByExtension(src).let { if (it == MediaType.UNKNOWN) MediaType.VIDEO else it },
                    title = el.attr("title").ifEmpty { "Video" })
            }
        }
        doc.select("audio[src], audio source[src]").forEach { el ->
            val src = el.absUrl("src").takeIf { it.isNotEmpty() } ?: return@forEach
            if (!shouldSkipUrl(src))
                results += MediaItem(src,
                    detectTypeByExtension(src).let { if (it == MediaType.UNKNOWN) MediaType.AUDIO else it },
                    title = "Audio")
        }

        // ── Tier 3: <img> tags — filtered, capped at 40 ───────────────────────
        var imgCount = 0
        doc.select("img").forEach { el ->
            if (imgCount >= 40) return@forEach
            if (shouldSkipImg(el)) return@forEach
            val src = el.absUrl("src").takeIf { it.isNotEmpty() }
                ?: el.absUrl("data-src").takeIf { it.isNotEmpty() }
                ?: el.absUrl("data-lazy-src").takeIf { it.isNotEmpty() }
                ?: el.absUrl("data-original").takeIf { it.isNotEmpty() }
                ?: return@forEach
            val type  = detectTypeByExtension(src).let { if (it == MediaType.UNKNOWN) MediaType.IMAGE else it }
            val title = el.attr("alt").ifEmpty { src.substringAfterLast('/').take(60) }
            val w     = el.attr("width").replace("px", "").toIntOrNull() ?: 0
            val h     = el.attr("height").replace("px", "").toIntOrNull() ?: 0
            results += MediaItem(src, type, title,
                thumbnailUrl = if (type == MediaType.IMAGE || type == MediaType.GIF) src else null,
                width = w, height = h)
            imgCount++
        }

        // ── Tier 4: <a href> to downloadable non-image files ──────────────────
        doc.select("a[href]").forEach { el ->
            val href = el.absUrl("href").takeIf { it.isNotEmpty() } ?: return@forEach
            val type = detectTypeByExtension(href)
            if (type != MediaType.UNKNOWN && type != MediaType.IMAGE && type != MediaType.GIF) {
                results += MediaItem(href, type,
                    title = el.text().ifEmpty { href.substringAfterLast('/').take(60) })
            }
        }

        // ── Tier 5: Regex scan — capped at 40 ────────────────────────────────
        MEDIA_URL_REGEX.findAll(body).take(40).forEach { match ->
            val raw = match.value.trimEnd(',', ';', '"', '\'', ')', ']', '}')
            if (shouldSkipUrl(raw)) return@forEach
            val type  = detectTypeByExtension(raw)
            val title = raw.substringAfterLast('/').substringBefore('?').take(60)
            results += MediaItem(raw, type, title,
                thumbnailUrl = if (type == MediaType.IMAGE || type == MediaType.GIF) raw else null)
        }

        // De-duplicate; cap images at 30 (sorted largest first)
        val deduped   = results.distinctBy { it.url }
        val nonImages = deduped.filter { it.type != MediaType.IMAGE && it.type != MediaType.GIF }
        val images    = deduped.filter { it.type == MediaType.IMAGE || it.type == MediaType.GIF }
            .sortedByDescending { it.width * it.height }
            .take(30)

        return (nonImages + images).distinctBy { it.url }
    }

    // ── Type detection helpers ────────────────────────────────────────────────

    fun detectTypeByExtension(url: String): MediaType {
        val ext = url.substringAfterLast('.').substringBefore('?')
            .substringBefore('#').lowercase().trim()
        return when {
            ext == "gif"           -> MediaType.GIF
            ext in IMAGE_EXTS      -> MediaType.IMAGE
            ext in VIDEO_EXTS      -> MediaType.VIDEO
            ext in AUDIO_EXTS      -> MediaType.AUDIO
            ext in PDF_EXTS        -> MediaType.PDF
            ext in DOC_EXTS        -> MediaType.DOCUMENT
            ext in ARCHIVE_EXTS    -> MediaType.ARCHIVE
            ext in SUBTITLE_EXTS   -> MediaType.SUBTITLE
            ext in EBOOK_EXTS      -> MediaType.EBOOK
            else                   -> MediaType.UNKNOWN
        }
    }

    private fun detectTypeByContentType(contentType: String): MediaType {
        val ct = contentType.lowercase()
        return when {
            ct.startsWith("image/gif")   -> MediaType.GIF
            ct.startsWith("image/")      -> MediaType.IMAGE
            ct.startsWith("video/")      -> MediaType.VIDEO
            ct.startsWith("audio/")      -> MediaType.AUDIO
            ct.contains("pdf")           -> MediaType.PDF
            ct.contains("epub")          -> MediaType.EBOOK
            ct.contains("zip") || ct.contains("archive") ||
                ct.contains("compressed") || ct.contains("x-tar") ||
                ct.contains("x-rar")     -> MediaType.ARCHIVE
            ct.contains("msword") || ct.contains("officedocument") ||
                ct.startsWith("text/plain") || ct.startsWith("text/csv") -> MediaType.DOCUMENT
            else                         -> MediaType.UNKNOWN
        }
    }
}
