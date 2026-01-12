package com.netstat.speedmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.TrafficStats
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.netstat.speedmonitor.R
import com.netstat.speedmonitor.ui.MainActivity
import com.netstat.speedmonitor.utils.SpeedFormatter

class NetworkMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "network_speed_channel"
        const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL = 1000L

        fun start(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NetworkMonitorService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTime = 0L

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification(0.0, 0.0))
        handler.post(updateRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDiff = (currentTime - lastTime) / 1000.0
        if (timeDiff > 0) {
            val downloadSpeed = (currentRxBytes - lastRxBytes) / timeDiff
            val uploadSpeed = (currentTxBytes - lastTxBytes) / timeDiff

            val notification = createNotification(downloadSpeed, uploadSpeed)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }

        lastRxBytes = currentRxBytes
        lastTxBytes = currentTxBytes
        lastTime = currentTime
    }

    private fun createNotification(downloadSpeed: Double, uploadSpeed: Double): Notification {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val unit = prefs.getString("speed_unit", "auto") ?: "auto"
        val showUpload = prefs.getBoolean("show_upload", true)
        val showDownload = prefs.getBoolean("show_download", true)
        val iconStyle = prefs.getString("icon_style", "combined") ?: "combined"
        val textColor = prefs.getInt("icon_text_color", Color.WHITE)
        val fontSize = prefs.getInt("icon_font_size", 12)

        val downloadStr = SpeedFormatter.format(downloadSpeed, unit)
        val uploadStr = SpeedFormatter.format(uploadSpeed, unit)

        val contentText = buildString {
            if (showDownload) append("↓ $downloadStr")
            if (showDownload && showUpload) append("  ")
            if (showUpload) append("↑ $uploadStr")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val icon = createSpeedIcon(downloadSpeed, uploadSpeed, iconStyle, textColor, fontSize, showDownload, showUpload, unit)

        return NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(androidx.core.graphics.drawable.IconCompat.createWithBitmap(icon))
            setContentTitle(getString(R.string.notification_title))
            setContentText(contentText)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setContentIntent(pendingIntent)
            priority = NotificationCompat.PRIORITY_LOW
            setCategory(NotificationCompat.CATEGORY_STATUS)
        }.build()
    }

    private fun createSpeedIcon(
        downloadSpeed: Double,
        uploadSpeed: Double,
        style: String,
        textColor: Int,
        fontSize: Int,
        showDownload: Boolean,
        showUpload: Boolean,
        unit: String
    ): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = textColor
            textSize = fontSize.toFloat() * 2.5f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        when (style) {
            "combined" -> {
                val text = if (showDownload && showUpload) {
                    val dl = SpeedFormatter.formatShort(downloadSpeed, unit)
                    val ul = SpeedFormatter.formatShort(uploadSpeed, unit)
                    "$dl\n$ul"
                } else if (showDownload) {
                    SpeedFormatter.formatShort(downloadSpeed, unit)
                } else {
                    SpeedFormatter.formatShort(uploadSpeed, unit)
                }
                
                val lines = text.split("\n")
                if (lines.size == 2) {
                    paint.textSize = fontSize.toFloat() * 2f
                    canvas.drawText("↓${lines[0]}", size / 2f, size / 2f - 5, paint)
                    canvas.drawText("↑${lines[1]}", size / 2f, size / 2f + paint.textSize, paint)
                } else {
                    canvas.drawText(text, size / 2f, size / 2f + paint.textSize / 3, paint)
                }
            }
            "download_only" -> {
                val text = "↓" + SpeedFormatter.formatShort(downloadSpeed, unit)
                canvas.drawText(text, size / 2f, size / 2f + paint.textSize / 3, paint)
            }
            "upload_only" -> {
                val text = "↑" + SpeedFormatter.formatShort(uploadSpeed, unit)
                canvas.drawText(text, size / 2f, size / 2f + paint.textSize / 3, paint)
            }
        }

        return bitmap
    }
}
