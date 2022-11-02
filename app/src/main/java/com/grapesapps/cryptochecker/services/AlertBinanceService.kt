package com.grapesapps.cryptochecker.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.grapesapps.cryptochecker.R

class AlertBinanceService : Service() {

    companion object {
        private const val TAG = "AlarmMediaService"
        private const val CHANNEL_ID = "Alarm Media Service Channel"
        private const val TITLE_MESSAGE = "TITLE_MESSAGE"
        private const val CHANNEL_START_ACTION = "START_ACTION"
        private const val CHANNEL_STOP_ACTION = "STOP_ACTION"
        private const val CHANNEL_STOP_MESSAGE = "Отключить"
    }

    private lateinit var mMediaPlayer: MediaPlayer

    override fun onCreate() {
        super.onCreate()
        mMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == CHANNEL_STOP_ACTION || intent?.action == null) {
            stopForeground(true)
            return START_STICKY
        }

        val title = intent.getStringExtra(TITLE_MESSAGE)

        val closeIntent = Intent(this, CryptoAlarmService::class.java)

        closeIntent.action = CHANNEL_STOP_ACTION

        val closePendingIntent =
            PendingIntent.getForegroundService(
                this,
                0,
                closeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        createNotificationChannel(CHANNEL_ID, "Alarm Media Service")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title ?: "?")
            .setSmallIcon(R.drawable.notification_icon)
            .setSilent(false)
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            .setVibrate(LongArray(0))
//            .setContentIntent(notificationPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                CHANNEL_STOP_MESSAGE,
                closePendingIntent
            )
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(1, notification)


        return START_NOT_STICKY
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}