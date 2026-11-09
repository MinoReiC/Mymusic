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

    suspend fun getOnlineSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = try {
            val response = MusicApiClient.service.getTrendingSongs()
            response.map { it.toSong() }
        } catch (e: Exception) {
            // Fallback to sample streaming list when remote server is offline
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
