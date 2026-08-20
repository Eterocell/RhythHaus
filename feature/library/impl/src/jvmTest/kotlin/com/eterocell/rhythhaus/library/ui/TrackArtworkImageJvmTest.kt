package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay

class TrackArtworkImageJvmTest {
    @Test
    fun initialStatesDistinguishEagerLazyAndAbsent() {
        val eager =
            initialTrackArtworkLoadState(
                trackId = "t-1", eagerArtworkBytes = byteArrayOf(9))
        assertTrue(eager is TrackArtworkLoadState.Available)
        assertContentEquals(byteArrayOf(9), eager.bytes)
        assertEquals(
            TrackArtworkLoadState.Loading,
            initialTrackArtworkLoadState(
                trackId = "t-1", eagerArtworkBytes = null),
        )
        assertEquals(
            TrackArtworkLoadState.Unavailable,
            initialTrackArtworkLoadState(
                trackId = null, eagerArtworkBytes = null),
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lazyStateTransitionsLoadingToLoadedWithInjectedLoader() =
        runComposeUiTest {
            val states = mutableListOf<State<TrackArtworkLoadState>>()
            setContent {
                states +=
                    rememberLazyTrackArtworkState(
                        trackId = "t-1",
                        eagerArtworkBytes = null,
                        artworkLoader = { id -> bytesFor(id) },
                    )
            }
            waitForIdle()
            val state = states.last()
            waitUntil(timeoutMillis = 5_000) {
                state.value is TrackArtworkLoadState.Available
            }
            assertContentEquals(
                bytesFor("t-1"),
                (state.value as TrackArtworkLoadState.Available).bytes,
            )
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun staleLoadIsRejectedWhenTrackIdentityChanges() = runComposeUiTest {
        val trackId = mutableStateOf<String?>("t-1")
        val states = mutableListOf<State<TrackArtworkLoadState>>()
        setContent {
            states +=
                rememberLazyTrackArtworkState(
                    trackId = trackId.value,
                    eagerArtworkBytes = null,
                    artworkLoader = { id ->
                        if (id == "t-1") {
                            delay(10_000)
                            bytesFor("t-1")
                        } else {
                            bytesFor("t-2")
                        }
                    },
                )
        }
        waitForIdle()
        assertTrue(states.last().value is TrackArtworkLoadState.Loading)

        trackId.value = "t-2"
        waitForIdle()
        val state = states.last()
        waitUntil(timeoutMillis = 5_000) {
            state.value is TrackArtworkLoadState.Available
        }
        assertContentEquals(
            bytesFor("t-2"),
            (state.value as TrackArtworkLoadState.Available).bytes,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedLazyArtworkRendersFallback() = runComposeUiTest {
        setContent {
            Box(Modifier.size(80.dp)) {
                LazyTrackArtworkImage(
                    trackId = "t-1",
                    eagerArtworkBytes = null,
                    contentDescription = "album art",
                    role = ArtworkImageRole.Thumbnail,
                    artworkLoader = { error("decode failed") },
                    fallback = {
                        Box(Modifier.size(10.dp).testTag("artwork-fallback"))
                    },
                )
            }
        }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("artwork-fallback"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun eagerArtworkRendersWithoutLoaderAndSuppressesFallback() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(80.dp)) {
                    LazyTrackArtworkImage(
                        trackId = null,
                        eagerArtworkBytes = pngFixture(),
                        contentDescription = "album art",
                        role = ArtworkImageRole.Thumbnail,
                        artworkLoader = { null },
                        fallback = {
                            Box(
                                Modifier.size(10.dp)
                                    .testTag("artwork-fallback"))
                        },
                    )
                }
            }
            waitForIdle()
            onNodeWithTag("artwork-fallback").assertDoesNotExist()
        }

    /**
     * A valid 1x1 PNG. The eager path feeds the bytes to Coil; undecodable
     * fixture bytes asynchronously fail and render the fallback, racing the
     * assertion. Decodable bytes make fallback suppression deterministic.
     */
    private fun pngFixture(): ByteArray {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0xFFFFFFFF.toInt())
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    private fun bytesFor(id: String): ByteArray? =
        when (id) {
            "t-1" -> byteArrayOf(1, 2)
            "t-2" -> byteArrayOf(3, 4)
            else -> null
        }
}
