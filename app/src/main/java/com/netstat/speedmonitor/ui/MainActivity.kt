package com.netstat.speedmonitor.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.netstat.speedmonitor.R
import com.netstat.speedmonitor.databinding.ActivityMainBinding
import com.netstat.speedmonitor.service.NetworkMonitorService
import com.netstat.speedmonitor.utils.SpeedFormatter
import com.netstat.speedmonitor.utils.SpeedHistoryManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false
    private val speedHistory = SpeedHistoryManager()
    private var pulseAnimator: AnimatorSet? = null
    private var cardsShown = false

    private val speedUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadSpeed = intent.getDoubleExtra(NetworkMonitorService.EXTRA_DOWNLOAD_SPEED, 0.0)
            val uploadSpeed = intent.getDoubleExtra(NetworkMonitorService.EXTRA_UPLOAD_SPEED, 0.0)
            updateSpeedDisplay(downloadSpeed, uploadSpeed)
            speedHistory.addDataPoint(downloadSpeed, uploadSpeed)
            binding.speedGraph.setData(
                speedHistory.getDownloadHistory(),
                speedHistory.getUploadHistory()
            )
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitoring()
        } else {
            Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupUI()
        animateEntrance()
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            speedUpdateReceiver,
            IntentFilter(NetworkMonitorService.ACTION_SPEED_UPDATE)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(speedUpdateReceiver)
    }

    override fun onDestroy() {
        pulseAnimator?.cancel()
        super.onDestroy()
    }

    private fun setupUI() {
        binding.btnToggleService.setOnClickListener {
            if (isServiceRunning) {
                stopMonitoring()
            } else {
                checkPermissionAndStart()
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.switchStartOnBoot.isChecked = 
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("start_on_boot", false)

        binding.switchStartOnBoot.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean("start_on_boot", isChecked)
                .apply()
        }

        binding.legendDownload.setOnClickListener {
            binding.speedGraph.showDownload = !binding.speedGraph.showDownload
            val alpha = if (binding.speedGraph.showDownload) 1f else 0.3f
            binding.dotDownload.animate().alpha(alpha).setDuration(200).start()
            binding.labelDownload.animate().alpha(alpha).setDuration(200).start()
            binding.speedGraph.invalidate()
        }

        binding.legendUpload.setOnClickListener {
            binding.speedGraph.showUpload = !binding.speedGraph.showUpload
            val alpha = if (binding.speedGraph.showUpload) 1f else 0.3f
            binding.dotUpload.animate().alpha(alpha).setDuration(200).start()
            binding.labelUpload.animate().alpha(alpha).setDuration(200).start()
            binding.speedGraph.invalidate()
        }
    }

    private fun animateEntrance() {
        val views = listOf<View>(binding.statusCard, binding.btnToggleService)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 60f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(100L + index * 80L)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        }
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startMonitoring()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        NetworkMonitorService.start(this)
        isServiceRunning = true
        speedHistory.clear()
        updateUI()
        Snackbar.make(binding.root, R.string.monitoring_started_message, Snackbar.LENGTH_LONG).show()
    }

    private fun stopMonitoring() {
        NetworkMonitorService.stop(this)
        isServiceRunning = false
        updateUI()
    }

    private fun updateServiceStatus() {
        isServiceRunning = isServiceRunning()
        updateUI()
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (NetworkMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateSpeedDisplay(downloadSpeed: Double, uploadSpeed: Double) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val unit = prefs.getString("speed_unit", "auto") ?: "auto"
        
        binding.tvDownloadSpeed.text = SpeedFormatter.format(downloadSpeed, unit)
        binding.tvUploadSpeed.text = SpeedFormatter.format(uploadSpeed, unit)
    }

    private fun updateUI() {
        if (isServiceRunning) {
            binding.btnToggleService.text = getString(R.string.stop_monitoring)
            binding.btnToggleService.setIconResource(R.drawable.ic_stop)
            binding.tvStatus.text = getString(R.string.status_running)
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_on)

            if (!cardsShown) {
                cardsShown = true
                showCardAnimated(binding.speedCard, 0)
                showCardAnimated(binding.graphCard, 100)
                binding.speedGraph.animateIn()
            }

            startPulseAnimation()
        } else {
            binding.btnToggleService.text = getString(R.string.start_monitoring)
            binding.btnToggleService.setIconResource(R.drawable.ic_play)
            binding.tvStatus.text = getString(R.string.status_stopped)
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_off)

            if (cardsShown) {
                cardsShown = false
                hideCardAnimated(binding.speedCard)
                hideCardAnimated(binding.graphCard)
            }

            binding.tvDownloadSpeed.text = "0 B/s"
            binding.tvUploadSpeed.text = "0 B/s"

            stopPulseAnimation()
        }
    }

    private fun showCardAnimated(card: View, delay: Long) {
        card.visibility = View.VISIBLE
        card.alpha = 0f
        card.translationY = 30f
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }

    private fun hideCardAnimated(card: View) {
        card.animate()
            .alpha(0f)
            .translationY(20f)
            .setDuration(250)
            .withEndAction { card.visibility = View.GONE }
            .start()
    }

    private fun startPulseAnimation() {
        if (pulseAnimator != null) return
        val ring = binding.statusPulseRing
        val scaleX = ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.85f, 1.15f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1200
        }
        val scaleY = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.85f, 1.15f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1200
        }
        val alpha = ObjectAnimator.ofFloat(ring, View.ALPHA, 0f, 0.5f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1200
        }
        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.statusPulseRing.alpha = 0f
    }
}
