package com.khayrrr.mangaviewer.data.model

data class ImageItem(
    val uri: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String
)

data class Album(
    val uri: String,
    val name: String,
    val path: String,
    val totalImages: Int,
    val totalSize: Long,
    val coverUri: String,
    val lastModified: Long,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isRemoved: Boolean = false,
    val tags: List<String> = emptyList(),
    val lastPage: Int = 0,
    val readPercent: Float = 0f,
    val lastReadTimestamp: Long = 0L
)