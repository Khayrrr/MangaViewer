package com.khayrrr.mangaviewer.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoriteItem(
    val imageUri: String,
    val albumUri: String,
    val imageName: String,
    val albumName: String
)

class FavoritesViewModel(
    private val mangaRepository: MangaRepository
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteItem>> = combine(
        mangaRepository.favoriteImagesFlow,
        mangaRepository.albumsFlow
    ) { favs, albums ->
        val names = albums.associate { it.uri to it.name }
        favs.map {
            FavoriteItem(
                imageUri = it.imageUri,
                albumUri = it.albumUri,
                imageName = it.imageName,
                albumName = names[it.albumUri].orEmpty()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun remove(item: FavoriteItem) {
        viewModelScope.launch {
            // toggleFavorite deletes the entry when it already exists
            mangaRepository.toggleFavorite(item.imageUri, item.albumUri, item.imageName)
        }
    }

    /** Position of the favorite image inside its album, used as the reader's start page. */
    suspend fun pageOf(item: FavoriteItem): Int {
        return try {
            mangaRepository.getImagesForAlbum(item.albumUri)
                .indexOfFirst { it.uri == item.imageUri }
                .coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }
}