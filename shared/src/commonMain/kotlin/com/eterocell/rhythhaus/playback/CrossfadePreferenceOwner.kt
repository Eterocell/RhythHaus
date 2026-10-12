package com.eterocell.rhythhaus.playback

import com.eterocell.rhythhaus.CrossfadeDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Authoritative Settings projection, never an in-flight requested duration. */
public data class CrossfadePreferenceState(
    /** Last restored or successfully saved duration. */
    public val selectedDuration: CrossfadeDuration = CrossfadeDuration.Off,
    /** Excludes input during startup restoration and an admitted write. */
    public val isBusy: Boolean = true,
    /** Recoverable save error; Settings owns its localized presentation. */
    public val saveFailed: Boolean = false,
)

/**
 * Shared's process-lifetime owner of persistence and controller publication.
 *
 * Restore before admitting playback commands, observe [state] in Settings, and
 * route every duration request through [select]. Cancellation before admission
 * has no side effects; after admission, durable write and accepted publication
 * settle together before cancellation propagates. Never collect the store
 * directly into the controller, bypassing this accepted-state gate.
 */
public class CrossfadePreferenceOwner(
    private val store: CrossfadePreferenceStore,
    private val applyDuration: (CrossfadeDuration) -> Unit,
) {
    private val gate = Mutex()
    private val mutableState = MutableStateFlow(CrossfadePreferenceState())
    private var restored = false

    /**
     * Accepted duration and recoverable UI status, never an optimistic request.
     */
    public val state: StateFlow<CrossfadePreferenceState> =
        mutableState.asStateFlow()

    /**
     * Restores once; a late startup call cannot overwrite a newer selection.
     */
    public suspend fun restore() {
        gate.withLock {
            withContext(NonCancellable) {
                try {
                    restoreLocked()
                } finally {
                    mutableState.value = mutableState.value.copy(isBusy = false)
                }
            }
        }
        currentCoroutineContext().ensureActive()
    }

    /**
     * Saves serially, accepting and applying only a successful durable write.
     */
    public suspend fun select(duration: CrossfadeDuration) {
        gate.withLock {
            withContext(NonCancellable) {
                try {
                    restoreLocked()
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = true, saveFailed = false)
                    try {
                        store.setDuration(duration)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        mutableState.value =
                            mutableState.value.copy(saveFailed = true)
                        return@withContext
                    }
                    publishLocked(duration)
                } finally {
                    mutableState.value = mutableState.value.copy(isBusy = false)
                }
            }
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun restoreLocked() {
        if (restored) return
        val duration =
            try {
                store.readDuration()
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                CrossfadeDuration.Off
            }
        publishLocked(duration)
        restored = true
    }

    private fun publishLocked(duration: CrossfadeDuration) {
        applyDuration(duration)
        mutableState.value =
            mutableState.value.copy(
                selectedDuration = duration, saveFailed = false)
    }
}
