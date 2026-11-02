package com.khayrrr.mangaviewer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.khayrrr.mangaviewer.ui.navigation.MangaNavGraph
import com.khayrrr.mangaviewer.ui.settings.SettingsViewModel
import com.khayrrr.mangaviewer.ui.theme.MangaViewerTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsState()

            MangaViewerTheme(
                themeMode = settings.themeMode,
                useDynamicColor = settings.useDynamicColor,
                customSeedColor = settings.customSeedColor
            ) {
                val navController = rememberNavController()
                MangaNavGraph(navController = navController)
            }
        }
    }
}