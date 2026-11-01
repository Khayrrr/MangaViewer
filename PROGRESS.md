# Project Progress - MangaViewer

Offline, local manga and image reader app for Android built with Kotlin, Jetpack Compose, Material 3 Expressive, Room, DataStore, and Coil 3.

## Completed Phases
- **Phase 0: Git and CI setup**
  - Initialized git on branch `main` with remote `https://github.com/Khayrrr/MangaViewer.git`.
  - Added Android `.gitignore` excluding build artifacts, caches, and keystores.
  - Committed Gradle wrapper binaries and scripts (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
  - Added GitHub Actions CI workflow (`.github/workflows/android.yml`) to automatically build debug APK and upload artifacts on push to `main`.
  - Pushed initial commit to GitHub repository.

- **Phase 1: Project setup, theme (dynamic color, dark mode), navigation, settings storage**
  - Configured Gradle version catalog (`libs.versions.toml`) and build scripts (`build.gradle.kts`, `app/build.gradle.kts`) with AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, Koin 4.0.0, Coil 3.0.4, Room 2.6.1, and DataStore 1.1.1.
  - Verified no `INTERNET` permission in `AndroidManifest.xml` (fully offline and local).
  - Implemented Material 3 Expressive theme with calm teal-green design tokens (`#006B5F` / `#80D5C8`), dynamic Monet color support (Android 12+), custom seed color palette generator, rounded expressive shapes, and spring motion specs.
  - Implemented DataStore preferences `AppSettings` and `SettingsRepository` for theme mode, dynamic seed color, custom seed color, album view style, sorting, reading direction, page indicator format, system bars toggle, animated GIF/AVIF, landscape double-page, keep screen on, reader background canvas, and biometric lock.
  - Created `MangaApplication` with Koin module and Coil 3 `ImageLoader` supporting animated decoders.
  - Implemented `MainActivity` with edge-to-edge support and `MangaNavGraph` with animated screen transitions.
  - Built `SettingsScreen` matching Screen 20 mockup with full interactive settings controls and `SettingsViewModel`.
  - Built scaffolding for `HomeScreen`, `ReaderScreen`, `HiddenAlbumsScreen`, and `FavoritesScreen`.

## Current Phase
- **Phase 2: Folder picker, recursive album scanning, Room database, home screen (grid/list/mini-grid, sorting, search)**
  - Status: Not started (awaiting instruction)

## Known Issues
- None.

## Exact Next Step
- Phase 2: Implement SAF folder picker (`OpenDocumentTree`), recursive background scanner with `DocumentsContract`, Room database entities/DAOs for album metadata & favorites, and full Home screen with 2-column grid, list view, 3-column mini-grid, search filtering, and sorting.
