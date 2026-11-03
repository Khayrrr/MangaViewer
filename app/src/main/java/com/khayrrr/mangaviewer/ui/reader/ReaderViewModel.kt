package com.khayrrr.mangaviewer.ui.reader

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.AppSettings
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

data class ReaderState(
    val loading: Boolean = true,
    val images: List<ImageItem> = emptyList(),
    val startPage: Int = 0
)

enum class WallpaperTarget(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
}

class ReaderViewModel(
    private val context: Context,
    private val mangaRepository: MangaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    /** width / height of each page, measured ahead of the reading position. */
    private val _aspects = MutableStateFlow<Map<String, Float>>(emptyMap())
    val aspects: StateFlow<Map<String, Float>> = _aspects.asStateFlow()

    /** Short messages for the screen to show (toasts). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val favoriteImageUris: StateFlow<Set<String>> = mangaRepository.favoriteImagesFlow
        .map { list -> list.map { it.imageUri }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var albumUri: String = ""
    private var loadedFor: String? = null
    private var images: List<ImageItem> = emptyList()
    private val failedMeasure = mutableSetOf<String>()
    private var measureJob: Job? = null
    private var saveJob: Job? = null
    private var currentPage: Int = -1

    /** initialPage < 0 means "resume where I left off". */
    fun load(albumUri: String, initialPage: Int) {
        if (loadedFor == albumUri) return
        loadedFor = albumUri
        this.albumUri = albumUri
        viewModelScope.launch {
            val loaded = try {
                mangaRepository.getImagesForAlbum(albumUri)
            } catch (e: Exception) {
                emptyList()
            }
            images = loaded
            // lastPage is stored as "pages read" (1-based), so the page index is lastPage - 1
            val album = mangaRepository.albumsFlow.value.firstOrNull { it.uri == albumUri }
            val resumeIndex = ((album?.lastPage ?: 0) - 1).coerceAtLeast(0)
            val start = (if (initialPage >= 0) initialPage else resumeIndex)
                .coerceIn(0, (loaded.size - 1).coerceAtLeast(0))

            // First window is measured before showing, so the opening pages do not jump
            withContext(Dispatchers.IO) { measureRange(start - 2, start + 9) }
            _state.value = ReaderState(loading = false, images = loaded, startPage = start)
        }
    }

    fun onPageChanged(page: Int) {
        if (images.isEmpty()) return
        currentPage = page

        measureJob?.cancel()
        measureJob = viewModelScope.launch(Dispatchers.IO) {
            measureRange(page - 2, page + 12)
        }

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            saveProgress(page)
        }
    }

    /** Called when leaving the reader so the last page is never lost. */
    fun saveNow() {
        val page = currentPage
        if (page < 0 || images.isEmpty()) return
        saveJob?.cancel()
        viewModelScope.launch(NonCancellable) { saveProgress(page) }
    }

    private suspend fun saveProgress(page: Int) {
        val total = images.size
        if (total == 0) return
        mangaRepository.updateReadingProgress(
            albumUri,
            (page + 1).coerceAtMost(total),
            (page + 1).coerceAtMost(total).toFloat() / total
        )
    }

    fun reportAspect(uri: String, aspect: Float) {
        if (!aspect.isFinite() || aspect <= 0f) return
        if (_aspects.value.containsKey(uri)) return
        _aspects.update { it + (uri to aspect) }
    }

    fun toggleFavorite(image: ImageItem) {
        viewModelScope.launch {
            mangaRepository.toggleFavorite(image.uri, albumUri, image.name)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setHideSystemBars(hide: Boolean) {
        viewModelScope.launch { settingsRepository.setHideSystemBars(hide) }
    }

    // ---- Stage B: page actions ----

    fun setWallpaper(image: ImageItem, target: WallpaperTarget) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(image.uri))?.use { input ->
                        WallpaperManager.getInstance(context).setStream(input, null, true, target.flags)
                        true
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            _messages.tryEmit(if (ok) "Wallpaper set" else "Could not set wallpaper")
        }
    }

    /** [baseName] is the new name without extension; the original extension is kept. */
    fun renameImage(image: ImageItem, baseName: String) {
        val ext = image.name.substringAfterLast('.', "")
        val finalName = if (ext.isEmpty()) baseName else "$baseName.$ext"
        viewModelScope.launch {
            val newUri = mangaRepository.renameImage(image.uri, finalName)
            if (newUri == null) {
                _messages.tryEmit("Could not rename image")
                return@launch
            }
            _aspects.value[image.uri]?.let { aspect ->
                _aspects.update { it + (newUri to aspect) }
            }
            refreshImages()
            _messages.tryEmit("Image renamed")
            launch { mangaRepository.rescan() }
        }
    }

    fun deleteImage(image: ImageItem) {
        viewModelScope.launch {
            val ok = mangaRepository.deleteImage(image.uri)
            if (!ok) {
                _messages.tryEmit("Could not delete image")
                return@launch
            }
            refreshImages()
            _messages.tryEmit("Image deleted")
            launch { mangaRepository.rescan() }
        }
    }

    private suspend fun refreshImages() {
        val loaded = try {
            mangaRepository.getImagesForAlbum(albumUri)
        } catch (e: Exception) {
            return
        }
        images = loaded
        _state.value = _state.value.copy(images = loaded)
    }

    private suspend fun measureRange(from: Int, to: Int) {
        val list = images
        if (list.isEmpty()) return
        for (i in from.coerceAtLeast(0)..to.coerceAtMost(list.lastIndex)) {
            yield()
            val uri = list[i].uri
            if (_aspects.value.containsKey(uri) || failedMeasure.contains(uri)) continue
            val aspect = measureAspect(uri)
            if (aspect == null) {
                failedMeasure.add(uri)
            } else {
                reportAspect(uri, aspect)
            }
        }
    }

    private fun measureAspect(uri: String): Float? {
        return try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    options.outWidth.toFloat() / options.outHeight
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}