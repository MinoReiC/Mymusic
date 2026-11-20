package com.musicforme.app.data.remote

import com.musicforme.app.data.model.Song
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MusicApiClient {
    // Official iTunes / Apple Music Search API (free, worldwide, no API key needed)
    private const val BASE_URL = "https://itunes.apple.com/"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: OnlineMusicService by lazy {
        retrofit.create(OnlineMusicService::class.java)
    }

    /**
     * Fallback offline sample songs if network is unavailable.
     */
    fun getSampleOnlineSongs(): List<Song> {
        return listOf(
            Song(
                id = "online_1",
                title = "Lofi Chill Beats",
                artist = "MusicForMe Relax",
                album = "Study & Relax Session",
                duration = 180000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = "https://picsum.photos/seed/chill1/400/400",
                isOnline = true,
                downloadUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            ),
            Song(
                id = "online_2",
                title = "Acoustic Sunset Breeze",
                artist = "Acoustic Melody",
                album = "Acoustic Vibes 2024",
                duration = 210000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = "https://picsum.photos/seed/acoustic2/400/400",
                isOnline = true,
                downloadUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            ),
            Song(
                id = "online_3",
                title = "Electro Synth Pulse",
                artist = "Cyber Drift",
                album = "Neon City Nights",
                duration = 240000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                albumArtUri = "https://picsum.photos/seed/synth3/400/400",
                isOnline = true,
                downloadUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            ),
            Song(
                id = "online_4",
                title = "Piano Reverie",
                artist = "Classical Horizons",
                album = "Deep Focus Piano",
                duration = 195000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                albumArtUri = "https://picsum.photos/seed/piano4/400/400",
                isOnline = true,
                downloadUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            ),
            Song(
                id = "online_5",
                title = "Midnight Coffee Jazz",
                artist = "Blue Note Collective",
                album = "Cafe Backgrounds",
                duration = 225000L,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                albumArtUri = "https://picsum.photos/seed/jazz5/400/400",
                isOnline = true,
                downloadUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
            )
        )
    }
}
