package com.example.urldownloader.data

import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        .followSslRedirects(true)
        .build()

    private val VIDEO_EXTS = setOf("mp4","webm","m3u8","mkv","avi","mov","flv","ts","3gp","wmv","mpd","ogv")
    private val AUDIO_EXTS = setOf("mp3","ogg","wav","flac","aac","m4a","opus","wma","m4b","oga","aiff")
    private val IMAGE_EXTS = setOf("jpg","jpeg","png","webp","bmp","tiff","tif","avif","heic","heif","jxl")
    private val PDF_EXTS = setOf("pdf")
    private val DOC_EXTS = setOf("doc","docx","xls","xlsx","ppt","pptx","txt","csv","xml","json","rtf","odt","ods","odp","md")
    private val ARCHIVE_EXTS = setOf("zip","rar","7z","tar","gz","bz2","xz","zst","cab","dmg","iso")
    private val SUBTITLE_EXTS = setOf("srt","vtt","ass","ssa","sub","sbv")
    private val EBOOK_EXTS = setOf("epub","mobi","azw","azw3","fb2","lit")

    private val allMediaExts = VIDEO_EXTS + AUDIO_EXTS + IMAGE_EXTS + setOf("gif") +
        PDF_EXTS + DOC_EXTS + ARCHIVE_EXTS + SUBTITLE_EXTS + EBOOK_EXTS

    private val MEDIA_URL_REGEX = Regex(
        """https?://[^\s"'<>{}|\\\^\[\]]+\.(${allMediaExts.joinToString("|")})(\?[^\s"'<>]*)?""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val GENERIC_URL_REGEX = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
    private val SKIP_FRAGMENTS = setOf(
        "favicon", "icon/", "/icons/", "/logo", "logo/", "sprite",
        "pixel", "beacon", "tracking", "analytics", "googletagmanager",
        "doubleclick", "facebook.com/tr", "bat.bing", "placeholder",
        "spacer", "transparent", "blank.gif", "loading", "spinner",
        "1x1", "2x2", "avatar/default", "default_avatar"
    )

    data class ResolvedInput(val sourceText: String, val url: String)

    data class RemoteMetadata(
        val finalUrl: String,
        val contentType: String? = null,
        val contentDisposition: String? = null,
        val contentLength: Long = -1L,
        val directType: MediaType = MediaType.UNKNOWN
    )

    fun resolveInput(input: String): ResolvedInput? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        val direct = normalizeUrl(trimmed)
        if (direct != null) return ResolvedInput(trimmed, direct)
        val embedded = GENERIC_URL_REGEX.find(trimmed)?.value?.let(::normalizeUrl)
        return embedded?.let { ResolvedInput(trimmed, it) }
    }

    fun extract(input: String): List<MediaItem> {
        val resolved = resolveInput(input)
            ?: throw IllegalArgumentException("Paste a valid http:// or https:// URL.")
        val normalizedUrl = resolved.url

        val directType = detectTypeByExtension(normalizedUrl)
        if (directType != MediaType.UNKNOWN) {
            val title = fileNameFromUrl(normalizedUrl)
            val thumb = if (directType == MediaType.IMAGE || directType == MediaType.GIF) normalizedUrl else null
            return listOf(MediaItem(normalizedUrl, directType, title, thumbnailUrl = thumb))
        }

        probeUrl(normalizedUrl)?.takeIf { it.directType != MediaType.UNKNOWN }?.let { meta ->
            val title = inferTitle(meta.finalUrl, meta.contentDisposition)
            val thumb = if (meta.directType == MediaType.IMAGE || meta.directType == MediaType.GIF) meta.finalUrl else null
            return listOf(
                MediaItem(
                    url = meta.finalUrl,
                    type = meta.directType,
                    title = title,
                    mimeType = meta.contentType?.substringBefore(';')?.trim(),
                    fileSize = meta.contentLength,
                    thumbnailUrl = thumb,
                    downloadUrl = meta.finalUrl
                )
            )
        }

        PlatformExtractor.extract(normalizedUrl, client)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        return scrapeGenericPage(normalizedUrl)
    }

    internal fun normalizeUrl(raw: String): String? {
        val candidate = raw.trim().trim('<', '>', '"', '\'').replace("\\u0026", "&")
        val withScheme = when {
            candidate.startsWith("http://", ignoreCase = true) || candidate.startsWith("https://", ignoreCase = true) -> candidate
            candidate.startsWith("//") -> "https:$candidate"
            candidate.startsWith("www.", ignoreCase = true) -> "https://$candidate"
            else -> return null
        }
        val httpUrl = withScheme.toHttpUrlOrNull() ?: return null
        return httpUrl.newBuilder().build().toString()
    }

    private fun shouldSkipUrl(url: String): Boolean {
        if (url.isBlank() || url.startsWith("data:")) return true
        val lo = url.lowercase()
        val ext = lo.substringAfterLast(".").substringBefore("?").substringBefore("#").take(6)
        if (ext == "svg" || ext == "ico") return true
        return SKIP_FRAGMENTS.any { lo.contains(it) }
    }

    private fun shouldSkipImg(el: Element): Boolean {
        val src = firstAttrUrl(el, "src", "data-src", "data-lazy-src", "data-original", "srcset") ?: return true
        if (shouldSkipUrl(src)) return true
        val w = el.attr("width").replace("px", "").toIntOrNull() ?: 0
        val h = el.attr("height").replace("px", "").toIntOrNull() ?: 0
        if ((w in 1..79) || (h in 1..79)) return true
        return false
    }

    private fun scrapeGenericPage(url: String): List<MediaItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.google.com/")
            .build()

        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type") ?: ""
            val body = response.body?.string() ?: ""

            val ctType = detectTypeByContentType(contentType)
            if (ctType != MediaType.UNKNOWN) {
                val title = inferTitle(finalUrl, response.header("Content-Disposition"))
                val mime = contentType.substringBefore(';').trim()
                val thumb = if (ctType == MediaType.IMAGE || ctType == MediaType.GIF) finalUrl else null
                return listOf(MediaItem(finalUrl, ctType, title, mimeType = mime, thumbnailUrl = thumb, downloadUrl = finalUrl))
            }

            val results = mutableListOf<MediaItem>()
            val doc = Jsoup.parse(body, finalUrl)
            val pageTitle = doc.title().ifBlank { fileNameFromUrl(finalUrl) }

            fun firstMeta(vararg selectors: String): String? = selectors.firstNotNullOfOrNull { sel ->
                doc.select(sel).firstOrNull()?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
            }

            val metaTitle = firstMeta("meta[property=og:title]", "meta[name=twitter:title]") ?: pageTitle

            collectCandidate(results, firstMeta("meta[property=og:image]", "meta[name=og:image]", "meta[name=twitter:image]", "meta[name=twitter:image:src]"), metaTitle, finalUrl)
            collectCandidate(results, firstMeta("meta[property=og:video]", "meta[property=og:video:url]", "meta[name=twitter:player:stream]"), metaTitle, finalUrl, fallbackType = MediaType.VIDEO)
            collectCandidate(results, firstMeta("meta[property=og:audio]", "meta[name=twitter:audio]"), metaTitle, finalUrl, fallbackType = MediaType.AUDIO)
            collectCandidate(results, firstMeta("meta[property=og:url]"), metaTitle, finalUrl)

            doc.select("video[src], video source[src], source[type^=video]").forEach { el ->
                collectCandidate(results, firstAttrUrl(el, "src", "srcset"), el.attr("title").ifEmpty { metaTitle }, finalUrl, fallbackType = MediaType.VIDEO)
            }
            doc.select("audio[src], audio source[src], source[type^=audio]").forEach { el ->
                collectCandidate(results, firstAttrUrl(el, "src", "srcset"), el.attr("title").ifEmpty { metaTitle }, finalUrl, fallbackType = MediaType.AUDIO)
            }
            doc.select("embed[src], iframe[src], object[data]").forEach { el ->
                collectCandidate(results, firstAttrUrl(el, "src", "data"), el.attr("title").ifEmpty { metaTitle }, finalUrl)
            }
            doc.select("img").take(60).forEach { el ->
                if (shouldSkipImg(el)) return@forEach
                val src = firstAttrUrl(el, "src", "data-src", "data-lazy-src", "data-original", "srcset") ?: return@forEach
                val type = detectTypeByExtension(src).let { if (it == MediaType.UNKNOWN) MediaType.IMAGE else it }
                val title = el.attr("alt").ifEmpty { metaTitle }
                val w = el.attr("width").replace("px", "").toIntOrNull() ?: 0
                val h = el.attr("height").replace("px", "").toIntOrNull() ?: 0
                results += MediaItem(src, type, title, thumbnailUrl = if (type == MediaType.IMAGE || type == MediaType.GIF) src else null, width = w, height = h, downloadUrl = src)
            }
            doc.select("a[href]").forEach { el ->
                val href = el.absUrl("href").takeIf { it.isNotEmpty() } ?: return@forEach
                val title = el.text().ifBlank { metaTitle }
                collectCandidate(results, href, title, finalUrl)
            }
            doc.select("script, noscript").forEach { el ->
                MEDIA_URL_REGEX.findAll(el.data() + " " + el.html()).take(30).forEach { match ->
                    collectCandidate(results, match.value, metaTitle, finalUrl)
                }
            }
            MEDIA_URL_REGEX.findAll(body).take(40).forEach { match ->
                collectCandidate(results, match.value, metaTitle, finalUrl)
            }

            val deduped = results
                .mapNotNull { item -> item.url.takeIf { it.isNotBlank() }?.let { item.copy(url = it.trimEnd(',', ';', '"', '\'', ')', ']', '}')) } }
                .filterNot { shouldSkipUrl(it.url) }
                .distinctBy { it.url }

            val nonImages = deduped.filter { it.type != MediaType.IMAGE && it.type != MediaType.GIF }
            val images = deduped.filter { it.type == MediaType.IMAGE || it.type == MediaType.GIF }
                .sortedByDescending { it.width * it.height }
                .take(30)

            return (nonImages + images).distinctBy { it.url }
        }
    }

    private fun collectCandidate(
        results: MutableList<MediaItem>,
        rawUrl: String?,
        title: String,
        baseUrl: String,
        fallbackType: MediaType = MediaType.UNKNOWN
    ) {
        val resolvedUrl = rawUrl?.let { resolveRelativeUrl(baseUrl, it) } ?: return
        if (shouldSkipUrl(resolvedUrl)) return
        val type = detectTypeByExtension(resolvedUrl).let { if (it == MediaType.UNKNOWN) fallbackType else it }
        if (type == MediaType.UNKNOWN && PlatformExtractor.platform(resolvedUrl) == null) return
        val thumb = if (type == MediaType.IMAGE || type == MediaType.GIF) resolvedUrl else null
        results += MediaItem(resolvedUrl, type, title = title.ifBlank { fileNameFromUrl(resolvedUrl) }, thumbnailUrl = thumb, downloadUrl = resolvedUrl)
    }

    private fun probeUrl(url: String): RemoteMetadata? {
        return executeProbe(url, headOnly = true) ?: executeProbe(url, headOnly = false)
    }

    private fun executeProbe(url: String, headOnly: Boolean): RemoteMetadata? = runCatching {
        val request = Request.Builder()
            .url(url)
            .apply { if (headOnly) head() else get() }
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*")
            .build()
        client.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type")
            val disposition = response.header("Content-Disposition")
            val finalUrl = response.request.url.toString()
            val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val typeFromCt = detectTypeByContentType(contentType.orEmpty())
            val typeFromUrl = detectTypeByExtension(finalUrl)
            val directType = if (typeFromCt != MediaType.UNKNOWN) typeFromCt else typeFromUrl
            if (!response.isSuccessful && directType == MediaType.UNKNOWN) return@use null
            RemoteMetadata(finalUrl, contentType, disposition, length, directType)
        }
    }.getOrNull()

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
            ext in SUBTITLE_EXTS -> MediaType.SUBTITLE
            ext in EBOOK_EXTS -> MediaType.EBOOK
            else -> MediaType.UNKNOWN
        }
    }

    private fun detectTypeByContentType(contentType: String): MediaType {
        val ct = contentType.lowercase()
        return when {
            ct.startsWith("image/gif") -> MediaType.GIF
            ct.startsWith("image/") -> MediaType.IMAGE
            ct.startsWith("video/") || ct.contains("mpegurl") || ct.contains("dash+xml") -> MediaType.VIDEO
            ct.startsWith("audio/") -> MediaType.AUDIO
            ct.contains("pdf") -> MediaType.PDF
            ct.contains("epub") || ct.contains("ebook") -> MediaType.EBOOK
            ct.contains("zip") || ct.contains("archive") || ct.contains("compressed") || ct.contains("x-tar") || ct.contains("x-rar") -> MediaType.ARCHIVE
            ct.contains("vtt") || ct.contains("subrip") || ct.contains("subtitle") -> MediaType.SUBTITLE
            ct.contains("msword") || ct.contains("officedocument") || ct.startsWith("text/plain") || ct.startsWith("text/csv") || ct.contains("json") || ct.contains("rtf") || ct.contains("markdown") -> MediaType.DOCUMENT
            else -> MediaType.UNKNOWN
        }
    }

    private fun inferTitle(url: String, contentDisposition: String?): String {
        val dispositionName = contentDisposition
            ?.substringAfter("filename*=", "")
            ?.substringAfter("''", "")
            ?.substringBefore(';')
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
            ?: contentDisposition
                ?.substringAfter("filename=", "")
                ?.substringBefore(';')
                ?.trim('"')
                ?.takeIf { it.isNotBlank() }
        return dispositionName ?: fileNameFromUrl(url)
    }

    private fun fileNameFromUrl(url: String): String =
        url.substringAfterLast('/').substringBefore('?').substringBefore('#').ifBlank { "downloaded_file" }

    private fun firstAttrUrl(element: Element, vararg attrs: String): String? {
        attrs.forEach { attr ->
            val raw = when (attr) {
                "srcset" -> bestSrcsetCandidate(element.attr(attr))
                else -> element.attr(attr)
            }.trim()
            if (raw.isNotEmpty()) {
                val abs = when (attr) {
                    "data", "src", "data-src", "data-lazy-src", "data-original" -> element.absUrl(attr)
                    else -> ""
                }
                val value = if (abs.isNotEmpty()) abs else raw
                val normalized = normalizeUrl(value)
                if (normalized != null) return normalized
            }
        }
        return null
    }

    private fun bestSrcsetCandidate(srcset: String): String =
        srcset.split(',').map { it.trim().substringBefore(' ') }.lastOrNull().orEmpty()

    private fun resolveRelativeUrl(baseUrl: String, rawUrl: String): String? {
        normalizeUrl(rawUrl)?.let { return it }
        return runCatching {
            baseUrl.toHttpUrlOrNull()?.resolve(rawUrl)?.toString()
        }.getOrNull()
    }
}
