package com.example.urldownloader

enum class MediaType {
    IMAGE,
    GIF,
    VIDEO,
    AUDIO,
    UNKNOWN
}

data class MediaItem(
    val url: String,
    val type: MediaType
)