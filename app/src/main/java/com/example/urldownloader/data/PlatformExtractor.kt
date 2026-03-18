package com.example.urldownloader.data

import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * Platform-specific media extraction for well-known sites.
 * Returns null when the URL is not a recognised platform → caller falls back to generic scrape.
 */
object PlatformExtractor {

    // ── Platform detection ────────────────────────────────────────────────────

    fun platform(url: String): String? {
        val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: return null
        return when {
            "youtube.com" in host || "youtu.be" in host       -> "youtube"
            "reddit.com"  in host || "redd.it"  in host       -> "reddit"
            "drive.google.com" in host                         -> "googledrive"
            "twitter.com" in host || "x.com" in host           -> "twitter"
            "instagram.com" in host                            -> "instagram"
            "facebook.com" in host || "fb.com" in host         -> "facebook"
            "imgur.com" in host                                -> "imgur"
            "giphy.com" in host                                -> "giphy"
            "tiktok.com" in host                               -> "tiktok"
            "vimeo.com" in host                                -> "vimeo"
            "dailymotion.com" in host                          -> "dailymotion"
            "twitch.tv" in host                                -> "twitch"
            "pinterest.com" in host || "pin.it" in host        -> "pinterest"
            else                                               -> null
        }
    }

    /** Returns null → caller should use the generic scraper instead. */
    fun extract(url: String, client: OkHttpClient): List<MediaItem>? =
        when (platform(url)) {
            "youtube"     -> extractYouTube(url, client)
            "reddit"      -> extractReddit(url, client)
            "googledrive" -> extractDrive(url)
            "imgur"       -> extractImgur(url, client)
            "giphy"       -> extractGiphy(url, client)
            "vimeo"       -> extractVimeo(url, client)
            else          -> null   // twitter/instagram/facebook → generic OG extraction
        }

    // ── YouTube ───────────────────────────────────────────────────────────────

    private fun extractYouTube(url: String, client: OkHttpClient): List<MediaItem> {
        val videoId = youTubeVideoId(url) ?: return emptyList()

        // Thumbnail URLs (maxres may 404 on very old videos; hq always exists)
        val thumbHq  = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        val thumbMax = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"

        var title       = "YouTube Video"
        var channelName = ""
        runCatching {
            val enc = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
            val json = fetch("https://www.youtube.com/oembed?url=$enc&format=json", client)
            if (!json.isNullOrBlank()) {
                val obj = org.json.JSONObject(json)
                title       = obj.optString("title", title)
                channelName = obj.optString("author_name", "")
            }
        }

        val displayTitle = if (channelName.isNotEmpty()) "$title · $channelName" else title

        return buildList {
            // The HD thumbnail as a downloadable image
            add(MediaItem(thumbMax, MediaType.IMAGE,
                "Thumbnail: $title",
                thumbnailUrl = thumbHq,
                platform     = "youtube"))
            // The video itself — ExoPlayer will attempt playback; download opens yt URL
            add(MediaItem(url, MediaType.VIDEO,
                displayTitle,
                thumbnailUrl = thumbHq,
                platform     = "youtube"))
        }
    }

