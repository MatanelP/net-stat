# Copilot Instructions for NetStat

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew assembleRelease        # Release build (requires signing keys in local.properties)
```

There are no tests or linters configured in this project.

## Architecture

NetStat is a single-module Android app (package: `com.netstat.speedmonitor`) that monitors network speed via a foreground service and displays it as a custom-drawn notification icon in the status bar.

### Data flow

1. **NetworkMonitorService** polls `TrafficStats.getTotalRxBytes/TxBytes()` every 750ms, calculates download/upload speed deltas, and broadcasts them via `LocalBroadcastManager`.
2. **MainActivity** registers a `BroadcastReceiver` in `onResume()`/`onPause()` to receive speed updates and display them in the UI.
3. **SpeedFormatter** handles unit conversion and formatting — the `formatShortSplit()` method returns a (number, unit) tuple used by the notification icon renderer.

### Notification icon rendering

The notification icon is **not** a static drawable. `NetworkMonitorService.createSpeedIcon()` draws speed text directly onto an 80×80 `Bitmap` using `Canvas` + `Paint`. It reads user preferences (font size, color, arrow style, unit visibility) on every update to render the icon dynamically.

### Preferences

All settings are defined in `res/xml/preferences.xml` and managed by `SettingsFragment` (extends `PreferenceFragmentCompat`). Both `MainActivity` and `NetworkMonitorService` read preferences via `PreferenceManager.getDefaultSharedPreferences()` — there is no reactive listener pattern; values are read directly each time they're needed.

## Key Conventions

- **View Binding** is used in all activities (no `findViewById`).
- **Material 3 / Material You** dynamic colors are applied in activities.
- The service uses a **zero-threshold** (3 consecutive zero readings) before displaying 0 speed, to prevent notification flickering.
- Java 21 source/target compatibility; Kotlin 2.1.0.
- Release signing config reads `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` from `local.properties`.
