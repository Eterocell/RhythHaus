package com.eterocell.rhythhaus.library

import org.koin.core.module.Module
import org.koin.dsl.module

internal fun playlistsImplementationModule(): Module = module {
    single<PlaylistRepository> { SqlDelightPlaylistRepository(get()) }
}
