package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformAudioScanner
import com.eterocell.rhythhaus.library.PlatformScanEvent
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class RhythHausDiTest {
    @Test
    fun koinResolvesLibraryScannerFromTestSafeDependencies() {
        stopKoin()
        val koinApplication = startKoin {
            modules(
                module {
                    single<TagLibReader> { FakeTagLibReader }
                    single { AudioMetadataReader(tagLibReader = get()) }
                    single<LibraryRepository> { InMemoryLibraryRepository() }
                    single<PlatformSourceAccess> { FakePlatformSourceAccess }
                    single<PlatformAudioScanner> { get<PlatformSourceAccess>() as PlatformAudioScanner }
                    single {
                        LibraryScanner(
                            repository = get(),
                            platformScanner = get(),
                            metadataReader = get(),
                            now = { 100L },
                            idFactory = { prefix -> "$prefix-id" },
                        )
                    }
                },
            )
        }

        try {
            val scanner = koinApplication.koin.get<LibraryScanner>()
            val source = LibrarySource(
                id = "source-1",
                platformKind = LibraryPlatformKind.JvmFolder,
                displayName = "Music",
                handle = "/Music",
                createdAtEpochMillis = 1L,
            )

            val result = scanner.scan(source)

            assertNotNull(scanner)
            assertEquals(ScanStatus.Completed, result.status)
            assertEquals(0, result.filesVisited)
        } finally {
            stopKoin()
        }
    }
}

private object FakePlatformSourceAccess : PlatformSourceAccess, PlatformAudioScanner {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = emptySequence()
}

private object FakeTagLibReader : TagLibReader {
    override fun readPath(path: String): TagReadResult = TagReadResult.Unsupported("not used")
    override fun readProperties(path: String): Map<String, String> = emptyMap()
}
