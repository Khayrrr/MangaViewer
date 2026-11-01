package com.khayrrr.mangaviewer.util

import android.text.format.DateUtils

object TimeUtils {
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
            diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m ago"
            diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}h ago"
            diff < 2 * DateUtils.DAY_IN_MILLIS -> "Yesterday"
            diff < 7 * DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.DAY_IN_MILLIS} days ago"
            else -> DateUtils.formatDateTime(
                null,
                timestamp,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
            )
        }
    }
}
