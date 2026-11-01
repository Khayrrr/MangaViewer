package com.khayrrr.mangaviewer.data.preferences

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AlbumViewStyle {
    GRID_2,       // Default 2-column grid
    LIST,         // Horizontal row list
    MINI_GRID_3   // Compact 3-column mini-grid
}

enum class AlbumSortBy {
    LAST_MODIFIED,
    ALPHABETICAL,
    OLDEST
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class ReadingDirection {
    VERTICAL,
    LTR,
    RTL
}

enum class PageIndicatorFormat {
    COUNT,   // e.g. "12/48"
    PERCENT  // e.g. "35%"
}

enum class ReaderBackground {
    BLACK,   // OLED Black (#000000)
    DARK,    // Dark gray (#18181B)
    SEPIA,   // Warm sepia (#F5EBD7)
    WHITE    // Clean white (#FFFFFF)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val customSeedColor: Long = 0xFF006B5FL, // Default calm teal-green
    val viewStyle: AlbumViewStyle = AlbumViewStyle.GRID_2,
    val sortBy: AlbumSortBy = AlbumSortBy.LAST_MODIFIED,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val readingDirection: ReadingDirection = ReadingDirection.VERTICAL,
    val pageIndicatorFormat: PageIndicatorFormat = PageIndicatorFormat.COUNT,
    val hideSystemBars: Boolean = false,
    val animateGifAvif: Boolean = true,
    val landscapeDoublePage: Boolean = false,
    val keepScreenOn: Boolean = false,
    val readerBackground: ReaderBackground = ReaderBackground.BLACK,
    val biometricLock: Boolean = false,
    val rootFolderUris: Set<String> = emptySet()
)
