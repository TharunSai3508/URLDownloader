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

    fun platform(url: String): String? {
        val host = runCatching { java.net.URI(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()
            ?: return null
        return when {
            host == "youtube.com" || host == "m.youtube.com" || host == "youtu.be" -> "youtube"
            host.endsWith("reddit.com") || host == "redd.it" || host.endsWith("redd.it") -> "reddit"
            host == "drive.google.com" -> "googledrive"
            host == "twitter.com" || host == "x.com" || host.endsWith("x.com") -> "twitter"
            host.endsWith("instagram.com") -> "instagram"
            host.endsWith("facebook.com") || host == "fb.com" -> "facebook"
            host.endsWith("imgur.com") -> "imgur"
            host.endsWith("giphy.com") -> "giphy"
            host.endsWith("tiktok.com") -> "tiktok"
            host.endsWith("vimeo.com") -> "vimeo"
            host.endsWith("dailymotion.com") || host == "dai.ly" -> "dailymotion"
            host.endsWith("twitch.tv") || host.endsWith("clips.twitch.tv") -> "twitch"
            host.endsWith("pinterest.com") || host == "pin.it" -> "pinterest"
            host.endsWith("tumblr.com") -> "tumblr"
            host.endsWith("soundcloud.com") -> "soundcloud"
            host.endsWith("spotify.com") || host == "open.spotify.com" -> "spotify"
            host.endsWith("telegram.me") || host.endsWith("t.me") -> "telegram"
            else -> null
        }
    }

    fun extract(url: String, client: OkHttpClient): List<MediaItem>? =
        when (platform(url)) {
            "youtube"     -> extractYouTube(url, client)
            "reddit"      -> extractReddit(url, client)
            "googledrive" -> extractDrive(url)
            "imgur"       -> extractImgur(url, client)
            "giphy"       -> extractGiphy(url, client)
            "vimeo"       -> extractVimeo(url, client)
            else          -> null
        }

    private fun extractYouTube(url: String, client: OkHttpClient): List<MediaItem> {
        val videoId = youTubeVideoId(url) ?: return emptyList()
        val thumbHq = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        val thumbMax = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"

        var title = "YouTube Video"
        var channelName = ""
        runCatching {
            val enc = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
            val json = fetch("https://www.youtube.com/oembed?url=$enc&format=json", client)
            if (!json.isNullOrBlank()) {
                val obj = org.json.JSONObject(json)
                title = obj.optString("title", title)
                channelName = obj.optString("author_name", "")
            }
        }

        val displayTitle = if (channelName.isNotEmpty()) "$title · $channelName" else title
        return buildList {
            add(MediaItem(thumbMax, MediaType.IMAGE, "Thumbnail: $title", thumbnailUrl = thumbHq, platform = "youtube"))
            add(MediaItem(url, MediaType.VIDEO, displayTitle, thumbnailUrl = thumbHq, platform = "youtube"))
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

    private fun extractReddit(url: String, client: OkHttpClient): List<MediaItem> {
        val apiUrl = buildRedditApiUrl(url) ?: return emptyList()
        val json = fetch(apiUrl, client) ?: return emptyList()
        val results = mutableListOf<MediaItem>()

        runCatching {
            val arr = org.json.JSONArray(json)
            val postData = arr.getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0)
                .getJSONObject("data")

            val title = postData.optString("title", "Reddit Post")
            val postUrl = postData.optString("url", "")
            val previewThumb = previewImage(postData)

            postData.optJSONObject("media")?.optJSONObject("reddit_video")?.let { rv ->
                val dashUrl = rv.optString("dash_url", "")
                val fallbackUrl = rv.optString("fallback_url", "").replace("?source=fallback", "")
                val streamUrl = dashUrl.takeIf { it.isNotEmpty() } ?: fallbackUrl
                val dlUrl = fallbackUrl.takeIf { it.isNotEmpty() && it != streamUrl }
                if (streamUrl.isNotEmpty()) {
                    results += MediaItem(streamUrl, MediaType.VIDEO, title, thumbnailUrl = previewThumb, platform = "reddit", downloadUrl = dlUrl)
                }
            }

            postData.optJSONObject("media_metadata")?.let { metadata ->
                for (key in metadata.keys()) {
                    metadata.optJSONObject(key)?.let { item ->
                        if (item.optString("status") != "valid") return@let
                        val s = item.optJSONObject("s") ?: return@let
                        val mp4 = s.optString("mp4", "").unescape()
                        val gif = s.optString("gif", "").unescape()
                        val img = s.optString("u", "").unescape()
                        val w = s.optInt("x", 0)
                        val h = s.optInt("y", 0)
                        when {
                            mp4.isNotEmpty() -> results += MediaItem(mp4, MediaType.VIDEO, title, width = w, height = h, platform = "reddit")
                            gif.isNotEmpty() -> results += MediaItem(gif, MediaType.GIF, title, width = w, height = h)
                            img.isNotEmpty() -> results += MediaItem(img, MediaType.IMAGE, title, thumbnailUrl = img, width = w, height = h)
                        }
                    }
                }
            }

            if (results.none { it.type == MediaType.VIDEO } && postUrl.isNotEmpty()) {
                val type = MediaExtractor.detectTypeByExtension(postUrl)
                if (type != MediaType.UNKNOWN) {
                    results += MediaItem(postUrl, type, title, thumbnailUrl = previewThumb)
                }
            }

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
        val clean = url.substringBefore('?').trimEnd('/')
        return if ("reddit.com/r/" in clean || "reddit.com/u/" in clean || "reddit.com/comments/" in clean)
            "$clean.json?raw_json=1" else null
    }

    private fun extractDrive(url: String): List<MediaItem> {
        val idMatch = Regex("""/(?:file/d|open\?id=)/([a-zA-Z0-9_-]{20,})""").find(url)
            ?: Regex("""[?&]id=([a-zA-Z0-9_-]{20,})""").find(url)
        val fileId = idMatch?.groupValues?.get(1) ?: return emptyList()

        val downloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"
        val thumbUrl = "https://drive.google.com/thumbnail?id=$fileId&sz=w400-h300"

        return listOf(MediaItem(downloadUrl, MediaType.UNKNOWN, "Google Drive File", thumbnailUrl = thumbUrl, platform = "googledrive"))
    }

    private fun extractImgur(url: String, client: OkHttpClient): List<MediaItem> {
        val html = fetch(url, client) ?: return emptyList()
        val doc = Jsoup.parse(html, url)
        val results = mutableListOf<MediaItem>()

        doc.select("meta[property=og:video], meta[property=og:video:secure_url]")
            .firstOrNull()?.attr("content")?.takeIf { it.isNotEmpty() }?.let { src ->
                val thumb = doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
                results += MediaItem(src, MediaType.VIDEO, doc.title(), thumbnailUrl = thumb, platform = "imgur")
            }

        if (results.isEmpty()) {
            doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
                ?.takeIf { it.isNotEmpty() }?.let { src ->
                    val type = if (".gif" in src) MediaType.GIF else MediaType.IMAGE
                    results += MediaItem(src, type, doc.title(), platform = "imgur")
                }
        }

        return results
    }

    private fun extractGiphy(url: String, client: OkHttpClient): List<MediaItem> {
        val html = fetch(url, client) ?: return emptyList()
        val doc = Jsoup.parse(html, url)
        val results = mutableListOf<MediaItem>()
        val pageTitle = doc.title().ifEmpty { "Giphy" }

        val gifCdnPattern = Regex("""https://media\d*\.giphy\.com/media/[^"'\s]+?/giphy\.gif""")
        gifCdnPattern.find(html)?.value?.let { gifUrl ->
            results += MediaItem(gifUrl, MediaType.GIF, pageTitle, thumbnailUrl = gifUrl)
        }

        val mp4CdnPattern = Regex("""https://media\d*\.giphy\.com/media/[^"'\s]+?/giphy\.mp4""")
        mp4CdnPattern.find(html)?.value?.let { mp4 ->
            val thumb = results.firstOrNull()?.url
            results += MediaItem(mp4, MediaType.VIDEO, pageTitle, thumbnailUrl = thumb, platform = "giphy")
        }

        if (results.isEmpty()) {
            doc.select("meta[property=og:image]").firstOrNull()?.attr("content")
                ?.takeIf { it.isNotEmpty() }?.let { src ->
                    val type = if (".gif" in src) MediaType.GIF else MediaType.IMAGE
                    results += MediaItem(src, type, pageTitle)
                }
        }

        return results.distinctBy { it.url }
    }

    private fun extractVimeo(url: String, client: OkHttpClient): List<MediaItem> {
        val videoId = Regex("""vimeo\.com/(\d+)""").find(url)?.groupValues?.get(1)
            ?: return emptyList()

        var title = "Vimeo Video"
        var thumbUrl = ""
        runCatching {
            val enc = URLEncoder.encode("https://vimeo.com/$videoId", "UTF-8")
            val json = fetch("https://vimeo.com/api/oembed.json?url=$enc", client)
            if (!json.isNullOrBlank()) {
                val obj = org.json.JSONObject(json)
                title = obj.optString("title", title)
                thumbUrl = obj.optString("thumbnail_url", "")
            }
        }

        return listOf(MediaItem(url, MediaType.VIDEO, title, thumbnailUrl = thumbUrl.takeIf { it.isNotEmpty() }, platform = "vimeo"))
    }

    private fun fetch(url: String, client: OkHttpClient): String? = runCatching {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.google.com/")
            .build()
        client.newCall(req).execute().use { it.body?.string() }
    }.getOrNull()

    private fun String.unescape(): String = replace("&amp;", "&")
}
