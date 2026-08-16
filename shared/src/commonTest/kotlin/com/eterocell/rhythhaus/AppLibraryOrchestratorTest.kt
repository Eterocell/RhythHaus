package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class AppLibraryOrchestratorTest {
    @Test
    fun cancelledAdmittedScanCompletesAndAllowsSubsequentOperation() =
        runBlocking {
            assertCancelledOperationCompletesAndAllowsSubsequentOperation(
                start = { orchestrator, operation ->
                    orchestrator.launchScan(operation)
                },
            )
        }

    @Test
    fun cancelledAdmittedMutationCompletesAndAllowsSubsequentOperation() =
        runBlocking {
            assertCancelledOperationCompletesAndAllowsSubsequentOperation(
                start = { orchestrator, operation ->
                    orchestrator.launch(LibraryOperationKind.Clear, operation)
                },
            )
        }

    private suspend fun assertCancelledOperationCompletesAndAllowsSubsequentOperation(
        start:
            suspend (
                AppLibraryOrchestrator,
                suspend (LibraryOperationToken) -> Unit,
            ) -> Unit,
    ) {
        val coordinator = AppLibraryOperationCoordinator {}
        val errors = mutableListOf<String>()
        val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)
        val started = CompletableDeferred<Unit>()
        val observedCancellation = CompletableDeferred<CancellationException>()
        val rethrownCancellation = CompletableDeferred<CancellationException>()
        val cancellation = CancellationException("cancel running operation")

        coroutineScope {
            val job = async {
                try {
                    start(orchestrator) {
                        started.complete(Unit)
                        try {
                            awaitCancellation()
                        } catch (failure: CancellationException) {
                            observedCancellation.complete(failure)
                            throw failure
                        }
                    }
                } catch (failure: CancellationException) {
                    rethrownCancellation.complete(failure)
                    throw failure
                }
            }

            withTimeout(1_000) { started.await() }
            job.cancel(cancellation)
            job.join()
        }

        assertSame(observedCancellation.await(), rethrownCancellation.await())
        assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        assertEquals(emptyList(), errors)

        var subsequentOperationRan = false
        orchestrator.launch(LibraryOperationKind.Clear) {
            subsequentOperationRan = true
        }

        assertTrue(subsequentOperationRan)
        assertEquals(LibraryOperationState.Idle, coordinator.state.value)
    }
}
