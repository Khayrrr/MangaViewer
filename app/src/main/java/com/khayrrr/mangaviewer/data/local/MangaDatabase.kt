package com.khayrrr.mangaviewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.khayrrr.mangaviewer.data.local.dao.AlbumDao
import com.khayrrr.mangaviewer.data.local.dao.FavoriteDao
import com.khayrrr.mangaviewer.data.local.entity.AlbumMetadataEntity
import com.khayrrr.mangaviewer.data.local.entity.FavoriteImageEntity

@Database(
    entities = [
        AlbumMetadataEntity::class,
        FavoriteImageEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MangaDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun favoriteDao(): FavoriteDao
}
