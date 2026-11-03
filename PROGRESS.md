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

- **Phase 2: Folder picker, recursive album scanning, Room database, home screen (grid/list/mini-grid, sorting, search)**
  - **Stage A (Data Layer)**:
    - Natural sort utility (`NaturalOrderComparator`) with unit tests for alphanumeric sorting (e.g. "page 2" before "page 10").
    - Room database (`MangaDatabase`) with `AlbumMetadataEntity` (pinned, hidden, custom cover URI, tags, reading progress) and `FavoriteImageEntity`.
    - Room DAOs (`AlbumDao`, `FavoriteDao`) and `Converters` for tag list serialization.
    - Recursive SAF folder scanner (`SafScanner`) using `DocumentsContract` cursor queries over child documents to detect albums containing supported image formats (PNG, JPEG, WebP, GIF, AVIF, HEIC, HEIF).
    - Domain models (`Album`, `ImageItem`) and `MangaRepository` (`MangaRepositoryImpl`) handling persisted SAF folder permissions (`takePersistableUriPermission`/`releasePersistableUriPermission`), background rescans, and combined Room metadata flows.
    - `HomeViewModel` with sorting strategies, search filtering across album names and tags, and UI state management.
    - Koin DI configured in `AppModule` providing Room database, DAOs, scanner, repository, and ViewModels.
  - **Stage B (Home Screen UI)**:
    - Full Home screen UI matching design mockups (Screen 2: Grid view, Screen 3: List view).
    - Grid view (2 columns, default), List view (horizontal rows with 64x64 thumbnails and inline progress bars), and Mini-grid view (3 columns).
    - Pinned albums section pinned at top with pin badge icon on thumbnails.
    - Three-dots overflow menu containing:
      - View style selection (Grid 2 columns / List / Mini-grid 3 columns).
      - Sort by (Alphabetical / Last modified / Oldest).
      - Order (Ascending / Descending &mdash; enabled strictly only when Alphabetical is active, disabled otherwise).
      - Favorites navigation.
      - Hidden albums navigation.
      - Refresh action.
      - Settings navigation.
    - Expandable top search bar with real-time filtering across both album names and tags.
    - Pull-to-refresh integration (`PullToRefreshBox`).
    - Coil 3 image loading with asynchronous thumbnail caching and fallback gradient placeholders.
    - Empty states for initial setup (folder picker prompt) and zero search results.
    - Root folder picker integration in both Home screen and Settings screen.
    - Fixed alignment parameter type mismatch (`Alignment.CenterHorizontally` for Column) in `AlbumItemComponents.kt`.

## Current Phase
Phase 5 complete (merged to main): reader horizontal mode (LTR/RTL) with zoom and edge page-turning, vertical seekbar, reading direction menu, GIF/AVIF animate setting, Removed Albums screen with restore, .nomedia written when hiding albums.

## Known issues / limits
- Zoom is centered, no flick momentum when zoomed
- Horizontal zoom bounds are measured against the whole screen
- Share-as-images has a size limit on very large albums
- Albums hidden before Phase 5 need unhide + re-hide to get .nomedia
- .nomedia also hides sub-albums in gallery apps

## Next step
Phase 6: PDF support (each PDF as its own album, rendered with PdfRenderer).