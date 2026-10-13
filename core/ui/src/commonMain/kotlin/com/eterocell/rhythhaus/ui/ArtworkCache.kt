package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.CancellationException

/** Result of clearing the application-owned artwork caches. */
public sealed interface ArtworkCacheClearResult {
    /** Both configured caches were cleared. */
    public data object Success : ArtworkCacheClearResult

    /** Cache infrastructure reported a recoverable failure. */
    public data object Failure : ArtworkCacheClearResult
}

/**
 * Returns the active Coil cache action without exposing Coil to feature
 * modules.
 */
@Composable
public fun rememberArtworkCacheClearAction():
    suspend () -> ArtworkCacheClearResult {
    val context = LocalPlatformContext.current
    return remember(context) {
        suspend {
            try {
                val loader = SingletonImageLoader.get(context)
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
                ArtworkCacheClearResult.Success
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                ArtworkCacheClearResult.Failure
            }
        }
    }
}
