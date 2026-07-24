package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.PlatformPlaybackEngine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackProcessLifecycle
import com.eterocell.rhythhaus.createPlatformPlaybackEngine
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.SqlDelightPlaylistRepository
import com.eterocell.rhythhaus.library.createLibraryDatabase
import com.eterocell.rhythhaus.library.createPlatformSourceAccess
import com.eterocell.rhythhaus.library.currentTimeMillis
import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.session.PlaybackSessionController
import com.eterocell.rhythhaus.session.PlaybackSessionCoordinator
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import com.eterocell.rhythhaus.session.PlaybackSessionStore
import com.eterocell.rhythhaus.session.createPlaybackSessionStore
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.createTagLibReader
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.theme.createThemePreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun rhythHausModule(): Module = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<TagLibReader> { createTagLibReader() }
    single { AudioMetadataReader(tagLibReader = get()) }
    single<LibraryDatabase> { createLibraryDatabase() }
    single<LibraryRepository> { SqlDelightLibraryRepository(get()) }
    single<PlaylistRepository> { SqlDelightPlaylistRepository(get()) }
    single<PlatformSourceAccess> { createPlatformSourceAccess() }
    single<PlatformPlaybackEngine> { createPlatformPlaybackEngine() }
    single {
        PlaybackController(
            engine = get(),
            artworkLoader = { trackId -> get<LibraryRepository>().artworkForTrack(trackId)?.bytes },
        )
    }
    single<PlaybackSessionController> { get<PlaybackController>() }
    single<PlaybackSessionStore> { createPlaybackSessionStore() }
    single {
        PlaybackSessionCoordinator(
            controller = get(),
            store = get(),
            processScope = get(),
        )
    }
    single<PlaybackSessionReconciler> { get<PlaybackSessionCoordinator>() }
    single { PlaybackProcessLifecycle(coordinator = get(), processScope = get()) }
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
