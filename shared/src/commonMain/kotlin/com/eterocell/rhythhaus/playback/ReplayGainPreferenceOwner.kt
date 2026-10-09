package com.eterocell.rhythhaus.playback

import com.eterocell.rhythhaus.ReplayGainMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Authoritative Settings projection, independent of an in-flight requested
 * mode.
 */
public data class ReplayGainPreferenceState(
    /** Last restored or successfully saved mode. */
    public val selectedMode: ReplayGainMode = ReplayGainMode.Off,
    /**
     * Excludes Settings input during startup restoration and an admitted write.
     */
    public val isBusy: Boolean = true,
    /** A recoverable save error; Settings owns its localized presentation. */
    public val saveFailed: Boolean = false,
)

/**
 * Shared's single owner of persisted normalization and controller publication.
 *
 * Call [restore] at startup, observe [state] for Settings, and route selections
 * through [select]. [applyMode] is the controller's non-suspending mode
 * callback. The write and publication form one admitted transaction:
 * cancellation while waiting for admission does nothing; after admission it
 * waits for persistence and publication to settle before propagating. Never
 * also collect the store into the controller, which would bypass this
 * accepted-state boundary.
 */
public class ReplayGainPreferenceOwner(
    private val store: ReplayGainPreferenceStore,
    private val applyMode: (ReplayGainMode) -> Unit,
) {
    private val gate = Mutex()
    private val mutableState = MutableStateFlow(ReplayGainPreferenceState())
    private var restored = false

    /** Accepted mode and recoverable UI status, never an optimistic request. */
    public val state: StateFlow<ReplayGainPreferenceState> =
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
    public suspend fun select(mode: ReplayGainMode) {
        gate.withLock {
            withContext(NonCancellable) {
                try {
                    restoreLocked()
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = true, saveFailed = false)
                    try {
                        store.setSelectedMode(mode)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        mutableState.value =
                            mutableState.value.copy(saveFailed = true)
                        return@withContext
                    }
                    publishLocked(mode)
                } finally {
                    mutableState.value = mutableState.value.copy(isBusy = false)
                }
            }
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun restoreLocked() {
        if (restored) return
        val mode =
            try {
                store.selectedMode.first()
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                ReplayGainMode.Off
            }
        publishLocked(mode)
        restored = true
    }

    private fun publishLocked(mode: ReplayGainMode) {
        applyMode(mode)
        mutableState.value =
            mutableState.value.copy(selectedMode = mode, saveFailed = false)
    }
}
