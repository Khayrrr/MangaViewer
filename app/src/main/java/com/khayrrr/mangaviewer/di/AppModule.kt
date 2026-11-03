package com.khayrrr.mangaviewer.di

import com.khayrrr.mangaviewer.ui.reader.ReaderViewModel
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import com.khayrrr.mangaviewer.data.local.MangaDatabase
import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.preferences.SettingsRepositoryImpl
import com.khayrrr.mangaviewer.data.repository.MangaRepository
import com.khayrrr.mangaviewer.data.repository.MangaRepositoryImpl
import com.khayrrr.mangaviewer.data.scanner.SafScanner
import com.khayrrr.mangaviewer.ui.favorites.FavoritesViewModel
import com.khayrrr.mangaviewer.ui.hidden.HiddenAlbumsViewModel
import com.khayrrr.mangaviewer.ui.home.HomeViewModel
import com.khayrrr.mangaviewer.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.khayrrr.mangaviewer.ui.removed.RemovedAlbumsViewModel

val appModule = module {
    // Room Database
        single {
        Room.databaseBuilder(
            androidContext(),
            MangaDatabase::class.java,
            "manga_viewer.db"
        ).addMigrations(object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE album_metadata ADD COLUMN isRemoved INTEGER NOT NULL DEFAULT 0")
            }
        }).build()
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
    viewModel { HiddenAlbumsViewModel(mangaRepository = get(), settingsRepository = get()) }
    viewModel { FavoritesViewModel(mangaRepository = get()) }
    viewModel { ReaderViewModel(context = androidContext(), mangaRepository = get(), settingsRepository = get()) }
    viewModel { RemovedAlbumsViewModel(mangaRepository = get()) }
}
