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
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.androidSafSourceId
import com.eterocell.rhythhaus.library.emptyLibrarySourceMutationsAllowed
import com.eterocell.rhythhaus.library.jvmFolderSourceId
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.settings.SourceAccessLabel
import com.eterocell.rhythhaus.settings.SourceDialogName
import com.eterocell.rhythhaus.settings.SourceScanLabel
import com.eterocell.rhythhaus.settings.sourceDialogName
import com.eterocell.rhythhaus.settings.sourceManagementLabels
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
    fun sourceMutationsAreAllowedOnlyWhenProgressAndJobAreInactive() {
        assertTrue(sourceMutationsAllowed(isProgressActive = false, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = true, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = false, isJobActive = true))
        assertFalse(sourceMutationsAllowed(isProgressActive = true, isJobActive = true))
    }

    @Test
    fun emptyLibraryMutationGateBlocksJobOnlyScanStartupWindow() {
        assertFalse(emptyLibrarySourceMutationsAllowed(isProgressActive = false, isJobActive = true))
    }

    @Test
    fun pickedSourceReusesPersistedIdentityAndCreationTimeWhenHandleMatches() {
        val persisted = source("legacy-id").copy(
            handle = "content://provider/tree/Aa",
            createdAtEpochMillis = 42L,
        )
        val picked = persisted.copy(
            id = "android-saf-new-id",
            displayName = "Renamed folder",
            createdAtEpochMillis = 99L,
        )

        assertEquals(
            picked.copy(id = "legacy-id", createdAtEpochMillis = 42L),
            normalizePickedSource(picked, listOf(persisted)),
        )
    }

    @Test
    fun pickedSourceKeepsNewIdentityWhenHandleDoesNotMatch() {
        val picked = source("new-id").copy(handle = "content://provider/tree/new", createdAtEpochMillis = 99L)

        assertEquals(picked, normalizePickedSource(picked, listOf(source("existing-id"))))
    }

    @Test
    fun androidSafIdentityDistinguishesKnownJavaHashCollisionUris() {
        val first = "content://provider/tree/Aa"
        val second = "content://provider/tree/BB"
        assertEquals(first.hashCode(), second.hashCode())

        assertTrue(androidSafSourceId(first) != androidSafSourceId(second))
    }

    @Test
    fun jvmFolderIdentityDistinguishesCanonicalPathsWithLegacyHashCollision() {
        val first = "/music/Aa"
        val second = "/music/BB"
        assertEquals(first.hashCode(), second.hashCode())

        assertTrue(jvmFolderSourceId(first) != jvmFolderSourceId(second))
    }

    @Test
    fun repickedJvmFolderKeepsPersistedLegacyIdentityForExactHandle() {
        val canonicalPath = "/music/Aa"
        val persisted = source("jvm-folder-legacy-hash").copy(
            handle = canonicalPath,
            createdAtEpochMillis = 42L,
        )
        val picked = persisted.copy(
            id = jvmFolderSourceId(canonicalPath),
            createdAtEpochMillis = 99L,
        )

        assertEquals(
            picked.copy(id = persisted.id, createdAtEpochMillis = persisted.createdAtEpochMillis),
            normalizePickedSource(picked, listOf(persisted)),
        )
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
    fun sourceMutationsFollowTerminalProgressWhenNoJobIsActive() {
        assertTrue(sourceMutationsAllowed(isProgressActive = false, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = scanProgress(ScanStatus.Scanning).isActive, isJobActive = false))
        assertFalse(sourceMutationsAllowed(isProgressActive = scanProgress(ScanStatus.Cancelling).isActive, isJobActive = false))
        assertTrue(sourceMutationsAllowed(isProgressActive = scanProgress(ScanStatus.Completed).isActive, isJobActive = false))
    }

    @Test
    fun removalDialogBoundsVisualNameButPreservesFullAccessibilityName() {
        val fullName = "A".repeat(80)

        assertEquals(
            SourceDialogName(
                visual = "${"A".repeat(63)}…",
                accessibility = fullName,
            ),
            sourceDialogName(source("source").copy(displayName = fullName), unnamedLabel = "Unnamed folder"),
        )
    }

    @Test
    fun blankSourceDisplayNameUsesNeutralLabelWithoutExposingHandle() {
        val unnamed = source("source").copy(displayName = "", handle = "content://private/provider/tree/secret")

        assertEquals(
            SourceDialogName(visual = "Unnamed folder", accessibility = "Unnamed folder"),
            sourceDialogName(unnamed, unnamedLabel = "Unnamed folder"),
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
