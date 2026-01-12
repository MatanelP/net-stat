package com.netstat.speedmonitor.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.netstat.speedmonitor.R
import com.netstat.speedmonitor.databinding.ActivityMainBinding
import com.netstat.speedmonitor.service.NetworkMonitorService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false

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
        }
    }
}
