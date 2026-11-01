package com.khayrrr.mangaviewer.ui.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HiddenAlbumsViewModel(
    private val mangaRepository: MangaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = mangaRepository.albumsFlow
        .map { list ->
            list.filter { it.isHidden && !it.isRemoved }
                .sortedBy { it.name.lowercase() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun isLockRequired(): Boolean {
        return settingsRepository.settingsFlow.first().biometricLock
    }

    fun unhide(albumUri: String) {
        viewModelScope.launch {
            mangaRepository.setHidden(albumUri, false)
        }
    }
}