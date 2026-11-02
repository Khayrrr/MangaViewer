package com.khayrrr.mangaviewer.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.khayrrr.mangaviewer.data.local.dao.AlbumDao
import com.khayrrr.mangaviewer.data.local.dao.FavoriteDao
import com.khayrrr.mangaviewer.data.local.entity.AlbumMetadataEntity
import com.khayrrr.mangaviewer.data.local.entity.FavoriteImageEntity
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.scanner.SafScanner
import com.khayrrr.mangaviewer.data.scanner.ScannedAlbumResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface MangaRepository {
    val albumsFlow: StateFlow<List<Album>>
    val isScanning: StateFlow<Boolean>
    val favoriteImagesFlow: Flow<List<FavoriteImageEntity>>

    suspend fun rescan()
    suspend fun addRootFolder(treeUri: Uri)
    suspend fun removeRootFolder(treeUri: Uri)
    suspend fun setPinned(folderUri: String, isPinned: Boolean)
    suspend fun setHidden(folderUri: String, isHidden: Boolean)
    suspend fun setCustomCover(folderUri: String, coverUri: String?)
    suspend fun setTags(folderUri: String, tags: List<String>)
    suspend fun updateReadingProgress(folderUri: String, lastPage: Int, readPercent: Float)
    suspend fun toggleFavorite(imageUri: String, albumUri: String, imageName: String)
    fun isFavoriteFlow(imageUri: String): Flow<Boolean>
    suspend fun isFavorite(imageUri: String): Boolean
    suspend fun getImagesForAlbum(folderUri: String): List<ImageItem>
    suspend fun renameAlbum(folderUri: String, newName: String): Boolean
    suspend fun deleteAlbumFromDevice(folderUri: String): Boolean
    suspend fun removeAlbumFromLibrary(folderUri: String)
}

