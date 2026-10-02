package com.eterocell.rhythhaus.mediastore

import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.androidMediaStoreAudioSource

/**
 * Registers device audio only after an explicit, coordinator-admitted add
 * action.
 */
internal fun registerMediaStoreAudioSource(
    repository: LibraryRepository,
    permission: MediaStoreAudioPermissionState,
    createdAtEpochMillis: Long,
): LibrarySource? {
    if (permission != MediaStoreAudioPermissionState.Granted) return null
    val source = androidMediaStoreAudioSource(createdAtEpochMillis)
    val existing = repository.sources().firstOrNull { it.id == source.id }
    if (existing != null) return existing
    repository.upsertSource(source)
    return source
}
