package com.eterocell.rhythhaus.theme

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

/** Current progress and recoverable failure state for appearance writes. */
internal data class ThemePreferenceWriteState(
    val isBusy: Boolean = false,
    val saveFailed: Boolean = false,
)

/**
 * Serializes appearance writes and retains the last failed action for retry.
 */
internal class ThemePreferenceOwner(
    private val store: ThemePreferenceStore,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(ThemePreferenceWriteState())
    private var failedAction: Action? = null

    val state: StateFlow<ThemePreferenceWriteState> = mutableState.asStateFlow()

    suspend fun selectMode(mode: RhythHausThemeMode) {
        persist(Action.Mode(mode))
    }

    suspend fun selectPalette(paletteId: HausPaletteId) {
        persist(Action.Palette(paletteId))
    }

    suspend fun selectDynamicColor(enabled: Boolean) {
        persist(Action.DynamicColor(enabled))
    }

    suspend fun retry() {
        mutex.withLock {
            val action = failedAction ?: return@withLock
            persistLocked(action)
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun persist(action: Action) {
        mutex.withLock { persistLocked(action) }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun persistLocked(action: Action) {
        val previousFailedAction = failedAction
        withContext(NonCancellable) {
            failedAction = null
            mutableState.value = ThemePreferenceWriteState(isBusy = true)
            try {
                action.persist(store)
            } catch (cancelled: CancellationException) {
                failedAction = previousFailedAction
                mutableState.value =
                    ThemePreferenceWriteState(
                        saveFailed = previousFailedAction != null,
                    )
                throw cancelled
            } catch (_: Exception) {
                failedAction = action
                mutableState.value =
                    ThemePreferenceWriteState(saveFailed = true)
                return@withContext
            }
            mutableState.value = ThemePreferenceWriteState()
        }
    }

    private sealed interface Action {
        suspend fun persist(store: ThemePreferenceStore)

        data class Mode(val value: RhythHausThemeMode) : Action {
            override suspend fun persist(store: ThemePreferenceStore) {
                store.setMode(value)
            }
        }

        data class Palette(val value: HausPaletteId) : Action {
            override suspend fun persist(store: ThemePreferenceStore) {
                store.setPaletteId(value)
            }
        }

        data class DynamicColor(val value: Boolean) : Action {
            override suspend fun persist(store: ThemePreferenceStore) {
                store.setDynamicColorEnabled(value)
            }
        }
    }
}
