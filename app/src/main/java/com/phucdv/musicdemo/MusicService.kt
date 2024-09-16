package com.phucdv.musicdemo

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var listSong = emptyList<Song>()
    private var currentPlaying: Pair<Int, Song>? = null
    private var repeatMode = REPEAT_MODE_OFF
    private var isSuffer = false

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())

        setOnPreparedListener {
            start()
        }
        setOnCompletionListener {

        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
        scope.launch {
            listSong = repository.getAllSongs()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_PLAY_PAUSE -> {
                val songId = intent.getLongExtra(EXTRA_SONG_ID, -1L)
                if(songId != -1L) {
                    playPause()
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

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if(mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}