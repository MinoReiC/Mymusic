package com.musicforme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicforme.app.data.model.Song
import com.musicforme.app.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: MusicRepository) : ViewModel() {

    val currentTab = MutableStateFlow(0) // 0: Library, 1: Online, 2: Favorites

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _onlineSongs = MutableStateFlow<List<Song>>(emptyList())
    val onlineSongs: StateFlow<List<Song>> = _onlineSongs.asStateFlow()

    val favoriteSongs: StateFlow<List<Song>> = repository.getFavoriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingOnline = MutableStateFlow(false)
    val isLoadingOnline: StateFlow<Boolean> = _isLoadingOnline.asStateFlow()

    val searchQuery = MutableStateFlow("")

    val filteredLocalSongs: StateFlow<List<Song>> = combine(_localSongs, searchQuery) { songs, query ->
        filterSongs(songs, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFavoriteSongs: StateFlow<List<Song>> = combine(favoriteSongs, searchQuery) { songs, query ->
        filterSongs(songs, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var onlineSearchJob: Job? = null

    init {
        // Live search for Online tab with 500ms debounce
        viewModelScope.launch {
            searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    if (currentTab.value == 1) {
                        searchOnline(query)
                    }
                }
        }
    }

    private fun filterSongs(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        val trimmed = query.trim().lowercase()
        return songs.filter {
            it.title.lowercase().contains(trimmed) ||
                    it.artist.lowercase().contains(trimmed) ||
                    it.album.lowercase().contains(trimmed)
        }
    }

    fun loadLocalSongs() {
        viewModelScope.launch {
            val songs = repository.getLocalSongs()
            _localSongs.value = songs
        }
    }

    fun searchOnline(query: String = "") {
        onlineSearchJob?.cancel()
        onlineSearchJob = viewModelScope.launch {
            _isLoadingOnline.value = true
            val results = repository.searchOnlineSongs(query)
            _onlineSongs.value = results
            _isLoadingOnline.value = false
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(song)
            // Update local state in memory immediately
            _localSongs.value = _localSongs.value.map {
                if (it.id == song.id) it.copy(isFavorite = isNowFav) else it
            }
            _onlineSongs.value = _onlineSongs.value.map {
                if (it.id == song.id) it.copy(isFavorite = isNowFav) else it
            }
        }
    }
}
