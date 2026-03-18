package com.example.urldownloader.model

enum class MediaType {
    IMAGE,
    GIF,
    VIDEO,
    AUDIO,
    PDF,
    DOCUMENT,
    ARCHIVE,
    SUBTITLE,   // .srt .vtt .ass .ssa
    EBOOK,      // .epub .mobi .azw
    UNKNOWN
}

data class MediaItem(
    val url: String,
    val type: MediaType,
    val title: String = "",
    val mimeType: String? = null,
    val fileSize: Long = -1L,
    /** Pre-resolved thumbnail image URL (used in grid cards) */
    val thumbnailUrl: String? = null,
    /** Source platform tag: "youtube", "reddit", "googledrive", "imgur", etc. */
    val platform: String? = null,
    /** Explicit pixel width from HTML/API (0 = unknown) */
    val width: Int = 0,
    /** Explicit pixel height from HTML/API (0 = unknown) */
    val height: Int = 0
)
