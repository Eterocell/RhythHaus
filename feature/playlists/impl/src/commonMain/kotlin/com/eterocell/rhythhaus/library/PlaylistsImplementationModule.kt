package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Provides Shared's playlist binding.
 *
 * The returned module binds only [PlaylistRepository] to
 * [SqlDelightPlaylistRepository] and creates [PlaylistStateOwner] as a
 * singleton. It neither constructs a launcher nor a backup controller and
 * exposes no database types.
 */
public fun playlistsImplementationModule(): Module = module {
    single<PlaylistRepository> { SqlDelightPlaylistRepository(get()) }
    single { PlaylistStateOwner(get(), Dispatchers.Default) }
}
