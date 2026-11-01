package com.khayrrr.mangaviewer.ui.home

import android.net.Uri
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val albums: List<Album> = emptyList(),
    val pinnedAlbums: List<Album> = emptyList(),
    val otherAlbums: List<Album> = emptyList(),
    val viewStyle: AlbumViewStyle = AlbumViewStyle.GRID_2,
    val sortBy: AlbumSortBy = AlbumSortBy.LAST_MODIFIED,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isScanning: Boolean = false,
    val hasRootFolders: Boolean = false,
    val selectedAlbumUris: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

class HomeViewModel(
    private val mangaRepository: MangaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _selectedAlbumUris = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        mangaRepository.albumsFlow,
        settingsRepository.settingsFlow,
        _searchQuery,
        _isSearchActive,
        _selectedAlbumUris,
        mangaRepository.isScanning
    ) { albums, settings, query, searchActive, selectedUris, isScanning ->
        // Filter visible non-hidden albums
        val visibleAlbums = albums.filter { !it.isHidden }

        // Filter by search query (name AND tags)
        val filteredAlbums = if (query.isBlank()) {
            visibleAlbums
        } else {
            val q = query.trim().lowercase()
            visibleAlbums.filter { album ->
                album.name.lowercase().contains(q) || album.tags.any { it.lowercase().contains(q) }
            }
        }

        // Sort albums: Pinned first, then by selected sort strategy
        val comparator = createAlbumComparator(settings.sortBy, settings.sortOrder)
        val sortedAlbums = filteredAlbums.sortedWith(comparator)

        val pinned = sortedAlbums.filter { it.isPinned }
        val other = sortedAlbums.filter { !it.isPinned }

        HomeUiState(
            albums = sortedAlbums,
            pinnedAlbums = pinned,
            otherAlbums = other,
            viewStyle = settings.viewStyle,
            sortBy = settings.sortBy,
            sortOrder = settings.sortOrder,
            searchQuery = query,
            isSearchActive = searchActive,
            isScanning = isScanning,
            hasRootFolders = settings.rootFolderUris.isNotEmpty(),
            selectedAlbumUris = selectedUris,
            isSelectionMode = selectedUris.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private fun createAlbumComparator(sortBy: AlbumSortBy, sortOrder: SortOrder): Comparator<Album> {
        return Comparator { a, b ->
            // Always pinned albums first
            if (a.isPinned != b.isPinned) {
                return@Comparator if (a.isPinned) -1 else 1
            }

            when (sortBy) {
                AlbumSortBy.ALPHABETICAL -> {
                    val comp = NaturalOrderComparator.compare(a.name, b.name)
                    if (sortOrder == SortOrder.ASCENDING) comp else -comp
                }
                AlbumSortBy.LAST_MODIFIED -> {
                    b.lastModified.compareTo(a.lastModified)
                }
                AlbumSortBy.OLDEST -> {
                    a.lastModified.compareTo(b.lastModified)
                }
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
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

    fun refresh() {
        viewModelScope.launch {
            mangaRepository.rescan()
        }
    }

    fun addRootFolder(uri: Uri) {
        viewModelScope.launch {
            mangaRepository.addRootFolder(uri)
        }
    }

    // Selection Mode
    fun toggleAlbumSelection(uri: String) {
        val current = _selectedAlbumUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedAlbumUris.value = current
    }

    fun enterSelectionMode(uri: String) {
        _selectedAlbumUris.value = setOf(uri)
    }

    fun clearSelection() {
        _selectedAlbumUris.value = emptySet()
    }

    fun getSelectedAlbums(): List<Album> {
        val selectedSet = _selectedAlbumUris.value
        return uiState.value.albums.filter { selectedSet.contains(it.uri) }
    }

    fun removeSelectedAlbums(fromDevice: Boolean) {
        val selected = getSelectedAlbums()
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

    fun togglePinSelectedAlbum() {
        val album = getSelectedAlbums().firstOrNull() ?: return
        viewModelScope.launch {
            mangaRepository.setPinned(album.uri, !album.isPinned)
            clearSelection()
        }
    }

    fun renameSelectedAlbum(newName: String) {
        val album = getSelectedAlbums().firstOrNull() ?: return
        viewModelScope.launch {
            mangaRepository.renameAlbum(album.uri, newName)
            clearSelection()
        }
    }

    fun hideSelectedAlbum() {
        val album = getSelectedAlbums().firstOrNull() ?: return
        viewModelScope.launch {
            mangaRepository.setHidden(album.uri, true)
            clearSelection()
        }
    }

    fun setCustomCover(albumUri: String, coverUri: String) {
        viewModelScope.launch {
            mangaRepository.setCustomCover(albumUri, coverUri)
            clearSelection()
        }
    }

    fun setAlbumTags(albumUri: String, tags: List<String>) {
        viewModelScope.launch {
            mangaRepository.setTags(albumUri, tags)
        }
    }

    suspend fun getImagesForAlbum(albumUri: String): List<ImageItem> {
        return mangaRepository.getImagesForAlbum(albumUri)
    }
}
