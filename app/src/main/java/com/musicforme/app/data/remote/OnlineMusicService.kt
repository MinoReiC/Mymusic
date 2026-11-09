package com.musicforme.app.data.remote

import retrofit2.http.GET

interface OnlineMusicService {
    @GET("music_catalog.json")
    suspend fun getTrendingSongs(): List<OnlineSongDto>
}
