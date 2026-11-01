package com.khayrrr.mangaviewer.di

import com.khayrrr.mangaviewer.data.preferences.SettingsRepository
import com.khayrrr.mangaviewer.data.preferences.SettingsRepositoryImpl
import com.khayrrr.mangaviewer.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    viewModel { SettingsViewModel(get()) }
}
