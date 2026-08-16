package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.ScanProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Single admission boundary for App-owned library operations. */
internal class AppLibraryOperationCoordinator(
    private val requestScanCancellationAndJoin: suspend () -> Unit,
) {
    private val mutex = Mutex()
    private var nextOperationId = 0L
    private var current: LibraryOperationToken? = null
    private val _state: MutableStateFlow<LibraryOperationState> =
        MutableStateFlow(LibraryOperationState.Idle)

    val state: StateFlow<LibraryOperationState> = _state.asStateFlow()

    suspend fun admitScan(): LibraryOperationAdmission = mutex.withLock {
        if (current != null) return@withLock LibraryOperationAdmission.Rejected
        val token = token(LibraryOperationKind.Scan)
        current = token
        _state.value = LibraryOperationState.Running(token)
        LibraryOperationAdmission.Admitted(token)
    }

    suspend fun admitMutation(
        kind: LibraryOperationKind
    ): LibraryOperationAdmission {
        val scan = mutex.withLock {
            when (val active = current) {
                null -> null
                else ->
                    if (active.kind == LibraryOperationKind.Scan) {
                        _state.value = LibraryOperationState.Cancelling(active)
                        active
                    } else {
                        return@withLock LibraryOperationAdmission.Rejected
                    }
            }
        }

        scan?.let { token ->
            try {
                requestScanCancellationAndJoin()
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (current == token) {
                            current = null
                            _state.value = LibraryOperationState.Idle
                        }
                    }
                }
                throw failure
            }
            mutex.withLock {
                if (current == token) {
                    current = null
                    _state.value = LibraryOperationState.Idle
                }
            }
        }

        return mutex.withLock {
            if (current != null)
                return@withLock LibraryOperationAdmission.Rejected
            val token = token(kind)
            current = token
            _state.value = LibraryOperationState.Running(token)
            LibraryOperationAdmission.Admitted(token)
        }
    }

    suspend fun complete(token: LibraryOperationToken) {
        mutex.withLock {
            if (current == token) {
                current = null
                _state.value = LibraryOperationState.Idle
            }
        }
    }

    suspend fun <T> publishIfCurrent(
        token: LibraryOperationToken,
        publication: suspend () -> T,
    ): T? = mutex.withLock {
        if (current == token) publication() else null
    }

    fun isCurrent(token: LibraryOperationToken): Boolean = current == token

    private fun token(kind: LibraryOperationKind) =
        LibraryOperationToken(++nextOperationId, kind)
}

/** Serializes scanner callbacks so a terminal publication is observed last. */
internal class OrderedScanProgressCallbacks(
    private val scope: CoroutineScope,
    private val publish: suspend (ScanProgress) -> Unit,
) {
    private var tail: Job? = null

    fun offer(progress: ScanProgress) {
        val previous = tail
        tail = scope.launch {
            previous?.join()
            publish(progress)
        }
    }

    suspend fun awaitPublished() {
        tail?.join()
    }
}

internal enum class LibraryOperationKind {
    Scan,
    RemoveMissingTracks,
    RemoveSource,
    Clear,
}

internal data class LibraryOperationToken(
    val id: Long,
    val kind: LibraryOperationKind,
)

internal sealed interface LibraryOperationAdmission {
    data class Admitted(val token: LibraryOperationToken) :
        LibraryOperationAdmission

    data object Rejected : LibraryOperationAdmission
}

internal sealed interface LibraryOperationState {
    data object Idle : LibraryOperationState

    data class Running(val token: LibraryOperationToken) : LibraryOperationState

    data class Cancelling(val token: LibraryOperationToken) :
        LibraryOperationState
}
