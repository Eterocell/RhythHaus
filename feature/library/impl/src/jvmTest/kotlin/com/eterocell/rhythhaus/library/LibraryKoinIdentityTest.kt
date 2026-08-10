package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.library.impl.JvmFolderSourceAccess
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class LibraryKoinIdentityTest {
    @Test
    fun productionModuleResolvesLibrarySingletonsToStableIdentities() {
        val koin = startKoin { modules(libraryImplementationModule()) }.koin
        try {
            val repository = koin.get<LibraryRepository>()
            assertIs<SqlDelightLibraryRepository>(repository)
            assertSame(repository, koin.get<LibraryRepository>())

            val scanner = koin.get<LibraryScanner>()
            assertIs<LibraryScanner>(scanner)
            assertSame(scanner, koin.get<LibraryScanner>())

            val sourceAccess = koin.get<PlatformSourceAccess>()
            assertIs<JvmFolderSourceAccess>(sourceAccess)
            assertSame(sourceAccess, koin.get<PlatformSourceAccess>())

            val database = koin.get<LibraryDatabase>()
            assertSame(database, koin.get<LibraryDatabase>())
        } finally {
            stopKoin()
        }
    }
}
