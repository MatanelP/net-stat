# NetStat - Network Speed Monitor

[![Build](https://github.com/MatanelP/net-stat/actions/workflows/build.yml/badge.svg)](https://github.com/MatanelP/net-stat/actions/workflows/build.yml)
[![Android](https://img.shields.io/badge/Android-API%2028%2B-brightgreen.svg)](https://developer.android.com/about/versions/pie)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A modern Android app that displays real-time network speed (download/upload) in your status bar notification.

<p align="center">
  <img src="screenshots/main.png" width="250" alt="Main Screen"/>
  <img src="screenshots/settings.png" width="250" alt="Settings"/>
</p>

## Features

- 📊 **Real-time Monitoring** - Live download and upload speed in status bar
- 🎨 **Material You Design** - Dynamic colors that match your wallpaper (Android 12+)
- ⚙️ **Highly Customizable**
  - Speed units (Auto, Kbps, Mbps, KB/s, MB/s)
  - Arrow styles (↓↑, ▼▲, DU, or none)
  - Icon position (9 positions)
  - Text color and font size
  - Show/hide upload or download
  - Show/hide unit in icon
- 🚀 **Start on Boot** - Optionally start monitoring when device boots
- 🔋 **Battery Efficient** - Lightweight foreground service
- 📱 **Modern Android** - Targets Android 16 (API 36)

## Requirements

- Android 9.0 (API 28) or higher
- Notification permission (Android 13+)

## Installation

### From Releases
1. Download the latest APK from [Releases](https://github.com/MatanelP/net-stat/releases)
2. Install on your Android device
3. Grant notification permission when prompted

### Build from Source
```bash
# Clone the repository
git clone https://github.com/MatanelP/net-stat.git
cd net-stat

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Usage

1. Open the app
2. Tap **"Start Monitoring"**
3. Grant notification permission if prompted
4. Network speed will appear in your status bar
5. Customize appearance in **Settings**

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI**: Material 3 / Material You
- **Architecture**: Android Services + LocalBroadcastManager
- **Build**: Gradle 8.11.1 + AGP 8.7.3
- **Min SDK**: 28 (Android 9)
- **Target SDK**: 36 (Android 16)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Material Design 3](https://m3.material.io/) for design guidelines
- Android team for the excellent TrafficStats API
