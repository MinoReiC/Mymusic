package com.musicforme.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L,
    val uriString: String,
    val albumArtUri: String? = null,
    val isOnline: Boolean = false,
    val isFavorite: Boolean = false,
    val downloadUrl: String? = null
) : Parcelable
