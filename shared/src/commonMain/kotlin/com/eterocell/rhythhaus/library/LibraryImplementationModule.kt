package com.eterocell.rhythhaus.library

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.createTagLibReader
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun libraryImplementationModule(): Module = module {
    single<TagLibReader> { createTagLibReader() }
    single { AudioMetadataReader(tagLibReader = get()) }
    single<LibraryDatabase> { createLibraryDatabase() }
    single<LibraryRepository> { SqlDelightLibraryRepository(get()) }
    single<PlatformSourceAccess> { createPlatformSourceAccess() }
    single {
        val platformAccess = get<PlatformSourceAccess>()
        LibraryScanner(get(), platformAccess, get(), { currentTimeMillis() }) {
            uuid4()
        }
    }
}
