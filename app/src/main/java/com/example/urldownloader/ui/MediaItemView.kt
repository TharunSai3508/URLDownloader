package com.example.urldownloader.ui

import android.content.Context
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.example.urldownloader.Downloader
import com.example.urldownloader.MediaItem
import com.example.urldownloader.MediaType
import com.example.urldownloader.download.Downloader

@Composable
fun MediaItemView(media: MediaItem, context: Context) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {

        Column(Modifier.padding(12.dp)) {

            when (media.type) {

                MediaType.IMAGE,
                MediaType.GIF -> {

                    AsyncImage(
                        model = media.url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                MediaType.VIDEO -> {

                    AndroidView(
                        factory = { ctx ->

                            VideoView(ctx).apply {

                                setVideoURI(Uri.parse(media.url))
                                start()

                            }

                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                else -> {

                    Text(media.url)
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {

                    Downloader.download(context, media.url)

                }
            ) {
                Text("Download")
            }
        }
    }
}