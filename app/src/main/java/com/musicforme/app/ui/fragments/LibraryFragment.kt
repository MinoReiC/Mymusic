package com.musicforme.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.musicforme.app.MusicApplication
import com.musicforme.app.databinding.FragmentLibraryBinding
import com.musicforme.app.service.PlayerManager
import com.musicforme.app.ui.MainViewModel
import com.musicforme.app.ui.MainViewModelFactory
import com.musicforme.app.ui.adapter.SongAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(MusicApplication.instance.repository)
    }

    private lateinit var songAdapter: SongAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            binding.layoutPermission.visibility = View.GONE
            viewModel.loadLocalSongs()
        } else {
            binding.layoutPermission.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        checkStoragePermissionAndLoad()
        observeData()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song, playlist ->
                PlayerManager.playSong(song, playlist)
            },
            onFavoriteClick = { song ->
                viewModel.toggleFavorite(song)
            }
        )

        binding.rvSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadLocalSongs()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnGrantPermission.setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun checkStoragePermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            binding.layoutPermission.visibility = View.GONE
            viewModel.loadLocalSongs()
        } else {
            binding.layoutPermission.visibility = View.VISIBLE
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredLocalSongs.collectLatest { songs ->
                songAdapter.submitList(songs)
                if (songs.isEmpty() && binding.layoutPermission.visibility != View.VISIBLE) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
