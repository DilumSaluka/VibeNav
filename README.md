# VibeNav

**Navigate with confidence.** VibeNav is a free Android navigation app that tracks your live location on OpenStreetMap and alerts you — with vibration, sound, and voice — when you're approaching your destination. No API keys, no cost, no ads.

Created by **[Dilum Saluka](https://github.com/DilumSaluka)**.

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
| 🌙 **Day/Night Theme** | Auto-switches between light and dark colors |

## Build Instructions

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

## Tech Stack
- **Kotlin** — Android app logic
- **osmdroid** — OpenStreetMap rendering
- **Nominatim** — free address geocoding & search
- **Open-Meteo** — free weather API (no key needed)
- **Android Foreground Service** — background GPS tracking
- **TextToSpeech** — voice distance announcements
- **ToneGenerator + Vibrator** — audio and haptic alerts

## License
MIT License — see [LICENSE](LICENSE) file.

Copyright (c) 2026 Dilum Saluka
