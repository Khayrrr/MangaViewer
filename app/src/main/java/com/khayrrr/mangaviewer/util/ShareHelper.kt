package com.khayrrr.mangaviewer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.khayrrr.mangaviewer.data.model.Album
import com.khayrrr.mangaviewer.data.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ShareHelper {
    private const val TAG = "ShareHelper"

    fun shareImages(context: Context, imageUris: List<Uri>, title: String = "Share Images") {
        if (imageUris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
    
    fun shareSingleImage(context: Context, imageUri: Uri, mimeType: String, title: String = "Share image") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "image/*" }
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    suspend fun shareAlbumsAsZip(
        context: Context,
        albumsWithImages: Map<Album, List<ImageItem>>,
        zipBaseName: String = "manga_export"
    ) = withContext(Dispatchers.IO) {
        val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
                shareDir.listFiles()?.forEach { it.delete() }
        val cleanName = zipBaseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val zipFile = File(shareDir, "$cleanName.zip")

        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val buffer = ByteArray(8192)

                albumsWithImages.forEach { (album, images) ->
                    val folderPrefix = if (albumsWithImages.size > 1) {
                        "${album.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")}/"
                    } else ""

                    images.forEach { image ->
                        val entryName = "$folderPrefix${image.name}"
                        zos.putNextEntry(ZipEntry(entryName))

                        try {
                            context.contentResolver.openInputStream(Uri.parse(image.uri))?.use { input ->
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    zos.write(buffer, 0, bytesRead)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to read image ${image.name} for ZIP", e)
                        } finally {
                            zos.closeEntry()
                        }
                    }
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, zipFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share as ZIP").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ZIP for sharing", e)
        }
    }
}
