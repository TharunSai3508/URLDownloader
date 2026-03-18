package com.example.urldownloader

import com.example.urldownloader.data.MediaExtractor
import com.example.urldownloader.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaExtractorTest {

    @Test
    fun resolveInput_extractsUrlFromSharedText() {
        val resolved = MediaExtractor.resolveInput("Check this out https://example.com/files/book.epub right now")

        assertNotNull(resolved)
        assertEquals("https://example.com/files/book.epub", resolved?.url)
    }

    @Test
    fun normalizeUrl_addsSchemeForBareDomain() {
        val resolved = MediaExtractor.resolveInput("www.example.com/path/video.mp4")

        assertEquals("https://www.example.com/path/video.mp4", resolved?.url)
    }

    @Test
    fun detectTypeByExtension_supportsCommonDocumentAndNovelFormats() {
        assertEquals(MediaType.EBOOK, MediaExtractor.detectTypeByExtension("https://cdn.example.com/story.epub"))
        assertEquals(MediaType.PDF, MediaExtractor.detectTypeByExtension("https://cdn.example.com/guide.pdf"))
        assertEquals(MediaType.AUDIO, MediaExtractor.detectTypeByExtension("https://cdn.example.com/podcast.m4a"))
    }
}
