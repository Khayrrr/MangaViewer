package com.khayrrr.mangaviewer.ui.navigation

import com.khayrrr.mangaviewer.ui.about.AboutScreen
import com.khayrrr.mangaviewer.ui.removed.RemovedAlbumsScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.khayrrr.mangaviewer.ui.favorites.FavoritesScreen
import com.khayrrr.mangaviewer.ui.hidden.HiddenAlbumsScreen
import com.khayrrr.mangaviewer.ui.home.HomeScreen
import com.khayrrr.mangaviewer.ui.reader.ReaderScreen
import com.khayrrr.mangaviewer.ui.settings.SettingsScreen
import com.khayrrr.mangaviewer.ui.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MangaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToHidden = { navController.navigate(Screen.HiddenAlbums.route) },
                onNavigateToRemoved = { navController.navigate(Screen.RemovedAlbums.route) },
                onOpenReader = { albumUri ->
                    navController.navigate(Screen.Reader.createRoute(albumUri))
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("albumUri") {
                    type = NavType.StringType
                },
                navArgument("initialPage") {
                    type = NavType.IntType
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val albumUri = backStackEntry.arguments?.getString("albumUri").orEmpty()
            val initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0
            ReaderScreen(
                albumUri = albumUri,
                initialPage = initialPage,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HiddenAlbums.route) {
            HiddenAlbumsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenReader = { albumUri ->
                    navController.navigate(Screen.Reader.createRoute(albumUri))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenFavorite = { albumUri, page ->
                    navController.navigate(Screen.Reader.createRoute(albumUri, page))
                }
            )
        }

                composable(Screen.RemovedAlbums.route) {
            RemovedAlbumsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
                composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
