package com.musicforme.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.musicforme.app.MusicApplication
import com.musicforme.app.R
import com.musicforme.app.data.model.Song
import com.musicforme.app.databinding.ActivityMainBinding
import com.musicforme.app.service.PlayerManager
import com.musicforme.app.ui.fragments.FavoritesFragment
import com.musicforme.app.ui.fragments.LibraryFragment
import com.musicforme.app.ui.fragments.OnlineFragment
import com.musicforme.app.ui.player.PlayerActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(MusicApplication.instance.repository)
    }

    private val libraryFragment by lazy { LibraryFragment() }
    private val onlineFragment by lazy { OnlineFragment() }
    private val favoritesFragment by lazy { FavoritesFragment() }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupSearch()
        setupMiniPlayer()
        observePlaybackState()
        requestNotificationPermissionIfNeeded()
    }

    private fun setupNavigation() {
        // Default to Library fragment
        viewModel.currentTab.value = 0
        replaceFragment(libraryFragment)

        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_library -> {
                    viewModel.currentTab.value = 0
                    replaceFragment(libraryFragment)
                    true
                }
                R.id.nav_online -> {
                    viewModel.currentTab.value = 1
                    replaceFragment(onlineFragment)
                    true
                }
                R.id.nav_favorites -> {
                    viewModel.currentTab.value = 2
                    replaceFragment(favoritesFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.searchQuery.value = text?.toString().orEmpty()
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (viewModel.currentTab.value == 1) {
                    viewModel.searchOnline(v.text?.toString().orEmpty())
                }
                v.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener {
            val currentSong = PlayerManager.currentSong.value ?: return@setOnClickListener
            val intent = Intent(this, PlayerActivity::class.java)
            startActivity(intent)
        }

        binding.miniBtnPlayPause.setOnClickListener {
            PlayerManager.togglePlayPause()
        }

        binding.miniBtnNext.setOnClickListener {
            PlayerManager.skipToNext()
        }
    }

    private fun observePlaybackState() {
        // Observe Current Song
        lifecycleScope.launch {
            PlayerManager.currentSong.collectLatest { song ->
                if (song != null) {
                    binding.miniPlayer.visibility = View.VISIBLE
                    updateMiniPlayerUI(song)
                } else {
                    binding.miniPlayer.visibility = View.GONE
                }
            }
        }

        // Observe Play/Pause state for mini player button
        lifecycleScope.launch {
            PlayerManager.isPlaying.collectLatest { isPlaying ->
                binding.miniBtnPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }
        }
    }

    private fun updateMiniPlayerUI(song: Song) {
        binding.miniSongTitle.text = song.title
        binding.miniSongArtist.text = song.artist

        Glide.with(this)
            .load(song.albumArtUri)
            .placeholder(R.drawable.bg_album_placeholder)
            .error(R.drawable.bg_album_placeholder)
            .transform(CenterCrop(), RoundedCorners(12))
            .into(binding.miniAlbumArt)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
