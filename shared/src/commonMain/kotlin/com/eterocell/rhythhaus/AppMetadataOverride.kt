package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** A metadata edit never enters the scan/destructive-operation coordinator. */
internal suspend fun setTrackMetadataOverrideAndPublish(
    owner: AuthoritativeLibraryPublicationOwner,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    trackId: String,
    override: TrackMetadataOverride,
    ioDispatcher: CoroutineDispatcher,
    publish: suspend (AuthoritativeLibraryPublication) -> Unit,
): Boolean =
    withContext(ioDispatcher + NonCancellable) {
        var saved = false
        owner.mutateAndPublish(
            mutation = {
                saved = repository.setTrackMetadataOverride(trackId, override)
                if (saved) loadLibraryContent(repository, platformAccess)
                else null
            },
            publish = publish,
        )
        saved
    }
