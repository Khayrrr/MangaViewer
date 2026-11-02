package com.khayrrr.mangaviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khayrrr.mangaviewer.data.local.entity.FavoriteImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_images ORDER BY addedTimestamp DESC")
    fun observeAllFavorites(): Flow<List<FavoriteImageEntity>>

    @Query("SELECT * FROM favorite_images")
    suspend fun getAllFavorites(): List<FavoriteImageEntity>

    @Query("SELECT * FROM favorite_images WHERE albumUri = :albumUri")
    fun observeFavoritesForAlbum(albumUri: String): Flow<List<FavoriteImageEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_images WHERE imageUri = :imageUri LIMIT 1)")
    fun isFavoriteFlow(imageUri: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_images WHERE imageUri = :imageUri LIMIT 1)")
    suspend fun isFavorite(imageUri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteImageEntity)

    @Query("DELETE FROM favorite_images WHERE imageUri = :imageUri")
    suspend fun deleteFavorite(imageUri: String)

    @Query("DELETE FROM favorite_images WHERE albumUri = :albumUri")
    suspend fun deleteFavoritesForAlbum(albumUri: String)
}
