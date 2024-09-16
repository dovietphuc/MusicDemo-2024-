package com.phucdv.musicdemo

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MusicService : Service() {
    companion object {
        val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        val EXTRA_SONG_ID = "extra_song_id" // Long

        val ACTION_NEXT = "ACTION_NEXT"
        val ACTION_PREV = "ACTION_PREV"
        val ACTION_SEEK = "ACTION_SEEK"
        val EXTRA_SEEK_VALUE = "extra_seek_value" // Long

        val ACTION_REPEAT = "ACTION_REPEAT"
        val EXTRA_REPEAT_MODE = "extra_repeat_mode" // Int (0, 1, 2)
        val REPEAT_MODE_OFF = 0
        val REPEAT_MODE_ONE = 1
        val REPEAT_MODE_ALL = 2

        val ACTION_SUFFER = "ACTION_SUFFER"
        val EXTRA_SUFFER_MODE = "extra_suffer_mode" // Boolean
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: MusicRepository
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs = _allSongs.asStateFlow()
    val currentPlaying = MutableStateFlow<Pair<Int, Song>?>(null)
    val isPlayingFlow = MutableStateFlow(false)

    val repeatMode = MutableStateFlow<Int>(REPEAT_MODE_OFF)
    val isSuffer = MutableStateFlow<Boolean>(false)

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())

        setOnPreparedListener {
            start()
        }
        setOnCompletionListener {
            playOnEnd()
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
        scope.launch {
            _allSongs.value = repository.getAllSongs()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_PLAY_PAUSE -> {
                val songId = intent.getLongExtra(EXTRA_SONG_ID, -1L)
                if(songId != -1L) {
                    playPause(songId)
                }
            }
            ACTION_NEXT -> {
                playNext()
            }
            ACTION_PREV -> {
                playPrev()
            }
            ACTION_SEEK -> {
                val seekValue = intent.getLongExtra(EXTRA_SEEK_VALUE, -1L)
                if(seekValue != -1L) {
                    seekTo(seekValue)
                }
            }
            ACTION_REPEAT -> {
                val repeatMode = intent.getIntExtra(EXTRA_REPEAT_MODE, -1)
                if(repeatMode != -1) {
                    setRepeatMode(repeatMode)
                }
            }
            ACTION_SUFFER -> {
                val sufferMode = intent.getBooleanExtra(EXTRA_SUFFER_MODE, false)
                setSufferMode(sufferMode)
            }
            else -> {
                throw IllegalArgumentException("Unsupported action ${intent?.action}")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setSufferMode(sufferMode: Boolean) {
        this.isSuffer.value = sufferMode
    }

    private fun setRepeatMode(repeatMode: Int) {
        this.repeatMode.value = repeatMode
    }

    private fun seekTo(seekValue: Long) {
        mediaPlayer.seekTo(seekValue.toInt())
    }

    private fun playPrev() {
        val listSong = allSongs.value
        if(listSong.isEmpty()) return

        val currentPlaying = currentPlaying.value
        val songId = if (currentPlaying == null) {
                listSong[listSong.size - 1].id
            } else if (currentPlaying.first == 0) {
                listSong[listSong.size - 1].id
            } else {
                listSong[(currentPlaying.first ?: 1) - 1].id
            }
        playSong(songId.toLong())
    }

    private fun playOnEnd() {
        val repeatMode = repeatMode.value
        when(repeatMode) {
            REPEAT_MODE_OFF -> {
                isPlayingFlow.value = false
            }
            REPEAT_MODE_ONE -> {
                val currentPlaying = currentPlaying.value
                currentPlaying?.let {
                    playSong(it.second.id.toLong())
                }
            }
            REPEAT_MODE_ALL -> {
                playNext()
            }
        }
    }

    private fun playNext() {
        val listSong = allSongs.value
        if(listSong.isEmpty()) return

        val isSuffer = isSuffer.value
        val currentPlaying = currentPlaying.value

        val songId = if(isSuffer) {
            val index = Random(listSong.size - 1).nextInt()
            listSong[index].id
        } else {
            if(currentPlaying == null) {
                listSong[0].id
            } else if(currentPlaying.first == listSong.size - 1) {
                listSong[0].id
            } else {
                listSong[(currentPlaying.first ?: -1) + 1].id
            }
        }
        playSong(songId.toLong())
    }

    private fun playPause(songId: Long) {
        if(currentPlaying.value?.second?.id == songId.toInt()) {
            if(mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlayingFlow.value = false
            } else {
                mediaPlayer.start()
                isPlayingFlow.value = true
            }
        } else {
            playSong(songId)
        }
    }

    private fun playSong(songId: Long) {
        isPlayingFlow.value = true
        val listSong = allSongs.value
        mediaPlayer.reset()
        val index = listSong.indexOfFirst {
            it.id.toLong() == songId
        }
        currentPlaying.value = index to listSong[index]
        val trackUri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        try {
            mediaPlayer.setDataSource(this, trackUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error starting data source", e)
        }
        mediaPlayer.prepareAsync()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if(mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return MusicBinder()
    }

    inner class MusicBinder : Binder() {
        public fun getMusicService() : MusicService {
            return this@MusicService
        }
    }
}