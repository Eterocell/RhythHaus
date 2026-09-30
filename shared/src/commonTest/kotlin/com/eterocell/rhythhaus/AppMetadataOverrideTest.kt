package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AppMetadataOverrideTest {
    @Test
    fun acceptedCorrectionWinsOverPreviouslyCapturedScanPublication() =
        runBlocking {
            val repository = InMemoryLibraryRepository()
            repository.upsertSource(
                LibrarySource(
                    "source",
                    LibraryPlatformKind.JvmFolder,
                    "Music",
                    "/music",
                    1L))
            repository.upsertTrack(
                LibraryTrack(
                    id = "track",
                    sourceId = "source",
                    sourceLocalKey = "track.mp3",
                    audioSource = AudioSource.FilePath("/music/track.mp3"),
                    displayName = "track.mp3",
                    title = "Raw",
                    artist = "Artist",
                    album = "Album",
                    durationMillis = null,
                    sizeBytes = null,
                    modifiedAtEpochMillis = null,
                    lastSeenScanId = null,
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 1L,
                ),
            )
            val access =
                object : PlatformSourceAccess {
                    override fun scan(
                        source: LibrarySource
                    ): Sequence<PlatformScanEvent> = emptySequence()
                }
            val owner = AuthoritativeLibraryPublicationOwner()
            val ui = AppLibraryContentState()
            val oldScanSnapshot = loadLibraryContent(repository, access)
            val saved =
                setTrackMetadataOverrideAndPublish(
                    owner,
                    repository,
                    access,
                    "track",
                    TrackMetadataOverride(title = "Edited"),
                    Dispatchers.Default,
                    { ui.apply(it) },
                )
            assertTrue(saved)
            // An earlier scan's captured rows are not allowed to roll the
            // projection back.
            val freshScanPublication =
                owner.publish(loadLibraryContent(repository, access))
            assertTrue(ui.apply(freshScanPublication))
            assertEquals("Edited", ui.content.tracks.single().title)
            assertEquals("Raw", oldScanSnapshot.tracks.single().title)
        }

    @Test
    fun missingTrackDoesNotPublishOrCreateOverride() = runBlocking {
        val repository = InMemoryLibraryRepository()
        val owner = AuthoritativeLibraryPublicationOwner()
        val access =
            object : PlatformSourceAccess {
                override fun scan(
                    source: LibrarySource
                ): Sequence<PlatformScanEvent> = emptySequence()
            }
        var publications = 0
        val saved =
            setTrackMetadataOverrideAndPublish(
                owner,
                repository,
                access,
                "missing",
                TrackMetadataOverride(title = "Ghost"),
                Dispatchers.Default,
                { publications++ },
            )
        assertFalse(saved)
        assertEquals(0, publications)
        assertEquals(0L, owner.revision)
    }
}
