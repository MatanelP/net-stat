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
        private const val UPDATE_INTERVAL = 750L

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

    private val updateRunnable =
            object : Runnable {
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
        val channel =
                NotificationChannel(
                                CHANNEL_ID,
                                getString(R.string.notification_channel_name),
                                NotificationManager.IMPORTANCE_LOW
                        )
                        .apply {
                            description = getString(R.string.notification_channel_description)
                            setShowBadge(false)
                            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        }
        manager.createNotificationChannel(channel)

        // Hidden channel - minimum importance to hide from shade
        val hiddenChannel =
                NotificationChannel(
                                CHANNEL_ID_HIDDEN,
                                getString(R.string.notification_channel_name_hidden),
                                NotificationManager.IMPORTANCE_MIN
                        )
                        .apply {
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
            val updateIntent =
                    Intent(ACTION_SPEED_UPDATE).apply {
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
        val fontSize = prefs.getInt("icon_font_size", 12)

        // Handle potential type mismatch from old preferences (was stored as Int, now String)
        val textColorName =
                try {
                    prefs.getString("icon_text_color", "white") ?: "white"
                } catch (e: ClassCastException) {
                    // Clear old preference and use default
                    prefs.edit().remove("icon_text_color").apply()
                    "white"
                }

        val fontStyle =
                try {
                    prefs.getString("icon_font_style", "bold") ?: "bold"
                } catch (e: ClassCastException) {
                    prefs.edit().remove("icon_font_style").apply()
                    "bold"
                }

        // Convert color name to Color int
        val textColor =
                when (textColorName) {
                    "white" -> Color.WHITE
                    "black" -> Color.BLACK
                    "green" -> Color.GREEN
                    "cyan" -> Color.CYAN
                    "yellow" -> Color.YELLOW
                    "red" -> Color.RED
                    "orange" -> Color.rgb(255, 165, 0)
                    else -> Color.WHITE
                }

        val (downArrow, upArrow) = getArrowSymbols(arrowStyle)

        val downloadStr = SpeedFormatter.format(downloadSpeed, unit)
        val uploadStr = SpeedFormatter.format(uploadSpeed, unit)

        val contentText = buildString {
            if (showDownload) append("$downArrow $downloadStr")
            if (showDownload && showUpload) append("  ")
            if (showUpload) append("$upArrow $uploadStr")
        }

        val pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                )

        val icon =
                createSpeedIcon(
                        downloadSpeed,
                        uploadSpeed,
                        iconStyle,
                        arrowStyle,
                        showUnitInIcon,
                        fontSize,
                        showDownload,
                        showUpload,
                        unit,
                        textColor,
                        fontStyle
                )

        val channelId = if (hideNotification) CHANNEL_ID_HIDDEN else CHANNEL_ID

        return NotificationCompat.Builder(this, channelId)
                .apply {
                    setSmallIcon(androidx.core.graphics.drawable.IconCompat.createWithBitmap(icon))
                    if (!hideNotification) {
                        setContentTitle(getString(R.string.notification_title))
                        setContentText(contentText)
                    }
                    setOngoing(true)
                    setOnlyAlertOnce(true)
                    setContentIntent(pendingIntent)
                    priority =
                            if (hideNotification) NotificationCompat.PRIORITY_MIN
                            else NotificationCompat.PRIORITY_LOW
                    setCategory(NotificationCompat.CATEGORY_STATUS)
                    if (hideNotification) {
                        setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    }
                }
                .build()
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
            fontSize: Int,
            showDownload: Boolean,
            showUpload: Boolean,
            unit: String,
            textColor: Int,
            fontStyle: String
    ): Bitmap {
        // Android notification icons in status bar are constrained to ~24dp height
        // For text-based icons, we need to balance width vs height
        // A narrower icon renders LARGER because Android scales by the constraining dimension
        // Using 80 pixels gives us a square icon that renders at full status bar height
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val (downArrow, upArrow) = getArrowSymbols(arrowStyle)

        // Determine typeface based on font style
        val typeface =
                when (fontStyle) {
                    "bold" -> Typeface.DEFAULT_BOLD
                    "italic" -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    "bold_italic" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    else -> Typeface.DEFAULT
                }

        val paint =
                Paint().apply {
                    color = textColor
                    this.typeface = typeface
                    isAntiAlias = true
                    textAlign = Paint.Align.LEFT
                }

        // fontSize (8-20) controls text size as percentage of icon
        val textScale = fontSize.toFloat() / 20f // Range: 0.4 to 1.0

        // Helper function to draw text with smaller unit
        fun drawTextWithUnit(
                canvas: Canvas,
                paint: Paint,
                arrow: String,
                number: String,
                unitStr: String,
                x: Float,
                y: Float,
                showUnit: Boolean
        ) {
            val baseSize = paint.textSize
            var xPos = x

            // Draw arrow
            if (arrow.isNotEmpty()) {
                canvas.drawText(arrow, xPos, y, paint)
                xPos += paint.measureText(arrow)
            }

            // Draw number
            canvas.drawText(number, xPos, y, paint)
            xPos += paint.measureText(number)

            // Draw unit at 0.75 size
            if (showUnit) {
                paint.textSize = baseSize * 0.75f
                canvas.drawText(unitStr, xPos, y, paint)
                paint.textSize = baseSize // Restore
            }
        }

        when (style) {
            "combined" -> {
                if (showDownload && showUpload) {
                    val (dlNum, dlUnit) = SpeedFormatter.formatShortSplit(downloadSpeed, unit)
                    val (ulNum, ulUnit) = SpeedFormatter.formatShortSplit(uploadSpeed, unit)

                    // Two lines stacked - each line gets ~45% of icon height
                    paint.textSize = size * textScale * 0.45f

                    val lineHeight = paint.textSize * 1.1f
                    val totalHeight = lineHeight * 2
                    val startY = (size - totalHeight) / 2 + paint.textSize

                    drawTextWithUnit(
                            canvas,
                            paint,
                            downArrow,
                            dlNum,
                            dlUnit,
                            1f,
                            startY,
                            showUnitInIcon
                    )
                    drawTextWithUnit(
                            canvas,
                            paint,
                            upArrow,
                            ulNum,
                            ulUnit,
                            1f,
                            startY + lineHeight,
                            showUnitInIcon
                    )
                } else {
                    val speed = if (showDownload) downloadSpeed else uploadSpeed
                    val arrow = if (showDownload) downArrow else upArrow
                    val (num, unitStr) = SpeedFormatter.formatShortSplit(speed, unit)

                    // Single line - use most of icon height
                    paint.textSize = size * textScale * 0.8f
                    val yPos = size / 2f + paint.textSize / 3

                    drawTextWithUnit(canvas, paint, arrow, num, unitStr, 1f, yPos, showUnitInIcon)
                }
            }
            "download_only" -> {
                val (dlNum, dlUnit) = SpeedFormatter.formatShortSplit(downloadSpeed, unit)
                paint.textSize = size * textScale * 0.8f
                val yPos = size / 2f + paint.textSize / 3

                drawTextWithUnit(canvas, paint, downArrow, dlNum, dlUnit, 1f, yPos, showUnitInIcon)
            }
            "upload_only" -> {
                val (ulNum, ulUnit) = SpeedFormatter.formatShortSplit(uploadSpeed, unit)
                paint.textSize = size * textScale * 0.8f
                val yPos = size / 2f + paint.textSize / 3

                drawTextWithUnit(canvas, paint, upArrow, ulNum, ulUnit, 1f, yPos, showUnitInIcon)
            }
        }

        return bitmap
    }
}
