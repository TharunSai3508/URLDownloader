package com.example.urldownloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object Downloader {

    fun download(context: Context, url: String) {

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                Uri.parse(url).lastPathSegment ?: "file"
            )

        val manager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        manager.enqueue(request)
    }
}