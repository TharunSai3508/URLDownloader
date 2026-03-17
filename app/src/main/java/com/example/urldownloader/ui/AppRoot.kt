package com.example.urldownloader.ui


import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun AppRoot() {

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {

        delay(2000)

        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        MainScreen()
    }
}