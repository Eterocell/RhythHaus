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

public data class AutomaticResumePreferenceState(
    public val selectedPolicy: AutomaticResumePolicy =
        AutomaticResumePolicy.FollowSystemRecommendation,
    public val isBusy: Boolean = true,
    public val saveFailed: Boolean = false,
)

public class AutomaticResumePreferenceOwner(
    private val store: AutomaticResumePreferenceStore,
    private val applyPolicy: (AutomaticResumePolicy) -> Unit,
) {
    private val gate = Mutex()
    private val mutableState =
        MutableStateFlow(AutomaticResumePreferenceState())
    private var restored = false

    public val state: StateFlow<AutomaticResumePreferenceState> =
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

    public suspend fun select(policy: AutomaticResumePolicy) {
        gate.withLock {
            withContext(NonCancellable) {
                try {
                    restoreLocked()
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = true, saveFailed = false)
                    try {
                        store.setPolicy(policy)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        mutableState.value =
                            mutableState.value.copy(saveFailed = true)
                        return@withContext
                    }
                    publishLocked(policy)
                } finally {
                    mutableState.value = mutableState.value.copy(isBusy = false)
                }
            }
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun restoreLocked() {
        if (restored) return
        val policy =
            try {
                store.readPolicy()
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                AutomaticResumePolicy.FollowSystemRecommendation
            }
        publishLocked(policy)
        restored = true
    }

    private fun publishLocked(policy: AutomaticResumePolicy) {
        applyPolicy(policy)
        mutableState.value =
            mutableState.value.copy(selectedPolicy = policy, saveFailed = false)
    }
}
