package com.example.urldownloader.model

enum class MediaType {
    IMAGE,
    GIF,
    VIDEO,
    AUDIO,
    PDF,
    DOCUMENT,
    ARCHIVE,
    UNKNOWN
}

data class MediaItem(
    val url: String,
    val type: MediaType,
    val title: String = "",
    val mimeType: String? = null,
    val fileSize: Long = -1L
)
