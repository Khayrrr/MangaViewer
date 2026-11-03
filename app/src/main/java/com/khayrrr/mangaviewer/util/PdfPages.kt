package com.khayrrr.mangaviewer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantLock

/**
 * Helpers for reading PDF pages. Pages are addressed as
 * mvpdf://page?p=<index>&src=<pdf uri>[&t=1&v=<version>]
 * where t=1 marks a small cached thumbnail (used for covers).
 */
object PdfPages {
    const val SCHEME = "mvpdf"
    private const val READER_WIDTH = 1400
    private const val THUMB_WIDTH = 480

    private class OpenPdf(
        val src: String,
        val pfd: ParcelFileDescriptor,
        val renderer: PdfRenderer
    )

    // PdfRenderer allows only one open page at a time, so each slot is serialized.
    // Thumbnails and reader pages use separate slots so covers never block the reader.
    private class Slot {
        val lock = ReentrantLock()
        var open: OpenPdf? = null
    }

    private val readerSlot = Slot()
    private val thumbSlot = Slot()

    class PageRef(val src: String, val page: Int, val thumb: Boolean, val version: Long)

    fun pageUri(src: String, page: Int): String =
        "$SCHEME://page?p=$page&src=${Uri.encode(src)}"

    fun thumbnailUri(src: String, page: Int, version: Long): String =
        "$SCHEME://page?p=$page&t=1&v=$version&src=${Uri.encode(src)}"

    fun isPdfPage(uri: String): Boolean = uri.startsWith("$SCHEME://")

    fun parseRef(uri: String): PageRef? {
        if (!isPdfPage(uri)) return null
        val parsed = Uri.parse(uri)
        val src = parsed.getQueryParameter("src") ?: return null
        val page = parsed.getQueryParameter("p")?.toIntOrNull() ?: return null
        val thumb = parsed.getQueryParameter("t") == "1"
        val version = parsed.getQueryParameter("v")?.toLongOrNull() ?: 0L
        return PageRef(src, page, thumb, version)
    }

    /** Returns (pdf uri, page index) or null if [uri] is not a PDF page. */
    fun parse(uri: String): Pair<String, Int>? = parseRef(uri)?.let { it.src to it.page }

    /** Number of pages, or 0 if the file can't be opened (damaged or password-protected). */
    fun pageCount(context: Context, src: String): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(Uri.parse(src), "r") ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun pageAspect(context: Context, src: String, page: Int): Float? {
        val slot = readerSlot
        return try {
            slot.lock.lockInterruptibly()
            try {
                val current = openPdf(slot, context, src) ?: return null
                if (page !in 0 until current.renderer.pageCount) return null
                val p = current.renderer.openPage(page)
                try {
                    if (p.width > 0 && p.height > 0) p.width.toFloat() / p.height else null
                } finally {
                    p.close()
                }
            } finally {
                slot.lock.unlock()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Renders one page to a white-background bitmap. Thumbnails are cached on disk. */
    fun render(context: Context, ref: PageRef): Bitmap? {
        val width = if (ref.thumb) THUMB_WIDTH else READER_WIDTH
        val cacheFile = if (ref.thumb) thumbFile(context, ref) else null

        if (cacheFile != null && cacheFile.exists()) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { return it }
        }

        val slot = if (ref.thumb) thumbSlot else readerSlot
        val bitmap = try {
            slot.lock.lockInterruptibly() // cancelled requests stop waiting here
            try {
                val current = openPdf(slot, context, ref.src) ?: return null
                if (ref.page !in 0 until current.renderer.pageCount) return null
                val p = current.renderer.openPage(ref.page)
                try {
                    if (p.width <= 0 || p.height <= 0) return null
                    val height = (width.toLong() * p.height / p.width).toInt().coerceIn(1, 8000)
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                } finally {
                    p.close()
                }
            } finally {
                slot.lock.unlock()
            }
        } catch (e: InterruptedException) {
            throw e
        } catch (e: Exception) {
            return null
        }

        if (cacheFile != null) saveThumb(cacheFile, bitmap)
        return bitmap
    }

    private fun thumbFile(context: Context, ref: PageRef): File {
        val dir = File(context.cacheDir, "pdf_thumbs").apply { mkdirs() }
        val key = "${ref.src}|${ref.page}|${ref.version}"
        val hash = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.jpg")
    }

    private fun saveThumb(file: File, bitmap: Bitmap) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (!tmp.renameTo(file)) tmp.delete()
        } catch (_: Exception) {
        }
    }

    private fun openPdf(slot: Slot, context: Context, src: String): OpenPdf? {
        slot.open?.let { if (it.src == src) return it }
        slot.open?.let {
            try { it.renderer.close() } catch (_: Exception) {}
            try { it.pfd.close() } catch (_: Exception) {}
        }
        slot.open = null
        return try {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(src), "r") ?: return null
            val renderer = try {
                PdfRenderer(pfd)
            } catch (e: Exception) {
                pfd.close()
                throw e
            }
            OpenPdf(src, pfd, renderer).also { slot.open = it }
        } catch (e: Exception) {
            null
        }
    }
}