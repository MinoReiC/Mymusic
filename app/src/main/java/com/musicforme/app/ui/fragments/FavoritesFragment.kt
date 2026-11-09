package com.musicforme.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicforme.app.MusicApplication
import com.musicforme.app.databinding.FragmentFavoritesBinding
import com.musicforme.app.service.PlayerManager
import com.musicforme.app.ui.MainViewModel
import com.musicforme.app.ui.MainViewModelFactory
import com.musicforme.app.ui.adapter.SongAdapter
import com.musicforme.app.utils.DownloadHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(MusicApplication.instance.repository)
    }

    private lateinit var songAdapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song, playlist ->
                PlayerManager.playSong(song, playlist)
            },
            onFavoriteClick = { song ->
                viewModel.toggleFavorite(song)
            },
            onDownloadClick = { song ->
                DownloadHelper.downloadSong(requireContext(), song)
            }
        )

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredFavoriteSongs.collectLatest { songs ->
                songAdapter.submitList(songs)
                if (songs.isEmpty()) {
                    binding.layoutEmptyFavorites.visibility = View.VISIBLE
                } else {
                    binding.layoutEmptyFavorites.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
