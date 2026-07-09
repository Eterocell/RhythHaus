package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
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
    single {
        PlaybackController(
            artworkLoader = { trackId -> get<LibraryRepository>().artworkForTrack(trackId)?.bytes },
        )
    }
    single<ThemePreferenceStore> { createThemePreferenceStore() }
    single {
        val platformAccess = get<PlatformSourceAccess>()
        LibraryScanner(
            repository = get(),
            platformScanner = platformAccess,
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
