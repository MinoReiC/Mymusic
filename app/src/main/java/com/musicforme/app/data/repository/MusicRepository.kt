package com.musicforme.app.data.repository

import com.musicforme.app.data.local.FavoriteDao
import com.musicforme.app.data.local.FavoriteEntity
import com.musicforme.app.data.local.MediaStoreScanner
import com.musicforme.app.data.model.Song
import com.musicforme.app.data.remote.MusicApiClient
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

    suspend fun searchOnlineSongs(query: String = "top hits"): List<Song> = withContext(Dispatchers.IO) {
        val term = query.trim().ifBlank { "top hits" }
        val songs = try {
            val response = MusicApiClient.service.searchSongs(term = term, limit = 40)
            response.results
                .filter { !it.previewUrl.isNullOrBlank() }
                .map { it.toSong() }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sample streaming list when remote server is offline or fails
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
