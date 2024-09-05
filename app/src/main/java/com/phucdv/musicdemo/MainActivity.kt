package com.phucdv.musicdemo

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.phucdv.musicdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory(this)
    }

    private val isAtLeast13 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val permission = if (isAtLeast13) android.Manifest.permission.READ_MEDIA_AUDIO
    else android.Manifest.permission.READ_EXTERNAL_STORAGE

    private val adapter = SongAdapter { song ->
        if(viewModel.isPlayingFlow.value) {
            if(viewModel.playingSongFlow.value == song) {
                viewModel.isPlayingFlow.value = false
                mediaPlayer.pause()
            } else {
                playSong(song)
            }
        } else {
            if(viewModel.playingSongFlow.value == song) {
                viewModel.isPlayingFlow.value = true
                mediaPlayer.start()
            } else {
                playSong(song)
            }
        }
    }

    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalIfNeed()
        } else {
            listenViewModel()
        }

        initView()

        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())

        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
        mediaPlayer.setOnCompletionListener {
            viewModel.isPlayingFlow.value = false
            viewModel.playingSongFlow.value = null
        }
    }

    private val requestReadExternalPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                listenViewModel()
            } else {
                showPermissionDialog()
            }
        }

    private fun openAppSettings() {
        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addCategory(CATEGORY_DEFAULT)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            addFlags(FLAG_ACTIVITY_NO_HISTORY)
            addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        startActivity(intent)
    }

    private fun requestReadExternalIfNeed() {
        when {
            checkSelfPermission(permission)
                    == PackageManager.PERMISSION_GRANTED -> {
                listenViewModel()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionDialog()
            }

            else -> {
                requestReadExternalPermissionLauncher.launch(
                    permission
                )
            }
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Cảnh báo")
            setCancelable(false)
            setMessage("Ứng dụng cần truy cập vào bộ nhớ để phát nhạc. Vui lòng cấp quyền và khởi động lại ứng dụng!")
            setPositiveButton("Cấp Quyền") { _, _ ->
                finish()
                openAppSettings()
            }
            setNegativeButton("Thoát") { _, _ ->
                finish()
            }
        }.show()
    }

    private fun initView() {
        binding.rcvSong.adapter = adapter
    }

    private fun listenViewModel() {
        lifecycleScope.launch {
            viewModel.allSongs.collectLatest { allSongs ->
                adapter.submitList(allSongs)
            }
        }

        lifecycleScope.launch {
            viewModel.isPlayingFlow.collectLatest {
                adapter.isPlaying = it
            }
        }

        lifecycleScope.launch {
            viewModel.playingSongFlow.collectLatest {
                adapter.playingSong = it
            }
        }
    }

    fun playSong(song: Song) {
        mediaPlayer.reset()
        val trackUri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id.toLong())
        try {
            mediaPlayer.setDataSource(this, trackUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error starting data source", e)
        }
        viewModel.playingSongFlow.value = song
        viewModel.isPlayingFlow.value = true
        mediaPlayer.prepareAsync()
    }

}