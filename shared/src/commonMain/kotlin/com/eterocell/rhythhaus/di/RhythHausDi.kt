package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformAudioScanner
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.createLibraryDatabase
import com.eterocell.rhythhaus.library.createPlatformSourceAccess
import com.eterocell.rhythhaus.library.currentTimeMillis
import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.createTagLibReader
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.theme.createThemePreferenceStore
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun rhythHausModule(): Module = module {
    single<TagLibReader> { createTagLibReader() }
    single { AudioMetadataReader(tagLibReader = get()) }
    single<LibraryDatabase> { createLibraryDatabase() }
    single<LibraryRepository> { SqlDelightLibraryRepository(get()) }
    single<PlatformSourceAccess> { createPlatformSourceAccess() }
    single<PlatformAudioScanner> { get<PlatformSourceAccess>() as PlatformAudioScanner }
    single { PlaybackController() }
    single<ThemePreferenceStore> { createThemePreferenceStore() }
    single {
        LibraryScanner(
            repository = get(),
            platformScanner = get(),
            metadataReader = get(),
            now = { currentTimeMillis() },
            idFactory = { _ -> uuid4() },
        )
    }
}

fun startRhythHausKoin() {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(rhythHausModule())
    }
}
