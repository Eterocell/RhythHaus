package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LightHausPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class HausDialogTest {
    @Test
    fun dialogPresentationUsesExactThemeAwareExteriorPolicy() {
        val dark = hausDialogPresentation(DarkHausPalette)
        val light = hausDialogPresentation(LightHausPalette)

        assertEquals(DarkHausPalette.panel, dark.panelColor)
        assertEquals(DarkHausPalette.paper.copy(alpha = 0.72f), dark.scrimColor)
        assertEquals(LightHausPalette.panel, light.panelColor)
        assertEquals(LightHausPalette.ink.copy(alpha = 0.36f), light.scrimColor)
        assertEquals(24.dp, dark.outerPadding)
        assertEquals(24.dp, dark.cornerRadius)
    }

    @Test
    fun lightDialogPresentationUsesSolidPanelAndRestrainedInkScrim() {
        val presentation = hausDialogPresentation(LightHausPalette)

        assertEquals(LightHausPalette.panel, presentation.panelColor)
        assertEquals(LightHausPalette.ink.copy(alpha = 0.36f), presentation.scrimColor)
    }

    @Test
    fun dialogPresentationBoundsScrollableContentForCompactWindows() {
        val presentation = hausDialogPresentation(LightHausPalette)

        assertEquals(480.dp, presentation.maxPanelHeight)
    }
}

@Composable
private fun hausDialogSeparateSlotsContract() {
    HausDialog(
        title = "Dialog",
        onDismiss = {},
        body = {},
        actions = {},
    )
}
