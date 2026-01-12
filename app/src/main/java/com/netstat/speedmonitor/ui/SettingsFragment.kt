package com.netstat.speedmonitor.ui

import android.graphics.Color
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.netstat.speedmonitor.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("speed_unit")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<ListPreference>("icon_style")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<ListPreference>("arrow_style")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<ListPreference>("icon_position")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<ListPreference>("icon_text_color_name")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newValue ->
                val color = when (newValue as String) {
                    "white" -> Color.WHITE
                    "black" -> Color.BLACK
                    "green" -> Color.GREEN
                    "cyan" -> Color.CYAN
                    "yellow" -> Color.YELLOW
                    else -> Color.WHITE
                }
                preferenceManager.sharedPreferences?.edit()
                    ?.putInt("icon_text_color", color)
                    ?.apply()
                true
            }
            
            // Initialize color int value on first load
            val currentColorName = preferenceManager.sharedPreferences?.getString("icon_text_color_name", "white")
            val color = when (currentColorName) {
                "white" -> Color.WHITE
                "black" -> Color.BLACK
                "green" -> Color.GREEN
                "cyan" -> Color.CYAN
                "yellow" -> Color.YELLOW
                else -> Color.WHITE
            }
            preferenceManager.sharedPreferences?.edit()
                ?.putInt("icon_text_color", color)
                ?.apply()
        }

        findPreference<SeekBarPreference>("icon_font_size")?.apply {
            min = 8
            max = 20
            setDefaultValue(12)
        }
    }
}
