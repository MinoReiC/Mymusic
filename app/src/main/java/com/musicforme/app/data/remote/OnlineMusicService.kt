package com.musicforme.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OnlineMusicService {
    @GET("search")
    suspend fun searchSongs(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 40
    ): ITunesSearchResponse
}
