package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformScanEvent
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.settings.SourceAccessLabel
import com.eterocell.rhythhaus.settings.SourceDialogName
import com.eterocell.rhythhaus.settings.SourceScanLabel
import com.eterocell.rhythhaus.settings.sourceDialogName
import com.eterocell.rhythhaus.settings.sourceManagementLabels
import com.eterocell.rhythhaus.settings.sourceMutationsEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibrarySourceManagementTest {
    @Test
    fun pickerActionIsVisibleForFirstSourceRegardlessOfAdditionalSourceCapability() {
        assertTrue(sourcePickerActionVisible(supportsAdditionalSources = true, sourceCount = 0))
        assertTrue(sourcePickerActionVisible(supportsAdditionalSources = false, sourceCount = 0))
    }

    @Test
    fun pickerActionRemainsVisibleForExistingSourcesWhenAdditionalSourcesAreSupported() {
        assertTrue(sourcePickerActionVisible(supportsAdditionalSources = true, sourceCount = 1))
    }

    @Test
    fun pickerActionIsHiddenForExistingSourceWhenAdditionalSourcesAreUnsupported() {
        assertFalse(sourcePickerActionVisible(supportsAdditionalSources = false, sourceCount = 1))
    }

    @Test
    fun sourceManagementLabelsMapAccessAndLastScanState() {
        assertEquals(
            SourceAccessLabel.Available to SourceScanLabel.NeverScanned,
            sourceManagementLabels(source("available")),
        )
        assertEquals(
            SourceAccessLabel.LostAccess to SourceScanLabel.LastScanned,
            sourceManagementLabels(
                source("lost").copy(
                    accessStatus = LibrarySourceAccessStatus.LostAccess,
                    lastScanAtEpochMillis = 2L,
                ),
            ),
        )
    }

    @Test
    fun sourceMutationsAreDisabledOnlyWhileScanIsActive() {
        assertTrue(sourceMutationsEnabled(scanProgress = null))
        assertFalse(sourceMutationsEnabled(scanProgress(ScanStatus.Scanning)))
        assertFalse(sourceMutationsEnabled(scanProgress(ScanStatus.Cancelling)))
        assertTrue(sourceMutationsEnabled(scanProgress(ScanStatus.Completed)))
    }

    @Test
    fun removalDialogBoundsVisualNameButPreservesFullAccessibilityName() {
        val fullName = "A".repeat(80)

        assertEquals(
            SourceDialogName(
                visual = "${"A".repeat(63)}…",
                accessibility = fullName,
            ),
            sourceDialogName(source("source").copy(displayName = fullName)),
        )
    }

    @Test
    fun libraryStateRefreshDecoratesPersistedSourcesWithCurrentPlatformAccess() {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("available"))
            upsertSource(source("lost"))
        }
        val platformAccess = FakePlatformSourceAccess(lostSourceId = "lost")

        val state = loadLibraryContent(repository, platformAccess)

        assertEquals(
            listOf(LibrarySourceAccessStatus.Available, LibrarySourceAccessStatus.LostAccess),
            state.sources.map { it.accessStatus },
        )
    }

    @Test
    fun sourceRemovalRefreshesBothSourcesAndTracks() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("remove"))
            upsertSource(source("keep"))
            upsertTrack(track("remove-track", "remove"))
            upsertTrack(track("keep-track", "keep"))
        }
        var refreshedState: LibraryContentState? = null

        removeSourceInBackground(
            sourceId = "remove",
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { refreshedState = it },
        )

        assertEquals(listOf("keep"), refreshedState?.sources?.map { it.id })
        assertEquals(listOf("keep-track"), refreshedState?.tracks?.map { it.id })
    }

    @Test
    fun clearLibraryRefreshesBothSourcesAndTracks() = runBlocking {
        val repository = InMemoryLibraryRepository().apply {
            upsertSource(source("source"))
            upsertTrack(track("track", "source"))
        }
        var refreshedState: LibraryContentState? = null

        clearLibraryInBackground(
            repository = repository,
            platformAccess = FakePlatformSourceAccess(),
            ioDispatcher = Dispatchers.Default,
            updateLibrary = { refreshedState = it },
        )

        assertEquals(emptyList(), refreshedState?.sources)
        assertEquals(emptyList(), refreshedState?.tracks)
    }

    private fun source(id: String) = LibrarySource(
        id = id,
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = id,
        handle = "/$id",
        createdAtEpochMillis = 1L,
    )

    private fun track(id: String, sourceId: String) = LibraryTrack(
        id = id,
        sourceId = sourceId,
        sourceLocalKey = "$id.mp3",
        audioSource = AudioSource.FilePath("/$id.mp3"),
        displayName = "$id.mp3",
        title = id,
        artist = "Artist",
        album = "Album",
        durationMillis = null,
        sizeBytes = null,
        modifiedAtEpochMillis = null,
        lastSeenScanId = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun scanProgress(status: ScanStatus) = ScanProgress(
        session = ScanSession(
            id = "scan",
            sourceId = "source",
            status = status,
            startedAtEpochMillis = 1L,
        ),
    )
}

private class FakePlatformSourceAccess(
    private val lostSourceId: String? = null,
) : PlatformSourceAccess {
    override fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus =
        if (source.id == lostSourceId) LibrarySourceAccessStatus.LostAccess else LibrarySourceAccessStatus.Available

    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> = emptySequence()
}