class MangaRepositoryImpl(
    private val context: Context,
    private val albumDao: AlbumDao,
    private val favoriteDao: FavoriteDao,
    private val settingsRepository: SettingsRepository,
    private val scanner: SafScanner
) : MangaRepository {

    companion object {
        private const val TAG = "MangaRepository"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _scannedAlbums = MutableStateFlow<List<ScannedAlbumResult>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override val favoriteImagesFlow: Flow<List<FavoriteImageEntity>> = favoriteDao.observeAllFavorites()

    override val albumsFlow: StateFlow<List<Album>> = combine(
        _scannedAlbums,
        albumDao.observeAllMetadata()
    ) { scannedList, metadataList ->
        val metadataMap = metadataList.associateBy { it.folderUri }

        scannedList.map { scanned ->
            val meta = metadataMap[scanned.folderUri]
            Album(
                uri = scanned.folderUri,
                name = scanned.name,
                path = scanned.path,
                totalImages = scanned.totalImages,
                totalSize = scanned.totalSize,
                coverUri = meta?.customCoverUri ?: scanned.defaultCoverUri,
                lastModified = scanned.lastModified,
                isPinned = meta?.isPinned ?: false,
                isHidden = meta?.isHidden ?: false,
                isRemoved = meta?.isRemoved ?: false,
                tags = meta?.tags ?: emptyList(),
                lastPage = meta?.lastPage ?: 0,
                readPercent = meta?.readPercent ?: 0f,
                lastReadTimestamp = meta?.lastReadTimestamp ?: 0L
            )
        }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        // Automatically scan folders on startup when rootFolderUris changes
        repositoryScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                scanConfiguredFolders(settings.rootFolderUris)
            }
        }
    }

    override suspend fun rescan() {
        val settings = settingsRepository.settingsFlow.first()
        scanConfiguredFolders(settings.rootFolderUris)
    }

    private suspend fun scanConfiguredFolders(uris: Set<String>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            _scannedAlbums.value = emptyList()
            return@withContext
        }

        _isScanning.value = true
        val allResults = mutableListOf<ScannedAlbumResult>()

        for (uriString in uris) {
            try {
                val uri = Uri.parse(uriString)
                val results = scanner.scanRootFolder(uri)
                allResults.addAll(results)
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning root folder $uriString", e)
            }
        }

        _scannedAlbums.value = allResults
        _isScanning.value = false
    }

    override suspend fun addRootFolder(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take persistable URI permission for $treeUri", e)
        }
        settingsRepository.addRootFolderUri(treeUri.toString())
        rescan()
    }

    override suspend fun removeRootFolder(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(treeUri, flags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release persistable URI permission for $treeUri", e)
        }
        settingsRepository.removeRootFolderUri(treeUri.toString())
        rescan()
    }

    override suspend fun setPinned(folderUri: String, isPinned: Boolean) {
        ensureMetadataExists(folderUri)
        albumDao.setPinned(folderUri, isPinned)
    }

    override suspend fun setHidden(folderUri: String, isHidden: Boolean) {
        ensureMetadataExists(folderUri)
        albumDao.setHidden(folderUri, isHidden)
    }

    override suspend fun setCustomCover(folderUri: String, coverUri: String?) {
        ensureMetadataExists(folderUri)
        albumDao.setCustomCover(folderUri, coverUri)
    }

    override suspend fun setTags(folderUri: String, tags: List<String>) {
        ensureMetadataExists(folderUri)
        albumDao.setTags(folderUri, tags)
    }

    override suspend fun updateReadingProgress(folderUri: String, lastPage: Int, readPercent: Float) {
        ensureMetadataExists(folderUri)
        albumDao.updateReadingProgress(folderUri, lastPage, readPercent)
    }

    private suspend fun ensureMetadataExists(folderUri: String) {
        if (albumDao.getMetadata(folderUri) == null) {
            albumDao.upsertMetadata(AlbumMetadataEntity(folderUri = folderUri))
        }
    }

    override suspend fun toggleFavorite(imageUri: String, albumUri: String, imageName: String) {
        if (favoriteDao.isFavorite(imageUri)) {
            favoriteDao.deleteFavorite(imageUri)
        } else {
            favoriteDao.insertFavorite(
                FavoriteImageEntity(
                    imageUri = imageUri,
                    albumUri = albumUri,
                    imageName = imageName
                )
            )
        }
    }

    override fun isFavoriteFlow(imageUri: String): Flow<Boolean> = favoriteDao.isFavoriteFlow(imageUri)

    override suspend fun isFavorite(imageUri: String): Boolean = favoriteDao.isFavorite(imageUri)

    override suspend fun getImagesForAlbum(folderUri: String): List<ImageItem> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(folderUri)
        val docId = DocumentsContract.getDocumentId(uri)
        scanner.getImagesInAlbum(uri, docId)
    }

    override suspend fun renameAlbum(folderUri: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldUri = Uri.parse(folderUri)
            val resultUri = DocumentsContract.renameDocument(context.contentResolver, oldUri, newName)
                ?: return@withContext false

            // Rebuild the new folder URI in the same tree-based form the scanner uses
            val authority = oldUri.authority ?: return@withContext false
            val treeUri = DocumentsContract.buildTreeDocumentUri(
                authority,
                DocumentsContract.getTreeDocumentId(oldUri)
            )
            val newFolderUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getDocumentId(resultUri)
            ).toString()

            if (newFolderUri != folderUri) {
                migrateUriPrefix(folderUri, newFolderUri)
            }
            rescan()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename album $folderUri to $newName", e)
            false
        }
    }

    /** Returns the remapped URI if [value] is the folder itself or inside it, else null. */
    private fun remapUri(value: String, oldPrefix: String, newPrefix: String): String? {
        if (value == oldPrefix) return newPrefix
        if (value.startsWith("$oldPrefix%2F")) return newPrefix + value.removePrefix(oldPrefix)
        return null
    }

    /** After a folder rename, point saved metadata, covers and favorites at the new URIs. */
    private suspend fun migrateUriPrefix(oldPrefix: String, newPrefix: String) {
        for (meta in albumDao.getAllMetadata()) {
            val newFolder = remapUri(meta.folderUri, oldPrefix, newPrefix) ?: continue
            val newCover = meta.customCoverUri?.let { remapUri(it, oldPrefix, newPrefix) ?: it }
            albumDao.deleteMetadata(meta.folderUri)
            albumDao.upsertMetadata(meta.copy(folderUri = newFolder, customCoverUri = newCover))
        }
        for (fav in favoriteDao.getAllFavorites()) {
            val newImage = remapUri(fav.imageUri, oldPrefix, newPrefix)
            val newAlbum = remapUri(fav.albumUri, oldPrefix, newPrefix)
            if (newImage == null && newAlbum == null) continue
            favoriteDao.deleteFavorite(fav.imageUri)
            favoriteDao.insertFavorite(
                fav.copy(
                    imageUri = newImage ?: fav.imageUri,
                    albumUri = newAlbum ?: fav.albumUri
                )
            )
        }
    }
    
    override suspend fun deleteAlbumFromDevice(folderUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(folderUri)
            val deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            albumDao.deleteMetadata(folderUri)
            favoriteDao.deleteFavoritesForAlbum(folderUri)
            rescan()
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete album $folderUri from device", e)
            false
        }
    }

       override suspend fun removeAlbumFromLibrary(folderUri: String) {
        ensureMetadataExists(folderUri)
        albumDao.setRemoved(folderUri, true)
    }
}
