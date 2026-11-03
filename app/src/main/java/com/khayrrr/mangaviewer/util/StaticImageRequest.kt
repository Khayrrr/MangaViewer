package com.khayrrr.mangaviewer.util

import android.content.Context
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest

/** Builds a request that decodes GIF/AVIF as a still image (first frame). */
fun staticImageRequest(context: Context, uri: String): ImageRequest {
    return ImageRequest.Builder(context)
        .data(uri)
        // Separate cache entry so still frames never get served to animated requests
        .memoryCacheKey("static:$uri")
        .decoderFactory(BitmapFactoryDecoder.Factory())
        .build()
}