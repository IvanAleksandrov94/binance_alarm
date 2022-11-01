package com.grapesapps.cryptochecker

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class CryptoAlarmService : Service() {
    companion object {
        private const val TAG = "Binance alert service"
        private const val CHANNEL_ID = "Binance request channel"
        private const val CHANNEL_START_ACTION = "START_ACTION"
        private const val CHANNEL_STOP_ACTION = "STOP_ACTION"
        private const val NOTIFICATION_TITLE_CURRENCY = "TITLE_CURRENCY"
        private const val NOTIFICATION_TITLE_PRICE = "TITLE_PRICE"
        private const val CHANNEL_STOP_MESSAGE = "Стоп"

    }

    lateinit var sharedPref: SharedPrefManager
    private val binder: Binder = CryptoAlarmBinder()


    @RequiresApi(Build.VERSION_CODES.S)
    private fun getPriceInService(cryptoCurrency: String, pair: String = "USDT") {
        val url: String = baseUrlV3 + "ticker/price?symbol=$cryptoCurrency$pair"
        val requestBinance = Request.Builder().url(url).build()

        OkHttpClient().newCall(requestBinance).execute().use { response ->
            if (response.isSuccessful) {
                val gson = Gson()
                try {
                    val priceModel = gson.fromJson(response.body?.string(), PriceModel::class.java)
                    val formattedPrice = String.format("%.3f", priceModel.price.toDoubleOrNull())
                    val notificationTitle = "$cryptoCurrency · $formattedPrice"
                    val locale = Locale.getDefault()

                    val urlBinance =
                        "https://www.binance.com/${locale.country.lowercase(locale)}/trade/$cryptoCurrency" + "_USDT?theme=dark&type=spot"

                    val notificationIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(urlBinance)
                    )
                    val pendingIntent =
                        PendingIntent.getActivity(
                            this,
                            0,
                            notificationIntent,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                        )
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(notificationTitle)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setSilent(true)
                        .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                        .setVibrate(LongArray(0))
                        .setContentIntent(pendingIntent)
                        .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                        .build()
                    val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    mNotificationManager.notify(1, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Update notification error: ${e.message}")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPref = SharedPrefManager(applicationContext)
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val wakeLock: PowerManager.WakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainActivity::CryptoAlarmWakeLock").apply {
                    acquire()
                }
            }
        if (intent?.action == CHANNEL_STOP_ACTION || intent?.action == null) {
            stopForeground(true)
            wakeLock.release()
            return START_STICKY
        }
        val currency = intent.getStringExtra(NOTIFICATION_TITLE_CURRENCY)
        val price = intent.getStringExtra(NOTIFICATION_TITLE_PRICE)
        val notificationTitle = "$currency · $price"

        val notificationIntent = Intent(this, MainActivity::class.java)
        val notificationPendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val closeIntent = Intent(this, CryptoAlarmService::class.java)

        closeIntent.action = CHANNEL_STOP_ACTION

        val closePendingIntent =
            PendingIntent.getForegroundService(
                this,
                0,
                closeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )


        createNotificationChannel(CHANNEL_ID, "Binance Service: $currency")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle ?: "?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            .setVibrate(LongArray(0))
            .setContentIntent(notificationPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                CHANNEL_STOP_MESSAGE,
                closePendingIntent
            )
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        startForeground(1, notification)
        val timer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            getPriceInService(currency!!)
        }, 0, 10, TimeUnit.SECONDS)

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

    override fun onBind(p0: Intent?): IBinder = binder

    inner class CryptoAlarmBinder : Binder() {
        val service: CryptoAlarmService
            get() = this@CryptoAlarmService
    }
}