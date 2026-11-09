package com.musicforme.app.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.musicforme.app.data.model.Song

object DownloadHelper {

    fun downloadSong(context: Context, song: Song) {
        val downloadUrl = song.downloadUrl ?: song.uriString
        if (downloadUrl.isBlank() || !downloadUrl.startsWith("http")) {
            Toast.makeText(context, "Link tải không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val sanitizedTitle = song.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val fileName = "$sanitizedTitle.mp3"

            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle(song.title)
                .setDescription("Đang tải xuống bởi MusicForMe")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "MusicForMe/$fileName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
            Toast.makeText(context, "Đã bắt đầu tải: ${song.title}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Không thể tải: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
