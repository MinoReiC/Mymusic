package com.musicforme.app.ui.player

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.musicforme.app.MusicApplication
import com.musicforme.app.R
import com.musicforme.app.data.model.Song
import com.musicforme.app.databinding.ActivityPlayerBinding
import com.musicforme.app.service.PlayerManager
import com.musicforme.app.utils.TimeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var isUserTrackingSeekBar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observePlaybackState()
    }

    private fun setupListeners() {
        binding.btnCollapse.setOnClickListener {
            finish()
        }

        binding.fabPlayPause.setOnClickListener {
            PlayerManager.togglePlayPause()
        }

        binding.btnNext.setOnClickListener {
            PlayerManager.skipToNext()
        }

        binding.btnPrev.setOnClickListener {
            PlayerManager.skipToPrevious()
        }

        binding.btnShuffle.setOnClickListener {
            PlayerManager.toggleShuffle()
        }

        binding.btnRepeat.setOnClickListener {
            PlayerManager.toggleRepeat()
        }

        binding.btnPlayerFavorite.setOnClickListener {
            val song = PlayerManager.currentSong.value ?: return@setOnClickListener
            lifecycleScope.launch {
                val isNowFav = MusicApplication.instance.repository.toggleFavorite(song)
                updateFavoriteIcon(isNowFav)
            }
        }

        binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = TimeUtils.formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserTrackingSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserTrackingSeekBar = false
                seekBar?.let {
                    PlayerManager.seekTo(it.progress.toLong())
                }
            }
        })
    }

    private fun observePlaybackState() {
        // Observe Current Song
        lifecycleScope.launch {
            PlayerManager.currentSong.collectLatest { song ->
                song?.let { updateSongUI(it) } ?: finish()
            }
        }

        // Observe Playing State
        lifecycleScope.launch {
            PlayerManager.isPlaying.collectLatest { isPlaying ->
                binding.fabPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }
        }

        // Observe Duration
        lifecycleScope.launch {
            PlayerManager.duration.collectLatest { durationMs ->
                binding.playerSeekBar.max = durationMs.toInt()
                binding.tvTotalTime.text = TimeUtils.formatDuration(durationMs)
            }
        }

        // Observe Current Position
        lifecycleScope.launch {
            PlayerManager.currentPosition.collectLatest { positionMs ->
                if (!isUserTrackingSeekBar) {
                    binding.playerSeekBar.progress = positionMs.toInt()
                    binding.tvCurrentTime.text = TimeUtils.formatDuration(positionMs)
                }
            }
        }

        // Observe Shuffle Mode
        lifecycleScope.launch {
            PlayerManager.isShuffle.collectLatest { isShuffle ->
                binding.btnShuffle.setColorFilter(
                    ContextCompat.getColor(
                        this@PlayerActivity,
                        if (isShuffle) R.color.control_active else R.color.control_inactive
                    )
                )
            }
        }

        // Observe Repeat Mode
        lifecycleScope.launch {
            PlayerManager.repeatMode.collectLatest { repeatMode ->
                val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
                binding.btnRepeat.setColorFilter(
                    ContextCompat.getColor(
                        this@PlayerActivity,
                        if (isRepeatActive) R.color.control_active else R.color.control_inactive
                    )
                )
            }
        }
    }

    private fun updateSongUI(song: Song) {
        binding.tvPlayerTitle.text = song.title
        binding.tvPlayerArtist.text = if (song.album.isNotBlank()) {
            "${song.artist} • ${song.album}"
        } else {
            song.artist
        }

        Glide.with(this)
            .load(song.albumArtUri)
            .placeholder(R.drawable.bg_album_placeholder)
            .error(R.drawable.bg_album_placeholder)
            .transform(CenterCrop(), RoundedCorners(32))
            .into(binding.imgPlayerAlbumArt)

        updateFavoriteIcon(song.isFavorite)
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnPlayerFavorite.setImageResource(R.drawable.ic_favorite)
            binding.btnPlayerFavorite.setColorFilter(
                ContextCompat.getColor(this, R.color.favorite_active)
            )
        } else {
            binding.btnPlayerFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.btnPlayerFavorite.setColorFilter(
                ContextCompat.getColor(this, R.color.text_secondary)
            )
        }
    }
}
