package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuardResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class PlaylistBackupRevisionGuardAdapterTest {
    @Test
    fun currentRevisionDelegatesControllerConfirmationBlockThroughOwner() =
        runBlocking {
            val owner = AuthoritativeLibraryPublicationOwner()
            val guard = authoritativePlaylistBackupRevisionGuard(owner)
            var calls = 0

            val result =
                guard.withCurrentRevision(0L) {
                    calls++
                    "confirmed"
                }

            assertEquals(1, calls)
            assertEquals(
                PlaylistBackupRevisionGuardResult.Current("confirmed"),
                result,
            )
        }

    @Test
    fun staleRevisionDoesNotInvokeControllerConfirmationBlock() = runBlocking {
        val owner = AuthoritativeLibraryPublicationOwner()
        owner.publish(LibraryContentState(emptyList(), emptyList()))
        val guard = authoritativePlaylistBackupRevisionGuard(owner)
        var calls = 0

        val result = guard.withCurrentRevision(0L) { calls++ }

        assertEquals(0, calls)
        assertEquals(PlaylistBackupRevisionGuardResult.Stale, result)
    }

    @Test
    fun cancellationFromControllerConfirmationBlockIsRethrownExactly() =
        runBlocking {
            val cancellation = CancellationException("cancel")
            val guard =
                authoritativePlaylistBackupRevisionGuard(
                    AuthoritativeLibraryPublicationOwner())

            val thrown =
                assertFailsWith<CancellationException> {
                    guard.withCurrentRevision(0L) { throw cancellation }
                }

            assertSame(cancellation, thrown)
        }
}
