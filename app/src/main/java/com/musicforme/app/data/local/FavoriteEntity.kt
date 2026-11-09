package com.musicforme.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicforme.app.data.model.Song

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val albumArtUri: String?,
    val isOnline: Boolean,
    val downloadUrl: String?
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uriString = uriString,
            albumArtUri = albumArtUri,
            isOnline = isOnline,
            isFavorite = true,
            downloadUrl = downloadUrl
        )
    }

    companion object {
        fun fromSong(song: Song): FavoriteEntity {
            return FavoriteEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                uriString = song.uriString,
                albumArtUri = song.albumArtUri,
                isOnline = song.isOnline,
                downloadUrl = song.downloadUrl
            )
        }
    }
}
