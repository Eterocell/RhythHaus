package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class LibraryOperationCoordinatorTest {
    @Test
    fun cancellationJoinFailureReturnsCoordinatorToIdleAndRethrowsOriginalFailure() =
        runBlocking {
            val failure = IllegalStateException("join failed")
            val coordinator = AppLibraryOperationCoordinator { throw failure }
            coordinator.admitScan()

            val thrown =
                assertFailsWith<IllegalStateException> {
                    coordinator.admitMutation(LibraryOperationKind.Clear)
                }

            assertSame(failure, thrown)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
            assertCanAdmitMutationAfterRecovery(coordinator)
        }

    @Test
    fun cancelledMutationWaitingForCancellationJoinReturnsCoordinatorToIdle() =
        runBlocking {
            val joinStarted = CompletableDeferred<Unit>()
            val continueJoin = CompletableDeferred<Unit>()
            var callbackCancellation: CancellationException? = null
            val coordinator = AppLibraryOperationCoordinator {
                joinStarted.complete(Unit)
                try {
                    continueJoin.await()
                } catch (failure: CancellationException) {
                    callbackCancellation = failure
                    throw failure
                }
            }
            coordinator.admitScan()
            val cancellation = CancellationException("mutation cancelled")
            var caught: CancellationException? = null

            coroutineScope {
                val mutation = async {
                    try {
                        coordinator.admitMutation(LibraryOperationKind.Clear)
                    } catch (failure: CancellationException) {
                        caught = failure
                        throw failure
                    }
                }
                joinStarted.await()
                mutation.cancel(cancellation)

                assertFailsWith<CancellationException> { mutation.await() }
            }

            assertSame(callbackCancellation, caught)
            assertTrue(callbackCancellation != null)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
            assertCanAdmitMutationAfterRecovery(coordinator)
        }

    private suspend fun assertCanAdmitMutationAfterRecovery(
        coordinator: AppLibraryOperationCoordinator,
    ) {
        val admission = coordinator.admitMutation(LibraryOperationKind.Clear)

        assertTrue(admission is LibraryOperationAdmission.Admitted)
        coordinator.complete(admission.token)
        assertEquals(LibraryOperationState.Idle, coordinator.state.value)
    }
}
