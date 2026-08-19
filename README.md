# BookStack Companion

[![Build](https://github.com/WinnyKing57/Bookstack-app/actions/workflows/build-apk.yml/badge.svg)](https://github.com/WinnyKing57/Bookstack-app/actions/workflows/build-apk.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)

**BookStack Companion** is a modern native Android client for accessing and browsing your self-hosted [BookStack](https://www.bookstackapp.com/) instances.

Developed by **WinnyKing** ([winnyking.cloud](https://winnyking.cloud)).

<p align="center">
  <a href="README_FR.md">
    <img src="https://img.shields.io/badge/Fran%C3%A7ais-Fran%C3%A7ais-blue?style=for-the-badge&logo=google-translate" alt="Français" />
  </a>
</p>

---

## Features

- **Secure Multi-Server**: Configure and switch between BookStack servers instantly. Credentials (`Token ID` and `Token Secret`) are encrypted locally via **Android Keystore** and **EncryptedSharedPreferences**.
- **Dashboard**: Quick access to books, shelves, search, favorites, and recently viewed pages.
- **Books & Shelves Browsing**: Grid and list views with local search, sorting, pull-to-refresh, and tree view (Books → Chapters → Pages).
- **Hybrid Page Reader (Compose + WebView)**: Elegant HTML rendering with Light/Dark theme support, dynamic font size adjustment, blockquotes, tables, and code blocks.
- **Offline Mode & Full Download**: Download an entire book for 100% offline access. A clear banner indicates when data comes from the local cache.
- **Favorites & History**: Bookmark pages and track your reading history, isolated per server.
- **Global API Search**: Instant search across books, chapters, and pages from your BookStack instance.
- **Custom Settings**: Theme (System, Light, Dark), text size, cache manager, and manual sync.

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM (Clean Architecture: UI / ViewModel / UseCases / Repository / Data)
- **Dependency Injection**: Hilt / Dagger
- **Networking**: Retrofit 2 + OkHttp 4 + Kotlinx Serialization
- **Local Database**: Room DB
- **Secure Storage**: AndroidX Security Crypto (Android Keystore)
- **Preferences**: Jetpack DataStore Preferences
- **Image Loading**: Coil Compose
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

---

## Create an API Token on BookStack

To connect the app to your BookStack instance:

1. Log in to your BookStack instance.
2. Go to your **Profile** → **Edit Profile** → **API Tokens** section.
3. Click **Add API Token**.
4. Give it a name (e.g., `BookStack Companion App`).
5. Carefully copy the generated **Token ID** and **Token Secret**.

---

## Build & Install

### Open the project in Android Studio
1. Open **Android Studio** (Jellyfish or newer recommended).
2. Select **Open** and choose the project folder.
3. Let Gradle sync dependencies.

### Run unit tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
./gradlew assembleRelease
```

For detailed Linux build instructions, see [info_build.md](info_build.md).

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a PR.

---

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

---

## Developer & Credits

- **Developer**: WinnyKing
- **Website**: [winnyking.cloud](https://winnyking.cloud)
- **Project**: BookStack Companion
