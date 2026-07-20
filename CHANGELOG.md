# Changelog

All notable changes to VibeNav are documented here.

---

## v1.3 — 📊 Notification ETA + Live Route Re-fetch + Offline Maps
*Released: 2026-07-19*

### New Features
- 📊 **Notification ETA** — Ongoing notification now shows distance, ETA, and speed (e.g. "1.2 km · 5m 30s · 15 km/h") with a Stop button
- 🔄 **Live Route Re-fetch** — Route line updates as you move more than 500m (3-second debounce, won't spam OSRM)
- 📥 **Offline Maps** — New "Download Nearby Area" button in Settings caches map tiles (zooms 10–18, ~5km radius) for use without internet; cache size displayed

### Updated
- 🔧 **Settings** — New "Offline Maps" section with download button and status text

---

## v1.2 — 🗺 Route Line + Search Autocomplete
*Released: 2026-07-19*

### New Features
- 🚗 **Route Line** — Blue OSRM road path drawn on the map from your location to destination, with road distance and travel time shown below the place name
- 🔍 **Search Autocomplete** — Suggestions appear as you type (3+ characters), tap to instantly set the pin
- 🌙 **Theme Toggle Button** — Dedicated 🌙/☀️ button in the top bar between `?` and ⚙ for one-tap light/dark switching
- 🔊 **Voice Toggle Moved** — Moved from top bar to map overlay, positioned next to 🛰 satellite button for easy access

### Updated
- 📖 **Tutorial** — Expanded to 14 pages covering route line, voice toggle, and theme toggle
- 📖 **Help Guide** — Updated with sections for route line, voice toggle, theme toggle, and search autocomplete

---

## v1.1 — Voice, Speed, Weather, Theme
*Released: 2026-07-18*

### New Features
- 🗣 **Voice Alerts** — Text-to-speech announces distances (rounded to nearest 100m) and arrival; can be muted via 🔇 button
- ⏱ **Speed & ETA** — Live speed (km/h) displayed in circular speed meter + estimated time to arrival in tracking panel
- 🌤 **Live Weather** — Current temperature + condition icon from Open-Meteo in top bar
- 📍 **Live Address** — Shows "📍 You are now in [area], [street], near [landmark]" during tracking
- 💾 **Saved Places** — Bookmark destinations and navigate to them anytime from Settings
- 📜 **Trip History** — Auto-saves every session with date, duration, distance; tap to load, long-press to delete
- 📤 **Share Destination** — Send Google Maps link via any app
- 📖 **Tutorial** — 12-page full-screen guide on first launch, accessible anytime from Settings
- 📖 **Help Guide** — Scrollable detailed walkthrough with step-by-step instructions
- 🌙 **Day/Night Theme** — Full theme support with light/dark colors, manual toggle in Settings
- 📲 **In-App Updates** — Check for new versions from Settings, download and install automatically
- 🛰 **Satellite Toggle** — Moved to map corner overlay for cleaner top bar
- © **Copyright Notice** — "© 2025 Dilum Saluka — MIT License" in Settings footer

### Changed
- 🔄 **Top bar redesign** — Dark navy, NoActionBar, pull-up via translationY
- 🔇 **Notification controls** — Silence and Stop buttons in alert notification
- ⚡ **Version bump** — versionCode=3, versionName="1.1"

### Fixed
- ✅ **CodeQL scanning** — All alerts resolved (backup disabled, cert pinning dismissed as false positive)

---

## v1.0 — Initial Release
*Released: 2026-07-17*

### Features
- 🗺 **Live Map** — OpenStreetMap with Google Satellite hybrid tiles, zoom up to 22
- 🔍 **Search** — Type any address (Nominatim) and fly to location
- 👆 **Long-press Pin** — Hold finger on any map spot to drop a destination pin
- 📍 **My Location** — One-tap center on current GPS position
- 🛰 **Satellite View** — Toggle between street map and satellite imagery
- 🎯 **Tracking** — Background foreground service with live distance updates
- 🔊 **Arrival Alert** — Vibration (max amplitude, pulsing) + audio beep (ToneGenerator)
- 📳 **Proximity Zone** — Configurable green circle (50m–1000m) around destination
- 🔔 **Lock-screen Alert** — Full-screen AlertActivity with alarm
- 🧭 **Direction Arrow** — Blue circle with ▲ rotating toward destination
- 🗺 **Compass** — Compass overlay on the map
- 🔄 **Map Rotation** — Two-finger rotation gesture
- ⚙ **Settings** — Alert radius slider, permission management, disable battery optimization
- 🧹 **Clear Map Cache** — Delete stored tiles to free space
- 🔐 **Permission Handling** — Rationale dialog + permanent denial detection with "Open Settings" button
- 🚫 **Remove Pin** — Red X button next to destination name
- 🐙 **Open Source** — MIT License, GitHub public repository
- 🔒 **Security** — Private vulnerability reporting, Dependabot, CodeQL scanning
- 📦 **CI/CD** — GitHub Actions auto-builds APK on tags, creates Release
- 🤝 **Community** — Code of Conduct, Contributing guidelines, issue/PR templates
