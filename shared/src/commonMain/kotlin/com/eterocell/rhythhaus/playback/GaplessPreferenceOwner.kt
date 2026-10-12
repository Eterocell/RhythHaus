package com.eterocell.rhythhaus.playback

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

public data class GaplessPreferenceState(
    public val selectedMode: GaplessPlaybackMode = GaplessPlaybackMode.Off,
    public val isBusy: Boolean = true,
    public val saveFailed: Boolean = false,
)

public class GaplessPreferenceOwner(
    private val store: GaplessPreferenceStore,
    private val applyMode: (GaplessPlaybackMode) -> Unit,
) {
    private val gate = Mutex()
    private val mutableState = MutableStateFlow(GaplessPreferenceState())
    private var restored = false
    public val state: StateFlow<GaplessPreferenceState> =
        mutableState.asStateFlow()

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

    public suspend fun select(mode: GaplessPlaybackMode) {
        gate.withLock {
            withContext(NonCancellable) {
                try {
                    restoreLocked()
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = true, saveFailed = false)
                    try {
                        store.setMode(mode)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        mutableState.value =
                            mutableState.value.copy(saveFailed = true)
                        return@withContext
                    }
                    publish(mode)
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
                store.readMode()
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                GaplessPlaybackMode.Off
            }
        publish(mode)
        restored = true
    }

    private fun publish(mode: GaplessPlaybackMode) {
        applyMode(mode)
        mutableState.value =
            mutableState.value.copy(selectedMode = mode, saveFailed = false)
    }
}
