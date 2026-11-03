package com.khayrrr.mangaviewer.util

import android.content.Context
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lets Coil load "mvpdf://" page URIs by rendering the PDF page. */
class PdfPageFetcher(
    private val context: Context,
    private val data: String
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val (src, page) = PdfPages.parse(data) ?: return null
        val bitmap = withContext(Dispatchers.IO) {
            PdfPages.render(context, src, page, RENDER_WIDTH)
        } ?: return null
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(context: Context) : Fetcher.Factory<Uri> {
        private val appContext = context.applicationContext

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.scheme == PdfPages.SCHEME) {
                PdfPageFetcher(appContext, data.toString())
            } else {
                null
            }
        }
    }

    private companion object {
        const val RENDER_WIDTH = 1400
    }
}