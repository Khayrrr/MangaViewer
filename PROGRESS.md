# Project Progress - MangaViewer

Offline, local manga and image reader app for Android built with Kotlin, Jetpack Compose, Material 3 Expressive, Room, DataStore, and Coil 3.

## Completed Phases
- **Phase 0: Git and CI setup**
  - Git initialized on branch `main` with remote `https://github.com/Khayrrr/MangaViewer.git`.
  - Android `.gitignore` added (excluding build outputs, gradle cache, idea, keystores, APKs).
  - Gradle wrapper files committed (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`).
  - GitHub Actions CI workflow created (`.github/workflows/android.yml`) to automatically assemble debug APK and upload `manga-reader-debug-apk` artifact on push to `main` and manual trigger.
  - Initialized `PROGRESS.md`.

## Current Phase
- **Phase 1: Project setup, theme (dynamic color, dark mode), navigation, settings storage**
  - Status: In progress

## Known Issues
- None.

## Exact Next Step
- Complete Phase 1: Set up Gradle build configuration (Koin, Compose, DataStore, Room, Coil 3), Material 3 Expressive theming tokens based on design mockups, NavHost navigation setup, and DataStore settings repository with SettingsViewModel.
