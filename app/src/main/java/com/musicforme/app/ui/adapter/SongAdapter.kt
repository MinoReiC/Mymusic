package com.musicforme.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.musicforme.app.R
import com.musicforme.app.data.model.Song
import com.musicforme.app.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClick: (Song, List<Song>) -> Unit,
    private val onFavoriteClick: (Song) -> Unit,
    private val onDownloadClick: ((Song) -> Unit)? = null
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.tvSongTitle.text = song.title
            binding.tvSongArtist.text = if (song.album.isNotBlank()) {
                "${song.artist} • ${song.album}"
            } else {
                song.artist
            }

            // Load album art with rounded corners
            Glide.with(binding.imgAlbumArt.context)
                .load(song.albumArtUri)
                .placeholder(R.drawable.bg_album_placeholder)
                .error(R.drawable.bg_album_placeholder)
                .transform(CenterCrop(), RoundedCorners(16))
                .into(binding.imgAlbumArt)

            // Favorite Icon State
            if (song.isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                binding.btnFavorite.setColorFilter(
                    ContextCompat.getColor(binding.btnFavorite.context, R.color.favorite_active)
                )
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                binding.btnFavorite.setColorFilter(
                    ContextCompat.getColor(binding.btnFavorite.context, R.color.text_secondary)
                )
            }

            // Download Button for Online Songs
            if (song.isOnline && onDownloadClick != null) {
                binding.btnDownload.visibility = View.VISIBLE
                binding.btnDownload.setOnClickListener {
                    onDownloadClick.invoke(song)
                }
            } else {
                binding.btnDownload.visibility = View.GONE
            }

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(song)
            }

            binding.root.setOnClickListener {
                onSongClick(song, currentList)
            }
        }
    }

    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}
