package com.musicforme.app.data.remote

import com.google.gson.annotations.SerializedName
import com.musicforme.app.data.model.Song

data class OnlineSongDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("audio_url") val audioUrl: String,
    @SerializedName("cover_art") val coverArt: String?
) {
    fun toSong(): Song {
        return Song(
            id = "online_$id",
            title = title,
            artist = artist,
            album = album ?: "Online Streaming",
            duration = duration ?: 0L,
            uriString = audioUrl,
            albumArtUri = coverArt,
            isOnline = true,
            downloadUrl = audioUrl
        )
    }
}
