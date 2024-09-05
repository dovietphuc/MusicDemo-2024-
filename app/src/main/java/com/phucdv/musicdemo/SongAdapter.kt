package com.phucdv.musicdemo

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.phucdv.musicdemo.databinding.SongItemBinding

class SongAdapter(val onItemClick: (Song) -> Unit) : ListAdapter<Song, SongAdapter.SongViewHolder>(object :
    DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem.id == newItem.id
    }
}) {

    var isPlaying = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var playingSong: Song? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            SongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SongViewHolder(val binding: SongItemBinding) : ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.tvTitle.text = song.title
            binding.tvTitle.setOnClickListener {
                onItemClick(song)
            }

            if(isPlaying) {
                if(playingSong == song) {
                    binding.tvTitle.setBackgroundColor(Color.GREEN)
                } else {
                    binding.tvTitle.setBackgroundColor(Color.TRANSPARENT)
                }
            } else {
                if(playingSong == song) {
                    binding.tvTitle.setBackgroundColor(Color.YELLOW)
                } else {
                    binding.tvTitle.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }
}