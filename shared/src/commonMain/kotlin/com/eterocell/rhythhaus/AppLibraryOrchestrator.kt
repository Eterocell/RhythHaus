package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * App-owned, non-Compose admission and publication boundary for library
 * operations.
 */
internal class AppLibraryOrchestrator(
    private val coordinator: AppLibraryOperationCoordinator,
    private val publishError: suspend (String) -> Unit,
) {
    suspend fun <T> publishIfCurrent(
        token: LibraryOperationToken,
        publication: suspend () -> T,
    ): T? = coordinator.publishIfCurrent(token, publication)

    suspend fun launch(
        kind: LibraryOperationKind,
        operation: suspend (LibraryOperationToken) -> Unit,
    ) = run(coordinator.admitMutation(kind), operation)

    suspend fun launchScan(
        operation: suspend (LibraryOperationToken) -> Unit,
    ) = run(coordinator.admitScan(), operation)

    private suspend fun run(
        admission: LibraryOperationAdmission,
        operation: suspend (LibraryOperationToken) -> Unit,
    ) {
        if (admission !is LibraryOperationAdmission.Admitted) return
        val token = admission.token
        try {
            operation(token)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            publishIfCurrent(token) {
                publishError(failure.appFailureMessage())
            }
        } finally {
            withContext(NonCancellable) {
                coordinator.complete(token)
            }
        }
    }
}

internal fun PlaylistStateAction.requireSuccessfulPublication():
    PlaylistStateAction =
    when (this) {
        is PlaylistStateAction.ReadFailed -> error(message)
        else -> this
    }
