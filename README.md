# VibeNav

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Latest Release](https://img.shields.io/github/v/release/DilumSaluka/VibeNav?label=version)](https://github.com/DilumSaluka/VibeNav/releases)
[![Build APK](https://github.com/DilumSaluka/VibeNav/actions/workflows/build-apk.yml/badge.svg)](https://github.com/DilumSaluka/VibeNav/actions/workflows/build-apk.yml)
[![CodeQL](https://github.com/DilumSaluka/VibeNav/actions/workflows/codeql.yml/badge.svg)](https://github.com/DilumSaluka/VibeNav/actions/workflows/codeql.yml)
[![Code scanning](https://img.shields.io/badge/security-CodeQL-purple)](https://github.com/DilumSaluka/VibeNav/security)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)

**Navigate with confidence.** VibeNav is a free Android navigation app that tracks your live location on OpenStreetMap and alerts you — with vibration, sound, and voice — when you're approaching your destination. No API keys, no cost, no ads.

Created by **[Dilum Saluka](https://github.com/DilumSaluka)**.

---

## 📲 Download

Get the latest APK from the **[Releases page](https://github.com/DilumSaluka/VibeNav/releases)** — just open the link on your phone and tap to install.

| Version | Download |
|---------|----------|
| v1.1 | [app-debug.apk](https://github.com/DilumSaluka/VibeNav/releases/download/v1.1/app-debug.apk) (latest) |
| v1.0 | [app-debug.apk](https://github.com/DilumSaluka/VibeNav/releases/download/v1.0/app-debug.apk) (8.3 MB) |

> ⚠ Enable **Install from unknown apps** in your phone settings to sideload.

---

## Features

| Feature | Description |
|---------|-------------|
| 🗺 **Live Map** | OpenStreetMap + Google Satellite hybrid tiles, zoom up to 22 |
| 🔍 **Search & Pin** | Type any address (Nominatim) or long-press anywhere on the map |
| 📍 **My Location** | One-tap center on your current GPS position |
| 🛰 **Satellite View** | Toggle between street map and satellite imagery |
| 🎯 **Tracking** | Background GPS service with live distance, speed, and ETA |
| 🔊 **Arrival Alert** | Full-screen lock-screen alarm with vibration + beep + voice |
| 🗣 **Voice Alerts** | Text-to-speech announces distances (1km, 500m, 300m, etc.) and arrival |
| ⚙ **Adjustable Radius** | Set alert trigger distance from 50m to 1000m |
| ⏱ **Auto-Stop** | Tracking stops automatically 60s after arrival |
| 🔇 **Notification Controls** | Silence or Stop alert from the notification bar |
| 📍 **Live Address** | Shows current location as "📍 You are now in [area], [street], near [landmark]" |
| 🌤 **Live Weather** | Current temperature + condition icon from Open-Meteo |
| 💾 **Save Places** | Bookmark destinations and navigate to them anytime |
| 📜 **Trip History** | Auto-saves every trip with distance, route, and timing |
| 📤 **Share** | Send destination as Google Maps link via any app |
| 🧭 **Direction Arrow** | Blue circle with rotating arrow pointing toward destination |
| 📖 **Tutorial** | 12-page step-by-step guide on first launch |
| 📲 **In-App Updates** | Check for new versions and install directly from Settings |
| 🌙 **Day/Night Theme** | Auto-switches between light and dark colors |

---

## Build from Source

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (free)
- Android phone with Developer Options + USB Debugging enabled

### Steps
1. Open Android Studio → **File → Open** → select `VibeNav/` folder
2. Wait for Gradle sync
3. Connect phone → **Run ▶** (green triangle)
4. Or build APK: **Build → Build APK** → file at `app/build/outputs/apk/debug/app-debug.apk`

### Permissions
- **Location** — "Allow all the time" for background tracking
- **Notifications** — required for tracking service and alerts

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin** |
| Map | **osmdroid** (OpenStreetMap) |
| Geocoding | **Nominatim** (free, no API key) |
| Weather | **Open-Meteo** (free, no API key) |
| Tracking | **Android Foreground Service** |
| Voice | **TextToSpeech** |
| Alerts | **ToneGenerator + Vibrator** |
| CI/CD | **GitHub Actions** — auto-build APK on tags |
| Security | **CodeQL** scanning + **Dependabot** alerts |
| Updates | **GitHub Releases API** — in-app update checker + download |

---

## Security

- [Private vulnerability reporting](https://github.com/DilumSaluka/VibeNav/security/advisories) — report issues privately
- [CodeQL analysis](https://github.com/DilumSaluka/VibeNav/security/code-scanning) runs on every push
- [Dependabot](https://github.com/DilumSaluka/VibeNav/security/dependabot) monitors dependency vulnerabilities
- See [SECURITY.md](.github/SECURITY.md) for details

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines. All contributions welcome!

---

## License

MIT License — see [LICENSE](LICENSE) file.

Copyright (c) 2026 Dilum Saluka
