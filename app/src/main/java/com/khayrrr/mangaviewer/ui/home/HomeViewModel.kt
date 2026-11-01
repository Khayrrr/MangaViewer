package com.khayrrr.mangaviewer.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.preferences.AlbumSortBy
import com.khayrrr.mangaviewer.data.preferences.AlbumViewStyle
import com.khayrrr.mangaviewer.data.preferences.AppSettings
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
    val hasRootFolders: Boolean = false
)

class HomeViewModel(
    private val mangaRepository: MangaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        mangaRepository.albumsFlow,
        settingsRepository.settingsFlow,
        _searchQuery,
        _isSearchActive,
        mangaRepository.isScanning
    ) { albums, settings, query, searchActive, isScanning ->
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
            hasRootFolders = settings.rootFolderUris.isNotEmpty()
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
                    // Newest first by default
                    b.lastModified.compareTo(a.lastModified)
                }
                AlbumSortBy.OLDEST -> {
                    // Oldest first
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

    fun togglePin(album: Album) {
        viewModelScope.launch {
            mangaRepository.setPinned(album.uri, !album.isPinned)
        }
    }

    fun toggleHide(album: Album) {
        viewModelScope.launch {
            mangaRepository.setHidden(album.uri, !album.isHidden)
        }
    }
}
