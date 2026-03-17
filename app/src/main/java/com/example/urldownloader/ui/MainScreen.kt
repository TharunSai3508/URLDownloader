package com.example.urldownloader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.example.urldownloader.data.MediaExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var results: List<com.example.urldownloader.MediaItem> by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Media Downloader") })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Paste URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row {

                Button(onClick = {

                    if (url.isBlank()) return@Button

                    loading = true

                    scope.launch {

                        val data = withContext(Dispatchers.IO) {
                            MediaExtractor.extract(url)
                        }

                        results = data
                        loading = false
                    }

                }) {
                    Text("Analyze")
                }

                Spacer(Modifier.width(12.dp))

                Button(onClick = {
                    url = ""
                    results = emptyList()
                }) {
                    Text("Clear")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (loading) {

                CircularProgressIndicator()

            } else {

                LazyColumn {

                    items(results) { media ->

                        MediaItemView(media, context)

                    }
                }
            }
        }
    }
}