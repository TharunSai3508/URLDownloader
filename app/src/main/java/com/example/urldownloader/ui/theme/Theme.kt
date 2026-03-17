package com.example.urldownloader.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun URLDownloaderTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}