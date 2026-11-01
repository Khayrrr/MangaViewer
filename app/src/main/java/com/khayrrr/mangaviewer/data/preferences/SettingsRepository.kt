package com.khayrrr.mangaviewer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manga_viewer_settings")

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUseDynamicColor(useDynamic: Boolean)
    suspend fun setCustomSeedColor(color: Long)
    suspend fun setViewStyle(style: AlbumViewStyle)
    suspend fun setSortBy(sortBy: AlbumSortBy)
    suspend fun setSortOrder(order: SortOrder)
    suspend fun setReadingDirection(direction: ReadingDirection)
    suspend fun setPageIndicatorFormat(format: PageIndicatorFormat)
    suspend fun setHideSystemBars(hide: Boolean)
    suspend fun setAnimateGifAvif(animate: Boolean)
    suspend fun setLandscapeDoublePage(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setReaderBackground(bg: ReaderBackground)
    suspend fun setBiometricLock(enabled: Boolean)
    suspend fun addRootFolderUri(uri: String)
    suspend fun removeRootFolderUri(uri: String)
}

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val CUSTOM_SEED_COLOR = longPreferencesKey("custom_seed_color")
        val VIEW_STYLE = stringPreferencesKey("view_style")
        val SORT_BY = stringPreferencesKey("sort_by")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val PAGE_INDICATOR_FORMAT = stringPreferencesKey("page_indicator_format")
        val HIDE_SYSTEM_BARS = booleanPreferencesKey("hide_system_bars")
        val ANIMATE_GIF_AVIF = booleanPreferencesKey("animate_gif_avif")
        val LANDSCAPE_DOUBLE_PAGE = booleanPreferencesKey("landscape_double_page")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val READER_BACKGROUND = stringPreferencesKey("reader_background")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val ROOT_FOLDER_URIS = stringSetPreferencesKey("root_folder_uris")
    }

    override val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let {
                    runCatching { ThemeMode.valueOf(it) }.getOrNull()
                } ?: ThemeMode.SYSTEM,
                useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true,
                customSeedColor = preferences[PreferencesKeys.CUSTOM_SEED_COLOR] ?: 0xFF006B5FL,
                viewStyle = preferences[PreferencesKeys.VIEW_STYLE]?.let {
                    runCatching { AlbumViewStyle.valueOf(it) }.getOrNull()
                } ?: AlbumViewStyle.GRID_2,
                sortBy = preferences[PreferencesKeys.SORT_BY]?.let {
                    runCatching { AlbumSortBy.valueOf(it) }.getOrNull()
                } ?: AlbumSortBy.LAST_MODIFIED,
                sortOrder = preferences[PreferencesKeys.SORT_ORDER]?.let {
                    runCatching { SortOrder.valueOf(it) }.getOrNull()
                } ?: SortOrder.DESCENDING,
                readingDirection = preferences[PreferencesKeys.READING_DIRECTION]?.let {
                    runCatching { ReadingDirection.valueOf(it) }.getOrNull()
                } ?: ReadingDirection.VERTICAL,
                pageIndicatorFormat = preferences[PreferencesKeys.PAGE_INDICATOR_FORMAT]?.let {
                    runCatching { PageIndicatorFormat.valueOf(it) }.getOrNull()
                } ?: PageIndicatorFormat.COUNT,
                hideSystemBars = preferences[PreferencesKeys.HIDE_SYSTEM_BARS] ?: false,
                animateGifAvif = preferences[PreferencesKeys.ANIMATE_GIF_AVIF] ?: true,
                landscapeDoublePage = preferences[PreferencesKeys.LANDSCAPE_DOUBLE_PAGE] ?: false,
                keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: false,
                readerBackground = preferences[PreferencesKeys.READER_BACKGROUND]?.let {
                    runCatching { ReaderBackground.valueOf(it) }.getOrNull()
                } ?: ReaderBackground.BLACK,
                biometricLock = preferences[PreferencesKeys.BIOMETRIC_LOCK] ?: false,
                rootFolderUris = preferences[PreferencesKeys.ROOT_FOLDER_URIS] ?: emptySet()
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    override suspend fun setUseDynamicColor(useDynamic: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_DYNAMIC_COLOR] = useDynamic }
    }

    override suspend fun setCustomSeedColor(color: Long) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_SEED_COLOR] = color }
    }

    override suspend fun setViewStyle(style: AlbumViewStyle) {
        context.dataStore.edit { it[PreferencesKeys.VIEW_STYLE] = style.name }
    }

    override suspend fun setSortBy(sortBy: AlbumSortBy) {
        context.dataStore.edit { it[PreferencesKeys.SORT_BY] = sortBy.name }
    }

    override suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[PreferencesKeys.SORT_ORDER] = order.name }
    }

    override suspend fun setReadingDirection(direction: ReadingDirection) {
        context.dataStore.edit { it[PreferencesKeys.READING_DIRECTION] = direction.name }
    }

    override suspend fun setPageIndicatorFormat(format: PageIndicatorFormat) {
        context.dataStore.edit { it[PreferencesKeys.PAGE_INDICATOR_FORMAT] = format.name }
    }

    override suspend fun setHideSystemBars(hide: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_SYSTEM_BARS] = hide }
    }

    override suspend fun setAnimateGifAvif(animate: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ANIMATE_GIF_AVIF] = animate }
    }

    override suspend fun setLandscapeDoublePage(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LANDSCAPE_DOUBLE_PAGE] = enabled }
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = enabled }
    }

    override suspend fun setReaderBackground(bg: ReaderBackground) {
        context.dataStore.edit { it[PreferencesKeys.READER_BACKGROUND] = bg.name }
    }

    override suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BIOMETRIC_LOCK] = enabled }
    }

    override suspend fun addRootFolderUri(uri: String) {
        context.dataStore.edit {
            val current = it[PreferencesKeys.ROOT_FOLDER_URIS] ?: emptySet()
            it[PreferencesKeys.ROOT_FOLDER_URIS] = current + uri
        }
    }

    override suspend fun removeRootFolderUri(uri: String) {
        context.dataStore.edit {
            val current = it[PreferencesKeys.ROOT_FOLDER_URIS] ?: emptySet()
            it[PreferencesKeys.ROOT_FOLDER_URIS] = current - uri
        }
    }
}
