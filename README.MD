# MangaViewer

A fast, fully offline manga and comic reader for Android. No accounts, no ads, no internet permission: your library stays on your device.

## Features
- **Local library:** pick one or more folders; every sub-folder of images (and every PDF) becomes an album
- **Formats:** JPG, PNG, WebP, GIF, AVIF (with an animation toggle) and PDF
- **Reading modes:** vertical scroll, left-to-right and right-to-left pages
- **Zoom:** smooth pinch and double-tap zoom, flick momentum, page turning at image edges
- **Navigation:** page seekbar, page counter, quick jump back to the first page
- **Organise:** pin, rename, tag, custom covers, favorites, three view styles (grid, mini-grid, list)
- **Search:** by title and by multiple tags, with tag suggestions
- **Privacy:** hide albums behind biometric/screen lock, restore removed albums
- **Share:** images, ZIP, or PDF
- **Design:** Material 3 with dynamic color, light/dark themes and Android 15 predictive back

## Install
Download the latest APK from the [Releases](../../releases) page and open it on your device (you may need to allow "Install unknown apps" for your browser). Requires Android 12 (API 31) or newer.

## Build
Debug build: `./gradlew assembleDebug`

Tech: Kotlin, Jetpack Compose, Material 3, Room, DataStore, Koin, Coil 3.

## Status
Version 1.0.0. See [PROGRESS.md](PROGRESS.md) for the development log and known limits.