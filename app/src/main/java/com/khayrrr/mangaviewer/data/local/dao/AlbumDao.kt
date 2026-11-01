package com.khayrrr.mangaviewer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.khayrrr.mangaviewer.data.local.entity.AlbumMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM album_metadata")
    fun observeAllMetadata(): Flow<List<AlbumMetadataEntity>>

    @Query("SELECT * FROM album_metadata WHERE folderUri = :folderUri LIMIT 1")
    suspend fun getMetadata(folderUri: String): AlbumMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: AlbumMetadataEntity)

    @Query("UPDATE album_metadata SET isPinned = :isPinned WHERE folderUri = :folderUri")
    suspend fun setPinned(folderUri: String, isPinned: Boolean)

    @Query("UPDATE album_metadata SET isHidden = :isHidden WHERE folderUri = :folderUri")
    suspend fun setHidden(folderUri: String, isHidden: Boolean)

    @Query("UPDATE album_metadata SET customCoverUri = :coverUri WHERE folderUri = :folderUri")
    suspend fun setCustomCover(folderUri: String, coverUri: String?)

    @Query("UPDATE album_metadata SET tags = :tags WHERE folderUri = :folderUri")
    suspend fun setTags(folderUri: String, tags: List<String>)

    @Query("""
        UPDATE album_metadata 
        SET lastPage = :lastPage, readPercent = :readPercent, lastReadTimestamp = :timestamp 
        WHERE folderUri = :folderUri
    """)
    suspend fun updateReadingProgress(
        folderUri: String,
        lastPage: Int,
        readPercent: Float,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM album_metadata WHERE folderUri = :folderUri")
    suspend fun deleteMetadata(folderUri: String)
}
