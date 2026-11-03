package com.khayrrr.mangaviewer.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.khayrrr.mangaviewer.data.model.ImageItem
import com.khayrrr.mangaviewer.util.NaturalOrderComparator
import com.khayrrr.mangaviewer.util.PdfPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafScanner(
    private val context: Context
) {
    companion object {
        private const val TAG = "SafScanner"
        private const val PDF_MIME = "application/pdf"

        private val SUPPORTED_MIME_TYPES = setOf(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif",
            "image/avif",
            "image/heic",
            "image/heif"
        )

        private val SUPPORTED_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "webp", "gif", "avif", "heic", "heif"
        )

        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }

    private val contentResolver: ContentResolver get() = context.contentResolver

    /**
     * Recursively scans a root folder tree URI for all albums
     * (folders directly containing >= 1 image, and every PDF file).
     */
    suspend fun scanRootFolder(treeUri: Uri): List<ScannedAlbumResult> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val albums = mutableListOf<ScannedAlbumResult>()

        val rootName = getDocumentDisplayName(treeUri, rootDocId) ?: "Root"

        traverseFolder(
            treeUri = treeUri,
            folderDocId = rootDocId,
            folderName = rootName,
            currentPath = rootName,
            results = albums
        )

        albums
    }

    private fun traverseFolder(
        treeUri: Uri,
        folderDocId: String,
        folderName: String,
        currentPath: String,
        results: MutableList<ScannedAlbumResult>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)
        val directImages = mutableListOf<ImageItem>()
        val subFolders = mutableListOf<SubFolderInfo>()
        val pdfFiles = mutableListOf<PdfInfo>()
        var folderLastModified = 0L

        try {
            contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = if (idIndex != -1) cursor.getString(idIndex) else continue
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val mimeType = if (mimeIndex != -1) cursor.getString(mimeIndex) ?: "" else ""
                    val lastMod = if (modifiedIndex != -1) cursor.getLong(modifiedIndex) else 0L
                    val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        subFolders.add(SubFolderInfo(docId, name))
                    } else if (isPdf(name, mimeType)) {
                        pdfFiles.add(PdfInfo(docId, name, size, lastMod))
                    } else if (isSupportedImage(name, mimeType)) {
                        val imageUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                        directImages.add(
                            ImageItem(
                                uri = imageUri,
                                name = name,
                                size = size,
                                lastModified = lastMod,
                                mimeType = mimeType
                            )
                        )
                        if (lastMod > folderLastModified) {
                            folderLastModified = lastMod
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying child documents for $folderDocId", e)
            return
        }

        // If this folder directly contains images, it is an ALBUM!
        if (directImages.isNotEmpty()) {
            directImages.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
            val firstImageUri = directImages.first().uri
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocId).toString()
            val totalSize = directImages.sumOf { it.size }

            results.add(
                ScannedAlbumResult(
                    folderUri = folderUri,
                    folderDocId = folderDocId,
                    treeUri = treeUri.toString(),
                    name = folderName,
                    path = currentPath,
                    totalImages = directImages.size,
                    totalSize = totalSize,
                    defaultCoverUri = firstImageUri,
                    lastModified = folderLastModified
                )
            )
        }

        // Every PDF file is its own album
        for (pdf in pdfFiles) {
            val pdfUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, pdf.docId).toString()
            val pages = PdfPages.pageCount(context, pdfUri)
            if (pages <= 0) continue // damaged or password-protected

            val baseName = pdf.name.substringBeforeLast('.', pdf.name)
            results.add(
                ScannedAlbumResult(
                    folderUri = pdfUri,
                    folderDocId = pdf.docId,
                    treeUri = treeUri.toString(),
                    name = baseName,
                    path = "$currentPath/$baseName",
                    totalImages = pages,
                    totalSize = pdf.size,
                    defaultCoverUri = PdfPages.pageUri(pdfUri, 0),
                    lastModified = pdf.lastModified
                )
            )
        }

        // Recursively traverse nested subfolders
        for (sub in subFolders) {
            val nextPath = "$currentPath/${sub.name}"
            traverseFolder(
                treeUri = treeUri,
                folderDocId = sub.docId,
                folderName = sub.name,
                currentPath = nextPath,
                results = results
            )
        }
    }

    /**
     * Loads and naturally sorts all images directly inside an album folder.
     * If the album is a PDF file, returns one entry per page instead.
     */
    suspend fun getImagesInAlbum(treeUri: Uri, albumDocId: String): List<ImageItem> = withContext(Dispatchers.IO) {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, albumDocId)
        val pdfInfo = queryPdfInfo(docUri)
        if (pdfInfo != null) {
            return@withContext pdfPagesAsImages(docUri.toString(), pdfInfo)
        }

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, albumDocId)
        val images = mutableListOf<ImageItem>()

        try {
            contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = if (idIndex != -1) cursor.getString(idIndex) else continue
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val mimeType = if (mimeIndex != -1) cursor.getString(mimeIndex) ?: "" else ""
                    val lastMod = if (modifiedIndex != -1) cursor.getLong(modifiedIndex) else 0L
                    val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L

                    if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR && isSupportedImage(name, mimeType)) {
                        val imageUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                        images.add(
                            ImageItem(
                                uri = imageUri,
                                name = name,
                                size = size,
                                lastModified = lastMod,
                                mimeType = mimeType
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying images in album $albumDocId", e)
        }

        images.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
        images
    }

    private fun pdfPagesAsImages(pdfUri: String, info: PdfInfo): List<ImageItem> {
        val pages = PdfPages.pageCount(context, pdfUri)
        val baseName = info.name.substringBeforeLast('.', info.name)
        return (0 until pages).map { index ->
            ImageItem(
                uri = PdfPages.pageUri(pdfUri, index),
                name = "$baseName - page ${index + 1}",
                size = 0L,
                lastModified = info.lastModified,
                mimeType = "image/png"
            )
        }
    }

    /** Returns file info if the document is a PDF, otherwise null. */
    private fun queryPdfInfo(docUri: Uri): PdfInfo? {
        return try {
            contentResolver.query(docUri, PROJECTION, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)) ?: return@use null
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)) ?: ""
                if (!isPdf(name, mime)) return@use null
                PdfInfo(
                    docId = DocumentsContract.getDocumentId(docUri),
                    name = name,
                    size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)),
                    lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDocumentDisplayName(treeUri: Uri, docId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return try {
            contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isSupportedImage(fileName: String, mimeType: String): Boolean {
        if (SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())) {
            return true
        }
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    private fun isPdf(fileName: String, mimeType: String): Boolean {
        return mimeType.lowercase() == PDF_MIME || fileName.endsWith(".pdf", ignoreCase = true)
    }

    private data class SubFolderInfo(val docId: String, val name: String)

    private data class PdfInfo(
        val docId: String,
        val name: String,
        val size: Long,
        val lastModified: Long
    )
}

data class ScannedAlbumResult(
    val folderUri: String,
    val folderDocId: String,
    val treeUri: String,
    val name: String,
    val path: String,
    val totalImages: Int,
    val totalSize: Long,
    val defaultCoverUri: String,
    val lastModified: Long
)