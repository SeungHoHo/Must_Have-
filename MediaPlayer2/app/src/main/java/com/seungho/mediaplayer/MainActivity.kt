package com.seungho.mediaplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.seungho.mediaplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {

    var mService: MusicPlayerService? = null

    val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = (service as MusicPlayerService.MusicPlayerBinder).getService() // MusicPlayerBinder로 타입 캐스팅 해줍니다.
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }

    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnPlay.setOnClickListener(this)
        binding.btnPause.setOnClickListener(this)
        binding.btnStop.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_play -> {
                play()
            }

            R.id.btn_pause -> {
                pause()
            }

            R.id.btn_stop -> {
                stop()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mService == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this,
                MusicPlayerService::class.java))
            } else {
                startService(Intent(applicationContext, MusicPlayerService::class.java))
            }
            val intent = Intent(this, MusicPlayerService::class.java)
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE) // 서비스와 바인드함
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun play() {
        mService?.play()
    }

    private fun pause() {
        mService?.pause()
    }

    private fun stop() {
        mService?.stop()
    }
}