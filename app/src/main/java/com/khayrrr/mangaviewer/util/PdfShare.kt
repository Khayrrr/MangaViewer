package com.khayrrr.mangaviewer.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

object PdfShare {
    fun sharePdfs(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            ShareHelper.shareSingleImage(context, uris.first(), "application/pdf", "Share PDF")
            return
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            clipData = ClipData.newRawUri("PDFs", uris.first()).also { data ->
                uris.drop(1).forEach { data.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share PDFs").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}