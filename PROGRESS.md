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
- **Phase 3: Selection mode, album actions, Hidden and Favorites screens** (done, merged to `main`)
  - Selection mode, Remove (library or device), Pin, Rename (library root renaming blocked), Share (images or ZIP), Change cover, Edit tags, Hide, Details.
  - Hidden Albums screen with once-per-session biometric lock, Favorites screen.
  - Room database v2 with migration, fixed debug keystore, `.debug` package suffix for debug builds.
- **Phase 4: Reader** (in progress, branch `phase4-reader`)
  - Stage A (done): vertical continuous reader, no gaps, fit to width, pinch and double-tap zoom, page indicator pill, thin progress bar with the toolbar, resume last page, favorite star, keep-screen-on and hide-system-bars toggles in the reader menu, rolling-window page measuring, progress saved as you read.
  - Stage B (next): toolbar actions (set wallpaper, rename, delete, share a page) and the animate GIF/AVIF setting.

## Known Issues
- Share as Images on a very large album can exceed Android's intent size limit; use Share as ZIP.
- Albums removed from the library cannot be restored yet (planned: "Restore removed albums" in Settings).
- Hiding an album only hides it inside this app; gallery apps still show the files.
- Renaming the library root folder is blocked inside the app, because Android revokes the app's access to it; rename it in a file manager and add it again.
- Reader zoom is centered horizontally (not on the pinch point) and has no flick momentum while zoomed.

## Exact Next Step
- Reader Stage B: set wallpaper, rename image, delete image, share image, and the animate GIF/AVIF setting.