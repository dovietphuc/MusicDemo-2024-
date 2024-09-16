package com.phucdv.musicdemo

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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

    private var service: MusicService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if(binder is MusicService.MusicBinder) {
                service = binder.getMusicService()
                listenViewModel()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val isAtLeast13 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val permission = if (isAtLeast13) android.Manifest.permission.READ_MEDIA_AUDIO
    else android.Manifest.permission.READ_EXTERNAL_STORAGE

    private val adapter = SongAdapter { song ->
        startService(Intent(this@MainActivity, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY_PAUSE
            putExtra(MusicService.EXTRA_SONG_ID, song.id.toLong())
        })
    }

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
            bindService(Intent(this, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
        }

        initView()

    }

    private val requestReadExternalPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                bindService(Intent(this, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
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
                bindService(Intent(this, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
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

        binding.btnPlayPause.setOnClickListener {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_PAUSE
                putExtra(MusicService.EXTRA_SONG_ID, service?.currentPlaying?.value?.second?.id?.toLong())
            })
        }
        binding.btnNext.setOnClickListener {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.ACTION_NEXT
            })
        }
        binding.btnPrev.setOnClickListener {
            startService(Intent(this@MainActivity, MusicService::class.java).apply {
                action = MusicService.ACTION_PREV
            })
        }
    }

    private fun listenViewModel() {
        lifecycleScope.launch {
            service?.allSongs?.collectLatest { allSongs ->
                adapter.submitList(allSongs)
            }
        }

        lifecycleScope.launch {
            service?.isPlayingFlow?.collectLatest {
                adapter.isPlaying = it
                binding.btnPlayPause.text = if(it) "Pause" else "Play"
            }
        }

        lifecycleScope.launch {
            service?.currentPlaying?.collectLatest {
                adapter.playingSong = it?.second
            }
        }

        lifecycleScope.launch {
            service?.repeatMode?.collectLatest {

            }
        }

        lifecycleScope.launch {
            service?.isSuffer?.collectLatest {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}