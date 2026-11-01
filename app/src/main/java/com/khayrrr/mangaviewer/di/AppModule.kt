package com.khayrrr.mangaviewer.di

import androidx.room.Room
import com.khayrrr.mangaviewer.data.local.MangaDatabase
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.preferences.SettingsRepositoryImpl
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import com.khayrrr.mangaviewer.data.repository.MangaRepositoryImpl
import com.khayrrr.mangaviewer.data.scanner.SafScanner
import com.khayrrr.mangaviewer.ui.home.HomeViewModel
import com.khayrrr.mangaviewer.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            MangaDatabase::class.java,
            "manga_viewer.db"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<MangaDatabase>().albumDao() }
    single { get<MangaDatabase>().favoriteDao() }

    // Settings
    single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }

    // SAF Scanner
    single { SafScanner(androidContext()) }

    // Repository
    single<MangaRepository> {
        MangaRepositoryImpl(
            context = androidContext(),
            albumDao = get(),
            favoriteDao = get(),
            settingsRepository = get(),
            scanner = get()
        )
    }

    // ViewModels
    viewModel { SettingsViewModel(settingsRepository = get()) }
    viewModel { HomeViewModel(mangaRepository = get(), settingsRepository = get()) }
}
