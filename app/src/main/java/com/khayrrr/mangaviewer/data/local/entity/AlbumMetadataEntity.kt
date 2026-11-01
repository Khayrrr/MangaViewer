package com.khayrrr.mangaviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_metadata")
data class AlbumMetadataEntity(
    @PrimaryKey
    val folderUri: String,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customCoverUri: String? = null,
    val tags: List<String> = emptyList(),
    val lastPage: Int = 0,
    val readPercent: Float = 0f,
    val lastReadTimestamp: Long = 0L
)
