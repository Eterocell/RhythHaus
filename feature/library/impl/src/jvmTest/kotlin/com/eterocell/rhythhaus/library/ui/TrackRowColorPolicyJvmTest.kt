package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.theme.HausPaletteId
import com.eterocell.rhythhaus.theme.hausPalette
import kotlin.test.Test
import kotlin.test.assertEquals

private val lightHausPalette = hausPalette(HausPaletteId.Moss, isDark = false)
private val darkHausPalette = hausPalette(HausPaletteId.Moss, isDark = true)

class TrackRowColorPolicyJvmTest {
    @Test
    fun selectedAndPlayingMetadataUsesHighContrastInk() {
        listOf(lightHausPalette, darkHausPalette).forEach { palette ->
            assertEquals(
                palette.textPrimary,
                trackRowMetadataColor(
                    palette = palette,
                    isNowPlaying = false,
                    isSelected = true,
                ),
            )
            assertEquals(
                palette.textPrimary,
                trackRowMetadataColor(
                    palette = palette,
                    isNowPlaying = true,
                    isSelected = false,
                ),
            )
            assertEquals(
                palette.textSecondary,
                trackRowMetadataColor(
                    palette = palette,
                    isNowPlaying = false,
                    isSelected = false,
                ),
            )
        }
    }
}
