package com.khayrrr.mangaviewer.ui.home

import kotlinx.coroutines.flow.map
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
    val isSelectionMode: Boolean = false,
    val tagFilters: List<String> = emptyList()
)

private data class HomeExtras(
    val query: String,
    val searchActive: Boolean,
    val selectedUris: Set<String>,
    val isScanning: Boolean,
    val tagFilters: List<String>
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

    private val _tagFilters = MutableStateFlow<List<String>>(emptyList())

    private val extrasFlow = combine(
        _searchQuery,
        _isSearchActive,
        _selectedAlbumUris,
        mangaRepository.isScanning,
        _tagFilters
    ) { query, searchActive, selectedUris, isScanning, tagFilters ->
        HomeExtras(query, searchActive, selectedUris, isScanning, tagFilters)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        mangaRepository.albumsFlow,
        settingsRepository.settingsFlow,
        extrasFlow
    ) { albums, settings, extras ->
        val query = extras.query
        val searchActive = extras.searchActive
        val selectedUris = extras.selectedUris
        val isScanning = extras.isScanning
        val tagFilters = extras.tagFilters

        // Filter visible non-hidden albums
        val visibleBase = albums.filter { !it.isHidden && !it.isRemoved }

        // Tag filters: an album must have every selected tag
        val visibleAlbums = if (tagFilters.isEmpty()) {
            visibleBase
        } else {
            visibleBase.filter { album ->
                tagFilters.all { filter -> album.tags.any { it.lowercase().contains(filter) } }
            }
        }

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
            isSelectionMode = selectedUris.isNotEmpty(),
            tagFilters = tagFilters
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    /** Every tag used on any album, most used first. */
    val allTags: StateFlow<List<String>> = mangaRepository.albumsFlow
        .map { albums ->
            albums.flatMap { it.tags }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
            _tagFilters.value = emptyList()
        }
    }

    fun addTagFilter(raw: String) {
        val tag = raw.trim().lowercase()
        if (tag.isEmpty()) return
        if (!_tagFilters.value.contains(tag)) {
            _tagFilters.value = _tagFilters.value + tag
        }
        _searchQuery.value = ""
    }

    fun removeTagFilter(tag: String) {
        _tagFilters.value = _tagFilters.value - tag
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

    fun setSort(sortBy: AlbumSortBy, order: SortOrder?) {
        viewModelScope.launch {
            settingsRepository.setSortBy(sortBy)
            if (order != null) settingsRepository.setSortOrder(order)
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

    fun selectAll() {
        _selectedAlbumUris.value = uiState.value.albums.map { it.uri }.toSet()
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
        val selected = getSelectedAlbums()
        if (selected.isEmpty()) return
        // If everything is already pinned, unpin; otherwise pin all
        val pin = !selected.all { it.isPinned }
        viewModelScope.launch {
            selected.forEach { mangaRepository.setPinned(it.uri, pin) }
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
        val selected = getSelectedAlbums()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            selected.forEach { mangaRepository.setHidden(it.uri, true) }
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