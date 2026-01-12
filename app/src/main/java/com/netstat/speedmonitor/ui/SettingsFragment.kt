package com.netstat.speedmonitor.ui

import android.graphics.Color
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
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

        findPreference<ListPreference>("icon_text_color")?.apply {
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
        }

        findPreference<SeekBarPreference>("icon_font_size")?.apply {
            min = 8
            max = 20
            setDefaultValue(12)
        }

        findPreference<SwitchPreferenceCompat>("show_download")?.apply {
            setDefaultValue(true)
        }

        findPreference<SwitchPreferenceCompat>("show_upload")?.apply {
            setDefaultValue(true)
        }
    }
}
