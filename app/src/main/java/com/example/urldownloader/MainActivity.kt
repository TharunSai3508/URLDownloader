package com.example.urldownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.urldownloader.ui.AppRoot
import com.example.urldownloader.ui.theme.URLDownloaderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            URLDownloaderTheme {

                AppRoot()

            }

        }
    }
}