# Kamick by Raza

**Kamick** is an advanced, offline-first Manga, Comic, and Webtoon reader for Android built with **Jetpack Compose** and **Material Design 3**. It features modular **Tachiyomi-style extension support**, multi-format reading, offline library management, and reading progress tracking.

---

## 📱 Features

- **Tachiyomi Extension Engine**:
  - Browse and install third-party comic, manga, and webtoon source extensions (MangaDex, MangaKakalot, Asura Scans, Webtoons, ComicExtra, Flame Comics, MangaSee, and ReadM).
  - Add custom external extension repositories via URL (e.g. Keiyoushi repo).
  - Search across all installed extension sources with language filtering (English, Japanese, Korean, Spanish, All).
- **Multiple Reading Modes**:
  - **Right-to-Left (RTL)**: Traditional Japanese manga reading layout.
  - **Left-to-Right (LTR)**: Western comics and graphic novels.
  - **Webtoon (Continuous Vertical)**: Seamless vertical scrolling tailored for modern color webtoons and manhwa.
- **Reader Customization**:
  - Customizable reader backgrounds: AMOLED Black, Dark Gray, White, and Warm Sepia.
  - Interactive zoom (pinch-to-zoom & double-tap to zoom).
  - Smooth page slider and quick chapter jump controls.
  - Brightness dimming filter for comfortable nighttime reading.
- **Offline Library & History**:
  - Persistent local database powered by **Room**.
  - Track reading progress per chapter, unread badges, and categories.
  - Updates feed showing newly released chapters and sync status.
- **Manga & Comic Trackers**:
  - Manage and track reading status (Reading, Completed, On Hold, Plan to Read).
  - Score ratings and sync interfaces ready for MyAnimeList and AniList.
- **Modern UI & Design**:
  - Pure Jetpack Compose with Material 3 dynamic styling.
  - Custom Kamick by Raza launcher icon and animated launch splash screen.
  - Edge-to-edge layout with full support for dark and light modes.

---

## 📥 Download APK

The pre-built debug APK is available directly in this repository:

- **Path**: [`release/kamick-debug.apk`](release/kamick-debug.apk)
- **Size**: ~29 MB
- **Target OS**: Android 8.0+ (API Level 26+)

### How to Install:
1. Download `kamick-debug.apk` onto your Android device (or emulator).
2. Tap the APK file to install. If prompted, enable **Install from Unknown Sources** in your device settings.
3. Open **Kamick** and enjoy reading!

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose, Material 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Repository pattern
- **Local Database**: Room Database (SQLite with Flow & Coroutines)
- **Image Loading**: Coil 3 with disk and memory caching
- **Navigation**: Jetpack Navigation Compose with type-safe routing
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Building from Source

To build the APK locally using Android Studio or Gradle:

```bash
# Clone the repository
git clone https://github.com/<username>/<repo>.git
cd <repo>

# Build debug APK
gradle assembleDebug

# The generated APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

Created by **Raza**. Distributed for educational and personal reading purposes.
