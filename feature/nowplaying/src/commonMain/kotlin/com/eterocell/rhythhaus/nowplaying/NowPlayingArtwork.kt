package com.eterocell.rhythhaus.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

/**
 * Resolves and remembers artwork bytes for one track, sharing the eager/lazy/
 * stale-guard state machine between the mini-player and the expanded player.
 *
 * Eager bytes are returned immediately and the loader is never invoked; lazy
 * tracks load through [loader] off the caller, and a stale load result is
 * dropped when the track or artwork identity changes while it is in flight.
 */
@Composable
internal fun rememberNowPlayingArtwork(
    trackId: String,
    eagerArtworkBytes: ByteArray?,
    artworkLoader: suspend (String) -> ByteArray?,
): ByteArray? {
    val inputKey = NowPlayingArtworkInputKey(eagerArtworkBytes)
    val loaderKey = NowPlayingArtworkLoaderKey(artworkLoader)
    val state =
        remember(trackId, inputKey, loaderKey) {
            NowPlayingArtworkState(eagerArtworkBytes)
        }
    LaunchedEffect(trackId, inputKey, loaderKey) {
        state.load(trackId, artworkLoader)
    }
    return state.artwork
}

internal class NowPlayingArtworkState(initialArtwork: ByteArray?) {
    private val hasEagerArtwork = initialArtwork != null
    var artwork by mutableStateOf(initialArtwork)
        private set

    private var generation = 0L

    suspend fun load(
        trackId: String,
        loader: suspend (String) -> ByteArray?,
    ) {
        val currentGeneration = ++generation
        if (hasEagerArtwork) return
        val loaded =
            try {
                loader(trackId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        if (currentGeneration == generation) artwork = loaded
    }
}

/**
 * Content-based identity for eager bytes: a new byte array with identical
 * content must not restart the remember slot, while a different array must.
 */
internal class NowPlayingArtworkInputKey(bytes: ByteArray?) {
    private val identity = bytes
    private val size = bytes?.size ?: 0
    private val contentHash = bytes?.contentHashCode() ?: 0

    override fun equals(other: Any?): Boolean =
        other is NowPlayingArtworkInputKey &&
            identity === other.identity &&
            size == other.size &&
            contentHash == other.contentHash

    override fun hashCode(): Int = 31 * size + contentHash
}

/** Identity-based loader key so a recreated loader restarts the load. */
internal class NowPlayingArtworkLoaderKey(
    private val loader: suspend (String) -> ByteArray?,
) {
    override fun equals(other: Any?): Boolean =
        other is NowPlayingArtworkLoaderKey && loader === other.loader

    override fun hashCode(): Int = loader.hashCode()
}
