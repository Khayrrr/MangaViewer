package com.khayrrr.mangaviewer.util

import android.net.Uri
import android.provider.DocumentsContract
import com.khayrrr.mangaviewer.data.model.Album

fun isPdfAlbumUri(albumUri: String): Boolean {
    return try {
        DocumentsContract.getDocumentId(Uri.parse(albumUri)).endsWith(".pdf", ignoreCase = true)
    } catch (e: Exception) {
        false
    }
}

val Album.isPdf: Boolean get() = isPdfAlbumUri(uri)

/** For PDF pages shown in grids: the small cached thumbnail instead of the full-size render. */
fun pdfThumbOrSelf(imageUri: String): String {
    val ref = PdfPages.parseRef(imageUri) ?: return imageUri
    return PdfPages.thumbnailUri(ref.src, ref.page, ref.version)
}