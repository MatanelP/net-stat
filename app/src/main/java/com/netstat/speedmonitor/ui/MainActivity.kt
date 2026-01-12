package com.netstat.speedmonitor.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.netstat.speedmonitor.R
import com.netstat.speedmonitor.databinding.ActivityMainBinding
import com.netstat.speedmonitor.service.NetworkMonitorService
import com.netstat.speedmonitor.utils.SpeedFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false

    private val speedUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadSpeed = intent.getDoubleExtra(NetworkMonitorService.EXTRA_DOWNLOAD_SPEED, 0.0)
            val uploadSpeed = intent.getDoubleExtra(NetworkMonitorService.EXTRA_UPLOAD_SPEED, 0.0)
            updateSpeedDisplay(downloadSpeed, uploadSpeed)
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
        updateUI()
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
            binding.speedCard.visibility = View.VISIBLE
        } else {
            binding.btnToggleService.text = getString(R.string.start_monitoring)
            binding.btnToggleService.setIconResource(R.drawable.ic_play)
            binding.tvStatus.text = getString(R.string.status_stopped)
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_off)
            binding.speedCard.visibility = View.GONE
            binding.tvDownloadSpeed.text = "0 B/s"
            binding.tvUploadSpeed.text = "0 B/s"
        }
    }
}
