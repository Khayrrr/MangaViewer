package com.khayrrr.mangaviewer.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Reader : Screen("reader/{albumUri}?initialPage={initialPage}") {
        /** initialPage = -1 means "resume where I left off". */
        fun createRoute(albumUri: String, initialPage: Int = -1): String {
            val encodedUri = Uri.encode(albumUri)
            return "reader/$encodedUri?initialPage=$initialPage"
        }
    }
    data object Settings : Screen("settings")
    data object HiddenAlbums : Screen("hidden_albums")
    data object Favorites : Screen("favorites")
    data object RemovedAlbums : Screen("removed_albums")
    data object About : Screen("about")
}