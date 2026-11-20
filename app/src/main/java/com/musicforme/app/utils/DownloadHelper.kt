package com.musicforme.app.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.musicforme.app.data.model.Song
import com.musicforme.app.data.remote.YouTubeMusicExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DownloadHelper {

    fun downloadSong(context: Context, song: Song) {
        val appContext = context.applicationContext
        val rawUrl = song.downloadUrl ?: song.uriString

        val videoId = YouTubeMusicExtractor.extractVideoId(rawUrl)
        if (videoId != null) {
            Toast.makeText(appContext, "Đang lấy link tải full: ${song.title}...", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
                val resolvedUrl = YouTubeMusicExtractor.resolveAudioStreamUrl(videoId)
                withContext(Dispatchers.Main) {
                    if (resolvedUrl != null) {
                        enqueueDownload(
                            context = appContext,
                            songTitle = song.title,
                            url = resolvedUrl,
                            extension = "m4a",
                            userAgent = YouTubeMusicExtractor.VR_USER_AGENT
                        )
                    } else {
                        Toast.makeText(appContext, "Không thể lấy link audio từ YouTube", Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }

        if (rawUrl.isBlank() || !rawUrl.startsWith("http")) {
            Toast.makeText(appContext, "Link tải không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        enqueueDownload(
            context = appContext,
            songTitle = song.title,
            url = rawUrl,
            extension = "mp3",
            userAgent = null
        )
    }

    private fun enqueueDownload(
        context: Context,
        songTitle: String,
        url: String,
        extension: String,
        userAgent: String?
    ) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val sanitizedTitle = songTitle
                .replace("[^\\p{L}\\p{N}._-]".toRegex(), "_")
                .take(60)
                .ifBlank { "music_track" }
            val fileName = "$sanitizedTitle.$extension"

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(songTitle)
                .setDescription("Đang tải xuống bởi MusicForMe")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "MusicForMe/$fileName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent)
            }

            downloadManager.enqueue(request)
            Toast.makeText(context, "Đã bắt đầu tải: $songTitle", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Không thể tải: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
