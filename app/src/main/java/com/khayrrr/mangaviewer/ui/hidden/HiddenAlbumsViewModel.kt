package com.khayrrr.mangaviewer.ui.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.AlbumSortBy
import com.khayrrr.mangaviewer.data.preferences.AlbumViewStyle
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.preferences.SortOrder
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import com.khayrrr.mangaviewer.util.NaturalOrderComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HiddenUiState(
    val albums: List<Album> = emptyList(),
    val viewStyle: AlbumViewStyle = AlbumViewStyle.GRID_2,
    val selectedUris: Set<String> = emptySet()
)

class HiddenAlbumsViewModel(
    private val mangaRepository: MangaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selected = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HiddenUiState> = combine(
        mangaRepository.albumsFlow,
        settingsRepository.settingsFlow,
        _selected
    ) { albums, settings, selected ->
        val hidden = albums.filter { it.isHidden && !it.isRemoved }
        val sorted = hidden.sortedWith(createComparator(settings.sortBy, settings.sortOrder))
        HiddenUiState(
            albums = sorted,
            viewStyle = settings.viewStyle,
            selectedUris = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HiddenUiState()
    )

    private fun createComparator(sortBy: AlbumSortBy, order: SortOrder): Comparator<Album> {
        return Comparator { a, b ->
            when (sortBy) {
                AlbumSortBy.ALPHABETICAL -> {
                    val comp = NaturalOrderComparator.compare(a.name, b.name)
                    if (order == SortOrder.ASCENDING) comp else -comp
                }
                AlbumSortBy.LAST_MODIFIED -> b.lastModified.compareTo(a.lastModified)
                AlbumSortBy.OLDEST -> a.lastModified.compareTo(b.lastModified)
            }
        }
    }

    suspend fun isLockRequired(): Boolean {
        return settingsRepository.settingsFlow.first().biometricLock
    }

    fun setViewStyle(style: AlbumViewStyle) {
        viewModelScope.launch { settingsRepository.setViewStyle(style) }
    }

    // Selection
    fun enterSelection(uri: String) {
        _selected.value = setOf(uri)
    }

    fun toggleSelection(uri: String) {
        val current = _selected.value.toMutableSet()
        if (!current.add(uri)) current.remove(uri)
        _selected.value = current
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun selectAll() {
        _selected.value = uiState.value.albums.map { it.uri }.toSet()
    }

    private fun selectedAlbums(): List<Album> {
        val selected = _selected.value
        return uiState.value.albums.filter { selected.contains(it.uri) }
    }

    // Actions
    fun unhideSelected() {
        val selected = selectedAlbums()
        viewModelScope.launch {
            selected.forEach { mangaRepository.setHidden(it.uri, false) }
            clearSelection()
        }
    }

    fun removeSelected(fromDevice: Boolean) {
        val selected = selectedAlbums()
        viewModelScope.launch {
            selected.forEach { album ->
                if (fromDevice) {
                    mangaRepository.deleteAlbumFromDevice(album.uri)
                } else {
                    mangaRepository.removeAlbumFromLibrary(album.uri)
                }
            }
            clearSelection()
        }
    }

    fun setCustomCover(albumUri: String, coverUri: String) {
        viewModelScope.launch {
            mangaRepository.setCustomCover(albumUri, coverUri)
            clearSelection()
        }
    }

    suspend fun getImagesForAlbum(albumUri: String): List<ImageItem> {
        return mangaRepository.getImagesForAlbum(albumUri)
    }
}