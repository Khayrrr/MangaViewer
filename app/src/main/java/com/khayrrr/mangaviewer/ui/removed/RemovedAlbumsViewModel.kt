package com.khayrrr.mangaviewer.ui.removed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemovedAlbumsViewModel(
    private val mangaRepository: MangaRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = mangaRepository.albumsFlow
        .map { list -> list.filter { it.isRemoved }.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(album: Album) {
        viewModelScope.launch { mangaRepository.restoreAlbum(album.uri) }
    }
}