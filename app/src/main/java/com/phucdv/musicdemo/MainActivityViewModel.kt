package com.phucdv.musicdemo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(val repository: MusicRepository) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs = _allSongs.asStateFlow()

    val isPlayingFlow = MutableStateFlow(false)
    val playingSongFlow = MutableStateFlow<Song?>(null)

    init {
        viewModelScope.launch {
            _allSongs.value = repository.getAllSongs()
        }
    }

    class Factory(val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(
                MusicRepository(context.applicationContext)
            ) as T
        }
    }
}