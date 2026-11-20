package com.musicforme.app.data.repository

import com.musicforme.app.data.local.FavoriteDao
import com.musicforme.app.data.local.FavoriteEntity
import com.musicforme.app.data.local.MediaStoreScanner
import com.musicforme.app.data.model.Song
import com.musicforme.app.data.remote.MusicApiClient
import com.musicforme.app.data.remote.YouTubeMusicExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val mediaStoreScanner: MediaStoreScanner,
    private val favoriteDao: FavoriteDao
) {
    suspend fun getLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val scanned = mediaStoreScanner.scanLocalSongs()
        // Map with favorite state
        scanned.map { song ->
            val isFav = favoriteDao.isFavoriteDirect(song.id)
            song.copy(isFavorite = isFav)
        }
    }

    suspend fun searchOnlineSongs(query: String = ""): List<Song> = withContext(Dispatchers.IO) {
        val term = query.trim().ifBlank { "nhạc trẻ hot nhất hiện nay" }
        val songs = try {
            val ytSongs = YouTubeMusicExtractor.searchSongs(term)
            if (ytSongs.isNotEmpty()) {
                ytSongs
            } else {
                // Fallback to sample list if search returns empty
                MusicApiClient.getSampleOnlineSongs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sample streaming list when network fails
            MusicApiClient.getSampleOnlineSongs()
        }

        songs.map { song ->
            val isFav = favoriteDao.isFavoriteDirect(song.id)
            song.copy(isFavorite = isFav)
        }
    }

    fun getFavoriteSongs(): Flow<List<Song>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toSong() }
        }
    }

    suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        val isCurrentlyFav = favoriteDao.isFavoriteDirect(song.id)
        if (isCurrentlyFav) {
            favoriteDao.deleteFavoriteById(song.id)
            false
        } else {
            favoriteDao.insertFavorite(FavoriteEntity.fromSong(song))
            true
        }
    }
}
