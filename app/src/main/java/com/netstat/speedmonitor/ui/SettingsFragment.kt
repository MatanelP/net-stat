package com.netstat.speedmonitor.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.netstat.speedmonitor.BuildConfig
import com.netstat.speedmonitor.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("theme_mode")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newValue ->
                AppCompatDelegate.setDefaultNightMode(
                    when (newValue as String) {
                        "light" -> AppCompatDelegate.MODE_NIGHT_NO
                        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                true
            }
        }

        findPreference<ListPreference>("speed_unit")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<ListPreference>("icon_style")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<ListPreference>("arrow_style")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<SeekBarPreference>("icon_font_size")?.apply {
            min = 8
            max = 20
            setDefaultValue(16)
        }

        findPreference<ListPreference>("icon_text_color")?.apply {
            summaryProvider =
                    Preference.SummaryProvider<ListPreference> { pref ->
                        val selectedEntry = pref.entry ?: "White"
                        "$selectedEntry (Supported devices only)"
                    }
        }

        findPreference<ListPreference>("icon_font_style")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }

        // About section
        findPreference<Preference>("version")?.apply { summary = BuildConfig.VERSION_NAME }

        findPreference<Preference>("source_code")?.apply {
            setOnPreferenceClickListener {
                val intent =
                        Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/MatanelP/net-stat")
                        )
                startActivity(intent)
                true
            }
        }
    }
}
