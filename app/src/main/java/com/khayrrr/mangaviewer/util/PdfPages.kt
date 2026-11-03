package com.khayrrr.mangaviewer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

/** Helpers for reading PDF pages. Pages are addressed as mvpdf://page?p=<index>&src=<pdf uri>. */
object PdfPages {
    const val SCHEME = "mvpdf"

    private class OpenPdf(
        val src: String,
        val pfd: ParcelFileDescriptor,
        val renderer: PdfRenderer
    )

    // PdfRenderer allows only one open page at a time, so access is serialized
    private val lock = Any()
    private var open: OpenPdf? = null

    fun pageUri(src: String, page: Int): String = "$SCHEME://page?p=$page&src=${Uri.encode(src)}"

    fun isPdfPage(uri: String): Boolean = uri.startsWith("$SCHEME://")

    /** Returns (pdf uri, page index) or null if [uri] is not a PDF page. */
    fun parse(uri: String): Pair<String, Int>? {
        if (!isPdfPage(uri)) return null
        val parsed = Uri.parse(uri)
        val src = parsed.getQueryParameter("src") ?: return null
        val page = parsed.getQueryParameter("p")?.toIntOrNull() ?: return null
        return src to page
    }

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

    fun pageAspect(context: Context, src: String, page: Int): Float? = synchronized(lock) {
        try {
            val current = openPdf(context, src) ?: return null
            if (page !in 0 until current.renderer.pageCount) return null
            val p = current.renderer.openPage(page)
            try {
                if (p.width > 0 && p.height > 0) p.width.toFloat() / p.height else null
            } finally {
                p.close()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Renders one page to a white-background bitmap of the given width. */
    fun render(context: Context, src: String, page: Int, targetWidth: Int): Bitmap? =
        synchronized(lock) {
            try {
                val current = openPdf(context, src) ?: return null
                if (page !in 0 until current.renderer.pageCount) return null
                val p = current.renderer.openPage(page)
                try {
                    if (p.width <= 0 || p.height <= 0) return null
                    val height = (targetWidth.toLong() * p.height / p.width)
                        .toInt().coerceIn(1, 8000)
                    val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } finally {
                    p.close()
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun openPdf(context: Context, src: String): OpenPdf? {
        open?.let { if (it.src == src) return it }
        open?.let {
            try { it.renderer.close() } catch (_: Exception) {}
            try { it.pfd.close() } catch (_: Exception) {}
        }
        open = null
        return try {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(src), "r") ?: return null
            val renderer = try {
                PdfRenderer(pfd)
            } catch (e: Exception) {
                pfd.close()
                throw e
            }
            OpenPdf(src, pfd, renderer).also { open = it }
        } catch (e: Exception) {
            null
        }
    }
}