    private fun youTubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("""(?:youtube\.com/watch\?.*v=|youtu\.be/|youtube\.com/embed/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})"""),
            Regex("""youtube\.com/v/([a-zA-Z0-9_-]{11})""")
        )
        for (p in patterns) p.find(url)?.let { return it.groupValues[1] }
        return null
    }

    // ── Reddit ────────────────────────────────────────────────────────────────

    private fun extractReddit(url: String, client: OkHttpClient): List<MediaItem> {
        val apiUrl = buildRedditApiUrl(url) ?: return emptyList()
        val json   = fetch(apiUrl, client) ?: return emptyList()
        val results = mutableListOf<MediaItem>()

        runCatching {
            val arr = org.json.JSONArray(json)
            val postData = arr.getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0)
                .getJSONObject("data")

            val title    = postData.optString("title", "Reddit Post")
            val postUrl  = postData.optString("url", "")
            val previewThumb = previewImage(postData)

            // Reddit-hosted video (DASH preferred for streaming; MP4 fallback for downloading)
            postData.optJSONObject("media")?.optJSONObject("reddit_video")?.let { rv ->
                val dashUrl     = rv.optString("dash_url", "")
                val fallbackUrl = rv.optString("fallback_url", "").replace("?source=fallback", "")
                val streamUrl   = dashUrl.takeIf { it.isNotEmpty() } ?: fallbackUrl
                val dlUrl       = fallbackUrl.takeIf { it.isNotEmpty() && it != streamUrl }
                if (streamUrl.isNotEmpty()) {
                    results += MediaItem(streamUrl, MediaType.VIDEO, title,
                        thumbnailUrl = previewThumb, platform = "reddit",
                        downloadUrl  = dlUrl)
                }
            }

            // Gallery (multiple images / animated GIFs / videos)
            postData.optJSONObject("media_metadata")?.let { metadata ->
                for (key in metadata.keys()) {
                    metadata.optJSONObject(key)?.let { item ->
                        if (item.optString("status") != "valid") return@let
                        val s = item.optJSONObject("s") ?: return@let
                        val mp4 = s.optString("mp4", "").unescape()
                        val gif = s.optString("gif", "").unescape()
                        val img = s.optString("u",   "").unescape()
                        val w   = s.optInt("x", 0)
                        val h   = s.optInt("y", 0)
                        when {
                            mp4.isNotEmpty() -> results += MediaItem(mp4, MediaType.VIDEO, title,
                                width = w, height = h, platform = "reddit")
                            gif.isNotEmpty() -> results += MediaItem(gif, MediaType.GIF, title,
                                width = w, height = h)
                            img.isNotEmpty() -> results += MediaItem(img, MediaType.IMAGE, title,
                                thumbnailUrl = img, width = w, height = h)
                        }
                    }
                }
            }

            // Direct post URL (i.redd.it image, external link, etc.)
            if (results.none { it.type == MediaType.VIDEO } && postUrl.isNotEmpty()) {
                val type = MediaExtractor.detectTypeByExtension(postUrl)
                if (type != MediaType.UNKNOWN) {
                    results += MediaItem(postUrl, type, title, thumbnailUrl = previewThumb)
                }
            }

            // Fallback: preview image
            if (results.isEmpty() && previewThumb != null) {
                results += MediaItem(previewThumb, MediaType.IMAGE, "$title (Preview)")
            }
        }

        return results.distinctBy { it.url }.filter { it.url.isNotEmpty() }
    }

    private fun previewImage(postData: org.json.JSONObject): String? =
        postData.optJSONObject("preview")
            ?.optJSONArray("images")
            ?.optJSONObject(0)
            ?.optJSONObject("source")
            ?.optString("url", "")
            ?.unescape()
            ?.takeIf { it.isNotEmpty() }

    private fun buildRedditApiUrl(url: String): String? {
        val clean = url.split("?")[0].trimEnd('/')
        return if ("reddit.com/r/" in clean || "reddit.com/u/" in clean)
            "$clean.json?raw_json=1" else null
    }

    // ── Google Drive ──────────────────────────────────────────────────────────

    private fun extractDrive(url: String): List<MediaItem> {
        val idMatch = Regex("""/(?:file/d|open\?id=)/([a-zA-Z0-9_-]{20,})""").find(url)
            ?: Regex("""[?&]id=([a-zA-Z0-9_-]{20,})""").find(url)
        val fileId = idMatch?.groupValues?.get(1) ?: return emptyList()

        val downloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"
        val thumbUrl    = "https://drive.google.com/thumbnail?id=$fileId&sz=w400-h300"

        return listOf(
            MediaItem(downloadUrl, MediaType.UNKNOWN,
                "Google Drive File",
                thumbnailUrl = thumbUrl,
                platform     = "googledrive")
        )
    }

    // ── Imgur ─────────────────────────────────────────────────────────────────

    private fun extractImgur(url: String, client: OkHttpClient): List<MediaItem> {
        // Fetch the page and read OG tags — much more reliable than URL heuristics
        val html = fetch(url, client) ?: return emptyList()
        val doc  = Jsoup.parse(html, url)
        val results = mutableListOf<MediaItem>()

        // OG video (gifv / mp4)
        doc.select("meta[property=og:video], meta[property=og:video:secure_url]")
            .firstOrNull()?.attr("content")?.takeIf { it.isNotEmpty() }?.let { src ->
                val thumb = doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
                results += MediaItem(src, MediaType.VIDEO, doc.title(),
                    thumbnailUrl = thumb, platform = "imgur")
            }

        // OG image (still image or first GIF frame)
        if (results.isEmpty()) {
            doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
                ?.takeIf { it.isNotEmpty() }?.let { src ->
                    val type = if (".gif" in src) MediaType.GIF else MediaType.IMAGE
                    results += MediaItem(src, type, doc.title(), platform = "imgur")
                }
        }

        return results
    }

    // ── Giphy ─────────────────────────────────────────────────────────────────

    private fun extractGiphy(url: String, client: OkHttpClient): List<MediaItem> {
        val html = fetch(url, client) ?: return emptyList()
        val doc  = Jsoup.parse(html, url)
        val results = mutableListOf<MediaItem>()
        val pageTitle = doc.title().ifEmpty { "Giphy" }

        // CDN GIF — direct animated URL from Giphy's media CDN
        val gifCdnPattern = Regex("""https://media\d*\.giphy\.com/media/[^"'\s]+?/giphy\.gif""")
        gifCdnPattern.find(html)?.value?.let { gifUrl ->
            results += MediaItem(gifUrl, MediaType.GIF, pageTitle, thumbnailUrl = gifUrl)
        }

        // MP4 version (lighter for playback)
        val mp4CdnPattern = Regex("""https://media\d*\.giphy\.com/media/[^"'\s]+?/giphy\.mp4""")
        mp4CdnPattern.find(html)?.value?.let { mp4 ->
            val thumb = results.firstOrNull()?.url
            results += MediaItem(mp4, MediaType.VIDEO, pageTitle,
                thumbnailUrl = thumb, platform = "giphy")
        }

        // Fallback — OG image
        if (results.isEmpty()) {
            doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
                ?.takeIf { it.isNotEmpty() }?.let { src ->
                    val type = if (".gif" in src) MediaType.GIF else MediaType.IMAGE
                    results += MediaItem(src, type, pageTitle)
                }
        }

        return results.distinctBy { it.url }
    }

    // ── Vimeo ─────────────────────────────────────────────────────────────────

    private fun extractVimeo(url: String, client: OkHttpClient): List<MediaItem> {
        val videoId = Regex("""vimeo\.com/(\d+)""").find(url)?.groupValues?.get(1)
            ?: return emptyList()

        var title    = "Vimeo Video"
        var thumbUrl = ""
        runCatching {
            val enc  = URLEncoder.encode("https://vimeo.com/$videoId", "UTF-8")
            val json = fetch("https://vimeo.com/api/oembed.json?url=$enc", client)
            if (!json.isNullOrBlank()) {
                val obj  = org.json.JSONObject(json)
                title    = obj.optString("title", title)
                thumbUrl = obj.optString("thumbnail_url", "")
            }
        }

        return listOf(
            MediaItem(url, MediaType.VIDEO, title,
                thumbnailUrl = thumbUrl.takeIf { it.isNotEmpty() },
                platform     = "vimeo")
        )
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private fun fetch(url: String, client: OkHttpClient): String? = runCatching {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent",
                "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.google.com/")
            .build()
        client.newCall(req).execute().use { it.body?.string() }
    }.getOrNull()

    /** Decode HTML-entity-encoded ampersands from Reddit/JSON URLs. */
    private fun String.unescape(): String = replace("&amp;", "&")
}
