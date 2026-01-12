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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.netstat.speedmonitor.R
import com.netstat.speedmonitor.ui.MainActivity
import com.netstat.speedmonitor.utils.SpeedFormatter

class NetworkMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "network_speed_channel"
        const val CHANNEL_ID_HIDDEN = "network_speed_channel_hidden"
        const val NOTIFICATION_ID = 1
        const val ACTION_SPEED_UPDATE = "com.netstat.speedmonitor.SPEED_UPDATE"
        const val EXTRA_DOWNLOAD_SPEED = "download_speed"
        const val EXTRA_UPLOAD_SPEED = "upload_speed"
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
        val manager = getSystemService(NotificationManager::class.java)
        
        // Regular channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        
        // Hidden channel - minimum importance to hide from shade
        val hiddenChannel = NotificationChannel(
            CHANNEL_ID_HIDDEN,
            getString(R.string.notification_channel_name_hidden),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        manager.createNotificationChannel(hiddenChannel)
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

            // Broadcast speed update to MainActivity
            val updateIntent = Intent(ACTION_SPEED_UPDATE).apply {
                putExtra(EXTRA_DOWNLOAD_SPEED, downloadSpeed)
                putExtra(EXTRA_UPLOAD_SPEED, uploadSpeed)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent)
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
        val arrowStyle = prefs.getString("arrow_style", "arrows") ?: "arrows"
        val showUnitInIcon = prefs.getBoolean("show_unit_in_icon", true)
        val hideNotification = prefs.getBoolean("hide_notification", false)
        val iconPosition = prefs.getString("icon_position", "center_left") ?: "center_left"
        val textColor = prefs.getInt("icon_text_color", Color.WHITE)
        val fontSize = prefs.getInt("icon_font_size", 12)

        val (downArrow, upArrow) = getArrowSymbols(arrowStyle)
        
        val downloadStr = SpeedFormatter.format(downloadSpeed, unit)
        val uploadStr = SpeedFormatter.format(uploadSpeed, unit)

        val contentText = buildString {
            if (showDownload) append("$downArrow $downloadStr")
            if (showDownload && showUpload) append("  ")
            if (showUpload) append("$upArrow $uploadStr")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val icon = createSpeedIcon(downloadSpeed, uploadSpeed, iconStyle, arrowStyle, showUnitInIcon, iconPosition, textColor, fontSize, showDownload, showUpload, unit)

        val channelId = if (hideNotification) CHANNEL_ID_HIDDEN else CHANNEL_ID
        
        return NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(androidx.core.graphics.drawable.IconCompat.createWithBitmap(icon))
            if (!hideNotification) {
                setContentTitle(getString(R.string.notification_title))
                setContentText(contentText)
            }
            setOngoing(true)
            setOnlyAlertOnce(true)
            setContentIntent(pendingIntent)
            priority = if (hideNotification) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW
            setCategory(NotificationCompat.CATEGORY_STATUS)
            if (hideNotification) {
                setVisibility(NotificationCompat.VISIBILITY_SECRET)
            }
        }.build()
    }

    private fun getArrowSymbols(style: String): Pair<String, String> {
        return when (style) {
            "arrows" -> Pair("↓", "↑")
            "triangles" -> Pair("▼", "▲")
            "letters" -> Pair("D", "U")
            "none" -> Pair("", "")
            else -> Pair("↓", "↑")
        }
    }

    private fun createSpeedIcon(
        downloadSpeed: Double,
        uploadSpeed: Double,
        style: String,
        arrowStyle: String,
        showUnitInIcon: Boolean,
        position: String,
        textColor: Int,
        fontSize: Int,
        showDownload: Boolean,
        showUpload: Boolean,
        unit: String
    ): Bitmap {
        val width = 256
        val height = 192
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val (downArrow, upArrow) = getArrowSymbols(arrowStyle)

        // Determine alignment from position
        val (hAlign, vAlign) = parsePosition(position)
        
        val paint = Paint().apply {
            color = textColor
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = when (hAlign) {
                "left" -> Paint.Align.LEFT
                "center" -> Paint.Align.CENTER
                "right" -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }
        }

        val xPos = when (hAlign) {
            "left" -> 2f
            "center" -> width / 2f
            "right" -> width - 2f
            else -> 2f
        }

        // Fixed font size based on user setting - optimized for 3 digits + unit
        val baseTextSize = fontSize.toFloat() * 5f

        when (style) {
            "combined" -> {
                if (showDownload && showUpload) {
                    val dl = SpeedFormatter.formatShort(downloadSpeed, unit, showUnitInIcon)
                    val ul = SpeedFormatter.formatShort(uploadSpeed, unit, showUnitInIcon)
                    val line1 = if (downArrow.isNotEmpty()) "$downArrow$dl" else dl
                    val line2 = if (upArrow.isNotEmpty()) "$upArrow$ul" else ul
                    
                    paint.textSize = baseTextSize
                    
                    val lineHeight = paint.textSize * 1.1f
                    val totalHeight = lineHeight * 2
                    
                    val startY = when (vAlign) {
                        "top" -> paint.textSize + 4f
                        "center" -> (height - totalHeight) / 2 + paint.textSize
                        "bottom" -> height - totalHeight - 4f + paint.textSize
                        else -> (height - totalHeight) / 2 + paint.textSize
                    }
                    
                    canvas.drawText(line1, xPos, startY, paint)
                    canvas.drawText(line2, xPos, startY + lineHeight, paint)
                } else {
                    val speed = if (showDownload) downloadSpeed else uploadSpeed
                    val arrow = if (showDownload) downArrow else upArrow
                    val formatted = SpeedFormatter.formatShort(speed, unit, showUnitInIcon)
                    val text = if (arrow.isNotEmpty()) "$arrow$formatted" else formatted
                    
                    paint.textSize = baseTextSize * 1.2f
                    
                    val yPos = when (vAlign) {
                        "top" -> paint.textSize + 4f
                        "center" -> height / 2f + paint.textSize / 3
                        "bottom" -> height - 4f
                        else -> height / 2f + paint.textSize / 3
                    }
                    
                    canvas.drawText(text, xPos, yPos, paint)
                }
            }
            "download_only" -> {
                val dl = SpeedFormatter.formatShort(downloadSpeed, unit, showUnitInIcon)
                val text = if (downArrow.isNotEmpty()) "$downArrow$dl" else dl
                paint.textSize = baseTextSize * 1.2f
                
                val yPos = when (vAlign) {
                    "top" -> paint.textSize + 4f
                    "center" -> height / 2f + paint.textSize / 3
                    "bottom" -> height - 4f
                    else -> height / 2f + paint.textSize / 3
                }
                
                canvas.drawText(text, xPos, yPos, paint)
            }
            "upload_only" -> {
                val ul = SpeedFormatter.formatShort(uploadSpeed, unit, showUnitInIcon)
                val text = if (upArrow.isNotEmpty()) "$upArrow$ul" else ul
                paint.textSize = baseTextSize * 1.2f
                
                val yPos = when (vAlign) {
                    "top" -> paint.textSize + 4f
                    "center" -> height / 2f + paint.textSize / 3
                    "bottom" -> height - 4f
                    else -> height / 2f + paint.textSize / 3
                }
                
                canvas.drawText(text, xPos, yPos, paint)
            }
        }

        return bitmap
    }

    private fun parsePosition(position: String): Pair<String, String> {
        return when (position) {
            "top_left" -> Pair("left", "top")
            "top_center" -> Pair("center", "top")
            "top_right" -> Pair("right", "top")
            "center_left" -> Pair("left", "center")
            "center" -> Pair("center", "center")
            "center_right" -> Pair("right", "center")
            "bottom_left" -> Pair("left", "bottom")
            "bottom_center" -> Pair("center", "bottom")
            "bottom_right" -> Pair("right", "bottom")
            else -> Pair("left", "center")
        }
    }
}
