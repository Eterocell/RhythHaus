package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackRowColorPolicyJvmTest {
    @Test
    fun selectedAndPlayingMetadataUsesHighContrastInk() {
        listOf(LightHausPalette, DarkHausPalette).forEach { palette ->
            assertEquals(
                palette.ink,
                trackRowMetadataColor(
                    palette = palette,
                    isNowPlaying = false,
                    isSelected = true,
                ),
            )
            assertEquals(
                palette.ink,
                trackRowMetadataColor(
                    palette = palette,
                    isNowPlaying = true,
                    isSelected = false,
                ),
            )
            assertEquals(
                palette.muted,
                trackRowMetadataColor(
                    palette = palette,
                    isNowPlaying = false,
                    isSelected = false,
                ),
            )
        }
    }
}
