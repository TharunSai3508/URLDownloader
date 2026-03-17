package com.example.urldownloader.data

import com.example.urldownloader.MediaItem
import com.example.urldownloader.MediaType

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

object MediaExtractor {

    private val client = OkHttpClient()

    private val mediaRegex =
        Regex("""https?:\/\/[^\s"'<>]+?\.(jpg|jpeg|png|webp|gif|mp4|webm|m3u8|mp3)""")

    fun extract(url: String): List<MediaItem> {

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        val response = client.newCall(request).execute()

        val html = response.body?.string() ?: ""

        val doc = Jsoup.parse(html)

        val results = mutableListOf<MediaItem>()

        doc.select("img").forEach {

            val src = it.absUrl("src")

            if (src.isNotEmpty()) {
                results.add(MediaItem(src, detectType(src)))
            }
        }

        doc.select("video source").forEach {

            val src = it.absUrl("src")

            if (src.isNotEmpty()) {
                results.add(MediaItem(src, detectType(src)))
            }
        }

        mediaRegex.findAll(html).forEach {

            val link = it.value

            results.add(MediaItem(link, detectType(link)))
        }

        return results.distinctBy { it.url }
    }

    private fun detectType(url: String): MediaType {

        return when {

            url.endsWith(".jpg") ||
                    url.endsWith(".jpeg") ||
                    url.endsWith(".png") ||
                    url.endsWith(".webp") -> MediaType.IMAGE

            url.endsWith(".gif") -> MediaType.GIF

            url.endsWith(".mp4") ||
                    url.endsWith(".webm") ||
                    url.endsWith(".m3u8") -> MediaType.VIDEO

            url.endsWith(".mp3") -> MediaType.AUDIO

            else -> MediaType.UNKNOWN
        }
    }
}