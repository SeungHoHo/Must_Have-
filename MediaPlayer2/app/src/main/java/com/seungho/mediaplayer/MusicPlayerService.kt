package com.seungho.mediaplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi

class MusicPlayerService : Service() {

    var mMediaPlayer : MediaPlayer? = null

    var mBinder: MusicPlayerBinder = MusicPlayerBinder()

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onBind(intent: Intent?): IBinder? { //바인더 반환
        return mBinder
    }

    //startService()를 호출하면 실행되는 롤백 함수
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }



    // 알림 채널 생성
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel( //알림 채널을 생성
                "CHANNEL_ID",
                "CHANNEL_NAME",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(mChannel)
        }

        // 알림 생성
        val notification: Notification = Notification.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_play) // 알림 아이콘
            .setContentTitle("뮤직 플레이어 앱") // 알림 제목
            .setContentText("앱이 실행 중입니다.") // 알림 내용
            .build()

        startForeground(1, notification)
    }


    //서비스 중단 처리
    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
    }

    fun isPlaying() : Boolean {
        return (mMediaPlayer != null && mMediaPlayer?.isPlaying ?: false)
    }

    fun play() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(this, R.raw.music01) // 음악 파일의 리소스를 가져와 미디어 플레이어 객체를 할당해줌
            mMediaPlayer?.setVolume(1.0f , 1.0f)
            mMediaPlayer?.isLooping = true
            mMediaPlayer?.start()
        } else {
            if (mMediaPlayer!!.isPlaying) {
                Toast.makeText(this, "이미 음악이 실행 중입니다.", Toast.LENGTH_SHORT).show()
            } else {
                mMediaPlayer?.start() // 음악을 재생
            }
        }
    }

    fun pause() {
        mMediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun stop() {
        mMediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release() // 미디어 플레이어에 할당된 자원을 해제시킴
                mMediaPlayer = null
            }
        }
    }


}