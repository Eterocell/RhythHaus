package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.playlistsImplementationModule
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class RhythHausDiFactoryJvmTest {
    @Test
    fun playlistFactoryOwnsPlaylistRepositoryWithoutLoadingLibraryFactory() {
        stopKoin()
        val database =
            LibraryDatabase(
                Files.createTempFile("rhythhaus-di", ".db").toFile())
        val application = startKoin {
            modules(
                module { single<LibraryDatabase> { database } },
                playlistsImplementationModule(),
            )
        }

        try {
            val koin = application.koin
            assertTrue(koin.get<PlaylistRepository>().playlists().isEmpty())
            assertSame(
                koin.get<PlaylistRepository>(), koin.get<PlaylistRepository>())
            assertSame(
                koin.get<PlaylistStateOwner>(), koin.get<PlaylistStateOwner>())
            assertEquals(null, koin.getOrNull<LibraryRepository>())
        } finally {
            stopKoin()
            database.driver.close()
        }
    }
}
