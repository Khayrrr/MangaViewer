package com.khayrrr.mangaviewer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.preferences.AlbumSortBy
import com.khayrrr.mangaviewer.data.preferences.AlbumViewStyle
import com.khayrrr.mangaviewer.data.preferences.AppSettings
import com.khayrrr.mangaviewer.data.preferences.PageIndicatorFormat
import com.khayrrr.mangaviewer.data.preferences.ReaderBackground
import com.khayrrr.mangaviewer.data.preferences.ReadingDirection
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.preferences.SortOrder
import com.khayrrr.mangaviewer.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setUseDynamicColor(useDynamic: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseDynamicColor(useDynamic)
        }
    }

    fun setCustomSeedColor(color: Long) {
        viewModelScope.launch {
            settingsRepository.setCustomSeedColor(color)
        }
    }

    fun setViewStyle(style: AlbumViewStyle) {
        viewModelScope.launch {
            settingsRepository.setViewStyle(style)
        }
    }

    fun setSortBy(sortBy: AlbumSortBy) {
        viewModelScope.launch {
            settingsRepository.setSortBy(sortBy)
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsRepository.setSortOrder(order)
        }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch {
            settingsRepository.setReadingDirection(direction)
        }
    }

    fun setPageIndicatorFormat(format: PageIndicatorFormat) {
        viewModelScope.launch {
            settingsRepository.setPageIndicatorFormat(format)
        }
    }

    fun setHideSystemBars(hide: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideSystemBars(hide)
        }
    }

    fun setAnimateGifAvif(animate: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAnimateGifAvif(animate)
        }
    }

    fun setLandscapeDoublePage(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLandscapeDoublePage(enabled)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }

    fun setReaderBackground(bg: ReaderBackground) {
        viewModelScope.launch {
            settingsRepository.setReaderBackground(bg)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricLock(enabled)
        }
    }

    fun addRootFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.addRootFolderUri(uri)
        }
    }

    fun removeRootFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.removeRootFolderUri(uri)
        }
    }
}
