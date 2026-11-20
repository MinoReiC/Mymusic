package com.musicforme.app.data.remote

import com.google.gson.annotations.SerializedName
import com.musicforme.app.data.model.Song

data class ITunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int = 0,
    @SerializedName("results") val results: List<ITunesTrackDto> = emptyList()
)

data class ITunesTrackDto(
    @SerializedName("trackId") val trackId: Long,
    @SerializedName("trackName") val trackName: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("collectionName") val collectionName: String?,
    @SerializedName("trackTimeMillis") val trackTimeMillis: Long?,
    @SerializedName("previewUrl") val previewUrl: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?
) {
    fun toSong(): Song {
        // Upgrade artwork from 100x100 to 600x600 for sharp display
        val highResArtwork = artworkUrl100?.replace("100x100bb", "600x600bb")
        return Song(
            id = "itunes_$trackId",
            title = trackName ?: "Unknown Title",
            artist = artistName ?: "Unknown Artist",
            album = collectionName ?: "",
            duration = trackTimeMillis ?: 30000L,
            uriString = previewUrl ?: "",
            albumArtUri = highResArtwork,
            isOnline = true,
            downloadUrl = previewUrl
        )
    }
}
