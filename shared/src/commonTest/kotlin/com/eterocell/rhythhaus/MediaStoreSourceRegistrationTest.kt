package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.androidMediaStoreAudioSource
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionState
import com.eterocell.rhythhaus.mediastore.registerMediaStoreAudioSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaStoreSourceRegistrationTest {
    @Test
    fun deniedOrUnavailableAccessNeverCreatesSourceOrChangesSaf() {
        val saf =
            LibrarySource(
                "saf",
                LibraryPlatformKind.AndroidSafTree,
                "Folder",
                "content://tree",
                10)
        for (state in
            MediaStoreAudioPermissionState.entries.filter {
                it != MediaStoreAudioPermissionState.Granted
            }) {
            val repository =
                InMemoryLibraryRepository().apply { upsertSource(saf) }
            assertNull(registerMediaStoreAudioSource(repository, state, 20))
            assertEquals(listOf(saf), repository.sources())
        }
    }

    @Test
    fun explicitAddIsIdempotentAndRetainsExistingSourceHistory() {
        val repository = InMemoryLibraryRepository()
        val created =
            registerMediaStoreAudioSource(
                repository, MediaStoreAudioPermissionState.Granted, 20)
        assertEquals(androidMediaStoreAudioSource(20), created)
        val scanned = requireNotNull(created).copy(lastScanAtEpochMillis = 30)
        repository.upsertSource(scanned)
        assertEquals(
            scanned,
            registerMediaStoreAudioSource(
                repository, MediaStoreAudioPermissionState.Granted, 40))
        assertEquals(listOf(scanned), repository.sources())
    }
}
