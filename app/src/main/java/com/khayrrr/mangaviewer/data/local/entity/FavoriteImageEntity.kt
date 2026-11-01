package com.khayrrr.mangaviewer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_images")
data class FavoriteImageEntity(
    @PrimaryKey
    val imageUri: String,
    val albumUri: String,
    val imageName: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
